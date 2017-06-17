package cd4017be.api.automation;

import java.util.List;

import cd4017be.lib.templates.InventoryItem.IItemInventory;
import cd4017be.lib.util.Utils;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public class InventoryItemHandler {

	public static interface IItemStorage extends IItemInventory {
		public int getSizeInventory(ItemStack item);
		public String getInventoryTag();
	}

	public static void addInformation(ItemStack item, List<String> list) {
		if (isInventoryItem(item)) {
			IItemStorage inv = (IItemStorage)item.getItem();
			int n = 0;
			if (item.getTagCompound() != null) {
				n = item.getTagCompound().getTagList(inv.getInventoryTag(), 10).tagCount();
			}
			list.add(String.format("Slots: %d / %d used", n, inv.getSizeInventory(item)));
		}
	}

	public static ItemStack insertItemStack(ItemStack item, ItemStack stack) {
		if (!isInventoryItem(item) || stack == null) return stack;
		stack = stack.copy();
		IItemStorage inv = (IItemStorage)item.getItem();
		if (item.getTagCompound() == null) item.setTagCompound(new NBTTagCompound());
		NBTTagList list = item.getTagCompound().getTagList(inv.getInventoryTag(), 10);
		boolean[] emptyS = new boolean[inv.getSizeInventory(item)];
		NBTTagCompound tag;
		int s;
		ItemStack it;
		for (int i = 0; i < list.tagCount() && stack != null; i++) {
			tag = list.getCompoundTagAt(i);
			s = tag.getByte("slot") & 0xff;
			if (s >= emptyS.length) continue;
			emptyS[s] = true;
			it = new ItemStack(tag);
			if (Utils.itemsEqual(stack, it)) {
				if (it.stackSize + stack.stackSize <= it.getMaxStackSize()){
					it.stackSize += stack.stackSize;
					stack = null;
				} else {
					stack.stackSize -= it.getMaxStackSize() - it.stackSize;
					it.stackSize = it.getMaxStackSize();
				}
				it.writeToNBT(tag);
			}
		}
		if (stack != null) {
			s = 0;
			while(emptyS[s]) if (++s >= emptyS.length) return stack;
			NBTTagCompound nbt = new NBTTagCompound();
			stack.writeToNBT(nbt);
			nbt.setByte("slot", (byte)s);
			list.appendTag(nbt);
		}
		return null;
	}

	public static ItemStack[] getItemList(ItemStack item) {
		if (!isInventoryItem(item) || item.getTagCompound() == null) return new ItemStack[0];
		IItemStorage inv = (IItemStorage)item.getItem();
		NBTTagList list = item.getTagCompound().getTagList(inv.getInventoryTag(), 10);
		ItemStack[] buff = new ItemStack[list.tagCount()];
		int n = 0;
		NBTTagCompound tag;
		ItemStack it;
		for (int i = 0; i < buff.length; i++) {
			tag = list.getCompoundTagAt(i);
			it = new ItemStack(tag);
			for (int j = 0; j < n && it != null; j++)
				if (Utils.itemsEqual(buff[j], it)) {
					buff[j].stackSize += it.stackSize;
					it = null;
				}
			if (it != null) buff[n++] = it;
		}
		ItemStack[] ret = new ItemStack[n];
		System.arraycopy(buff, 0, ret, 0, n);
		return ret;
	}

	public static boolean hasEmptySlot(ItemStack item) {
		if (!isInventoryItem(item)) return false;
		else if (item.getTagCompound() == null) return true;
		IItemStorage inv = (IItemStorage)item.getItem();
		return inv.getSizeInventory(item) > item.getTagCompound().getTagList(inv.getInventoryTag(), 10).tagCount();
	}

	public static int extractItemStack(ItemStack item, ItemStack stack) {
		if (!isInventoryItem(item) || item.getTagCompound() == null || stack == null) return 0;
		IItemStorage inv = (IItemStorage)item.getItem();
		NBTTagList list = item.getTagCompound().getTagList(inv.getInventoryTag(), 10);
		int n = stack.stackSize;
		NBTTagCompound tag;
		ItemStack it;
		for (int i = 0; i < list.tagCount() && n > 0; i++) {
			tag = list.getCompoundTagAt(i);
			it = new ItemStack(tag);
			if (Utils.itemsEqual(stack, it)) {
				if (it.stackSize <= n) {
					n -= it.stackSize;
					list.removeTag(i--);
				} else {
					it.stackSize -= n;
					n = 0;
					it.writeToNBT(tag);
				}
			}
		}
		return stack.stackSize - n;
	}

	public static boolean isInventoryItem(ItemStack item) {
		return item != null && item.getItem() instanceof IItemStorage;
	}

}
