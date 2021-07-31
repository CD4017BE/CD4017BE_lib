package cd4017be.lib.container;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**Limits insertion based on underlying inventory.
 * @author CD4017BE */
public class SmartSlot extends Slot {

	public SmartSlot(Container inv, int slot, int x, int y) {
		super(inv, slot, x, y);
	}

	@Override
	public boolean mayPlace(ItemStack stack) {
		return container.canPlaceItem(index, stack);
	}

}
