package cd4017be.lib;

import cd4017be.lib.templates.SlotHolo;
import cd4017be.lib.util.Utils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
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
		type = player.getCurrentEquippedItem();
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
		ItemStack item = player.getCurrentEquippedItem();
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
    public ItemStack slotClick(int s, int b, int m, EntityPlayer par4EntityPlayer)
    {   
    	Slot slot = null;
        if (s >= 0 && s < inventorySlots.size()) slot = getSlot(s);
		if (slot != null && slot.inventory instanceof InventoryPlayer && slot.getSlotIndex() == par4EntityPlayer.inventory.currentItem) return null;
		else if (slot != null && slot instanceof SlotHolo) {
            InventoryPlayer var6 = player.inventory;
            ItemStack var8;
            int var10;
            ItemStack var11;
            if (b == 0 || b == 1)
            {
                if (m == 1 && slot.canTakeStack(player))
                {
                    slot.decrStackSize(slot.getSlotStackLimit());
                }
                else if (m == 0)
                {
                    var8 = slot.getStack();
                    ItemStack var13 = var6.getItemStack();
                    if (var8 == null)
                    {
                        if (var13 != null && slot.isItemValid(var13))
                        {
                            var10 = b == 0 ? var13.stackSize : 1;
                            if (var10 > slot.getSlotStackLimit()) var10 = slot.getSlotStackLimit();
                            var11 = var13.copy();
                            var11.stackSize = var10;
                            slot.putStack(var11);
                        }
                    }
                    else if (slot.canTakeStack(player))
                    {
                        if (var13 == null)
                        {
                            var10 = b == 0 ? var8.stackSize : 1;
                            slot.decrStackSize(var10);
                        }
                        else if (slot.isItemValid(var13))
                        {
                            if (var8.getItem() == var13.getItem() && var8.getItemDamage() == var13.getItemDamage() && ItemStack.areItemStackTagsEqual(var8, var13))
                            {
                                var10 = b == 0 ? var13.stackSize : 1;
                                if (var10 > slot.getSlotStackLimit() - var8.stackSize) var10 = slot.getSlotStackLimit() - var8.stackSize;
                                var8.stackSize += var10;
                            }
                            else
                            {
                                ItemStack var14 = var13.copy();
                                if (var14.stackSize > slot.getSlotStackLimit()) var14.stackSize = slot.getSlotStackLimit();
                                slot.putStack(var14);
                            }
                        }
                    }
                    slot.onSlotChanged();
                }
            }
            return null;
        } else {
            return super.slotClick(s, b, m, player);
        }
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
