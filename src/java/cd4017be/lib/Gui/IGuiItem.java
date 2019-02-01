package cd4017be.lib.Gui;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import cd4017be.lib.network.IGuiHandlerItem;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Implemented by Items to enable GuiScreens
 * @author CD4017BE
 * @deprecated use {@link IGuiHandlerItem} instead
 */
@Deprecated
public interface IGuiItem extends IGuiHandlerItem {
	/**
	 * @param item
	 * @param player
	 * @param world
	 * @param pos
	 * @param slot
	 * @return The server gui object
	 */
	public Container getContainer(ItemStack item, EntityPlayer player, World world, BlockPos pos, int slot);

	/**
	 * @param item
	 * @param player
	 * @param world
	 * @param pos
	 * @param slot
	 * @return The client gui object
	 */
	@SideOnly(Side.CLIENT)
	public GuiContainer getGui(ItemStack item, EntityPlayer player, World world, BlockPos pos, int slot);

	@Override
	default Container getContainer(ItemStack stack, EntityPlayer player, int slot, int x, int y, int z) {
		return getContainer(stack, player, player.world, new BlockPos(x, y, z), slot);
	}

	@Override
	default GuiScreen getGuiScreen(ItemStack stack, EntityPlayer player, int slot, int x, int y, int z) {
		return getGui(stack, player, player.world, new BlockPos(x, y, z), slot);
	}

}
