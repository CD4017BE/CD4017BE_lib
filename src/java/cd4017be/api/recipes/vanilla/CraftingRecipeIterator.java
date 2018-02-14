package cd4017be.api.recipes.vanilla;

import java.util.function.Predicate;

import org.apache.logging.log4j.Level;

import cd4017be.lib.script.Function.Iterator;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.CraftingHelper.ShapedPrimer;
import net.minecraftforge.common.crafting.IShapedRecipe;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

/**
 * Lets config scripts iterate over all (or a filtered set of) crafting recipes in order to remove or edit them.
 * @author CD4017BE
 */
public class CraftingRecipeIterator implements Iterator {

	private java.util.Iterator<IRecipe> list;
	private final Predicate<Object> key;
	private final boolean in;
	private Object[] curElement;
	private IRecipe curRecipe;

	public CraftingRecipeIterator(Predicate<Object> key, boolean res) {
		this.key = key;
		this.list = CraftingManager.REGISTRY.iterator();
		this.in = !res;
	}

	@Override
	public Object get() {
		return curElement;
	}

	@Override
	public void set(Object o) {
		if (o == null) list.remove();//TODO potential fail
		else if (o == curElement && curElement[0] != curRecipe.getRecipeOutput()) {
			if (!(curElement[0] instanceof ItemStack)) throw new IllegalArgumentException("ItemStack expected");
			ItemStack item = (ItemStack)curElement[0];
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
				FMLLog.log("RECIPE_SCRIPT", Level.WARN, "could not replicate unknown recipe type: %s!", curRecipe.getClass());
				return;
			}
			CraftingManager.REGISTRY.putObject(curRecipe.getRegistryName(), curRecipe);
		}
	}

	@Override
	public boolean next() {
		while (list.hasNext()) {
			curRecipe = list.next();
			ItemStack result = curRecipe.getRecipeOutput();
			if (!(in || key.test(result))) continue;
			Iterator ingred = new IngredientIterator(curRecipe.getIngredients(), key, curRecipe instanceof IShapedRecipe);
			curElement = new Object[]{result, ingred};
			return true;
		}
		return false;
	}

	@Override
	public void reset() {
		list = CraftingManager.REGISTRY.iterator();
	}

}