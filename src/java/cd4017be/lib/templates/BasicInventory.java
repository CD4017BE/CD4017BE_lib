package cd4017be.lib.templates;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IChatComponent;

public class BasicInventory implements IInventory 
{

	public ItemStack[] items;
	
	public BasicInventory(int slots)
	{
		items = new ItemStack[slots];
	}
	
	@Override
	public int getSizeInventory() 
	{
		return items.length;
	}

	@Override
	public ItemStack getStackInSlot(int s) 
	{
		return items[s];
	}

	@Override
	public ItemStack decrStackSize(int s, int n) 
	{
		if (items[s] == null) return null;
		else if (items[s].stackSize <= n) {
			ItemStack item = items[s];
			items[s] = null;
			return item;
		} else return items[s].splitStack(n);
	}

	@Override
	public ItemStack removeStackFromSlot(int s) 
	{
		ItemStack item = items[s];
		items[s] = null;
		return item;
	}

	@Override
	public void setInventorySlotContents(int s, ItemStack item) 
	{
		items[s] = item;
	}

	@Override
	public IChatComponent getDisplayName() 
	{
		return null;
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
	public boolean isItemValidForSlot(int s, ItemStack item) 
	{
		return true;
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public boolean hasCustomName() {
		return false;
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
