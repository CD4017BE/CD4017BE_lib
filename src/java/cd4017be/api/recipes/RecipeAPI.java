package cd4017be.api.recipes;

import java.util.Arrays;
import java.util.HashMap;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import cd4017be.api.recipes.vanilla.CraftingRecipeIterator;
import cd4017be.api.recipes.vanilla.FuelHandler;
import cd4017be.api.recipes.vanilla.SmeltingIterator;
import cd4017be.lib.Lib;
import cd4017be.lib.script.Function.Iterator;
import cd4017be.lib.script.Function.ArrayIterator;
import cd4017be.lib.templates.NBTRecipe;
import cd4017be.lib.script.Function.FilteredIterator;
import cd4017be.lib.script.Parameters;
import cd4017be.lib.util.OreDictStack;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

/**
 * 
 * @author CD4017BE
 */
public class RecipeAPI {

	public static interface IRecipeHandler {
		public void addRecipe(Parameters param);
	}

	public static interface IRecipeList {
		public Iterator list(Parameters param);
	}

	public static final HashMap<String, IRecipeHandler> Handlers;
	public static final HashMap<String, IRecipeList> Lists;

	static {
		Handlers = new HashMap<String, IRecipeHandler>();
		Lists = new HashMap<String, IRecipeList>();
		
		Handlers.put("shaped", (p) -> {
			String[] pattern = p.getString(2).split("/");
			int n = p.param.length - 3;
			Object[] arr = new Object[n * 2 + pattern.length];
			for (int i = 0; i < pattern.length; i++) arr[i] = pattern[i];
			for (int i = 0; i < n; i++) {
				arr[pattern.length + i * 2] = Character.forDigit(i, 9);
				arr[pattern.length + i * 2 + 1] = p.param[i + 3];
			}
			GameRegistry.addRecipe(new ShapedOreRecipe(p.get(1, ItemStack.class), arr));
		});
		Handlers.put("shapedNBT", (p) -> {
			String[] pattern = p.getString(3).split("/");
			int n = p.param.length - 4;
			Object[] arr = new Object[n * 2 + pattern.length];
			for (int i = 0; i < pattern.length; i++) arr[i] = pattern[i];
			for (int i = 0; i < n; i++) {
				arr[pattern.length + i * 2] = Character.forDigit(i, 9);
				arr[pattern.length + i * 2 + 1] = p.param[i + 4];
			}
			GameRegistry.addRecipe(new NBTRecipe(p.get(2, ItemStack.class), p.getString(1), arr));
		});
		Handlers.put("ore", (p) -> {
			String name = p.getString(1);
			for (int i = 2; i < p.param.length; i++)
				OreDictionary.registerOre(name, p.get(i, ItemStack.class));
		});
		Lists.put("ore", (p) -> new FilteredIterator(new ArrayIterator(OreDictionary.getOreNames()), new RegexFilter(p.getString(1))));
		Lists.put("craftIng", (p) -> new CraftingRecipeIterator(getFilter(p.get(1)), false));
		Lists.put("craftRes", (p) -> new CraftingRecipeIterator(getFilter(p.get(1)), true));
		Lists.put("smeltIng", (p) -> new SmeltingIterator(getFilter(p.get(1)), false));
		Lists.put("smeltRes", (p) -> new SmeltingIterator(getFilter(p.get(1)), true));
		Handlers.put("shapeless", (p) -> GameRegistry.addRecipe(new ShapelessOreRecipe(p.get(1, ItemStack.class), Arrays.copyOfRange(p.param, 2, p.param.length))));
		Handlers.put("smelt", (p) -> GameRegistry.addSmelting(p.get(1, ItemStack.class), p.get(2, ItemStack.class), p.param.length > 3 ? (float)p.getNumber(3) : 0F));
		Handlers.put("fuel", new FuelHandler());
		Handlers.put("worldgen", new OreGenHandler());
		Handlers.put("item", (p) -> {
			int n = p.param.length;
			Lib.materials.addMaterial((int)p.getNumber(1), p.getString(2), n > 3 ? p.getString(3) : null, n > 4 ? p.getString(4) : null);
		});
		//TODO Handlers.put("fluidCont", (p) -> FluidContainerRegistry.registerFluidContainer(p.get(1, FluidStack.class), p.get(2, ItemStack.class), p.get(3, ItemStack.class)));
	}

	public static void addModModules(RecipeScriptContext cont) {
		//TODO include more mods
	}

	public static Predicate<Object> getFilter(Object o) {
		if (o instanceof String) return new RegexFilter((String)o);
		else if (o instanceof ItemStack) {
			final ItemStack item = (ItemStack)o;
			return (p) -> p instanceof ItemStack && item.isItemEqual((ItemStack)p);
		} else if (o instanceof FluidStack) {
			final FluidStack fluid = (FluidStack)o;
			return (p) -> p instanceof FluidStack && fluid.isFluidEqual((FluidStack)p);
		} else if (o instanceof OreDictStack) {
			final OreDictStack ore = (OreDictStack)o;
			return (p) ->
				p instanceof OreDictStack ? ore.ID == ((OreDictStack)p).ID :
				p instanceof String ? ore.id.equals((String)p) :
				p instanceof ItemStack && ore.isEqual((ItemStack)p);
		} else if (o == null) return (p) -> p == null;
		else return (p) -> o.equals(p);
	}

	public static class RegexFilter implements Predicate<Object> {
		public RegexFilter(String expr) {
			pattern = Pattern.compile(expr);
		}
		private final Pattern pattern;
		@Override
		public boolean test(Object o) {
			return o != null && pattern.matcher(o.toString()).matches();
		}
	}

	public static void createOreDictEntries(Class<?> c, String name) {
		if (Block.class.isAssignableFrom(c)) {
			Item item;
			for (Block block : Block.REGISTRY)
				if (c.isInstance(block) && (item = Item.getItemFromBlock(block)) != null)
					OreDictionary.registerOre(name, item);
		} else if (Item.class.isAssignableFrom(c)) {
			for (Item item : Item.REGISTRY)
				if (c.isInstance(item))
					OreDictionary.registerOre(name, item);
		} 
	}

}
