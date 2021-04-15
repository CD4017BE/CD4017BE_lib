package cd4017be.api.recipes;

import java.util.Arrays;
import java.util.HashMap;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import cd4017be.api.recipes.vanilla.CraftingRecipeIterator;
import cd4017be.api.recipes.vanilla.FuelHandler;
import cd4017be.api.recipes.vanilla.SmeltingIterator;
import cd4017be.lib.script.Parameters;
import cd4017be.lib.script.obj.FilteredIterator;
import cd4017be.lib.script.obj.IOperand;
import cd4017be.lib.script.obj.IOperand.OperandIterator;
import cd4017be.lib.script.obj.Text;
import net.minecraft.item.ItemStack;

/**
 * 
 * @author CD4017BE
 */
public class RecipeAPI {

	public static interface IRecipeHandler {
		public void addRecipe(Parameters param);
	}

	public static interface IRecipeList {
		public OperandIterator list(Parameters param);
	}

	public static final HashMap<String, IRecipeHandler> Handlers;
	public static final HashMap<String, IRecipeList> Lists;
	private static final HashMap<String, Integer> UsedNames;

	static {
		Handlers = new HashMap<String, IRecipeHandler>();
		Lists = new HashMap<String, IRecipeList>();
		UsedNames = new HashMap<String, Integer>();
		//Handlers.put("shaped", (p) -> addRecipe(new ShapedOreRecipe(null, p.get(1, ItemStack.class), decodePattern(p, 2))));
		//Handlers.put("shapedNBT", (p) -> addRecipe(new NBTRecipe(null, p.get(2, ItemStack.class), p.getString(1), decodePattern(p, 3))));
		/*Handlers.put("ore", (p) -> {
			String name = p.getString(1);
			for (int i = 2; i < p.param.length; i++)
				OreDictionary.registerOre(name, p.get(i, ItemStack.class));
		});*/
		Lists.put("ore", (p) -> new FilteredIterator(new OreDictList(), new RegexFilter(p.getString(1))));
		Lists.put("craftIng", (p) -> new CraftingRecipeIterator(p.get(1, ItemStack.class)));
		Lists.put("craftRes", (p) -> new CraftingRecipeIterator(getFilter(p.param[1])));
		Lists.put("smeltIng", (p) -> new SmeltingIterator(getFilter(p.param[1]), false));
		Lists.put("smeltRes", (p) -> new SmeltingIterator(getFilter(p.param[1]), true));
		//Handlers.put("shapeless", (p) -> addRecipe(new ShapelessOreRecipe(null, p.get(1, ItemStack.class), p.getArrayOrAll(2))));
		//Handlers.put("smelt", (p) -> GameRegistry.addSmelting(p.get(1, ItemStack.class), p.get(2, ItemStack.class), p.param.length > 3 ? (float)p.getNumber(3) : 0F));
		Handlers.put("fuel", new FuelHandler());
		Handlers.put("worldgen", new OreGenHandler());
		/*Handlers.put("item", (p) -> {
			int n = p.param.length;
			Lib.materials.addMaterial((int)p.getNumber(1), p.getString(2), n > 3 ? p.getString(3) : null, n > 4 ? p.getString(4) : null);
		});*/
		//TODO Handlers.put("fluidCont", (p) -> FluidContainerRegistry.registerFluidContainer(p.get(1, FluidStack.class), p.get(2, ItemStack.class), p.get(3, ItemStack.class)));
	}

	public static void addModModules(RecipeScriptContext cont) {
		//if (FMLLoader.isModLoaded("immersiveengineering")) cont.add(new ImmersiveEngineeringModule());
		//TODO include more mods
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
			arr[j++] = p.param[i + i0].value();
		}
		return j < arr.length ? Arrays.copyOf(arr, j) : arr;
	}

	/*
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

	public static <T extends Impl<IRecipe> & IRecipe> void addRecipe(T rcp) {
		ForgeRegistries.RECIPES.register(rcp.setRegistryName(genericName(rcp)));
	}*/

	public static Predicate<Object> getFilter(IOperand o) {
		if (o instanceof Text) return new RegexFilter(((Text)o).value);
		return (p) -> {
			return /*p instanceof IOperand ? o.nlsR((IOperand)p).asBool() :*/ o.value().equals(p);
		};
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

	/*
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
	}*/

}
