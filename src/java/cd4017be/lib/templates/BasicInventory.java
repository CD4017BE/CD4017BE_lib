package cd4017be.lib.templates;

import java.util.Arrays;
import java.util.function.ObjIntConsumer;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;

public class BasicInventory implements IItemHandlerModifiable {

	public final ItemStack[] items;
	public ObjIntConsumer<ItemStack> onModify;
	public Restriction restriction;

	public BasicInventory(int slots) {
		items = new ItemStack[slots];
		restriction = BasicInventory::insertAmount;
		Arrays.fill(items, ItemStack.EMPTY);
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
	public ItemStack insertItem(int i, ItemStack stack, boolean sim) {
		ItemStack item = items[i];
		int n = item.getCount();
		int m = Math.min(restriction.insertAmount(i, stack) - n, stack.getCount()); 
		if (m <= 0 || !(n == 0 || ItemHandlerHelper.canItemStacksStack(item, stack))) return stack;
		if (!sim) {
			if (n == 0) item = ItemHandlerHelper.copyStackWithSize(stack, m);
			else item.grow(m);
			if (onModify != null) onModify.accept(item, i);
			items[i] = item;
		}
		return ItemHandlerHelper.copyStackWithSize(stack, stack.getCount() - m);
	}

	@Override
	public ItemStack extractItem(int i, int m, boolean sim) {
		ItemStack item = items[i];
		if (item.getCount() < m) m = item.getCount();
		if (m <= 0) return ItemStack.EMPTY;
		if (!sim) {
			item.shrink(m);
			if (onModify != null) onModify.accept(item, m);
		}
		return ItemHandlerHelper.copyStackWithSize(item, m);
	}

	@Override
	public void setStackInSlot(int slot, ItemStack stack) {
		if (onModify != null) onModify.accept(stack, slot);
		items[slot] = stack;
	}

	@Override
	public int getSlotLimit(int slot) {
		return 64;
	}

	public static int insertAmount(int slot, ItemStack item) {
		return Math.min(64, item.getMaxStackSize());
	}

	public void read(NBTTagList list) {
		Arrays.fill(items, ItemStack.EMPTY);
		for (NBTBase nbt : list) {
			NBTTagCompound tag = (NBTTagCompound)nbt;
			int slot = tag.getByte("Slot") & 0xff;
			if (slot < items.length)
				items[slot] = new ItemStack(tag);
		}
	}

	public NBTTagList write() {
		NBTTagList list = new NBTTagList();
		for (int i = 0; i < items.length; i++) {
			ItemStack item = items[i];
			if (item.isEmpty()) continue;
			NBTTagCompound tag = item.writeToNBT(new NBTTagCompound());
			tag.setByte("Slot", (byte)i);
			list.appendTag(tag);
		}
		return list;
	}

	@FunctionalInterface
	public interface Restriction {
		int insertAmount(int slot, ItemStack item);
	}

}
