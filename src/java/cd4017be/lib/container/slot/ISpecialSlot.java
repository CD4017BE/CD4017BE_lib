package cd4017be.lib.container.slot;

import cd4017be.lib.container.AdvancedContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;

/**Allows Slots to have special interaction behavior
 * and more control over item extraction insertion
 * in {@link AdvancedContainer}s.
 * @author CD4017BE */
public interface ISpecialSlot {

	/**
	 * @param stack stack to insert
	 * @param sim whether to just simulate insertion
	 * @return left over
	 */
	ItemStack insertItem(ItemStack stack, boolean sim);

	/**
	 * @param am maximum amount to extract
	 * @param sim whether to just simulate extraction
	 * @return stack extracted
	 */
	ItemStack extractItem(int am, boolean sim);

	/**
	 * @param stack item to insert
	 * @return whether the given item should not get merged into other slots, even if this didn't fully take it.
	 */
	boolean insertHereOnly(ItemStack stack);

	/**
	 * perform custom click operation for this slot
	 * @param b "mouse button" used
	 * @param ct click type
	 * @param player
	 * @param advancedContainer
	 * @return transaction reference item
	 */
	ItemStack onClick(int b, ClickType ct, Player player, AdvancedContainer advancedContainer);

	/**
	 * lets the player quick select a specific item onto cursor
	 * @param player
	 * @param item the item type to quick select
	 */
	public static void quickSelect(Player player, ItemStack item, AdvancedContainer cont) {
		ItemStack stack = cont.getCarried();
		if (!stack.isEmpty() && !ItemHandlerHelper.canItemStacksStack(item, stack)) return;
		item = ItemHandlerHelper.copyStackWithSize(item, item.getMaxStackSize() - stack.getCount());
		if (item.isEmpty()) return;
		int n = stack.getCount() + getFromPlayerInv(item, player.getInventory());
		stack = ItemHandlerHelper.copyStackWithSize(item, player.isCreative() ? item.getMaxStackSize() : n);
		cont.setCarried(stack);
	}

	/**
	 * insert the given item into a players inventory
	 * @param item stack to insert
	 * @param inv player inventory
	 * @return amount left over
	 */
	public static int putInPlayerInv(ItemStack item, Inventory inv) {
		int x = item.getCount();
		int m = item.getMaxStackSize();
		int es = inv.items.size();
		for (int i = 0; i < inv.items.size(); i++) {
			ItemStack stack = inv.items.get(i);
			int n = stack.getCount();
			if (n > 0 && n < m && ItemHandlerHelper.canItemStacksStack(stack, item)) {
				if (x <= m - n) {
					stack.grow(x);
					return 0;
				} else {
					x -= m - n;
					stack.setCount(m);
				}
			} else if (n == 0 && i < es) es = i;
		}
		for (int i = es; i < inv.items.size(); i++)
			if (inv.items.get(i).isEmpty()) {
				if (x <= m) {
					item.setCount(x);
					inv.items.set(i, item);
					return 0;
				} else {
					x -= m;
					inv.items.set(i, ItemHandlerHelper.copyStackWithSize(item, m));
				}
			}
		return x;
	}

	/**
	 * take items of a certain type from the player inventory
	 * @param item filter which to take
	 * @param inv player inventory
	 * @return amount taken
	 */
	public static int getFromPlayerInv(ItemStack item, Inventory inv) {
		int n = 0;
		for (int i = 0; i < inv.items.size(); i++) {
			ItemStack stack = inv.items.get(i);
			if (ItemHandlerHelper.canItemStacksStack(item, stack)) {
				n += stack.getCount();
				if (n <= item.getCount()) {
					inv.items.set(i, ItemStack.EMPTY);
					if (n == item.getCount()) return n;
				} else {
					stack.setCount(n - item.getCount());
					return item.getCount();
				}
			}
		}
		return n;
	}

}
