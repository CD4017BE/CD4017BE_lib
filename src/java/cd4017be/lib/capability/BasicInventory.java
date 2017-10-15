package cd4017be.lib.capability;

import java.util.Arrays;
import java.util.function.ObjIntConsumer;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

/**
 * 
 * @author CD4017BE
 */
public class BasicInventory extends AbstractInventory {

	public final ItemStack[] items;
	public ObjIntConsumer<ItemStack> onModify;
	public Restriction restriction;

	public BasicInventory(int slots) {
		items = new ItemStack[slots];
		restriction = super::insertAm;
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
