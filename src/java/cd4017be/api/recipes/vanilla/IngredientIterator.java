package cd4017be.api.recipes.vanilla;

import java.util.List;
import java.util.function.Predicate;

import cd4017be.lib.script.Function.ListIterator;
import cd4017be.lib.util.OreDictStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraftforge.oredict.OreIngredient;


/**
 * @author CD4017BE
 *
 */
public class IngredientIterator extends ListIterator<Ingredient> {

	private final Predicate<Object> key;
	private Object curElement;

	public IngredientIterator(NonNullList<Ingredient> rcp, Predicate<Object> key) {
		super(rcp);
		this.key = key;
	}

	@Override
	public Object get() {
		return curElement;
	}

	@Override
	public void set(Object o) {
		if (o == curElement) return;
		if (o instanceof ItemStack) arr.set(idx, (Ingredient)o);
		else if (o instanceof OreDictStack) arr.set(idx, new OreIngredient(((OreDictStack)o).id));
		else if (o == null) arr.remove(idx--);
		else throw new IllegalArgumentException("exp. ItemStack or OreDictStack");
	}

	@Override
	public boolean next() {
		while (++idx < arr.size()) {
			curElement = arr.get(idx);
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
