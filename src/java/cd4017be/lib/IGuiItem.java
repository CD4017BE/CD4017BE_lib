/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cd4017be.lib;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;

/**
 * Implemented by Items to enable GuiScreens
 * @author CD4017BE
 */
public interface IGuiItem 
{
	/**
	 * @param world
	 * @param player
	 * @param x
	 * @param y
	 * @param z
	 * @return The server gui object
	 */
	public Container getContainer(World world, EntityPlayer player, int x, int y, int z);
	
	@SideOnly(Side.CLIENT)
	/**
	 * @param world
	 * @param player
	 * @param x
	 * @param y
	 * @param z
	 * @return The client gui object
	 */
	public GuiContainer getGui(World world, EntityPlayer player, int x, int y, int z);
	
	/**
	 * Called on server after BlockGuiHandler.sendPacketToPlayer is called on client
	 * @param item
	 * @param player
	 * @param data
	 * @throws IOException
	 */
	public void onPlayerCommand(ItemStack item, EntityPlayer player, PacketBuffer data);
}
