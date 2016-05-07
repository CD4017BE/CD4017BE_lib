package cd4017be.api.automation;

import java.util.List;

import cd4017be.lib.util.Utils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.ITextComponent;

public class InventoryItemHandler 
{
	public static interface IItemStorage {
		public int getSizeInventory(ItemStack item);
		public String getInventoryTag();
	}
	
	public static void addInformation(ItemStack item, List list)
    {
        if (isInventoryItem(item)) {
        	IItemStorage inv = (IItemStorage)item.getItem();
        	int n = 0;
            if (item.getTagCompound() != null) {
            	n = item.getTagCompound().getTagList(inv.getInventoryTag(), 10).tagCount();
            }
        	list.add(String.format("Slots: %d / %d used", n, inv.getSizeInventory(item)));
        }
    }
	
	public static ItemStack insertItemStack(ItemStack item, ItemStack stack)
	{
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
			it = ItemStack.loadItemStackFromNBT(tag);
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
			it = ItemStack.loadItemStackFromNBT(tag);
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
	
	public static boolean hasEmptySlot(ItemStack item)
	{
		if (!isInventoryItem(item)) return false;
		else if (item.getTagCompound() == null) return true;
		IItemStorage inv = (IItemStorage)item.getItem();
		return inv.getSizeInventory(item) > item.getTagCompound().getTagList(inv.getInventoryTag(), 10).tagCount();
	}
	
	public static int extractItemStack(ItemStack item, ItemStack stack)
	{
		if (!isInventoryItem(item) || item.getTagCompound() == null || stack == null) return 0;
		IItemStorage inv = (IItemStorage)item.getItem();
		NBTTagList list = item.getTagCompound().getTagList(inv.getInventoryTag(), 10);
		int n = stack.stackSize;
		NBTTagCompound tag;
		ItemStack it;
		for (int i = 0; i < list.tagCount() && n > 0; i++) {
			tag = list.getCompoundTagAt(i);
			it = ItemStack.loadItemStackFromNBT(tag);
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
	
	public static boolean isInventoryItem(ItemStack item)
    {
        return item != null && item.getItem() instanceof IItemStorage;
    }
	
	public static ItemInventory getInventory(ItemStack item)
	{
		if (!isInventoryItem(item)) return null;
		else return new ItemInventory(item);
	}
	
	public static class ItemInventory implements IInventory {
		
		private final int size;
		private ItemStack[] items;
		private ItemStack ref;
		
		private ItemInventory(ItemStack item)
		{
			IItemStorage inv = (IItemStorage)item.getItem();
			this.size = inv.getSizeInventory(item);
			this.load(item);
		}
		
		public void load(ItemStack item)
		{
			this.ref = item;
			items = new ItemStack[size];
			if (item.getTagCompound() == null) return;
			IItemStorage inv = (IItemStorage)item.getItem();
			NBTTagList list = item.getTagCompound().getTagList(inv.getInventoryTag(), 10);
			NBTTagCompound tag;
			for (int i = 0; i < list.tagCount(); i++) {
				tag = list.getCompoundTagAt(i);
				int s = tag.getByte("slot") & 0xff;
				if (s < size) items[s] = ItemStack.loadItemStackFromNBT(tag);
			}
		}
		
		public void save(ItemStack item)
		{
			if (!InventoryItemHandler.isInventoryItem(item)) return;
			if (item.getTagCompound() == null) item.setTagCompound(new NBTTagCompound());
			IItemStorage inv = (IItemStorage)item.getItem();
			NBTTagList list = new NBTTagList();
			NBTTagCompound tag;
			for (int i = 0; i < size; i++) {
				if (items[i] != null) {
					tag = new NBTTagCompound();
					items[i].writeToNBT(tag);
					tag.setByte("slot", (byte)i);
					list.appendTag(tag);
				}
			}
			item.getTagCompound().setTag(inv.getInventoryTag(), list);
		}
		
		@Override
		public int getSizeInventory() 
		{
			return size;
		}

		@Override
		public ItemStack getStackInSlot(int s) 
		{
			return items[s];
		}

		@Override
		public ItemStack decrStackSize(int s, int n) 
		{
			ItemStack stack = items[s];
			if (stack == null) return null;
			else if (n < stack.stackSize) return stack.splitStack(n);
			else {
				items[s] = null;
				return stack;
			}
		}

		@Override
		public ItemStack removeStackFromSlot(int s) 
		{
			ItemStack item = items[s];
			items[s] = null;
			return item;
		}

		@Override
		public void setInventorySlotContents(int s, ItemStack stack) 
		{
			items[s] = stack;
		}

		@Override
		public String getName() 
		{
			return ref.getDisplayName();
		}

		@Override
		public boolean hasCustomName() 
		{
			return true;
		}

		@Override
		public int getInventoryStackLimit() 
		{
			return 64;
		}

		@Override
		public void markDirty() {}

		@Override
		public boolean isUseableByPlayer(EntityPlayer player) 
		{
			return !player.isDead;
		}

		@Override
		public boolean isItemValidForSlot(int s, ItemStack stack) 
		{
			return true;
		}

		@Override
		public ITextComponent getDisplayName() {
			return new TextComponentString(this.getName());
		}

		@Override
		public void openInventory(EntityPlayer player) {}

		@Override
		public void closeInventory(EntityPlayer player) {}

		@Override
		public int getField(int id) {
			return 0;
		}

		@Override
		public void setField(int id, int value) {
		}

		@Override
		public int getFieldCount() {
			return 0;
		}

		@Override
		public void clear() {
			
		}
		
	}
}
