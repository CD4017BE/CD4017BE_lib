package cd4017be.api.recipes.vanilla;

import java.util.function.Predicate;

import com.google.common.base.Predicates;

import cd4017be.api.recipes.ItemOperand;
import cd4017be.lib.script.obj.Array;
import cd4017be.lib.script.obj.IOperand;
import cd4017be.lib.script.obj.IOperand.OperandIterator;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraftforge.common.crafting.IShapedRecipe;

/**
 * Lets config scripts iterate over all (or a filtered set of) crafting recipes in order to remove or edit them.
 * @author CD4017BE
 */
public class CraftingRecipeIterator implements OperandIterator {

	private java.util.Iterator<IRecipe> list;
	private final Predicate<Object> key;
	private final ItemStack item;
	private Array curElement;
	private IRecipe curRecipe;

	/**
	 * Iterates over all crafting recipes with given ingredient
	 * @param key the ItemStack as ingredient filter
	 */
	public CraftingRecipeIterator(ItemStack key) {
		//this.list = CraftingManager.REGISTRY.iterator();
		this.key = Predicates.alwaysTrue();
		this.item = key;
	}

	/**
	 * Iterates over all crafting recipes with given result
	 * @param key the result filter
	 */
	public CraftingRecipeIterator(Predicate<Object> key) {
		this.key = key;
		this.item = null;
		//this.list = CraftingManager.REGISTRY.iterator();
	}

	@Override
	public IOperand next() {
		return curElement;
	}

	@Override
	public void set(IOperand obj) {
		return;/*
		if (obj != curElement) {
			disableRecipe();
			return;
		}
		Object out = curElement.array[0].value();
		if (!(out instanceof ItemStack)) throw new IllegalArgumentException("ItemStack expected");
		if (out != curRecipe.getRecipeOutput()) {
			ItemStack item = (ItemStack)out;
			if (curRecipe instanceof ShapedRecipes) {
				ShapedRecipes sr = (ShapedRecipes)curRecipe;
				RecipeAPI.addRecipe(new ShapedRecipes(sr.getGroup(), sr.recipeWidth, sr.recipeHeight, copy(sr.recipeItems), item));
			} else if (curRecipe instanceof ShapelessRecipes) {
				ShapelessRecipes sr = (ShapelessRecipes)curRecipe;
				RecipeAPI.addRecipe(new ShapelessRecipes(sr.getGroup(), item, copy(sr.recipeItems)));
			} else if (curRecipe instanceof ShapedOreRecipe) {
				ShapedOreRecipe sr = (ShapedOreRecipe)curRecipe;
				ShapedPrimer p = new ShapedPrimer();
				p.width = sr.getRecipeWidth();
				p.height = sr.getRecipeHeight();
				p.input = copy(sr.getIngredients());
				RecipeAPI.addRecipe(new ShapedOreRecipe(new ResourceLocation(sr.getGroup()), item, p));
			} else if (curRecipe instanceof ShapelessOreRecipe) {
				ShapelessOreRecipe sr = (ShapelessOreRecipe)curRecipe;
				RecipeAPI.addRecipe(new ShapelessOreRecipe(new ResourceLocation(sr.getGroup()), copy(sr.getIngredients()), item));
			} else
				RecipeScriptContext.instance.LOG.warn(RecipeScriptContext.SCRIPT,"could not replicate unknown recipe type: {}!", curRecipe.getClass());
			disableRecipe();
		}*/
	}

	private static NonNullList<Ingredient> copy(NonNullList<Ingredient> list) {
		NonNullList<Ingredient> nlist = NonNullList.create();
		nlist.addAll(list);
		return nlist;
	}

	private void disableRecipe() {
		/*
		List<Ingredient> ingr = curRecipe.getIngredients();
		for (int i = 0, l = ingr.size(); i < l; i++)
			ingr.set(i, LOCK_INGRED);*/
	}

	@Override
	public boolean hasNext() {
		while (list.hasNext()) {
			curRecipe = list.next();
			ItemOperand result = new ItemOperand(curRecipe.getRecipeOutput());
			if (!key.test(result)) continue;
			result.onCopy();
			OperandIterator ingred = new IngredientIterator(curRecipe.getIngredients(), item, curRecipe instanceof IShapedRecipe);
			if (item != null) {
				if (!ingred.hasNext()) continue;
				ingred.reset();
			}
			curElement = new Array(result, ingred);
			return true;
		}
		return false;
	}

	@Override
	public void reset() {
		//list = CraftingManager.REGISTRY.iterator();
	}

	@Override
	public Object value() {
		return this;
	}

	/*
	public static final Ingredient LOCK_INGRED = new Ingredient(new ItemStack[] {new ItemStack(Lib.rrwi)}) {
		@Override
		public boolean apply(ItemStack stack) {return false;}
	};*/
}