package cd4017be.api.recipes.vanilla;

import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;

import cd4017be.lib.script.Function.Iterator;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;

/**
 * Lets config scripts iterate over all (or a filtered set of) furnace smelting recipes in order to remove or edit them.
 * @author CD4017BE
 */
public class SmeltingIterator implements Iterator {

	private final ArrayList<ItemStack> keys;
	private final Map<ItemStack, ItemStack> recipes;
	private ItemStack in, out;
	private int idx;

	public SmeltingIterator(Predicate<Object> key, boolean res) {
		keys = new ArrayList<ItemStack>();
		recipes = FurnaceRecipes.instance().getSmeltingList();
		for (Entry<ItemStack, ItemStack> e : recipes.entrySet())
			if (key.test(res ? e.getValue() : e.getKey()))
				keys.add(e.getKey());
		idx = -1;
	}

	@Override
	public Object get() {
		return new Object[] {in, out};
	}

	@Override
	public void set(Object o) {
		if (o instanceof Object[]) {
			Object[] arr = (Object[])o;
			if (arr[0] instanceof ItemStack && arr[1] instanceof ItemStack) {
				ItemStack a = (ItemStack)arr[0], b = (ItemStack)arr[1];
				if (a != in || b != out) {
					if (a != in) recipes.remove(in);
					recipes.put(a, b);
				}
				return;
			}
		}
		recipes.remove(in);
	}

	@Override
	public boolean next() {
		while (++idx < keys.size()) {
			in = keys.get(idx);
			out = recipes.get(in);
			if (out != null) return true;
		}
		return false;
	}

	@Override
	public void reset() {
		idx = -1;
	}

}
