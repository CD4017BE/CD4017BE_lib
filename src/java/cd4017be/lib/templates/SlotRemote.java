package cd4017be.lib.templates;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;

public class SlotRemote extends Slot 
{
	
	public final EnumFacing accessSide;
	public boolean dirty;
	
	public SlotRemote(IInventory inv, int s, int acs, int x, int y) 
	{
		super(inv, s, x, y);
		this.accessSide = EnumFacing.VALUES[acs];
	}

	@Override
	public boolean isItemValid(ItemStack item) 
	{
		boolean ret;
		if (this.inventory instanceof ISidedInventory)  ret =  ((ISidedInventory)this.inventory).canInsertItem(getSlotIndex(), item, accessSide);
		else ret =  this.inventory.isItemValidForSlot(getSlotIndex(), item);
		dirty |= !ret;
		return ret;
	}

	@Override
	public boolean canTakeStack(EntityPlayer player) 
	{
		if (this.inventory instanceof ISidedInventory && !((ISidedInventory)this.inventory).canExtractItem(getSlotIndex(), getStack(), accessSide)) {
			dirty = true;
			return false;
		}
		return true;
	}

	@Override
	public void putStack(ItemStack p_75215_1_) 
	{
		dirty = true;
		super.putStack(p_75215_1_);
	}

	@Override
	public ItemStack decrStackSize(int p_75209_1_) 
	{
		dirty = true;
		return super.decrStackSize(p_75209_1_);
	}
	
}
