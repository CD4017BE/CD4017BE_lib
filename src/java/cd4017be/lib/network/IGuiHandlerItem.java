package cd4017be.lib.network;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * implemented by Items to provide GUIs
 * @author CD4017BE
 */
public interface IGuiHandlerItem {

	/**
	 * @param stack
	 * @param player the player accessing the gui
	 * @param slot where the item is in the player's inventory
	 * @param x arbitrary data
	 * @param y arbitrary data
	 * @param z arbitrary data
	 * @return a Container instance for the server side GUI
	 */
	Container getContainer(ItemStack stack, EntityPlayer player, int slot, int x, int y, int z);

	/**
	 * @param stack
	 * @param player the player accessing the gui
	 * @param slot where the item is in the player's inventory
	 * @param x arbitrary data
	 * @param y arbitrary data
	 * @param z arbitrary data
	 * @return a GuiScreen instance for the server side GUI
	 */
	@SideOnly(Side.CLIENT)
	GuiScreen getGuiScreen(ItemStack stack, EntityPlayer player, int slot, int x, int y, int z);

}
