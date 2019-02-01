package cd4017be.lib.network;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * implemented by TileEntities to provide GUIs (as alternative to {@link IGuiHandlerBlock})
 * @author CD4017BE
 */
public interface IGuiHandlerTile {

	/**
	 * @param player the player accessing the gui
	 * @param id arbitrary data
	 * @return a Container instance for the server side GUI
	 */
	Container getContainer(EntityPlayer player, int id);

	/**
	 * @param player the player accessing the gui
	 * @param id arbitrary data
	 * @return a GuiScreen instance for the client side GUI
	 */
	@SideOnly(Side.CLIENT)
	GuiScreen getGuiScreen(EntityPlayer player, int id);

}