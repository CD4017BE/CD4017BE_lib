package cd4017be.lib.Gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * @author CD4017BE
 *
 */
public interface ISpecialSlot {

	/**
	 * @return this slot's inventory
	 */
	IItemHandler getItemHandler();

	/**
	 * @return the corresponding slot number in the inventory
	 */
	int getSlot();

	/**
	 * @param stack stack to insert
	 * @param sim whether to just simulate insertion
	 * @return left over
	 */
	default ItemStack insertItem(ItemStack stack, boolean sim) {
		return getItemHandler().insertItem(getSlot(), stack, sim);
	}

	/**
	 * @param am maximum amount to extract
	 * @param sim whether to just simulate extraction
	 * @return stack extracted
	 */
	default ItemStack extractItem(int am, boolean sim) {
		return getItemHandler().extractItem(getSlot(), am, sim);
	}

	/**
	 * set this slots content (for synchronization)
	 * @param stack new content
	 */
	default void setStack(ItemStack stack) {
		IItemHandler inv = getItemHandler();
		if (inv instanceof IItemHandlerModifiable)
			((IItemHandlerModifiable)inv).setStackInSlot(getSlot(), stack);
	}

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
	 * @param container
	 * @return transaction reference item
	 */
	ItemStack onClick(int b, ClickType ct, EntityPlayer player, AdvancedContainer container);

	/**
	 * lets the player quick select a specific item onto cursor
	 * @param player
	 * @param item the item type to quick select
	 */
	public static void quickSelect(EntityPlayer player, ItemStack item) {
		ItemStack stack = player.inventory.getItemStack();
		if (!stack.isEmpty() && !ItemHandlerHelper.canItemStacksStack(item, stack)) return;
		item = ItemHandlerHelper.copyStackWithSize(item, item.getMaxStackSize() - stack.getCount());
		if (item.isEmpty()) return;
		int n = stack.getCount() + getFromPlayerInv(item, player.inventory);
		stack = ItemHandlerHelper.copyStackWithSize(item, player.capabilities.isCreativeMode ? item.getMaxStackSize() : n);
		player.inventory.setItemStack(stack);
	}

	/**
	 * insert the given item into a players inventory
	 * @param item stack to insert
	 * @param inv player inventory
	 * @return amount left over
	 */
	public static int putInPlayerInv(ItemStack item, InventoryPlayer inv) {
		int x = item.getCount();
		int m = item.getMaxStackSize();
		int es = inv.mainInventory.size();
		for (int i = 0; i < inv.mainInventory.size(); i++) {
			ItemStack stack = inv.mainInventory.get(i);
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
		for (int i = es; i < inv.mainInventory.size(); i++)
			if (inv.mainInventory.get(i).isEmpty()) {
				if (x <= m) {
					item.setCount(x);
					inv.mainInventory.set(i, item);
					return 0;
				} else {
					x -= m;
					inv.mainInventory.set(i, ItemHandlerHelper.copyStackWithSize(item, m));
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
	public static int getFromPlayerInv(ItemStack item, InventoryPlayer inv) {
		int n = 0;
		for (int i = 0; i < inv.mainInventory.size(); i++) {
			ItemStack stack = inv.mainInventory.get(i);
			if (ItemHandlerHelper.canItemStacksStack(item, stack)) {
				n += stack.getCount();
				if (n <= item.getCount()) {
					inv.mainInventory.set(i, ItemStack.EMPTY);
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
