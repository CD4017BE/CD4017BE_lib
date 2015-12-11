/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cd4017be.lib;

import cd4017be.lib.TileBlockRegistry.TileBlockEntry;
import cd4017be.lib.util.Utils;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;

/**
 *
 * @author CD4017BE
 */
public class ModTileEntity extends TileEntity
{
    public TileEntityData netData;
    
    public boolean onActivated(EntityPlayer player, int s, float X, float Y, float Z)
    {
        TileBlockEntry entry = TileBlockRegistry.getBlockEntry(this.worldObj.getBlock(this.xCoord, this.yCoord, this.zCoord));
        if (entry != null && entry.container != null && !player.isSneaking()) {
            BlockGuiHandler.openGui(player, this.worldObj, this.xCoord, this.yCoord, this.zCoord);
            return true;
        } else return false;
    }
    
    public void onClicked(EntityPlayer player) {}
    
    public void onNeighborBlockChange(Block b) {}
    
    public void onNeighborTileChange(int tx, int ty, int tz) {}
    
    public void breakBlock()
    {
        if (this instanceof IInventory)
        {
            IInventory inv = (IInventory)this;
            for (int i = 0; i < inv.getSizeInventory(); i++)
            {
                ItemStack item = inv.getStackInSlotOnClosing(i);
                if (item == null) continue;
                EntityItem entity = new EntityItem(worldObj, xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D, item);
                worldObj.spawnEntityInWorld(entity);
            }
        }
    }
    
    public int redstoneLevel(int s, boolean str)
    {
        return 0;
    }
    
    public void onPlaced(EntityLivingBase entity, ItemStack item) {}
    
    public void onEntityCollided(Entity entity) {}
    
    public ArrayList<ItemStack> dropItem(int m, int fortune)
    {
        return new ArrayList();
    }
    
    public TileEntity getLoadedTile(int x, int y, int z)
    {
        if (!worldObj.blockExists(x, y, z)) return null;
        else return worldObj.getTileEntity(x, y, z);
    }
    
    public void markUpdate()
    {
    	this.worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }
    
    public void writeItemsToNBT(NBTTagCompound nbt, String name, ItemStack[] items)
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
    
    public ItemStack[] readItemsFromNBT(NBTTagCompound nbt, String name, int l)
    {
        NBTTagList list = nbt.getTagList(name, 10);
        ItemStack[] items = new ItemStack[l];
        for (int id = 0; id < list.tagCount(); ++id)
        {
            NBTTagCompound item = list.getCompoundTagAt(id);
            byte s = item.getByte("Slot");
            if (s >= 0 && s < l)
            {
                items[s] = ItemStack.loadItemStackFromNBT(item);
            }
        }
        return items;
    }
    
    public void onPlayerCommand(DataInputStream dis, EntityPlayerMP player) throws IOException
    {
        
    }
    
    public ByteArrayOutputStream getPacketTargetData() throws IOException
    {
        return BlockGuiHandler.getPacketTargetData(xCoord, yCoord, zCoord);
    }
    
    @Deprecated //use redstoneLevel(int s, boolean str) instead;
    public boolean isProvidingPower(int s, boolean strong)
    {
        return false;
    }
    
    public boolean isUseableByPlayer(EntityPlayer player) 
    {
        return true;
    }
    
    public void initContainer(TileContainer container)
    {
        
    }
    
    public void addCraftingToCrafters(TileContainer container, ICrafting crafting)
    {
        
    }
    
    public void updateProgressBar(int var, int val)
    {
        
    }
    
    public void updateNetData(DataInputStream dis, TileContainer container) throws IOException
    {
        if (this.netData != null) 
        {
        	if (container.refData == null) {
        		container.refData = new TileEntityData(this.netData);
        		this.netData = container.refData;
        	}
        	container.refData.readData(dis);
        }
    }
    
    public boolean detectAndSendChanges(TileContainer container, List<ICrafting> crafters, DataOutputStream dos) throws IOException 
    {
        return false;
    }
    
    public ItemStack transferStackInSlot(TileContainer container, EntityPlayer player, int id) 
    {
        Slot slot = (Slot)container.inventorySlots.get(id);
        if (slot == null || !slot.getHasStack()) return null;
        ItemStack stack = slot.getStack();
        ItemStack item = stack.copy();
        int[] t = this.stackTransferTarget(item, id, container);
        if (t != null) {
            if (!container.mergeItemStack(stack, t[0], t[1], false)) return null;
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
    
    public int[] stackTransferTarget(ItemStack item, int s, TileContainer container)
    {
        int[] pi = container.getPlayerInv();
        if (s < pi[0] || s >= pi[1]) return pi;
        else return null;
    }
    
    public ItemStack slotClick(TileContainer container, int s, int b, int m, EntityPlayer player) 
    {
        return container.standartSlotClick(s, b, m, player);
    }
    
    protected int readIntFromShort(int v, int s, boolean top)
    {
        return top ? (v & 0x0000ffff) | (s << 16) : (v & 0xffff0000) | s;
    }
    
    protected int writeIntToShort(int v, boolean top)
    {
        return top ? v >> 16 : v & 0xffff;
    }
    
    protected ItemStack putItemStack(ItemStack item, IInventory inv, int bs, int... s)
    {
        if (item == null) return null;
        int mss = item.getMaxStackSize() < inv.getInventoryStackLimit() ? item.getMaxStackSize() : inv.getInventoryStackLimit();
        boolean canCheck = bs >= 0 && inv instanceof ISidedInventory;
        boolean hasEmpty = false;
        for (int i : s)
        {
            if (canCheck && !((ISidedInventory)inv).canInsertItem(i, item, bs)) continue;
            ItemStack stack = inv.getStackInSlot(i);
            hasEmpty |= stack == null;
            if (stack != null && Utils.itemsEqual(stack, item))
            {
                int n = mss - stack.stackSize;
                if (n >= item.stackSize)
                {
                    stack.stackSize += item.stackSize;
                    inv.setInventorySlotContents(i, stack);
                    return null;
                } else
                {
                    stack.stackSize += n;
                    item.stackSize -= n;
                    inv.setInventorySlotContents(i, stack);
                }
            }
        }
        if (!hasEmpty) return item;
        for (int i : s)
        if (inv.getStackInSlot(i) == null)
        {
            if (canCheck && !((ISidedInventory)inv).canInsertItem(i, item, bs)) continue;
            if (item.stackSize <= mss)
            {
                inv.setInventorySlotContents(i, item);
                return null;
            } else
            {
                ItemStack stack = item.copy();
                stack.stackSize = mss;
                inv.setInventorySlotContents(i, stack);
                item.stackSize -= mss;
            }
        }
        return item;
    }
    
    protected int getItemStack(ItemStack item, ItemStack[] inv, int s, int e, boolean ore)
    {
        if (item == null) return -1;
        if (s < 0) s = 0;
        if (e > inv.length) e = inv.length;
        for (int i = s; i < e; i++)
        {
            ItemStack stack = inv[i];
            if (stack != null && (ore ? Utils.oresEqual(stack, item) : Utils.itemsEqual(stack, item))) return i;
        }
        return -1;
    }
    
    public byte getOrientation()
    {
        return (byte)(worldObj.getBlockMetadata(xCoord, yCoord, zCoord) % 6);
    }
    
    public String getInventoryName() 
    {
    	return StatCollector.translateToLocal(this.getBlockType().getUnlocalizedName().replaceFirst("tile.", "gui.cd4017be."));
    }
    
}
