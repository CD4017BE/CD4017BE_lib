package cd4017be.lib.capability;

import java.util.Arrays;
import java.util.function.ObjIntConsumer;

import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

/**
 * 
 * @author CD4017BE
 */
public class BasicInventory extends AbstractInventory {

	public final ItemStack[] items;
	public ObjIntConsumer<ItemStack> onModify;
	public Restriction restriction = super::insertAm;

	public BasicInventory(int slots) {
		this.items = new ItemStack[slots];
		Arrays.fill(items, ItemStack.EMPTY);
	}

	public BasicInventory(ItemStack[] items) {
		this.items = items;
	}

	@Override
	public ItemStack getStackInSlot(int s) {
		return items[s];
	}

	@Override
	public int getSlots() {
		return items.length;
	}

	@Override
	public void setStackInSlot(int slot, ItemStack stack) {
		if (onModify != null) onModify.accept(stack, slot);
		items[slot] = stack;
	}

	@Override
	public int insertAm(int slot, ItemStack item) {
		return restriction.insertAmount(slot, item);
	}

	public static int insertAmount(int slot, ItemStack item) {
		return Math.min(64, item.getMaxStackSize());
	}

	public void read(ListTag list) {
		Arrays.fill(items, ItemStack.EMPTY);
		for (Tag nbt : list) {
			CompoundTag tag = (CompoundTag)nbt;
			int slot = tag.getByte("Slot") & 0xff;
			if (slot < items.length)
				items[slot] = ItemStack.of(tag);
		}
	}

	public ListTag write() {
		ListTag list = new ListTag();
		for (int i = 0; i < items.length; i++) {
			ItemStack item = items[i];
			if (item.isEmpty()) continue;
			CompoundTag tag = item.save(new CompoundTag());
			tag.putByte("Slot", (byte)i);
			list.add(tag);
		}
		return list;
	}

	@FunctionalInterface
	public interface Restriction {
		int insertAmount(int slot, ItemStack item);
	}

}
