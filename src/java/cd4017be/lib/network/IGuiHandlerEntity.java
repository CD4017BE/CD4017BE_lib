package cd4017be.lib.network;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * implemented by Entities to provide GUIs
 * @author CD4017BE
 */
public interface IGuiHandlerEntity {

	/**
	 * @param player the player accessing the gui
	 * @param x arbitrary data
	 * @param y arbitrary data
	 * @return a Container instance for the server side GUI
	 */
	Container getContainer(EntityPlayer player, int x, int y);

	/**
	 * @param player the player accessing the gui
	 * @param x arbitrary data
	 * @param y arbitrary data
	 * @return a GuiScreen instance for the server side GUI
	 */
	@SideOnly(Side.CLIENT)
	GuiScreen getGuiScreen(EntityPlayer player, int x, int y);

}
