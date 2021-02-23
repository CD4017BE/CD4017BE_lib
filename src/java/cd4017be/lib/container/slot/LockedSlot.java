package cd4017be.lib.container.slot;

import net.minecraft.entity.player.PlayerEntity;
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
	public boolean canTakeStack(PlayerEntity playerIn) {return false;}

}
