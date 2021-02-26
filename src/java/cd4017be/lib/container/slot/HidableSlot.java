package cd4017be.lib.container.slot;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;


/**
 * @author CD4017BE
 *
 */
public class HidableSlot extends Slot {

	private boolean hidden, locked;

	/**
	 * @param inventoryIn
	 * @param index
	 * @param xPosition
	 * @param yPosition
	 */
	public HidableSlot(IInventory inventoryIn, int index, int xPosition, int yPosition) {
		super(inventoryIn, index, xPosition, yPosition);
	}

	@Override
	public boolean isItemValid(ItemStack stack) {return !locked;}

	@Override
	public boolean canTakeStack(PlayerEntity playerIn) {return !locked;}

	@Override
	@OnlyIn(Dist.CLIENT)
	public boolean isEnabled() {
		return !hidden;
	}

	public void hideSlot(boolean hidden) {
		this.hidden = hidden; 
	}

	public void lock() {
		this.locked = true;
	}

}
