package cd4017be.api.recipes;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import cd4017be.lib.Lib;
import cd4017be.lib.script.Function.Iterator;
import cd4017be.lib.script.Function.ArrayIterator;
import cd4017be.lib.script.Function.ListIterator;
import cd4017be.lib.templates.NBTRecipe;
import cd4017be.lib.script.Function.FilteredIterator;
import cd4017be.lib.script.Parameters;
import cd4017be.lib.util.OreDictStack;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.OreIngredient;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import net.minecraftforge.registries.IForgeRegistryEntry.Impl;

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
	private static final HashMap<String, Integer> UsedNames;

	static {
		Handlers = new HashMap<String, IRecipeHandler>();
		Lists = new HashMap<String, IRecipeList>();
		UsedNames = new HashMap<String, Integer>();
		Handlers.put("shaped", (p) -> addRecipe(new ShapedOreRecipe(null, p.get(1, ItemStack.class), decodePattern(p, 2))));
		Handlers.put("shapedNBT", (p) -> addRecipe(new NBTRecipe(null, p.get(2, ItemStack.class), p.getString(1), decodePattern(p, 3))));
		Handlers.put("ore", (p) -> {
			String name = p.getString(1);
			for (int i = 2; i < p.param.length; i++)
				OreDictionary.registerOre(name, p.get(i, ItemStack.class));
		});
		Lists.put("ore", (p) -> new FilteredIterator(new ArrayIterator(OreDictionary.getOreNames()), new RegexFilter(p.getString(1))));
		Lists.put("craftIng", (p) -> new CraftingRecipeIterator(getFilter(p.get(1)), false));
		Lists.put("craftRes", (p) -> new CraftingRecipeIterator(getFilter(p.get(1)), true));
		Handlers.put("shapeless", (p) -> addRecipe(new ShapelessOreRecipe(null, p.get(1, ItemStack.class), Arrays.copyOfRange(p.param, 2, p.param.length))));
		Handlers.put("smelt", (p) -> GameRegistry.addSmelting(p.get(1, ItemStack.class), p.get(2, ItemStack.class), p.param.length > 3 ? (float)p.getNumber(3) : 0F));
		Handlers.put("fuel", new FuelHandler());
		Handlers.put("worldgen", new OreGenHandler());
		Handlers.put("item", (p) -> Lib.materials.addMaterial((int)p.getNumber(1), p.getString(2)));
		//TODO Handlers.put("fluidCont", (p) -> FluidContainerRegistry.registerFluidContainer(p.get(1, FluidStack.class), p.get(2, ItemStack.class), p.get(3, ItemStack.class)));
	}

	private static Object[] decodePattern(Parameters p, int i0) {
		String s = p.getString(i0++);
		String[] pattern = s.split("/");
		int n = p.param.length - i0;
		Object[] arr = new Object[n * 2 + pattern.length];
		int j = 0;
		for (String l : pattern) arr[j++] = l;
		for (int i = 0; i < n; i++) {
			char c = Character.forDigit(i, 9);
			if (s.indexOf(c) < 0) continue;
			arr[j++] = c;
			arr[j++] = p.param[i + i0];
		}
		return j < arr.length ? Arrays.copyOf(arr, j) : arr;
	}

	public static String genericName(IRecipe rcp) {
		ItemStack stack = rcp.getRecipeOutput();
		Item item = stack.getItem();
		ResourceLocation res = item.getRegistryName();
		String name = res.getResourceDomain() + "/" + res.getResourcePath();
		if (item.getHasSubtypes()) name += "_" + stack.getItemDamage();
		int n = UsedNames.getOrDefault(name, 0);
		UsedNames.put(name, n + 1);
		if (n > 0) name += "_" + asLetter(n);
		return name;
	}

	private static String asLetter(int i) {
		String s = "";
		do {
			s += (char)(i % 26 + 'a');
			i /= 26;
		} while (i > 26);
		return s;
	}

	private static <T extends Impl<IRecipe> & IRecipe> void addRecipe(T rcp) {
		ForgeRegistries.RECIPES.register(rcp.setRegistryName(genericName(rcp)));
	}

	private static class FuelHandler implements IRecipeHandler {

		HashMap<Integer, Integer> fuelList;

		public FuelHandler() {
			fuelList = new HashMap<Integer, Integer>();
			MinecraftForge.EVENT_BUS.register(this);
		}

		int key(ItemStack item) {
			return Item.getIdFromItem(item.getItem()) & 0xffff | (item.getItemDamage() & 0xffff) << 16;
		}

		@Override
		public void addRecipe(Parameters p) {
			fuelList.put(key(p.get(1, ItemStack.class)), (int)p.getNumber(2));
		}

		@SubscribeEvent
		public void getBurnTime(FurnaceFuelBurnTimeEvent ev) {
			Integer val = fuelList.get(key(ev.getItemStack()));
			if (val != null) ev.setBurnTime(val);
		}

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

	private static class CraftingRecipeIterator implements Iterator {
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
			if (o == null) list.remove();
			else if (o == curElement && curElement[0] != curRecipe.getRecipeOutput()) {
				if (!(curElement[0] instanceof ItemStack)) throw new IllegalArgumentException("ItemStack expected");
				ItemStack item = (ItemStack)curElement[0];
				ItemStack res = curRecipe.getRecipeOutput();
				//TODO replace by new recipe with different result
			}
		}

		@Override
		public boolean next() {
			while (list.hasNext()) {
				curRecipe = list.next();
				ItemStack result = curRecipe.getRecipeOutput();
				if (!(in || key.test(result))) continue;
				Iterator ingred = new IngredientIterator(curRecipe.getIngredients(), key);
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

	private static class IngredientIterator extends ListIterator<Ingredient> {//TODO fix this
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
