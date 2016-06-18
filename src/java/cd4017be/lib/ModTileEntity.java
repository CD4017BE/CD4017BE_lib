/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cd4017be.lib;

import cd4017be.lib.TileBlockRegistry.TileBlockEntry;
import cd4017be.lib.util.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;

/**
 *
 * @author CD4017BE
 */
public class ModTileEntity extends TileEntity
{
    public TileEntityData netData;
    
    public int dimensionId;
    
    @Override
	public NBTTagCompound getUpdateTag() {
		return this.writeToNBT(new NBTTagCompound());
	}

	public boolean onActivated(EntityPlayer player, EnumHand hand, ItemStack item, EnumFacing s, float X, float Y, float Z)
    {
        TileBlockEntry entry = TileBlockRegistry.getBlockEntry(this.getBlockType());
        if (entry != null && entry.container != null && !player.isSneaking()) {
            BlockGuiHandler.openGui(player, this.worldObj, pos.getX(), pos.getY(), pos.getZ());
            return true;
        } else return false;
    }
    
    public EnumFacing getClickedSide(float X, float Y, float Z) {
    	X -= 0.5F;
        Y -= 0.5F;
        Z -= 0.5F;
        float dx = Math.abs(X);
        float dy = Math.abs(Y);
        float dz = Math.abs(Z);
        return dy > dz && dy > dx ? Y < 0 ? EnumFacing.DOWN : EnumFacing.UP : dz > dx ? Z < 0 ? EnumFacing.NORTH : EnumFacing.SOUTH : X < 0 ? EnumFacing.WEST : EnumFacing.EAST;
    }
    
    public void onClicked(EntityPlayer player) {}
    
    public void onNeighborBlockChange(Block b) {}
    
    public void onNeighborTileChange(BlockPos pos) {}
    
    public void breakBlock()
    {
        if (this instanceof IInventory) {
            IInventory inv = (IInventory)this;
            for (int i = 0; i < inv.getSizeInventory(); i++) {
                ItemStack item = inv.removeStackFromSlot(i);
                if (item != null) this.dropStack(item);
            }
        }
    }
    
    public int redstoneLevel(int s, boolean str)
    {
        return 0;
    }
    
    public void onPlaced(EntityLivingBase entity, ItemStack item) {}
    
    public void onEntityCollided(Entity entity) {}
    
    public ArrayList<ItemStack> dropItem(IBlockState state, int fortune)
    {
        return new ArrayList<ItemStack>();
    }
    
    public TileEntity getLoadedTile(BlockPos pos)
    {
        if (!worldObj.isBlockLoaded(pos)) return null;
        else return worldObj.getTileEntity(pos);
    }
    
    public void markUpdate()
    {
    	IBlockState state = worldObj.getBlockState(pos);
    	this.worldObj.notifyBlockUpdate(pos, state, state, 3);
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
    
    @Override
	public void setWorldObj(World world) {
		super.setWorldObj(world);
		this.dimensionId = world.provider.getDimension();
	}

	public void onPlayerCommand(PacketBuffer data, EntityPlayerMP player) throws IOException
    {
        
    }
    
    public PacketBuffer getPacketTargetData()
    {
        return BlockGuiHandler.getPacketTargetData(pos);
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
    
    public void updateProgressBar(int var, int val)
    {
        
    }
    
    public void updateNetData(PacketBuffer dis, TileContainer container) throws IOException
    {
        if (this.netData != null) this.netData.readData(dis);
    }
    
    public boolean detectAndSendChanges(TileContainer container, List<IContainerListener> crafters, PacketBuffer dos) throws IOException 
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
    
    public ItemStack slotClick(TileContainer container, int s, int b, ClickType m, EntityPlayer player) 
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
            if (canCheck && !((ISidedInventory)inv).canInsertItem(i, item, EnumFacing.VALUES[bs])) continue;
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
            if (canCheck && !((ISidedInventory)inv).canInsertItem(i, item, EnumFacing.VALUES[bs])) continue;
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
    
    public void dropStack(ItemStack stack) {
    	EntityItem ei = new EntityItem(worldObj, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack);
		worldObj.spawnEntityInWorld(ei);
    }
    
    /**
     * @return BTNSWE orientation of TileBlock in range 0-5
     */
    public byte getOrientation()
    {
    	return (byte)((this.getBlockMetadata() - 2) % 6);
    }
    
    public String getName() 
    {
    	return I18n.translateToLocal(this.getBlockType().getUnlocalizedName().replaceFirst("tile.", "gui.cd4017be.") + ".name");
    }

	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		return new AxisAlignedBB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
	}
    
}
