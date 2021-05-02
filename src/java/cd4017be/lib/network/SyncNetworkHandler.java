package cd4017be.lib.network;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import cd4017be.api.recipes.RecipeScriptContext.ConfigConstants;
import cd4017be.lib.Lib;
import cd4017be.lib.util.Utils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
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

	public static final int Y_TILEENTITY = 0, Y_ENTITY = -1, Y_ITEM = -2;
	public static final int HEADERSIZE = 9;
	private static int MAX_PACKSIZE;

	/**the instance */
	public static SyncNetworkHandler instance;

	public static void register(ConfigConstants cfg) {
		MAX_PACKSIZE = Math.min((int)cfg.getNumber("packet_chain_threshold", 255), 255) + HEADERSIZE;
		if (instance == null) instance = new SyncNetworkHandler(new ResourceLocation(Lib.ID, "sy"));
	}

	private final HashMap<ServerPlayerEntity, ByteBuf> chainedPackets;

	private SyncNetworkHandler(ResourceLocation channel) {
		super(channel);
		this.chainedPackets = new HashMap<>();
		MinecraftForge.EVENT_BUS.register(this);
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void handleServerPacket(PacketBuffer pkt) throws Exception {
		World world = Minecraft.getInstance().level;
		ClientPlayerEntity player = Minecraft.getInstance().player;
		for (PacketBuffer buf : new PacketSplitter(pkt)) {
			BlockPos target = buf.readBlockPos();
			int y = target.getY();
			if (y >= Y_TILEENTITY) {
				TileEntity te = Utils.getTileAt(world, target);
				if (te instanceof IServerPacketReceiver)
					((IServerPacketReceiver)te).handleServerPacket(buf);
			} else if (y == Y_ENTITY) {
				Entity entity = world.getEntity(target.getX() & 0xffff | target.getZ() << 16);
				if (entity instanceof IServerPacketReceiver)
					((IServerPacketReceiver)entity).handleServerPacket(buf);
			} else if (y == Y_ITEM) {
				int slot = target.getX();
				if (slot < 0 || slot >= player.inventory.getContainerSize()) continue;
				ItemStack stack = player.inventory.getItem(slot);
				Item item = stack.getItem();
				if (item instanceof IServerPacketReceiver.ItemSPR)
					((IServerPacketReceiver.ItemSPR)item).handleServerPacket(stack, player, slot, buf);
			}
		}
	}

	@Override
	public void handlePlayerPacket(PacketBuffer pkt, ServerPlayerEntity sender) throws Exception {
		World world = sender.level;
		for (PacketBuffer buf : new PacketSplitter(pkt)) {//TODO chained packets don't make much sense on client -> server
			BlockPos target = buf.readBlockPos();
			int y = target.getY();
			if (y >= Y_TILEENTITY) {
				TileEntity te = Utils.getTileAt(world, target);
				if (te instanceof IPlayerPacketReceiver)
					((IPlayerPacketReceiver)te).handlePlayerPacket(buf, sender);
			} else if (y == Y_ENTITY) {
				Entity entity = world.getEntity(target.getX() & 0xffff | target.getZ() << 16);
				if (entity instanceof IPlayerPacketReceiver)
					((IPlayerPacketReceiver)entity).handlePlayerPacket(buf, sender);
			} else if (y == Y_ITEM) {
				int slot = target.getX();
				if (slot < 0 || slot >= sender.inventory.getContainerSize()) continue;
				ItemStack stack = sender.inventory.getItem(slot);
				Item item = stack.getItem();
				if (item instanceof IPlayerPacketReceiver.ItemPPR)
					((IPlayerPacketReceiver.ItemPPR)item).handlePlayerPacket(stack, slot, buf, sender);
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.LOW)
	public void tick(ServerTickEvent event) {
		if (event.phase != Phase.END || chainedPackets.isEmpty()) return;
		for (Entry<ServerPlayerEntity, ByteBuf> e : chainedPackets.entrySet())
			super.sendToPlayer(new PacketBuffer(e.getValue()), e.getKey());
		chainedPackets.clear();
	}

	private void addPacket(ByteBuf pkt, ServerPlayerEntity player) {
		pkt.setByte(0, pkt.readableBytes() - HEADERSIZE);
		chainedPackets.merge(player, pkt, ByteBuf::writeBytes);
	}

	@Override
	public void sendToPlayer(PacketBuffer pkt, ServerPlayerEntity player) {
		if (pkt.readableBytes() <= MAX_PACKSIZE) addPacket(pkt, player);
		else super.sendToPlayer(pkt, player);
	}

	@Override
	public void sendToPlayers(PacketBuffer pkt, Collection<ServerPlayerEntity> players) {
		int l = pkt.readableBytes();
		if (l > MAX_PACKSIZE) super.sendToPlayers(pkt, players);
		else {
			pkt.setByte(0, pkt.readableBytes() - HEADERSIZE);
			for (ServerPlayerEntity player : players) {
				ByteBuf buf = chainedPackets.get(player);
				if (buf == null) chainedPackets.put(player, pkt.copy());
				else buf.writeBytes(pkt);
			}
		}
	}

	public static PacketBuffer preparePacket(BlockPos pos) {
		PacketBuffer pkt = new PacketBuffer(Unpooled.buffer());
		pkt.writeByte(0);
		pkt.writeBlockPos(pos);//TODO improve format
		return pkt;
	}

	/**
	 * @param tile the TileEntity to send to (on the other side)
	 * @return a new PacketBuffer with prepared header
	 * @see IServerPacketReceiver#handleServerPacket(PacketBuffer)
	 * @see IPlayerPacketReceiver#handlePlayerPacket(PacketBuffer, ServerPlayerEntity)
	 */
	public static PacketBuffer preparePacket(TileEntity tile) {
		return preparePacket(tile.getBlockPos());
	}

	/**
	 * @param entity the Entity to send to (on the other side)
	 * @return a new PacketBuffer with prepared header
	 * @see IServerPacketReceiver#handleServerPacket(PacketBuffer)
	 * @see IPlayerPacketReceiver#handlePlayerPacket(PacketBuffer, ServerPlayerEntity)
	 */
	public static PacketBuffer preparePacket(Entity entity) {
		int id = entity.getId();
		return preparePacket(new BlockPos(id & 0xffff, Y_ENTITY, id >> 16 & 0xffff));
	}

	/**
	 * @param slot the player inventory slot to send to
	 * @return a new PacketBuffer with prepared header
	 * @see IServerPacketReceiver.ItemSPR#handleServerPacket(ItemStack, PlayerEntitySP, int, PacketBuffer)
	 * @see IPlayerPacketReceiver.ItemPPR#handlePlayerPacket(ItemStack, int, PacketBuffer, ServerPlayerEntity)
	 */
	public static PacketBuffer preparePacket(int slot) {
		return preparePacket(new BlockPos(slot, Y_ITEM, 0));
	}

	/**
	 * @param player the player holding the item
	 * @param hand the held item to send to
	 * @return a new PacketBuffer with prepared header
	 * @see IServerPacketReceiver.ItemSPR#handleServerPacket(ItemStack, PlayerEntitySP, int, PacketBuffer)
	 * @see IPlayerPacketReceiver.ItemPPR#handlePlayerPacket(ItemStack, int, PacketBuffer, ServerPlayerEntity)
	 */
	public static PacketBuffer preparePacket(PlayerEntity player, Hand hand) {
		return preparePacket(new BlockPos(hand == Hand.MAIN_HAND ? 40 : player.inventory.selected, Y_ITEM, 0));
	}

	static class PacketSplitter implements Iterable<PacketBuffer>, Iterator<PacketBuffer> {

		final PacketBuffer pkt;
		boolean hasNext = true;

		public PacketSplitter(PacketBuffer pkt) {
			this.pkt = pkt;
		}

		@Override
		public boolean hasNext() {
			return hasNext;
		}

		@Override
		public PacketBuffer next() {
			int size = pkt.readUnsignedByte();
			if (size == 0) {
				hasNext = false;
				return pkt;
			} else {
				size += HEADERSIZE - 1;
				int p = pkt.readerIndex();
				hasNext = pkt.readerIndex(p + size).isReadable();
				return new PacketBuffer(pkt.slice(p, size));
			}
		}

		@Override
		public Iterator<PacketBuffer> iterator() {
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
