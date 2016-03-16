/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cd4017be.lib;

import cd4017be.lib.TileBlockRegistry.TileBlockEntry;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLEventChannel;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import io.netty.buffer.Unpooled;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.apache.logging.log4j.Level;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

/**
 * 
 * @author CD4017BE
 */
public class BlockGuiHandler implements IGuiHandler
{
    private static final String guiChannel = "CD4017BE_gui";
    
    public static BlockGuiHandler instance = new BlockGuiHandler();
    private static Object modRef;
    public static FMLEventChannel eventChannel;
    
    static {
        eventChannel = NetworkRegistry.INSTANCE.newEventDrivenChannel(guiChannel);
        eventChannel.register(instance);
    }
    
    /**
     * Set a mod to register this Handler with.
     * @param mod
     */
    public static void registerMod(Object mod)
    {
        if (modRef == null) {
            modRef = mod;
            NetworkRegistry.INSTANCE.registerGuiHandler(modRef, instance);
        }
    }
    
    /**
     * Will open a Gui registered for the block at given position.
     * @param player
     * @param world
     * @param x
     * @param y
     * @param z
     */
    public static void openGui(EntityPlayer player, World world, int x, int y, int z)
    {
        if (modRef != null) player.openGui(modRef, 0, world, x, y, z);
        else FMLLog.severe("CD4017BE-lib: BlockGuiHandler failed to open Gui! No Mod registered!");
    }
    
    /**
     * Will open a Gui for the item currently held by the player. The item has to implement IGuiItem.
     * @param player
     * @param world
     * @param x
     * @param y
     * @param z
     */
    public static void openItemGui(EntityPlayer player, World world, int x, int y, int z)
    {
        if (modRef != null) player.openGui(modRef, 1, world, x, y, z);
        else FMLLog.severe("CD4017BE-lib: BlockGuiHandler failed to open Gui! No Mod registered!");
    }
    
    /**
     * Sends a Gui command packet to the server. 
     * @param data
     */
    public static void sendPacketToServer(PacketBuffer data)
    {
        eventChannel.sendToServer(new FMLProxyPacket(data, guiChannel));
    }
    
    /**
     * Sends a Gui update packet to the given player.
     * @param player
     * @param data
     */
    public static void sendPacketToPlayer(EntityPlayerMP player, PacketBuffer data)
    {
        eventChannel.sendTo(new FMLProxyPacket(data, guiChannel), player);
    }
    
    /**
     * Creates a ByteArrayOutputStream for use with sendPacketToServer with the target position already written to the stream.
     * @param x
     * @param y
     * @param z
     * @return
     * @throws IOException
     */
    public static PacketBuffer getPacketTargetData(BlockPos pos)
    {
        PacketBuffer data = new PacketBuffer(Unpooled.buffer());
    	data.writeBlockPos(pos);
        return data;
    }
    
    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onServerPacketReceived(FMLNetworkEvent.ClientCustomPacketEvent event)
    {
    	FMLProxyPacket packet = event.packet;
    	if (!packet.channel().equals(guiChannel)) return;
    	Container container = Minecraft.getMinecraft().thePlayer.openContainer;
        if (container != null && container instanceof TileContainer) {
            try {
                PacketBuffer data = new PacketBuffer(packet.payload());
                ModTileEntity te = ((TileContainer)container).tileEntity;
                if (te.getPos().equals(data.readBlockPos())) ((TileContainer)container).onDataUpdate(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    @SubscribeEvent
    public void onPlayerPacketReceived(FMLNetworkEvent.ServerCustomPacketEvent event)
    {
    	FMLProxyPacket packet = event.packet;
    	if (!packet.channel().equals(guiChannel)) return;
    	if (!(event.handler instanceof NetHandlerPlayServer)) {
    		FMLLog.log(Level.WARN, "NetHandler not instanceof NetHandlerPlayServer!");
    	}
    	EntityPlayerMP player = ((NetHandlerPlayServer)event.handler).playerEntity;
        try {
            PacketBuffer data = new PacketBuffer(packet.payload());
            BlockPos pos = data.readBlockPos();
            if (pos.getY() < 0) {
                ItemStack item = player.getCurrentEquippedItem();
                if (item != null && item.getItem() instanceof IGuiItem) ((IGuiItem)item.getItem()).onPlayerCommand(player.worldObj, player, data);
            } else {
                TileEntity te = player.worldObj.getTileEntity(pos);
                if (te != null && te instanceof ModTileEntity) ((ModTileEntity)te).onPlayerCommand(data, player);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) 
    {
        if (ID == 0) {
        	BlockPos pos = new BlockPos(x, y, z);
            TileBlockEntry entry = TileBlockRegistry.getBlockEntry(world.getBlockState(pos).getBlock());
            TileEntity te = world.getTileEntity(pos);
            if (entry != null && entry.container != null && te != null && entry.tileEntity.isInstance(te)) {
                try {
                    return entry.container.getConstructor(ModTileEntity.class, EntityPlayer.class).newInstance((ModTileEntity)te, player);
                } catch (NoSuchMethodException ex) {
                    FMLLog.severe("CD4017BE-lib: TileContainer %1$s is missing the Constructor ( %2$s , %3$s )", entry.container.getName(), ModTileEntity.class.getName(), EntityPlayer.class.getName());
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
        } else if (ID == 1) {
            ItemStack item = player.getCurrentEquippedItem();
            if (item != null && item.getItem() instanceof IGuiItem) return ((IGuiItem)item.getItem()).getContainer(world, player, x, y, z);
            else return null;
        } else return null;
    }

    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) 
    {
        if (ID == 0) { //Block gui
        	BlockPos pos = new BlockPos(x, y, z);
            TileBlockEntry entry = TileBlockRegistry.getBlockEntry(world.getBlockState(pos).getBlock());
            TileEntity te = world.getTileEntity(pos);
            if (entry != null && entry.gui != null && te != null && entry.tileEntity.isInstance(te)) {
                try {
                    return entry.gui.getConstructor(entry.tileEntity, EntityPlayer.class).newInstance(entry.tileEntity.cast(te), player);
                } catch (NoSuchMethodException ex) {
                    FMLLog.severe("CD4017BE-lib: GuiContainer %1$s is missing the Constructor ( %2$s , %3$s )", entry.gui.getName(), entry.tileEntity.getName(), EntityPlayer.class.getName());
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
        } else if (ID == 1) { //Item gui
            ItemStack item = player.getCurrentEquippedItem();
            if (item != null && item.getItem() instanceof IGuiItem) return ((IGuiItem)item.getItem()).getGui(world, player, x, y, z);
            else return null;
        } else return null;
    }
    
}
