package cd4017be.api.recipes.vanilla;

import java.util.List;
import java.util.function.Predicate;

import cd4017be.lib.script.Function.ArrayIterator;
import cd4017be.lib.util.OreDictStack;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;

/**
 * Lets config scripts parse though ingredients of shaped crafting recipes and edit them.
 * @author CD4017BE
 */
class ShapedIngredients extends ArrayIterator {

	private final Predicate<Object> key;
	private Object curElement;

	public ShapedIngredients(ShapedOreRecipe rcp, Predicate<Object> key) {
		super(rcp.getInput()); //I am manipulating the values in this array. violently! on purpose! Effecting the recipe itself is exactly what I want.
		this.key = key;
	}

	@Override
	public Object get() {
		return curElement;
	}

	@Override
	public void set(Object o) {
		if (o == curElement) return;
		if (o == null || o instanceof ItemStack) arr[idx] = o;
		else if (o instanceof OreDictStack) arr[idx] = OreDictionary.getOres(((OreDictStack)o).id);
		else throw new IllegalArgumentException("exp. ItemStack or OreDictStack");
	}

	@Override
	public boolean next() {
		while (++idx < arr.length) {
			curElement = arr[idx];
			if (curElement instanceof List) {
				List<?> list = (List<?>)curElement;
				if (list.isEmpty()) continue;
				if (key == null) {
					curElement = list.get(0);
					return true;
				}
				for (Object o : list)
					if (key.test(o)) {
						curElement = o;
						return true;
					}
			} else if (key == null || key.test(curElement)) return true;
		}
		return false;
	}

}