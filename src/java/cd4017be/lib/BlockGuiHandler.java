package cd4017be.lib;

import cd4017be.lib.Gui.DataContainer;
import cd4017be.lib.Gui.IGuiItem;
import cd4017be.lib.Gui.DataContainer.IGuiData;
import cd4017be.lib.util.Utils;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLEventChannel;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import io.netty.buffer.Unpooled;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

import org.apache.logging.log4j.Level;

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
 */
public class BlockGuiHandler implements IGuiHandler {

	private static final String guiChannel = "CD4017BE_gui";
	private static HashMap<Block, GuiEntry> gui_registry = new HashMap<Block, GuiEntry>();

	public static BlockGuiHandler instance = new BlockGuiHandler();
	public static FMLEventChannel eventChannel;

	static void register() {
		eventChannel = NetworkRegistry.INSTANCE.newEventDrivenChannel(guiChannel);
		eventChannel.register(instance);
		NetworkRegistry.INSTANCE.registerGuiHandler(Lib.instance, instance);
	}

	/**
	 * register a Gui to a block.<br>
	 * {@code registerContainer(id, ...);} must be called before.
	 * @param id
	 * @param gui
	 */
	@SideOnly(Side.CLIENT)
	public static void registerGui(Block id, Class<? extends GuiContainer> gui) {
		GuiEntry entry = gui_registry.get(id);
		if (entry != null) {
			if (entry.gui != null) FMLLog.warning("CD4017BE-modlib: GuiContainer %1$s overrrides already registered GuiContainer %2$s for Block-ID %3$d !", gui.getName(), entry.gui.getName(), id);
			entry.gui = gui;
		} else FMLLog.warning("CD4017BE-modlib: Failed to register GuiContainer %1$s because Block-ID %2$s is not registered!", gui.getName(), id);
	}

	/**
	 * register a container to a block
	 * @param id
	 * @param container
	 */
	public static void registerContainer(Block id, Class<? extends Container> container) {
		GuiEntry entry = new GuiEntry();
		entry.container = container;
		gui_registry.put(id, entry);
	}

	/**
	 * Opens a Gui
	 * @param player the player using it
	 * @param world World
	 * @param pos optional position of a block associated with it
	 * @param guiType < 0: Block-gui variants, guiType >= 0: IGuiItem in inventory slot guiType
	 */
	public static void openGui(EntityPlayer player, World world, BlockPos pos, int guiType) {
		if (Lib.instance != null) player.openGui(Lib.instance, guiType, world, pos.getX(), pos.getY(), pos.getZ());
		else FMLLog.severe("CD4017BE-lib: BlockGuiHandler failed to open Gui! No Mod registered!");
	}

	/**
	 * Opens a Gui for an IGuiItem held by player
	 * @param player the player using it
	 * @param hand position of Item
	 */
	public static void openItemGui(EntityPlayer player, EnumHand hand) {
		openGui(player, player.world, Utils.NOWHERE, hand == EnumHand.MAIN_HAND ? player.inventory.currentItem : 40);
	}

	/**
	 * Opens the default Gui for a block
	 * @param player the player using it
	 * @param world World
	 * @param pos position of the block
	 */
	public static void openBlockGui(EntityPlayer player, World world, BlockPos pos) {
		openGui(player, world, pos, -1);
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
			String s = " ";
			byte[] d = data.array();
			for (int i = 8; i < data.writerIndex(); i++) s += Integer.toHexString(d[i] & 0xff) + " ";
			FMLLog.log("CD4017BE_packet", Level.ERROR, e, "reading server -> client packet for %s: [%s]", target, s);
		}
	}

	@SubscribeEvent
	public void onPlayerPacketReceived(FMLNetworkEvent.ServerCustomPacketEvent event) {
		FMLProxyPacket packet = event.getPacket();
		if (!packet.channel().equals(guiChannel)) return;
		if (!(event.getHandler() instanceof NetHandlerPlayServer)) {
			FMLLog.log(Level.WARN, "NetHandler not instanceof NetHandlerPlayServer!");
		}
		EntityPlayerMP player = ((NetHandlerPlayServer)event.getHandler()).player;
		try {
			PacketBuffer data = new PacketBuffer(packet.payload());
			BlockPos target = data.readBlockPos();
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
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
		BlockPos pos = new BlockPos(x, y, z);
		Container c;
		if (ID < 0) {
			GuiEntry entry = gui_registry.get(world.getBlockState(pos).getBlock());
			TileEntity te = world.getTileEntity(pos);
			if (entry != null && entry.container != null && te instanceof IGuiData) {
				try {
					c = entry.container.getConstructor(IGuiData.class, EntityPlayer.class).newInstance((IGuiData)te, player);
				} catch (NoSuchMethodException ex) {
					FMLLog.severe("CD4017BE-lib: TileContainer %1$s is missing the Constructor ( %2$s , %3$s )", entry.container.getName(), IGuiData.class.getName(), EntityPlayer.class.getName());
					return null;
				} catch (InstantiationException ex) {
					ex.printStackTrace();
					return null;
				} catch (IllegalAccessException ex) {
					ex.printStackTrace();
					return null;
				} catch (InvocationTargetException ex) {
					ex.printStackTrace();
					return null;
				}
			} else return null;
		} else {
			ItemStack item = player.inventory.getStackInSlot(ID);
			if (item.getItem() instanceof IGuiItem) c = ((IGuiItem)item.getItem()).getContainer(item, player, world, pos, ID);
			else return null;
		}
		if (c instanceof DataContainer) ((DataContainer)c).data.initContainer((DataContainer)c);
		return c;
	}

	@Override
	public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
		BlockPos pos = new BlockPos(x, y, z);
		GuiContainer g;
		if (ID < 0) { //Block gui
			GuiEntry entry = gui_registry.get(world.getBlockState(pos).getBlock());
			TileEntity te = world.getTileEntity(pos);
			if (entry != null && entry.gui != null && te instanceof IGuiData) {
				if(player.openContainer != null && player.openContainer instanceof DataContainer && ((DataContainer)player.openContainer).data == te) {
					return Minecraft.getMinecraft().currentScreen;
				}
				try {
					g = entry.gui.getConstructor(IGuiData.class, EntityPlayer.class).newInstance((IGuiData)te, player);
				} catch (NoSuchMethodException ex) {
					FMLLog.severe("CD4017BE-lib: GuiContainer %1$s is missing the Constructor ( %2$s , %3$s )", entry.gui.getName(), IGuiData.class.getName(), EntityPlayer.class.getName());
					return null;
				} catch (InstantiationException ex) {
					ex.printStackTrace();
					return null;
				} catch (IllegalAccessException ex) {
					ex.printStackTrace();
					return null;
				} catch (InvocationTargetException ex) {
					ex.printStackTrace();
					return null;
				}
			} else return null;
		} else { //Item gui
			ItemStack item = player.inventory.getStackInSlot(ID);
			if (item.getItem() instanceof IGuiItem) g = ((IGuiItem)item.getItem()).getGui(item, player, world, pos, ID);
			else return null;
		}
		if (g.inventorySlots instanceof DataContainer) {
			DataContainer c = (DataContainer)g.inventorySlots;
			c.data.initContainer(c);
			c.refInts = c.data.getSyncVariables();
			if(c.refInts != null && c.data instanceof TileEntity) 
				for (int i = 0; i < c.refInts.length; i++)
					c.data.setSyncVariable(i, 0);
		}
		return g;
	}

	/**
	 * implemented by TileEntities to receive data packets send via {@code sendPacketToPlayer} from server
	 * @author CD4017BE
	 */
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
	 */
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
	 */
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

	private static class GuiEntry {
		Class<? extends Container> container;
		@SideOnly(Side.CLIENT)
		Class<? extends GuiContainer> gui;
	}

}
