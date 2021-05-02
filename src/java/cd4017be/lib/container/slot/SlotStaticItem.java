package cd4017be.lib.container.slot;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;

/**
 * 
 * @author CD4017BE
 */
public class SlotStaticItem extends Slot {

	public SlotStaticItem(int x, int y, ItemStack stack) {
		super(new Inventory(1), 0, x, y);
		this.container.setItem(0, stack);
	}

	@Override
	public boolean mayPlace(ItemStack stack) {
		return false;
	}

	@Override
	public void set(ItemStack stack) {}

	@Override
	public ItemStack remove(int amount) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean mayPickup(PlayerEntity playerIn) {
		return false;
	}

	@Override
	public int getMaxStackSize() {
		return 0;
	}

}
