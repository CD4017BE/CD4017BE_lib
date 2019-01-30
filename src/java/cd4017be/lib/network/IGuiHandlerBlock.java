package cd4017be.lib.network;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * implemented by Blocks to provide GUIs
 * @author CD4017BE
 */
public interface IGuiHandlerBlock {

	/**
	 * @param state
	 * @param world
	 * @param pos
	 * @param player the player accessing the gui
	 * @param id arbitrary data
	 * @return a Container instance for the server side GUI
	 */
	Container getContainer(IBlockState state, World world, BlockPos pos, EntityPlayer player, int id);

	/**
	 * @param state
	 * @param world
	 * @param pos
	 * @param player the player accessing the gui
	 * @param id arbitrary data
	 * @return a GuiScreen instance for the client side GUI
	 */
	@SideOnly(Side.CLIENT)
	GuiScreen getGuiScreen(IBlockState state, World world, BlockPos pos, EntityPlayer player, int id);

}
