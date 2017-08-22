package cd4017be.lib.templates;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;

public abstract class AbstractInventory implements IItemHandlerModifiable {

	@Override
	public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
		ItemStack item = getStackInSlot(slot);
		int n = item.getCount();
		int m = Math.min(insertAm(slot, stack) - n, stack.getCount()); 
		if (m <= 0) return stack;
		if (n == 0) {
			if (!simulate) setStackInSlot(slot, ItemHandlerHelper.copyStackWithSize(stack, m));
		} else if (ItemHandlerHelper.canItemStacksStack(item, stack)) {
			if (!simulate) {
				item.grow(m);
				setStackInSlot(slot, item);
			}
		} else return stack;
		return ItemHandlerHelper.copyStackWithSize(stack, stack.getCount() - m);
	}

	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		ItemStack item = getStackInSlot(slot);
		if (item.getCount() < amount) amount = item.getCount();
		if (amount <= 0) return ItemStack.EMPTY;
		if (!simulate) {
			item.shrink(amount);
			setStackInSlot(slot, item);
		}
		return ItemHandlerHelper.copyStackWithSize(item, amount);
	}

	@Override
	public int getSlotLimit(int slot) {
		return insertAm(slot, ItemStack.EMPTY);
	}

	public int insertAm(int slot, ItemStack item) {
		return Math.min(64, item.getMaxStackSize());
	}

}
