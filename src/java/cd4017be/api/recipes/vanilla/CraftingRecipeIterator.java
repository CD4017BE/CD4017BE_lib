package cd4017be.api.recipes.vanilla;

import java.util.function.Predicate;

import com.google.common.base.Predicates;

import cd4017be.api.recipes.ItemOperand;
import cd4017be.lib.script.obj.Array;
import cd4017be.lib.script.obj.IOperand;
import cd4017be.lib.script.obj.IOperand.OperandIterator;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
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
		this.list = CraftingManager.REGISTRY.iterator();
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
		this.list = CraftingManager.REGISTRY.iterator();
	}

	@Override
	public IOperand next() {
		return curElement;
	}

	@Override
	public void set(IOperand obj) {
		//recipe post-manipulation not supported by Minecraft 1.12
		/*
		if (obj != curElement) {
			list.remove();
			return;
		}
		Object out = curElement.array[0].value();
		if (!(out instanceof ItemStack)) throw new IllegalArgumentException("ItemStack expected");
		if (out != curRecipe.getRecipeOutput()) {
			ItemStack item = (ItemStack)out;
			if (curRecipe instanceof ShapedRecipes) {
				ShapedRecipes sr = (ShapedRecipes)curRecipe;
				curRecipe = new ShapedRecipes(sr.getGroup(), sr.recipeWidth, sr.recipeHeight, sr.recipeItems, item);
			} else if (curRecipe instanceof ShapelessRecipes) {
				ShapelessRecipes sr = (ShapelessRecipes)curRecipe;
				curRecipe = new ShapelessRecipes(sr.getGroup(), item, sr.recipeItems);
			} else if (curRecipe instanceof ShapedOreRecipe) {
				ShapedOreRecipe sr = (ShapedOreRecipe)curRecipe;
				ShapedPrimer p = new ShapedPrimer();
				p.width = sr.getRecipeWidth();
				p.height = sr.getRecipeHeight();
				p.input = sr.getIngredients();
				curRecipe = new ShapedOreRecipe(new ResourceLocation(sr.getGroup()), item, p);
			} else if (curRecipe instanceof ShapelessOreRecipe) {
				ShapelessOreRecipe sr = (ShapelessOreRecipe)curRecipe;
				curRecipe = new ShapelessOreRecipe(new ResourceLocation(sr.getGroup()), sr.getIngredients(), item);
			} else {
				RecipeScriptContext.instance.LOG.warn(RecipeScriptContext.SCRIPT,"could not replicate unknown recipe type: {}!", curRecipe.getClass());
				return;
			}
			CraftingManager.REGISTRY.putObject(curRecipe.getRegistryName(), curRecipe);
		}
		*/
	}

	@Override
	public boolean hasNext() {
		while (list.hasNext()) {
			curRecipe = list.next();
			ItemOperand result = new ItemOperand(curRecipe.getRecipeOutput());
			if (!key.test(result)) continue;
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
		list = CraftingManager.REGISTRY.iterator();
	}

	@Override
	public Object value() {
		return this;
	}

}