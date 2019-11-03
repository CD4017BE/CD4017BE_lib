package cd4017be.lib.Gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.SlotItemHandler;

/**
 *
 * @author CD4017BE
 */
public class SlotHolo extends SlotItemHandler implements ISpecialSlot {

	private final boolean locked, stack;
	private final int index;

	public SlotHolo(IItemHandler inv, int id, int x, int y, boolean locked, boolean stack) {
		super(inv, id, x, y);
		this.locked = locked;
		this.stack = stack;
		this.index = id;
	}

	@Override
	public boolean canTakeStack(EntityPlayer player) {
		return !locked;
	}

	@Override
	public boolean isItemValid(ItemStack par1ItemStack) {
		return !locked;
	}

	@Override
	public int getSlotStackLimit() {
		return stack ? 127 : 1;
	}

	// new container system

	@Override
	public int getSlot() {
		return index;
	}

	@Override
	public boolean insertHereOnly(ItemStack stack) {
		return false;
	}

	@Override
	public ItemStack onClick(int b, ClickType ct, EntityPlayer player, AdvancedContainer container) {
		ItemStack item = getStack();
		if (ct == ClickType.CLONE) {
			ISpecialSlot.quickSelect(player, item);
			return ItemStack.EMPTY;
		} else if (ct != ClickType.PICKUP && ct != ClickType.QUICK_MOVE)
			return ItemStack.EMPTY;
		if (player.world.isRemote)
			return ItemStack.EMPTY;
		container.hardInvUpdate();
		ItemStack curItem = player.inventory.getItemStack();
		if (ct == ClickType.QUICK_MOVE) {
			putStack(curItem.copy());
			return ItemStack.EMPTY;
		}
		if (curItem.getCount() > 0 && (item.isEmpty() || ItemHandlerHelper.canItemStacksStack(item, curItem))) {
			int n = b == 0 ? curItem.getCount() : 1;
			insertItem(ItemHandlerHelper.copyStackWithSize(curItem, n), false);
		} else if (item.getCount() > 0) {
			int n = b == 0 ? 1 : 8;
			extractItem(n, false);
		}
		return ItemStack.EMPTY;
	}

	@Override
	public ItemStack insertItem(ItemStack stack, boolean sim) {
		if (!locked) getItemHandler().insertItem(getSlot(), stack, sim);
		return stack;
	}

	@Override
	public ItemStack extractItem(int am, boolean sim) {
		if (!locked) getItemHandler().extractItem(getSlot(), am, sim);
		return ItemStack.EMPTY;
	}

}
