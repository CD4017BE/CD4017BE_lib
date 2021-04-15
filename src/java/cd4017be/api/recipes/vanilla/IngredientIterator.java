package cd4017be.api.recipes.vanilla;

import cd4017be.api.recipes.ItemOperand;
import cd4017be.lib.script.obj.*;
import cd4017be.lib.script.obj.IOperand.OperandIterator;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;


/**
 * Lets config scripts parse though ingredients of crafting recipes and edit them.
 * @author CD4017BE
 */
public class IngredientIterator implements OperandIterator {

	private final NonNullList<Ingredient> ingreds;
	private final ItemStack key;
	private final boolean shaped;
	private int idx = -1;
	private PredicateWrap<ItemStack> curElement;

	public IngredientIterator(NonNullList<Ingredient> rcp, ItemStack key, boolean shaped) {
		this.ingreds = rcp;
		this.key = key;
		this.shaped = shaped;
	}

	@Override
	public IOperand next() {
		return curElement;
	}

	@Override
	public void set(IOperand obj) {
		if (obj == curElement) return;
		if (obj instanceof ItemOperand) ingreds.set(idx, Ingredient.fromStacks(((ItemOperand)obj).stack));
		//else if (obj instanceof OreDictStack) ingreds.set(idx, new OreIngredient(((OreDictStack)obj).id));
		else if (obj == Nil.NIL) {
			if (shaped) ingreds.set(idx, Ingredient.EMPTY);
			else ingreds.remove(idx--);
		} else throw new IllegalArgumentException("exp. ItemStack or OreDictStack");
	}

	@Override
	public boolean hasNext() {
		/*while (++idx < ingreds.size()) {
			Ingredient ingr = ingreds.get(idx);
			if (key == null || ingr.apply(key)) {
				curElement = new PredicateWrap<>(ingr, ItemStack.class);
				return true;
			}
		}*/
		return false;
	}

	@Override
	public void reset() {
		idx = -1;
	}

	@Override
	public Object value() {
		return this;
	}
/*
	@Override
	public IOperand len() {
		return new Number(ingreds.size());
	}

	@Override
	public IOperand get(IOperand idx) {
		return new Array(ingreds.get(idx.asIndex()).getMatchingStacks(), ItemOperand::new);
	}
*/
}
