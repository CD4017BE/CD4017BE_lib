/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cd4017be.lib;

import cd4017be.lib.templates.SlotHolo;
import cd4017be.lib.templates.TankContainer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
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
    public ArrayList<TankSlot> tankSlots = new ArrayList<TankSlot>();
    
    public static class TankSlot {
    	public final int xDisplayPosition, yDisplayPosition, tankNumber;
    	public final boolean bigSize;
    	public final TankContainer inventory;
    	public TankSlot(TankContainer inv, int id, int x, int y, boolean big) {
    		this.inventory = inv;
    		this.tankNumber = id;
    		this.xDisplayPosition = x;
    		this.yDisplayPosition = y;
    		this.bigSize = big;
    	}
    }
    
    public TileContainer(ModTileEntity tileEntity, EntityPlayer player)
    {
        this.tileEntity = tileEntity;
        this.player = player;
        tileEntity.initContainer(this);
    }
    
    @Override
    public boolean canInteractWith(EntityPlayer player) 
    {
        return tileEntity.canPlayerAccessUI(player);
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
    
    public void addTankSlot(TankSlot slot) {
    	this.tankSlots.add(slot);
    }
    
    @Override
    public boolean mergeItemStack(ItemStack item, int ss, int se, boolean d)
    {
        return super.mergeItemStack(item, ss, se, d);
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
            send |= tileEntity.detectAndSendChanges(this, this.listeners, data);
            if (send) for (IContainerListener crafter : this.listeners) {
                BlockGuiHandler.sendPacketToPlayer((EntityPlayerMP)crafter, data);
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
    public ItemStack slotClick(int s, int b, ClickType m, EntityPlayer player) 
    {
        return tileEntity.slotClick(this, s, b, m, player);
    }
    
    public ItemStack standartSlotClick(int s, int b, ClickType m, EntityPlayer player)
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
        } else return super.slotClick(s, b, m, player); 
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
