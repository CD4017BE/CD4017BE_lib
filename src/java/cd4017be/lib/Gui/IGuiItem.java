package cd4017be.lib.Gui;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import cd4017be.lib.network.IGuiHandlerItem;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
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
		Container c = getContainer(stack, player, player.world, new BlockPos(x, y, z), slot);
		if (c instanceof DataContainer) ((DataContainer)c).data.initContainer((DataContainer)c);
		return c;
	}

	@Override
	@SideOnly(Side.CLIENT)
	default GuiScreen getGuiScreen(ItemStack stack, EntityPlayer player, int slot, int x, int y, int z) {
		GuiContainer g = getGui(stack, player, player.world, new BlockPos(x, y, z), slot);
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

}
