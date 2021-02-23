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
		this.inventory.setInventorySlotContents(0, stack);
	}

	@Override
	public boolean isItemValid(ItemStack stack) {
		return false;
	}

	@Override
	public void putStack(ItemStack stack) {}

	@Override
	public ItemStack decrStackSize(int amount) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean canTakeStack(PlayerEntity playerIn) {
		return false;
	}

	@Override
	public int getSlotStackLimit() {
		return 0;
	}

}
