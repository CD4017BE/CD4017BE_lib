package cd4017be.lib;

import cd4017be.lib.Gui.DataContainer;
import cd4017be.lib.block.AdvancedBlock;
import cd4017be.lib.network.GuiNetworkHandler;
import cd4017be.lib.network.IGuiHandlerBlock;
import cd4017be.lib.network.IGuiHandlerTile;
import cd4017be.lib.network.IPlayerPacketReceiver;
import cd4017be.lib.network.IServerPacketReceiver;
import cd4017be.lib.network.NetworkHandler;
import cd4017be.lib.util.Utils;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLEventChannel;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import io.netty.buffer.Unpooled;

import java.io.IOException;
import java.util.Collection;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * 
 * @author CD4017BE
 * @deprecated used {@link GuiNetworkHandler} instead
 */
@Deprecated
public class BlockGuiHandler {

	private static final String guiChannel = "4017";

	public static BlockGuiHandler instance = new BlockGuiHandler();
	public static FMLEventChannel eventChannel;

	public static boolean OPEN_CLIENT;

	static void register() {
		eventChannel = NetworkRegistry.INSTANCE.newEventDrivenChannel(guiChannel);
		eventChannel.register(instance);
	}

	/**
	 * register a Gui to a block.<br>
	 * {@code registerContainer(id, ...);} must be called before.
	 * @param id
	 * @param gui
	 * @deprecated use {@link IGuiHandlerBlock} or {@link IGuiHandlerTile}
	 */
	@SideOnly(Side.CLIENT)
	@Deprecated
	public static void registerGui(Block id, Class<? extends GuiContainer> gui) {
		if (id instanceof AdvancedBlock)
			((AdvancedBlock)id).guiScreen = gui;
	}

	/**
	 * register a container to a block
	 * @param id
	 * @param container
	 * @deprecated use {@link IGuiHandlerBlock} or {@link IGuiHandlerTile}
	 */
	@Deprecated
	public static void registerContainer(Block id, Class<? extends Container> container) {
		if (id instanceof AdvancedBlock)
			((AdvancedBlock)id).container = container;
	}

	/**
	 * Opens a Gui
	 * @param player the player using it
	 * @param world World
	 * @param pos optional position of a block associated with it
	 * @param guiType < 0: Block-gui variants, guiType >= 0: IGuiItem in inventory slot guiType
	 * @deprecated use {@link GuiNetworkHandler#openItemGui(EntityPlayer, int, int, int, int)} instead
	 */
	@Deprecated
	public static void openGui(EntityPlayer player, World world, BlockPos pos, int guiType) {
		OPEN_CLIENT = true;
		if (guiType < 0) GuiNetworkHandler.openBlockGui(player, pos, -guiType);
		else GuiNetworkHandler.openItemGui(player, guiType, pos.getX(), pos.getY(), pos.getZ());
		OPEN_CLIENT = false;
	}

	/**
	 * Opens a Gui for an IGuiItem held by player
	 * @param player the player using it
	 * @param hand position of Item
	 * @deprecated use {@link GuiNetworkHandler#openHeldItemGui(EntityPlayer, EnumHand, int, int, int)} instead
	 */
	@Deprecated
	public static void openItemGui(EntityPlayer player, EnumHand hand) {
		OPEN_CLIENT = true;
		GuiNetworkHandler.openHeldItemGui(player, hand, 0, -1, 0);
		OPEN_CLIENT = false;
	}

	/**
	 * Opens the default Gui for a block
	 * @param player the player using it
	 * @param world World
	 * @param pos position of the block
	 * @deprecated use {@link GuiNetworkHandler#openBlockGui(EntityPlayer, BlockPos, int)} instead
	 */
	@Deprecated
	public static void openBlockGui(EntityPlayer player, World world, BlockPos pos) {
		OPEN_CLIENT = true;
		GuiNetworkHandler.openBlockGui(player, pos, 0);
		OPEN_CLIENT = false;
	}

	/**
	 * Sends a Gui command packet to the server. 
	 * @param data
	 */
	public static void sendPacketToServer(PacketBuffer data) {
		eventChannel.sendToServer(new FMLProxyPacket(data, guiChannel));
	}

	/**
	 * Sends a Gui update packet to the given player.
	 * @param player
	 * @param data
	 */
	public static void sendPacketToPlayer(EntityPlayerMP player, PacketBuffer data) {
		eventChannel.sendTo(new FMLProxyPacket(data, guiChannel), player);
	}

	/**
	 * Sends a Gui update packet to the given player.
	 * @param player
	 * @param data
	 */
	public static void sendPacketToPlayers(PacketBuffer data, Collection<EntityPlayerMP> players) {
		FMLProxyPacket packet = new FMLProxyPacket(data, guiChannel);
		for (EntityPlayerMP player : players)
			eventChannel.sendTo(packet, player);
	}

	/**
	 * Sends a Gui update packet to all players standing within given range to a TileEntity.
	 * @param tile
	 * @param range
	 * @param data
	 */
	public static void sendPacketToAllNear(TileEntity tile, double range, PacketBuffer data) {
		BlockPos pos = tile.getPos();
		eventChannel.sendToAllAround(new FMLProxyPacket(data, guiChannel), new TargetPoint(tile.getWorld().provider.getDimension(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, range));
	}

	/**
	 * Creates a PacketBuffer with an already written header to be send to given target position.
	 * @param pos target position (posY < 0 is for items, see {@link getPacketForItem})
	 * @return prepared PacketBuffer
	 */
	public static PacketBuffer getPacketTargetData(BlockPos pos) {
		PacketBuffer data = new PacketBuffer(Unpooled.buffer());
		data.writeBlockPos(pos);
		return data;
	}

	/**
	 * Creates a PacketBuffer with an already written header to be send to given player inventory slot.
	 * @param slot slot id of receiving item: [0-8] hotbar, [0-35] mainInventory, [36-39] armor, [40] offHand
	 * @return prepared PacketBuffer
	 */
	public static PacketBuffer getPacketForItem(int slot) {
		return getPacketTargetData(new BlockPos(slot, - 1, 0));
	}

	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void onServerPacketReceived(FMLNetworkEvent.ClientCustomPacketEvent event) {
		FMLProxyPacket packet = event.getPacket();
		if (!packet.channel().equals(guiChannel)) return;
		PacketBuffer data = new PacketBuffer(packet.payload());
		BlockPos target = null;
		try {
			target = data.readBlockPos();
			Container container = Minecraft.getMinecraft().player.openContainer;
			if (container instanceof DataContainer && ((DataContainer)container).data.pos().equals(target)) {
				((DataContainer)container).onDataUpdate(data);
			} else {
				TileEntity te = Utils.getTileAt(Minecraft.getMinecraft().world, target);
				if (te instanceof ServerPacketReceiver) ((ServerPacketReceiver)te).onPacketFromServer(data);
			}
		} catch (Exception e) {
			Lib.LOG.error(NetworkHandler.NETWORK, printPacketData(data).insert(0, "reading server -> client packet for " + target + ": "), e);
		}
	}

	@SubscribeEvent
	public void onPlayerPacketReceived(FMLNetworkEvent.ServerCustomPacketEvent event) {
		FMLProxyPacket packet = event.getPacket();
		if (!packet.channel().equals(guiChannel)) return;
		EntityPlayerMP player = ((NetHandlerPlayServer)event.getHandler()).player;
		PacketBuffer data = new PacketBuffer(packet.payload());
		BlockPos target = null;
		try {
			target = data.readBlockPos();
			if (target.getY() < 0) {
				int slot = target.getX();
				ItemStack item = player.inventory.getStackInSlot(slot);
				if (item.getItem() instanceof ClientItemPacketReceiver)
					((ClientItemPacketReceiver)item.getItem()).onPacketFromClient(data, player, item, slot);
			} else {
				TileEntity te = Utils.getTileAt(player.world, target);
				if (te instanceof ClientPacketReceiver)
					((ClientPacketReceiver)te).onPacketFromClient(data, player);
			}
		} catch (Exception e) {
			Lib.LOG.error(NetworkHandler.NETWORK, printPacketData(data).insert(0, "reading client -> server packet for " + target + ": "), e);
		}
	}

	private static StringBuilder printPacketData(PacketBuffer p) {
		int l = p.writerIndex();
		StringBuilder sb = new StringBuilder(3 * l - 23).append('[');
		byte[] d = p.array();
		for (int i = 8; i < l; i++)
			sb.append(String.format("%02X ", d[i] & 0xff));
		sb.setCharAt(sb.length() - 1, ']');
		return sb;
	}

	/**
	 * implemented by TileEntities to receive data packets send via {@code sendPacketToPlayer} from server
	 * @author CD4017BE
	 * @deprecated use {@link IServerPacketReceiver} instead
	 */
	@Deprecated
	public interface ServerPacketReceiver {
		/**
		 * Handle a data packet from server
		 * @param data contained payload
		 * @throws IOException
		 */
		@SideOnly(Side.CLIENT)
		public void onPacketFromServer(PacketBuffer data) throws IOException;
	}

	/**
	 * implemented by TileEntities to receive data packets send via {@code sendPacketToServer} from client
	 * @author CD4017BE
	 * @deprecated use {@link IPlayerPacketReceiver} instead
	 */
	@Deprecated
	public interface ClientPacketReceiver {
		/**
		 * Handle a data packet from given player
		 * @param data contained payload
		 * @param sender the player who sent this
		 * @throws IOException
		 */
		public void onPacketFromClient(PacketBuffer data, EntityPlayer sender) throws IOException;
	}

	/**
	 * implemented by Items to receive data packets send via {@code sendPacketToServer} from client
	 * @author CD4017BE
	 * @deprecated use {@link IPlayerPacketReceiver.ItemPPR} instead
	 */
	@Deprecated
	public interface ClientItemPacketReceiver {
		/**
		 * Handle a data packet from given player
		 * @param data contained payload
		 * @param sender the player who sent this
		 * @param item the actual ItemStack
		 * @param slot player inventory slot where the item is located
		 * @throws IOException
		 */
		public void onPacketFromClient(PacketBuffer data, EntityPlayer sender, ItemStack item, int slot) throws IOException;
	}

}
