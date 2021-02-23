package cd4017be.lib.container.slot;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Slot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;


/**
 * @author CD4017BE
 *
 */
public class HidableSlot extends Slot {

	private boolean hidden;

	/**
	 * @param inventoryIn
	 * @param index
	 * @param xPosition
	 * @param yPosition
	 */
	public HidableSlot(IInventory inventoryIn, int index, int xPosition, int yPosition) {
		super(inventoryIn, index, xPosition, yPosition);
		// TODO Auto-generated constructor stub
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public boolean isEnabled() {
		return !hidden;
	}

	public void hideSlot(boolean hidden) {
		this.hidden = hidden; 
	}

}
