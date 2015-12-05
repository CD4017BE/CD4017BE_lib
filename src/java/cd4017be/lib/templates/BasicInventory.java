package cd4017be.lib.templates;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

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
	public ItemStack getStackInSlotOnClosing(int s) 
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
	public String getInventoryName() 
	{
		return null;
	}

	@Override
	public boolean hasCustomInventoryName() 
	{
		return false;
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
	public void openInventory() {}

	@Override
	public void closeInventory() {}

	@Override
	public boolean isItemValidForSlot(int s, ItemStack item) 
	{
		return true;
	}

}
