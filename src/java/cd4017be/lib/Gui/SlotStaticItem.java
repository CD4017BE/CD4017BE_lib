package cd4017be.lib.Gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class SlotStaticItem extends Slot {

	public SlotStaticItem(int x, int y, ItemStack stack) {
		super(new InventoryBasic("const", true, 1), 0, x, y);
		this.inventory.setInventorySlotContents(0, stack);
	}

	@Override
	public boolean isItemValid(ItemStack stack) {
		return false;
	}

	@Override
	public void putStack(ItemStack stack) {}

	@Override
	public ItemStack decrStackSize(int amount) {
		return null;
	}

	@Override
	public boolean canTakeStack(EntityPlayer playerIn) {
		return false;
	}

	@Override
	public int getSlotStackLimit() {
		return 0;
	}

}
