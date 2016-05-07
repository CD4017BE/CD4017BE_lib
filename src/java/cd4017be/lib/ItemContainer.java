package cd4017be.lib;

import cd4017be.lib.templates.SlotHolo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ItemContainer extends Container 
{
	public EntityPlayer player;
	public ItemStack type;
	
	public ItemContainer(EntityPlayer player) 
	{
		this.player = player;
		type = player.getHeldItemMainhand();
	}

	protected void addPlayerInventory(int x, int y)
	{
		for (int i = 0; i < 9; i++) {
            this.addSlotToContainer(new Slot(player.inventory, i, x + i * 18, y + 58));
        }
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlotToContainer(new Slot(player.inventory, i * 9 + j + 9, x + j * 18, y + i * 18));
            }
        }
	}
	
	@Override
    public boolean canInteractWith(EntityPlayer player) 
    {
		ItemStack item = player.getHeldItemMainhand();
        return !player.isDead && type != null && item != null && item.getItem() == type.getItem();
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int id) 
    {
    	Slot slot = (Slot)this.inventorySlots.get(id);
        if (slot == null || !slot.getHasStack()) return null;
        ItemStack stack = slot.getStack();
        ItemStack item = stack.copy();
        int[] t = this.stackTransferTarget(item, id);
        if (t != null) {
            if (!this.mergeItemStack(stack, t[0], t[1], false)) return null;
            slot.onSlotChange(stack, item);
        } else return null;
        if (stack.stackSize == 0)
        {
            slot.putStack((ItemStack)null);
        }
        else
        {
            slot.onSlotChanged();
        }
        if (stack.stackSize == item.stackSize)
        {
            return null;
        }
        slot.onPickupFromSlot(player, stack);
        return item;
    }

    protected int[] stackTransferTarget(ItemStack item, int id) {
		return null;
	}

	@Override
    public ItemStack func_184996_a(int s, int b, ClickType m, EntityPlayer par4EntityPlayer)
    {   
		Slot slot = null;
        if (s >= 0 && s < inventorySlots.size()) slot = getSlot(s);
        if (slot != null && slot instanceof SlotHolo) {
            if (m == ClickType.PICKUP) {
            	ItemStack item = slot.getStack();
            	ItemStack curItem = player.inventory.getItemStack();
            	if (curItem != null && slot.isItemValid(curItem)) {
            		if (item != null && item.isItemEqual(curItem)) {
            			item.stackSize += b == 1 ? 1 : curItem.stackSize;
            		} else {
            			curItem = curItem.copy();
                		if (b == 1) curItem.stackSize = 1;
                		slot.putStack(curItem);
            		}
            	} else if (curItem == null && item != null && slot.canTakeStack(player)){
            		slot.decrStackSize(b == 0 ? slot.getSlotStackLimit() : 1);
            	} else {
            		return null;
            	}
                slot.onSlotChanged();
            }
            return null;
        } else return super.func_184996_a(s, b, m, player);
    }
    
    @Override //prevents client crash IndexOutOfBoundsException sometimes caused by incorrect netdata
    public void putStackInSlot(int par1, ItemStack bItemStack) 
    {
        if (par1 >= 0 && par1 < this.inventorySlots.size()) super.putStackInSlot(par1, bItemStack);
    }

    @Override //prevents client crash IndexOutOfBoundsException sometimes caused by incorrect netdata
    public void putStacksInSlots(ItemStack[] par1ArrayOfItemStack) 
    {
        if (par1ArrayOfItemStack.length <= this.inventorySlots.size())super.putStacksInSlots(par1ArrayOfItemStack);
    }
	
}
