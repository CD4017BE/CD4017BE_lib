package cd4017be.lib.util;

import net.minecraft.item.ItemStack;

/**
 * Used for efficient shaped recipe lookup in HashMaps
 * @author cd4017be
 */
public class ItemKey {

	public final ItemStack[] items;
	private final int hash;

	public ItemKey(ItemStack... items) {
		this.items = items;
		final int prime = 31;
		int result = 1;
		for (ItemStack item : items)
			result = prime * result + (item.isEmpty() ? 0 : item.getItem().getRegistryName().hashCode() ^ item.getMetadata());
		this.hash = result;
	}

	@Override
	public int hashCode() {
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj instanceof ItemKey) {
			ItemKey other = (ItemKey) obj;
			int n = items.length;
			if (other.items.length != n) return false;
			for (int i = 0; i < n; i++) {
				ItemStack item = items[i];
				if (!(item.isEmpty() ? other.items[i].isEmpty() : item.isItemEqual(other.items[i])))
					return false;
			}
			return true;
		}
		return false;
	}

}
