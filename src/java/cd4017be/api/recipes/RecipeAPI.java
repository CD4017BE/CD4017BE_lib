package cd4017be.api.recipes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import cd4017be.api.recipes.AutomationRecipes.*;
import cd4017be.lib.Lib;
import cd4017be.lib.NBTRecipe;
import cd4017be.lib.script.Parameters;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.IFuelHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

@SuppressWarnings("deprecation")
public class RecipeAPI {

	public static interface IRecipeHandler {
		public void addRecipe(Parameters param);
	}

	public static HashMap<String, IRecipeHandler> Handlers;

	static {
		Handlers = new HashMap<String, IRecipeHandler>();
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
		Handlers.put("shapeless", (p) -> GameRegistry.addRecipe(new ShapelessOreRecipe(p.get(1, ItemStack.class), Arrays.copyOfRange(p.param, 2, p.param.length))));
		Handlers.put("smelt", (p) -> GameRegistry.addSmelting(p.get(1, ItemStack.class), p.get(2, ItemStack.class), p.param.length > 3 ? (float)p.getNumber(3) : 0F));
		Handlers.put("fuel", new FuelHandler());
		Handlers.put("worldgen", new OreGenHandler());
		Handlers.put("item", (p) -> Lib.materials.addMaterial((int)p.getNumber(1), p.getString(2)));
		Handlers.put("fluidCont", (p) -> FluidContainerRegistry.registerFluidContainer(p.get(1, FluidStack.class), p.get(2, ItemStack.class), p.get(3, ItemStack.class)));
		if (Loader.isModLoaded("Automation")) {
			Handlers.put("advFurn", (p) -> {
				FluidStack Fin = null;
				FluidStack Fout = null;
				ArrayList<Object> Iin = new ArrayList<Object>();
				ArrayList<ItemStack> Iout = new ArrayList<ItemStack>();
				for (Object o : p.getArray(1)) {
					if (o instanceof FluidStack) Fin = (FluidStack)o;
					else Iin.add(o);
				}
				for (Object o : p.getArray(2)) {
					if (o instanceof FluidStack) Fout = (FluidStack)o;
					else if (o instanceof ItemStack) Iout.add((ItemStack)o);
					else throw new IllegalArgumentException("expected ItemStack or FluidStack as element of array @ 2");
				}
				AutomationRecipes.addRecipe(new LFRecipe(Fin, Iin.isEmpty() ? null : Iin.toArray(new Object[Iin.size()]), Fout, Iout.isEmpty() ? null : Iout.toArray(new ItemStack[Iout.size()]), (float)p.getNumber(3)));
			});
			Handlers.put("compAs", (p) -> AutomationRecipes.addCmpRecipe(p.get(1, ItemStack.class), Arrays.copyOfRange(p.param, 2, p.param.length)));
			Handlers.put("electr", (p) -> AutomationRecipes.addRecipe(new ElRecipe(p.get(1), p.get(2), p.get(3), (float)p.getNumber(4))));
			Handlers.put("cool", (p) -> AutomationRecipes.addRecipe(new CoolRecipe(p.get(1), p.get(2), p.get(3), p.get(4), (float)p.getNumber(5))));
			Handlers.put("trash", (p) -> AutomationRecipes.addRecipe(new GCRecipe(p.get(1, ItemStack.class), p.get(2, ItemStack.class), (int)p.getNumber(3))));
			Handlers.put("heatRad", (p) -> AutomationRecipes.addRadiatorRecipe(p.get(1, FluidStack.class), p.get(2, FluidStack.class)));
			Handlers.put("algae", (p) -> AutomationRecipes.bioList.add(new BioEntry(p.get(1), (int)p.getNumber(2), (int)p.getNumber(3))));
		}
	}

	private static class FuelHandler implements IRecipeHandler, IFuelHandler {
		HashMap<Integer, Integer> fuelList;
		public FuelHandler() {
			fuelList = new HashMap<Integer, Integer>();
			GameRegistry.registerFuelHandler(this);
		}
		int key(ItemStack item) {
			return Item.getIdFromItem(item.getItem()) & 0xffff | (item.getItemDamage() & 0xffff) << 16;
		}
		@Override
		public void addRecipe(Parameters p) {
			fuelList.put(key(p.get(1, ItemStack.class)), (int)p.getNumber(2));
		}
		@Override
		public int getBurnTime(ItemStack fuel) {
			Integer val = fuelList.get(key(fuel));
			return val == null ? 0 : val;
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
