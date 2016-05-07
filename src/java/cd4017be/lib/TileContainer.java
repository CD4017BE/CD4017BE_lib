/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cd4017be.lib;

import cd4017be.lib.templates.SlotHolo;

import java.io.IOException;
import java.util.BitSet;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;

/**
 *
 * @author CD4017BE
 */
public class TileContainer extends Container
{
    
    public ModTileEntity tileEntity;
    public EntityPlayer player;
    protected int invPlayerPos = 0;
    protected int invPlayerSize = 0;
    public TileEntityData refData;
    
    public TileContainer(ModTileEntity tileEntity, EntityPlayer player)
    {
        this.tileEntity = tileEntity;
        this.player = player;
        tileEntity.initContainer(this);
    }
    
    @Override
    public boolean canInteractWith(EntityPlayer player) 
    {
        return !tileEntity.isInvalid() && tileEntity.isUseableByPlayer(player);
    }
    
    public int[] getPlayerInv()
    {
        return new int[]{invPlayerPos, invPlayerPos + invPlayerSize};
    }
    
    public void addPlayerInventory(int x, int y)
    {
        this.invPlayerPos = this.inventorySlots.size();
        this.invPlayerSize = 36;
        for (int i = 0; i < 3; i++)
        {
            for (int j = 0; j < 9; j++)
            {
                this.addSlotToContainer(new Slot(player.inventory, i * 9 + j + 9, x + j * 18, y + i * 18));
            }
        }
        for (int i = 0; i < 9; i++)
        {
            this.addSlotToContainer(new Slot(player.inventory, i, x + i * 18, y + 58));
        }
    }
    
    public Slot addEntitySlot(Slot slot)
    {
        return this.addSlotToContainer(slot);
    }
    
    @Override
    public boolean mergeItemStack(ItemStack item, int ss, int se, boolean d)
    {
        return super.mergeItemStack(item, ss, se, d);
    }
    
    @Override
	public void onCraftGuiOpened(ICrafting crafting) {
		super.onCraftGuiOpened(crafting);
		tileEntity.addCraftingToCrafters(this, crafting);
	}

    @Override
    public void updateProgressBar(int var, int val) 
    {
        tileEntity.updateProgressBar(var, val);
    }
    
    public void onDataUpdate(PacketBuffer dis) throws IOException
    {
        tileEntity.updateNetData(dis, this);
    }

    @Override
    public void detectAndSendChanges() 
    {
        try {
            PacketBuffer data = this.tileEntity.getPacketTargetData();
            boolean send = false;
            if (tileEntity.netData != null) {
            	BitSet chng;
            	if (refData == null) {
            		chng = tileEntity.netData.detectChanges(refData = new TileEntityData(tileEntity.netData), true);
            	} else chng = tileEntity.netData.detectChanges(refData, false);
                tileEntity.netData.writeData(data, chng);
                send = !chng.isEmpty();
            }
            send |= tileEntity.detectAndSendChanges(this, crafters, data);
            if (send) for (int i = 0; i < this.crafters.size(); i++) {
                EntityPlayerMP crafter = (EntityPlayerMP)this.crafters.get(i);
                BlockGuiHandler.sendPacketToPlayer(crafter, data);
            }
        } catch (IOException e) {e.printStackTrace();}
        super.detectAndSendChanges();
    }
    
    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int id) 
    {
        return tileEntity.transferStackInSlot(this, player, id);
    }

    @Override
    public ItemStack func_184996_a(int s, int b, ClickType m, EntityPlayer player) 
    {
        return tileEntity.slotClick(this, s, b, m, player);
    }
    
    public ItemStack standartSlotClick(int s, int b, ClickType m, EntityPlayer player)
    {
        Slot slot = null;
        if (s >= 0 && s < inventorySlots.size()) slot = getSlot(s);
        if (slot != null && slot instanceof SlotHolo)
        {
            InventoryPlayer var6 = player.inventory;
            ItemStack var8;
            int var10;
            ItemStack var11;
            if (b == 0 || b == 1)
            {
                if (m == ClickType.PICKUP && slot.canTakeStack(player))
                {
                    slot.decrStackSize(slot.getSlotStackLimit());
                }
                else if (m == ClickType.PICKUP_ALL)
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
        } else
        {
            return super.func_184996_a(s, b, m, player);
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
