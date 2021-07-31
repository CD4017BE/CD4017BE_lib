package cd4017be.lib.network;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import cd4017be.lib.Lib;
import cd4017be.lib.util.Utils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;


/**
 * Handles general purpose communication packets send between server and client for TileEntities, Entities and player held ItemStacks
 * @author CD4017BE
 */
public class SyncNetworkHandler extends NetworkHandler {

	public static final int Y_TILEENTITY = 0, Y_ENTITY = -1, Y_ITEM = -2; //TODO set below y = -64 in 1.17
	public static final int HEADERSIZE = 9;
	private static int MAX_PACKSIZE;

	/**the instance */
	public static SyncNetworkHandler instance;

	public static void register() {
		MAX_PACKSIZE = Lib.CFG_COMMON.packet_chain_threshold.get() + HEADERSIZE;
		if (instance == null) instance = new SyncNetworkHandler(new ResourceLocation(Lib.ID, "sy"));
	}

	private final HashMap<ServerPlayer, ByteBuf> chainedPackets;

	private SyncNetworkHandler(ResourceLocation channel) {
		super(channel);
		this.chainedPackets = new HashMap<>();
		MinecraftForge.EVENT_BUS.register(this);
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void handleServerPacket(FriendlyByteBuf pkt) throws Exception {
		Minecraft mc = Minecraft.getInstance();
		Level world = mc.level;
		AbstractClientPlayer player = mc.player;
		for (FriendlyByteBuf buf : new PacketSplitter(pkt)) {
			BlockPos target = buf.readBlockPos();
			int y = target.getY();
			if (y >= Y_TILEENTITY) {
				BlockEntity te = Utils.getTileAt(world, target);
				if (te instanceof IServerPacketReceiver)
					((IServerPacketReceiver)te).handleServerPacket(buf);
			} else if (y == Y_ENTITY) {
				Entity entity = world.getEntity(target.getX() & 0xffff | target.getZ() << 16);
				if (entity instanceof IServerPacketReceiver)
					((IServerPacketReceiver)entity).handleServerPacket(buf);
			} else if (y == Y_ITEM) {
				int slot = target.getX();
				if (slot < 0 || slot >= player.getInventory().getContainerSize()) continue;
				ItemStack stack = player.getInventory().getItem(slot);
				Item item = stack.getItem();
				if (item instanceof IServerPacketReceiver.ItemSPR)
					((IServerPacketReceiver.ItemSPR)item).handleServerPacket(stack, player, slot, buf);
			}
		}
	}

	@Override
	public void handlePlayerPacket(FriendlyByteBuf pkt, ServerPlayer sender) throws Exception {
		Level world = sender.level;
		for (FriendlyByteBuf buf : new PacketSplitter(pkt)) {//TODO chained packets don't make much sense on client -> server
			BlockPos target = buf.readBlockPos();
			int y = target.getY();
			if (y >= Y_TILEENTITY) {
				BlockEntity te = Utils.getTileAt(world, target);
				if (te instanceof IPlayerPacketReceiver)
					((IPlayerPacketReceiver)te).handlePlayerPacket(buf, sender);
			} else if (y == Y_ENTITY) {
				Entity entity = world.getEntity(target.getX() & 0xffff | target.getZ() << 16);
				if (entity instanceof IPlayerPacketReceiver)
					((IPlayerPacketReceiver)entity).handlePlayerPacket(buf, sender);
			} else if (y == Y_ITEM) {
				int slot = target.getX();
				if (slot < 0 || slot >= sender.getInventory().getContainerSize()) continue;
				ItemStack stack = sender.getInventory().getItem(slot);
				Item item = stack.getItem();
				if (item instanceof IPlayerPacketReceiver.ItemPPR)
					((IPlayerPacketReceiver.ItemPPR)item).handlePlayerPacket(stack, slot, buf, sender);
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.LOW)
	public void tick(ServerTickEvent event) {
		if (event.phase != Phase.END || chainedPackets.isEmpty()) return;
		for (Entry<ServerPlayer, ByteBuf> e : chainedPackets.entrySet())
			super.sendToPlayer(new FriendlyByteBuf(e.getValue()), e.getKey());
		chainedPackets.clear();
	}

	private void addPacket(ByteBuf pkt, ServerPlayer player) {
		pkt.setByte(0, pkt.readableBytes() - HEADERSIZE);
		chainedPackets.merge(player, pkt, ByteBuf::writeBytes);
	}

	@Override
	public void sendToPlayer(FriendlyByteBuf pkt, ServerPlayer player) {
		if (pkt.readableBytes() <= MAX_PACKSIZE) addPacket(pkt, player);
		else super.sendToPlayer(pkt, player);
	}

	@Override
	public void sendToPlayers(FriendlyByteBuf pkt, Collection<ServerPlayer> players) {
		int l = pkt.readableBytes();
		if (l > MAX_PACKSIZE) super.sendToPlayers(pkt, players);
		else {
			pkt.setByte(0, pkt.readableBytes() - HEADERSIZE);
			for (ServerPlayer player : players) {
				ByteBuf buf = chainedPackets.get(player);
				if (buf == null) chainedPackets.put(player, pkt.copy());
				else buf.writeBytes(pkt);
			}
		}
	}

	public static FriendlyByteBuf preparePacket(BlockPos pos) {
		FriendlyByteBuf pkt = new FriendlyByteBuf(Unpooled.buffer());
		pkt.writeByte(0);
		pkt.writeLong(pos.asLong());
		return pkt;
	}

	/**
	 * @param tile the BlockEntity to send to (on the other side)
	 * @return a new FriendlyByteBuf with prepared header
	 * @see IServerPacketReceiver#handleServerPacket(FriendlyByteBuf)
	 * @see IPlayerPacketReceiver#handlePlayerPacket(FriendlyByteBuf, ServerPlayer)
	 */
	public static FriendlyByteBuf preparePacket(BlockEntity tile) {
		return preparePacket(tile.getBlockPos());
	}

	/**
	 * @param entity the Entity to send to (on the other side)
	 * @return a new FriendlyByteBuf with prepared header
	 * @see IServerPacketReceiver#handleServerPacket(FriendlyByteBuf)
	 * @see IPlayerPacketReceiver#handlePlayerPacket(FriendlyByteBuf, ServerPlayer)
	 */
	public static FriendlyByteBuf preparePacket(Entity entity) {
		int id = entity.getId();
		return preparePacket(new BlockPos(id & 0xffff, Y_ENTITY, id >> 16 & 0xffff));
	}

	/**
	 * @param slot the player inventory slot to send to
	 * @return a new FriendlyByteBuf with prepared header
	 * @see IServerPacketReceiver.ItemSPR#handleServerPacket(ItemStack, PlayerEntitySP, int, FriendlyByteBuf)
	 * @see IPlayerPacketReceiver.ItemPPR#handlePlayerPacket(ItemStack, int, FriendlyByteBuf, ServerPlayer)
	 */
	public static FriendlyByteBuf preparePacket(int slot) {
		return preparePacket(new BlockPos(slot, Y_ITEM, 0));
	}

	/**
	 * @param player the player holding the item
	 * @param hand the held item to send to
	 * @return a new FriendlyByteBuf with prepared header
	 * @see IServerPacketReceiver.ItemSPR#handleServerPacket(ItemStack, PlayerEntitySP, int, FriendlyByteBuf)
	 * @see IPlayerPacketReceiver.ItemPPR#handlePlayerPacket(ItemStack, int, FriendlyByteBuf, ServerPlayer)
	 */
	public static FriendlyByteBuf preparePacket(Player player, InteractionHand hand) {
		return preparePacket(new BlockPos(hand == InteractionHand.MAIN_HAND ? 40 : player.getInventory().selected, Y_ITEM, 0));
	}

	static class PacketSplitter implements Iterable<FriendlyByteBuf>, Iterator<FriendlyByteBuf> {

		final FriendlyByteBuf pkt;
		boolean hasNext = true;

		public PacketSplitter(FriendlyByteBuf pkt) {
			this.pkt = pkt;
		}

		@Override
		public boolean hasNext() {
			return hasNext;
		}

		@Override
		public FriendlyByteBuf next() {
			int size = pkt.readUnsignedByte();
			if (size == 0) {
				hasNext = false;
				return pkt;
			} else {
				size += HEADERSIZE - 1;
				int p = pkt.readerIndex();
				hasNext = pkt.readerIndex(p + size).isReadable();
				return new FriendlyByteBuf(pkt.slice(p, size));
			}
		}

		@Override
		public Iterator<FriendlyByteBuf> iterator() {
			pkt.readerIndex(0);
			hasNext = true;
			return this;
		}
	}

	@Override
	protected String version() {
		return "0";
	}

}
