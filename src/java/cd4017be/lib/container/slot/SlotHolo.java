package cd4017be.lib.container.slot;

import cd4017be.lib.container.AdvancedContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.*;

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
	public boolean mayPickup(Player player) {
		return !locked;
	}

	@Override
	public boolean mayPlace(ItemStack par1ItemStack) {
		return !locked;
	}

	@Override
	public int getMaxStackSize() {
		return stack ? 127 : 1;
	}

	// new container system

	@Override
	public boolean insertHereOnly(ItemStack stack) {
		return false;
	}

	@Override
	public ItemStack onClick(int b, ClickType ct, Player player, AdvancedContainer container) {
		ItemStack item = getItem();
		if (ct == ClickType.CLONE) {
			ISpecialSlot.quickSelect(player, item, container);
			return ItemStack.EMPTY;
		} else if (ct != ClickType.PICKUP && ct != ClickType.QUICK_MOVE)
			return ItemStack.EMPTY;
		if (player.level.isClientSide)
			return ItemStack.EMPTY;
		container.hardInvUpdate();
		ItemStack curItem = container.getCarried();
		if (ct == ClickType.QUICK_MOVE) {
			if (!locked) {
				curItem = curItem.copy();
				if (!stack) curItem.setCount(1);
				set(curItem);
			}
			return ItemStack.EMPTY;
		}
		if (curItem.getCount() > 0 && (item.isEmpty() || stack && ItemHandlerHelper.canItemStacksStack(item, curItem))) {
			int n = b == 0 && stack ? curItem.getCount() : 1;
			insertItem(ItemHandlerHelper.copyStackWithSize(curItem, n), false);
		} else if (item.getCount() > 0) {
			int n = b == 0 ? 1 : 8;
			extractItem(n, false);
		}
		return ItemStack.EMPTY;
	}

	@Override
	public ItemStack insertItem(ItemStack stack, boolean sim) {
		if (!locked) getItemHandler().insertItem(index, stack, sim);
		return stack;
	}

	@Override
	public ItemStack extractItem(int am, boolean sim) {
		if (!locked) getItemHandler().extractItem(index, am, sim);
		return ItemStack.EMPTY;
	}

}
