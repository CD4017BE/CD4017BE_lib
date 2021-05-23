package cd4017be.lib.container;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;

/**Limits insertion based on underlying inventory.
 * @author CD4017BE */
public class SmartSlot extends Slot {

	public SmartSlot(IInventory inv, int slot, int x, int y) {
		super(inv, slot, x, y);
	}

	@Override
	public boolean mayPlace(ItemStack stack) {
		return container.canPlaceItem(index, stack);
	}

}
