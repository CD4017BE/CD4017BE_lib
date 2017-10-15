package cd4017be.lib.Gui;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Implemented by Items to enable GuiScreens
 * @author CD4017BE
 */
public interface IGuiItem 
{
	/**
	 * @param item
	 * @param player
	 * @param world
	 * @param pos
	 * @param slot
	 * @return The server gui object
	 */
	public Container getContainer(ItemStack item, EntityPlayer player, World world, BlockPos pos, int slot);
	
	@SideOnly(Side.CLIENT)
	/**
	 * @param item
	 * @param player
	 * @param world
	 * @param pos
	 * @param slot
	 * @return The client gui object
	 */
	public GuiContainer getGui(ItemStack item, EntityPlayer player, World world, BlockPos pos, int slot);

}
