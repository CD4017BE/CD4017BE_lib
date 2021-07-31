package cd4017be.lib.capability;

import java.util.List;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * 
 * @author CD4017BE
 */
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
			if (amount == item.getCount()) setStackInSlot(slot, ItemStack.EMPTY);
			else {
				item.shrink(amount);
				setStackInSlot(slot, item);
			}
		}
		return ItemHandlerHelper.copyStackWithSize(item, amount);
	}

	@Override
	public int getSlotLimit(int slot) {
		return 64;
	}

	public int insertAm(int slot, ItemStack item) {
		return Math.min(getSlotLimit(slot), item.getMaxStackSize());
	}

	public void addToList(List<ItemStack> list) {
		for (int i = 0; i < getSlots(); i++) {
			ItemStack stack = getStackInSlot(i);
			if (!stack.isEmpty()) list.add(stack);
		}
	}

	@Override
	public boolean isItemValid(int slot, ItemStack stack) {
		return insertAm(slot, stack) > 0;
	}

}
