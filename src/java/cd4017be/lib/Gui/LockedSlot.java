package cd4017be.lib.Gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

/**
 * 
 * @author CD4017BE
 */
public class LockedSlot extends HidableSlot {

	public LockedSlot(IInventory inv, int slot, int xPosition, int yPosition) {
		super(inv, slot, xPosition, yPosition);
	}

	@Override
	public boolean isItemValid(ItemStack stack) {return false;}

	@Override
	public boolean canTakeStack(EntityPlayer playerIn) {return false;}

}
