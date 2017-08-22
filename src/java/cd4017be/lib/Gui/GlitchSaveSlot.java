package cd4017be.lib.Gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * Special Slot type for use with inventories that have over-ranged stack sizes that don't fit in 8-bit 
 * and/or where vanilla inventory slot interaction would cause bad glitches (requires special hard coded handling in containers).
 * @author CD4017BE
 */
public class GlitchSaveSlot extends SlotItemHandler {

	public final int index;
	public final boolean clientInteract;

	public GlitchSaveSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
		this(itemHandler, index, xPosition, yPosition, true);
	}

	public GlitchSaveSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition, boolean client) {
		super(itemHandler, index, xPosition, yPosition);
		this.index = index;
		this.clientInteract = client;
	}

	// prevent vanilla from synchronizing with low stack size resolution and other unwanted things like other mods's inventory sorting mechanisms messing up everything. //

	@Override
	public boolean isItemValid(ItemStack stack) {
		return false;
	}

	@Override
	public boolean canTakeStack(EntityPlayer playerIn) {
		return false;
	}

	@Override
	public ItemStack decrStackSize(int amount) {
		return ItemStack.EMPTY;
	}

	@Override
	public void putStack(ItemStack stack) {}

}
