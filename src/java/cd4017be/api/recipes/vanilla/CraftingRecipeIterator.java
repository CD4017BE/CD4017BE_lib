package cd4017be.api.recipes.vanilla;

import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.function.Predicate;

import org.apache.logging.log4j.Level;

import cd4017be.lib.script.Function.ArrayIterator;
import cd4017be.lib.script.Function.FilteredIterator;
import cd4017be.lib.script.Function.Iterator;
import cd4017be.lib.script.Function.ListIterator;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

/**
 * Lets config scripts iterate over all (or a filtered set of) crafting recipes in order to remove or edit them.
 * @author CD4017BE
 */
public class CraftingRecipeIterator implements Iterator {

	private final List<IRecipe> list;
	private final Predicate<Object> key;
	private final boolean in;
	private int idx;
	private Object[] curElement;
	private IRecipe curRecipe;

	public CraftingRecipeIterator(Predicate<Object> key, boolean res) {
		this.key = key;
		this.list = CraftingManager.getInstance().getRecipeList();
		this.in = !res;
		this.idx = -1;
	}

	@Override
	public Object get() {
		return curElement;
	}

	@Override
	public void set(Object o) {
		if (list.get(idx) != curRecipe) throw new ConcurrentModificationException();
		if (o == null) list.remove(idx--);
		else if (o == curElement && curElement[0] != curRecipe.getRecipeOutput()) {
			if (!(curElement[0] instanceof ItemStack)) throw new IllegalArgumentException("ItemStack expected");
			ItemStack item = (ItemStack)curElement[0];
			if (curRecipe instanceof ShapedRecipes) {
				ShapedRecipes sr = (ShapedRecipes)curRecipe;
				curRecipe = new ShapedRecipes(sr.recipeWidth, sr.recipeHeight, sr.recipeItems, item);
			} else if (curRecipe instanceof ShapelessRecipes) {
				ShapelessRecipes sr = (ShapelessRecipes)curRecipe;
				curRecipe = new ShapelessRecipes(item, sr.recipeItems);
			} else if (curRecipe instanceof ShapedOreRecipe) {
				ShapedOreRecipe sr = (ShapedOreRecipe)curRecipe;
				ShapedOreRecipe nr = new ShapedOreRecipe(item, emptyGrid(sr.getWidth(), sr.getHeight()));
				System.arraycopy(sr.getInput(), 0, nr.getInput(), 0, sr.getInput().length); //<- HACKING
				curRecipe = nr;
			} else if (curRecipe instanceof ShapelessOreRecipe) {
				ShapelessOreRecipe sr = (ShapelessOreRecipe)curRecipe;
				ShapelessOreRecipe nr = new ShapelessOreRecipe(item);
				nr.getInput().addAll(sr.getInput()); //<- HACKING
				curRecipe = nr;
			} else {
				FMLLog.log("RECIPE_SCRIPT", Level.WARN, "could not replicate unknown recipe type: %s!", curRecipe.getClass());
				return;
			}
			list.set(idx, curRecipe);
		}
	}

	@Override
	public boolean next() {
		int l = list.size();
		while (++idx < l) {
			curRecipe = list.get(idx);
			ItemStack result = curRecipe.getRecipeOutput();
			if (!(in || key.test(result))) continue;
			Iterator ingred;
			if (curRecipe instanceof ShapedOreRecipe) ingred = new ShapedIngredients((ShapedOreRecipe)curRecipe, in ? key : null);
			else if (curRecipe instanceof ShapelessOreRecipe) ingred = new ShapelessIngredients((ShapelessOreRecipe)curRecipe, in ? key : null);
			else if (curRecipe instanceof ShapedRecipes) {
				ingred = new ArrayIterator(((ShapedRecipes)curRecipe).recipeItems);
				if (in) ingred = new FilteredIterator(ingred, key);
			} else if (curRecipe instanceof ShapelessRecipes) {
				ingred = new ListIterator<ItemStack>(((ShapelessRecipes)curRecipe).recipeItems);
				if (in) ingred = new FilteredIterator(ingred, key);
			}
			else continue;
			if (in) {
				if (ingred.next()) ingred.reset();
				else continue;
			}
			curElement = new Object[]{result, ingred};
			return true;
		}
		return false;
	}

	@Override
	public void reset() {
		idx = -1;
	}

	private static Object[] emptyGrid(int w, int h) {
		String line = "";
		for (int i = 0; i < w; i++) line += " ";
		Object[] rcp = new Object[h];
		for (int i = 0; i < h; i++) rcp[i] = line;
		return rcp;
	}

}