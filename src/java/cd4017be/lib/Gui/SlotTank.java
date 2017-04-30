package cd4017be.lib.Gui;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 *
 * @author CD4017BE
 */
public class SlotTank extends SlotItemHandler {

	public SlotTank(IItemHandler inv, int slot, int x, int y){
		super(inv, slot, x, y);
	}

	@Override
	public boolean isItemValid(ItemStack item) {
		return FluidUtil.getFluidHandler(item) != null;
	}

}
