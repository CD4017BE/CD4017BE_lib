package cd4017be.lib.capability;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

/**
 * 
 * @author CD4017BE
 */
public class InventoryItem extends AbstractInventory {

	private final PlayerInventory ref;
	private final IItemInventory inv;
	private final ItemStack[] cache;

	public InventoryItem(PlayerEntity player) {
		this.ref = player.inventory;
		ItemStack item = ref.items.get(ref.selected);
		if (!(item.getItem() instanceof IItemInventory)) throw new IllegalArgumentException("Held item not InventoryItem compatible!");
		this.inv = (IItemInventory)item.getItem();
		this.cache = inv.loadInventory(item, player);
	}

	@Override
	public int getSlots() {
		return cache.length;
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		return cache[slot];
	}

	@Override
	public void setStackInSlot(int slot, ItemStack stack) {
		cache[slot] = stack;
		ItemStack item = ref.items.get(ref.selected);
		if (item.getItem() == inv)
			inv.saveInventory(item, ref.player, cache);
	}

	public void update() {
		ItemStack item = ref.items.get(ref.selected);
		if (item.getItem() == inv) {
			ItemStack[] items = inv.loadInventory(item, ref.player);
			System.arraycopy(items, 0, cache, 0, Math.min(cache.length, items.length));
		}
	}

	public interface IItemInventory {
		public ItemStack[] loadInventory(ItemStack inv, PlayerEntity player);
		public void saveInventory(ItemStack inv, PlayerEntity player, ItemStack[] items);
	}

}
