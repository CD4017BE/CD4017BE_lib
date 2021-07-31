package cd4017be.lib.capability;

import net.minecraft.world.item.ItemStack;
import java.util.function.ObjIntConsumer;
import java.util.function.IntFunction;

/**
 * 
 * @author CD4017BE
 */
public class LinkedInventory extends AbstractInventory {

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
	public void setStackInSlot(int slot, ItemStack stack) {
		set.accept(stack, slot);
	}

	@Override
	public int getSlotLimit(int slot) {
		return stackSize;
	}

}
