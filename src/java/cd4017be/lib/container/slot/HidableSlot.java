package cd4017be.lib.container.slot;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
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
	public HidableSlot(Container inventoryIn, int index, int xPosition, int yPosition) {
		super(inventoryIn, index, xPosition, yPosition);
	}

	@Override
	public boolean mayPlace(ItemStack stack) {return !locked;}

	@Override
	public boolean mayPickup(Player playerIn) {return !locked;}

	@Override
	@OnlyIn(Dist.CLIENT)
	public boolean isActive() {
		return !hidden;
	}

	public void hideSlot(boolean hidden) {
		this.hidden = hidden; 
	}

	public void lock() {
		this.locked = true;
	}

}
