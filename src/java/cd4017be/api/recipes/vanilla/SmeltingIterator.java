package cd4017be.api.recipes.vanilla;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;

import cd4017be.api.recipes.ItemOperand;
import cd4017be.lib.script.obj.Array;
import cd4017be.lib.script.obj.IOperand;
import cd4017be.lib.script.obj.IOperand.OperandIterator;
import net.minecraft.item.ItemStack;

/**
 * Lets config scripts iterate over all (or a filtered set of) furnace smelting recipes in order to remove or edit them.
 * @author CD4017BE
 */
public class SmeltingIterator implements OperandIterator {

	private final ArrayList<ItemStack> keys;
	private final Map<ItemStack, ItemStack> recipes;
	private ItemStack in, out;
	private int idx;

	public SmeltingIterator(Predicate<Object> key, boolean res) {
		keys = new ArrayList<ItemStack>();
		recipes = new HashMap<>(); // FurnaceRecipes.instance().getSmeltingList();
		for (Entry<ItemStack, ItemStack> e : recipes.entrySet())
			if (key.test(res ? e.getValue() : e.getKey()))
				keys.add(e.getKey());
		idx = -1;
	}

	@Override
	public boolean hasNext() {
		while (++idx < keys.size()) {
			in = keys.get(idx);
			out = recipes.get(in);
			if (out != null) return true;
		}
		return false;
	}

	@Override
	public IOperand next() {
		return new Array(new ItemOperand(in), new ItemOperand(out));
	}

	@Override
	public Object value() {
		return recipes;
	}

	@Override
	public void set(IOperand obj) {
		if (obj instanceof Array) {
			Object[] arr = (Object[])obj.value();
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
	public void reset() {
		idx = -1;
	}

}
