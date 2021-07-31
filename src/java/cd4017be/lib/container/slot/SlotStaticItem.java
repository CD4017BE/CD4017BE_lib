package cd4017be.lib.container.slot;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 
 * @author CD4017BE
 */
public class SlotStaticItem extends Slot {

	public SlotStaticItem(int x, int y, ItemStack stack) {
		super(new SimpleContainer(1), 0, x, y);
		this.container.setItem(0, stack);
	}

	@Override
	public boolean mayPlace(ItemStack stack) {
		return false;
	}

	@Override
	public void set(ItemStack stack) {}

	@Override
	public ItemStack remove(int amount) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean mayPickup(Player playerIn) {
		return false;
	}

	@Override
	public int getMaxStackSize() {
		return 0;
	}

}
