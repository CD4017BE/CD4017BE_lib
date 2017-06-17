package cd4017be.lib.templates;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import static net.minecraftforge.items.ItemHandlerHelper.*;

import java.util.function.ObjIntConsumer;
import java.util.function.IntFunction;

public class LinkedInventory implements IItemHandlerModifiable {

	private final int slots, stackSize;
	private final IntFunction<ItemStack> get;
	private final ObjIntConsumer<ItemStack> set;

	public LinkedInventory(int slots, int stackSize, IntFunction<ItemStack> get, ObjIntConsumer<ItemStack> set) {
		this.slots = slots;
		this.stackSize = stackSize;
		this.get = get;
		this.set = set;
	}

	@Override
	public int getSlots() {return slots;}

	@Override
	public ItemStack getStackInSlot(int slot) {return get.apply(slot);}

	@Override
	public ItemStack insertItem(int slot, ItemStack stack, boolean sim) {
		ItemStack item = get.apply(slot);
		if (item == null) {
			int n = stack.getMaxStackSize();
			if (stack.getCount() <= n) {
				if (!sim) set.accept(stack.copy(), slot);
				return null;
			} else {
				if (!sim) set.accept(copyStackWithSize(stack, n), slot);
				return copyStackWithSize(stack, stack.getCount() - n);
			}
		} else if (canItemStacksStack(stack, item)) {
			int n = item.getMaxStackSize() - item.getCount();
			if (n <= 0) return stack;
			else if (stack.getCount() <= n) {
				if (!sim) {
					item.grow(stack.getCount());
					set.accept(item, slot);
				}
				return null;
			} else {
				if (!sim) {
					item.grow(n);
					set.accept(item, slot);
				}
				return copyStackWithSize(stack, stack.getCount() - n);
			}
		} else return stack;
	}

	@Override
	public ItemStack extractItem(int slot, int amount, boolean sim) {
		ItemStack item = get.apply(slot);
		if (item == null) return null;
		if (amount >= item.getCount()) {
			if (!sim) set.accept(null, slot);
			return item;
		} else {
			if (!sim) {
				item.shrink(amount);
				set.accept(item, slot);
			}
			return copyStackWithSize(item, amount);
		}
	}

	@Override
	public void setStackInSlot(int slot, ItemStack stack) {
		set.accept(stack, slot);
	}

	@Override
	public int getSlotLimit(int slot) {
		return stackSize;
	}

}
