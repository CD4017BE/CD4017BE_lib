package cd4017be.lib.container.slot;

import net.minecraft.world.item.ItemStack;
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
	public boolean mayPlace(ItemStack item) {
		return FluidUtil.getFluidHandler(item) != null;
	}

}
