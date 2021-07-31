package cd4017be.lib.container.slot;

import cd4017be.lib.container.AdvancedContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.*;

/**
 * Special Slot type for use with inventories that have over-ranged stack sizes that don't fit in 8-bit 
 * and/or where vanilla inventory slot interaction would cause bad glitches (requires special hard coded handling in containers).
 * @author CD4017BE
 */
public class GlitchSaveSlot extends SlotItemHandler implements ISpecialSlot {

	public final int index;
	public final boolean clientInteract;
	private int[] transferTarget;

	public GlitchSaveSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
		this(itemHandler, index, xPosition, yPosition, true);
	}

	public GlitchSaveSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition, boolean client) {
		super(itemHandler, index, xPosition, yPosition);
		this.index = index;
		this.clientInteract = client;
	}

	/**
	 * define where to put items taken out of this slot
	 * @param target an array of {start slot id, end slot id} pairs
	 * @return this
	 */
	public GlitchSaveSlot setTarget(int... target) {
		if ((target.length & 1) != 0)
			throw new IllegalArgumentException("array of slot id pairs expected!");
		this.transferTarget = target;
		return this;
	}

	// prevent vanilla from synchronizing with low stack size resolution and other unwanted things like other mods's inventory sorting mechanisms messing up everything. //

	@Override
	public boolean mayPlace(ItemStack stack) {
		return false;
	}

	@Override
	public boolean mayPickup(Player playerIn) {
		return false;
	}

	@Override
	public ItemStack remove(int amount) {
		return ItemStack.EMPTY;
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
		if (!clientInteract) {
			if (player.level.isClientSide)
				return ItemStack.EMPTY;
			container.hardInvUpdate();
		}
		boolean boost = ct == ClickType.QUICK_MOVE;
		ItemStack curItem = container.getCarried();
		if (curItem.getCount() > 0 && (item.isEmpty() || ItemHandlerHelper.canItemStacksStack(item, curItem))) {
			if (boost) {
				ItemStack rem = insertItem(ItemHandlerHelper.copyStackWithSize(curItem, 65536), true);
				int n = 65536 - rem.getCount(), n1 = 0;
				if (n <= 0) return ItemStack.EMPTY;
				if (b == 0) {
					if (n < curItem.getCount()) curItem.shrink(n1 = n);
					else {
						n1 = curItem.getCount();
						container.setCarried(ItemStack.EMPTY);
					}
				}
				if (n1 < n)
					n1 += ISpecialSlot.getFromPlayerInv(ItemHandlerHelper.copyStackWithSize(curItem, n - n1), player.getInventory());
				insertItem(ItemHandlerHelper.copyStackWithSize(curItem, n1), false);
			} else {
				int n = b == 0 ? curItem.getCount() : 1;
				ItemStack rem = insertItem(ItemHandlerHelper.copyStackWithSize(curItem, n), false);
				curItem.shrink(n - rem.getCount());
				if (curItem.getCount() <= 0) container.setCarried(ItemStack.EMPTY);
			}
		} else if (item.getCount() > 0) {
			int n = boost ? (b == 0 ? item.getMaxStackSize() : 65536) : (b == 0 ? 1 : 8);
			if ((item = extractItem(n, true)).getCount() == 0) return ItemStack.EMPTY;
			ItemStack item1 = item.copy();
			if (transferTarget != null)
				for (int i = 0; i < transferTarget.length; i+=2) {
					int ss = transferTarget[i], se = transferTarget[i|1];
					if (container.moveItemStackTo(item1, Math.min(ss, se), Math.max(ss, se), ss > se))
						break;
				}
			int rem = item1.getCount() <= 0 ? 0 : ISpecialSlot.putInPlayerInv(item1, player.getInventory());
			extractItem(item.getCount() - rem, false);
		}
		return ItemStack.EMPTY;
	}

	public ItemStack insertItem(ItemStack stack, boolean sim) {
		return getItemHandler().insertItem(index, stack, sim);
	}

	public ItemStack extractItem(int am, boolean sim) {
		return getItemHandler().extractItem(index, am, sim);
	}

}
