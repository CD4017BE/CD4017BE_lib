/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cd4017be.lib.templates;

import cd4017be.api.automation.IItemPipeCon;
import cd4017be.lib.ModTileEntity;
import cd4017be.lib.util.Utils;
import cd4017be.lib.util.Utils.ItemType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IChatComponent;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

/**
 *
 * @author CD4017BE
 */
public class Inventory implements ISidedInventory
{
    public int netIdxLong = 0;
    private ModTileEntity tile;
    public ItemStack[] items;
    public final Component[] componets;
    public String invName;
    
    public static class Component
    {
        public final int s;
        public final int e;
        public final byte d;
        public boolean invChange = true;
        public Component(int s, int e, int d)
        {
            this.s = s;
            this.e = e;
            this.d = (byte)d;
        }
        
        public int[] slots()
        {
        	int[] slots = new int[e - s];
        	for (int i = 0; i < slots.length; i++) slots[i] = s + i;
        	return slots;
        }
    }
    
    public Inventory(ModTileEntity tile, int l, Component... cmp)
    {
        this.tile = tile;
        this.componets = cmp == null ? new Component[0] : cmp;
        this.items = new ItemStack[l];
    }
    
    public Inventory setInvName(String name)
    {
        this.invName = name;
        return this;
    }
    
    public Inventory setNetLong(int idx)
    {
        this.netIdxLong = idx;
        return this; 
    }
    
    public void update()
    {
        if (tile.netData.longs[netIdxLong] == 0) return;
        for (int i = 0; i < this.componets.length; i++) {
            if (this.componets[i].d == 0) continue;
            for (int s = 0; s < 6; s++) {
            	byte m = this.getConfig(tile.netData.longs[netIdxLong], s, i);
            	if (m < 2 || ((this.componets[i].d > 0 ^ m == 3) && !(tile instanceof IItemPipeCon))) continue;
            	TileEntity te = Utils.getTileOnSide(tile, (byte)s);
                if (te == null || !(te instanceof IInventory)) continue;
                IInventory inv = (IInventory)te;
                if ((this.componets[i].d > 0 ^ m == 3) && !(te instanceof IPipe)) continue;
                if (m == 3) {
                    Utils.transfer(this, s, componets[i].slots(), inv, s^1, Utils.accessibleSlots(inv, s^1), new ItemType());
                    //int[] src = findItemStack(this, s, null, 64, true);
                    //if (src.length == 0) break;
                    //int[] dst = findPlaceFor(inv, s^1, this.items[src[0]], 64, true);
                    //if (dst.length == 0) break;
                    //transferItems(this, src, 64, (IInventory)te, dst);
                } else {
                    Utils.transfer(inv, s^1, Utils.accessibleSlots(inv, s^1), this, s, componets[i].slots(), new ItemType());
                    //int[] src = findItemStack(inv, getSlots(inv, s^1), s^1, null, 64, true);
                    //if (src.length == 0) break;
                    //int[] dst = findPlaceFor(this, s, ((IInventory)te).getStackInSlot(src[0]), 64, true);
                    //if (dst.length == 0) break;
                    //transferItems((IInventory)te, src, 64, this, dst);
                }
            }
        }
    }
    
    public void writeToNBT(NBTTagCompound nbt, String name)
    {
        NBTTagList list = new NBTTagList();
        for (int s = 0; s < items.length; s++)
        {
            if (items[s] != null)
            {
                NBTTagCompound item = new NBTTagCompound();
                item.setByte("Slot", (byte)s);
                items[s].writeToNBT(item);
                list.appendTag(item);
            }
        }
        nbt.setTag(name, list);
    }
    
    public void readFromNBT(NBTTagCompound nbt, String name)
    {
        NBTTagList list = nbt.getTagList(name, 10);
        for (int id = 0; id < list.tagCount(); ++id)
        {
            NBTTagCompound item = list.getCompoundTagAt(id);
            byte s = item.getByte("Slot");
            if (s >= 0 && s < items.length)
            {
                items[s] = ItemStack.loadItemStackFromNBT(item);
            }
        }
    }
    
    public static int[] getSlots(IInventory inv, int s)
    {
        if (inv instanceof ISidedInventory) {
            return ((ISidedInventory)inv).getSlotsForFace(EnumFacing.VALUES[s]);
        } else {
            int[] array = new int[inv.getSizeInventory()];
            for (int i = 0; i < array.length; i++) array[i] = i;
            return array;
        }
    }
    
    public static int[] findItemStack(IInventory inv, int[] slots, int s, ItemStack type, int n, boolean check)
    {
        boolean sided = check && inv instanceof ISidedInventory;
        int[] array = new int[slots.length];
        int i = 0;
        for (int slot : slots) {
            ItemStack stack = inv.getStackInSlot(slot);
            if (stack != null && (type == null || Utils.itemsEqual(stack, type)) && (!sided || ((ISidedInventory)inv).canExtractItem(slot, stack, EnumFacing.VALUES[s]))) {
                array[i++] = slot;
                if (type == null) type = stack;
                if ((n -= stack.stackSize) <= 0) break;
            }
        }
        slots = new int[i];
        System.arraycopy(array, 0, slots, 0, i);
        return slots;
    }
    
    public static int[] findPlaceFor(IInventory inv, int s, ItemStack item, int n, boolean check)
    {
        if (item == null) return new int[0];
        item = item.copy();
        boolean sided = check && inv instanceof ISidedInventory;
        int[] slots = getSlots(inv, s);
        int[] array = new int[slots.length];
        boolean hasnull = false;
        int i = 0;
        for (int slot : slots) {
            ItemStack stack = inv.getStackInSlot(slot);
            if (stack != null && Utils.itemsEqual(stack, item) && (!sided || ((ISidedInventory)inv).canInsertItem(slot, item, EnumFacing.VALUES[s]))){
                int m = Math.min(inv.getInventoryStackLimit(), stack.getMaxStackSize()) - stack.stackSize;
                if (m <= 0) continue;
                array[i++] = slot;
                if ((n -= m) <= 0) break;
            } else if (stack == null)hasnull = true;
        }
        if (hasnull && n > 0)
        for (int slot : slots) {
            ItemStack stack = inv.getStackInSlot(slot);
            if (stack == null && (!sided || ((ISidedInventory)inv).canInsertItem(slot, item, EnumFacing.VALUES[s]))) {
                array[i++] = slot;
                if ((n -= inv.getInventoryStackLimit()) <= 0) break; 
            }
        }
        slots = new int[i];
        System.arraycopy(array, 0, slots, 0, i);
        return slots;
    }
    
    public static void transferItems(IInventory srcI, int[] srcS, int n, IInventory dstI, int[] dstS)
    {
        int i = 0;
        for (int slot : dstS) {
            ItemStack stack = dstI.getStackInSlot(slot);
            int m = stack == null ? dstI.getInventoryStackLimit() : Math.min(dstI.getInventoryStackLimit(), stack.getMaxStackSize()) - stack.stackSize;
            while (m > 0 && n > 0) {
                ItemStack item = srcI.decrStackSize(srcS[i], Math.min(m, n));
                if (item == null) {
                    if (++i >= srcS.length) n = 0;
                    continue;
                }
                if (stack == null){
                    stack = item;
                    m = Math.min(m, stack.getMaxStackSize());
                } else stack.stackSize += item.stackSize;
                m -= item.stackSize;
                n -= item.stackSize;
            }
            dstI.setInventorySlotContents(slot, stack);
            if (n <= 0) break;
        }
        srcI.markDirty();
        dstI.markDirty();
    }
    
    public byte getConfig(long cfg, int s, int id)
    {
        return (byte)(cfg >> (2 * s + 12 * id) & 3);
    }
    
    @Override
    public int getSizeInventory() 
    {
        return items.length;
    }

    @Override
    public ItemStack getStackInSlot(int i) 
    {
        return items[i];
    }

    @Override
    public void setInventorySlotContents(int i, ItemStack itemstack) 
    {
        ItemStack old = items[i];
        items[i] = itemstack;
        this.slotChange(old, items[i], i);
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }
    
    private void slotChange(ItemStack oldItem, ItemStack newItem, int slot)
    {
        if (tile instanceof IAutomatedInv) ((IAutomatedInv)tile).slotChange(oldItem, newItem, slot);
        for (Component cmp : this.componets)
            if (slot >= cmp.s && slot < cmp.e)
            {
                cmp.invChange = true;
                return;
            }
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer entityplayer) {return true;}
    
    @Override
    public int[] getSlotsForFace(EnumFacing side) 
    {
        int[] array = new int[items.length];
        int n = 0;
        for (int i = 0; i < this.componets.length; i++) {
            byte m = this.getConfig(tile.netData.longs[netIdxLong], side.getIndex(), i);
            if (m != 0)
            for (int j = this.componets[i].s; j < this.componets[i].e; j++)
            array[n++] = j;
        }
        int[] slots = new int[n];
        System.arraycopy(array, 0, slots, 0, n);
        return slots;
    }

    @Override
    public boolean canInsertItem(int i, ItemStack itemstack, EnumFacing s) 
    {
        for (int j = 0; j < this.componets.length; j++) {
            if (i >= this.componets[j].s && i < this.componets[j].e) {
                byte m = this.getConfig(tile.netData.longs[netIdxLong], s.getIndex(), j);
                return m == 1 || m == 2 && (tile instanceof IAutomatedInv ? ((IAutomatedInv)tile).canInsert(itemstack, j, i) : true);
            }
        }
        return false;
    }

    @Override
    public boolean canExtractItem(int i, ItemStack itemstack, EnumFacing s) 
    {
        for (int j = 0; j < this.componets.length; j++) {
            if (i >= this.componets[j].s && i < this.componets[j].e) {
                byte m = this.getConfig(tile.netData.longs[netIdxLong], s.getIndex(), j);
                return m == 1 || m == 3 && (tile instanceof IAutomatedInv ? ((IAutomatedInv)tile).canExtract(itemstack, j, i) : true);
            }
        }
        return false;
    }
    
    @Override
    public boolean isItemValidForSlot(int i, ItemStack itemstack) 
    {
        for (int j = 0; j < this.componets.length; j++) {
            if (i >= this.componets[j].s && i < this.componets[j].e) {
                return tile instanceof IAutomatedInv ? ((IAutomatedInv)tile).isValid(itemstack, j, i) : true;
            }
        }
        return false;
    }

    @Override
    public ItemStack decrStackSize(int i, int n) 
    {
        if (items[i] == null) return null;
        ItemStack old = items[i].copy();
        ItemStack ret;
        if (n < items[i].stackSize) ret = items[i].splitStack(n);
        else {
            ret = items[i];
            items[i] = null;
        }
        this.slotChange(old, items[i], i);
        return ret;
    }

    @Override
    public ItemStack removeStackFromSlot(int i) 
    {
        ItemStack item = items[i];
        items[i] = null;
        return item;
    }

	@Override
	public String getName() {
		return invName;
	}

	@Override
	public boolean hasCustomName() {
		return true;
	}

	@Override
	public void markDirty() {}

	@Override
	public void openInventory(EntityPlayer player) {}

	@Override
	public void closeInventory(EntityPlayer player) {}

	@Override
	public int getField(int id) {
		return 0;
	}

	@Override
	public void setField(int id, int value) {}

	@Override
	public int getFieldCount() {
		return 0;
	}

	@Override
	public void clear() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public IChatComponent getDisplayName() {
		return new ChatComponentText(this.getName());
	}
    
}
