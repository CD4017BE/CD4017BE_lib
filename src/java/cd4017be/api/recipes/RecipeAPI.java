package cd4017be.api.recipes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import org.apache.logging.log4j.Level;

import cd4017be.api.recipes.AutomationRecipes.*;
import cd4017be.lib.NBTRecipe;
import cd4017be.lib.util.ScriptCompiler;
import cd4017be.lib.util.ScriptCompiler.CompileException;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.IFuelHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

public class RecipeAPI {

	public static HashMap<String, IRecipeHandler> Handlers;
	
	static {
		Handlers = new HashMap<String, IRecipeHandler>();
		Handlers.put("shapeless", new ShapelessCraftingHandler());
		Handlers.put("shaped", new ShapedCraftingHandler());
		Handlers.put("shapedNBT", new NBTCraftingHandler());
		Handlers.put("ore", new OreDictionaryHandler());
		Handlers.put("smelt", new SmeltingHandler());
		Handlers.put("fuel", new FuelHandler());
		if (Loader.isModLoaded("Automation")){
			Handlers.put("compAs", new MechanicAssemblerHandler());
			Handlers.put("advFurn", new ThermalAssemblerHandler());
			Handlers.put("electr", new ElectrolyserHandler());
			Handlers.put("cool", new DecompCoolHandler());
			Handlers.put("trash", new GravitationalCondHandler());
			Handlers.put("heatRad", new HeatRadiatorHandler());
		}
	}
	
	public static void loadRecipes(String fileName) {
		RecipeScriptParser p = new RecipeScriptParser(new HashMap<String, Object>());
		try {
			p.extScript(p.variables, fileName, ScriptCompiler.defaultRecLimit);
		} catch (CompileException e) {
			FMLLog.log("cd4017be_lib", Level.ERROR, e, "recipe script loading failed for %s", fileName);
		}
	}
	
	public static interface IRecipeHandler {
		public boolean addRecipe(Object... param);
	}
	
	static class ShapelessCraftingHandler implements IRecipeHandler {
		@Override
		public boolean addRecipe(Object... param) {
			if (param.length < 3 || !(param[1] instanceof ItemStack)) return false;
			GameRegistry.addRecipe(new ShapelessOreRecipe((ItemStack)param[1], Arrays.copyOfRange(param, 2, param.length)));
			return true;
		}
	}
	
	static class ShapedCraftingHandler implements IRecipeHandler {
		@Override
		public boolean addRecipe(Object... param) {
			if (param.length < 4 || !(param[1] instanceof ItemStack && param[2] instanceof String)) return false;
			String[] pattern = ((String)param[2]).split("/");
			int n = param.length - 3;
			Object[] arr = new Object[n * 2 + pattern.length];
			for (int i = 0; i < pattern.length; i++) arr[i] = pattern[i];
			for (int i = 0; i < n; i++) {
				arr[pattern.length + i * 2] = Character.forDigit(i, 9);
				arr[pattern.length + i * 2 + 1] = param[i + 3];
			}
			GameRegistry.addRecipe(new ShapedOreRecipe((ItemStack)param[1], arr));
			return true;
		}
	}
	
	static class NBTCraftingHandler implements IRecipeHandler {
		@Override
		public boolean addRecipe(Object... param) {
			if (param.length < 5 || !(param[1] instanceof String && param[2] instanceof ItemStack && param[3] instanceof String)) return false;
			String[] pattern = ((String)param[3]).split("/");
			int n = param.length - 4;
			Object[] arr = new Object[n * 2 + pattern.length];
			for (int i = 0; i < pattern.length; i++) arr[i] = pattern[i];
			for (int i = 0; i < n; i++) {
				arr[pattern.length + i * 2] = Character.forDigit(i, 9);
				arr[pattern.length + i * 2 + 1] = param[i + 4];
			}
			GameRegistry.addRecipe(new NBTRecipe((ItemStack)param[2], (String)param[1], arr));
			return true;
		}
	}
	
	static class OreDictionaryHandler implements IRecipeHandler {
		@Override
		public boolean addRecipe(Object... param) {
			if (param.length < 3 || !(param[1] instanceof String)) return false;
			for (int i = 2; i < param.length; i++) {
				if (!(param[i] instanceof ItemStack)) return false;
				OreDictionary.registerOre((String)param[1], (ItemStack)param[i]);
			}
			return true;
		}
	}
	
	static class SmeltingHandler implements IRecipeHandler {
		@Override
		public boolean addRecipe(Object... param) {
			if (param.length < 3 || !(param[1] instanceof ItemStack && param[2] instanceof ItemStack)) return false;
			float xp;
			if (param.length < 4) xp = 0;
			else if (param[3] instanceof Double) xp = ((Double)param[3]).floatValue();
			else return false;
			GameRegistry.addSmelting((ItemStack)param[1], (ItemStack)param[2], xp);
			return true;
		}
	}
	
	static class FuelHandler implements IRecipeHandler, IFuelHandler {
		HashMap<Integer, Integer> fuelList;
		public FuelHandler() {
			fuelList = new HashMap<Integer, Integer>();
			GameRegistry.registerFuelHandler(this);
		}
		int key(ItemStack item) {
			return Item.getIdFromItem(item.getItem()) & 0xffff | (item.getItemDamage() & 0xffff) << 16;
		}
		@Override
		public boolean addRecipe(Object... param) {
			if (param.length != 3 || !(param[1] instanceof ItemStack && param[2] instanceof Double)) return false;
			fuelList.put(key((ItemStack)param[1]), ((Double)param[2]).intValue());
			return true;
		}
		@Override
		public int getBurnTime(ItemStack fuel) {
			Integer val = fuelList.get(key(fuel));
			return val == null ? 0 : val;
		}
	}
	
	static class MechanicAssemblerHandler implements IRecipeHandler {
		@Override
		public boolean addRecipe(Object... param) {
			if (param.length < 3 || param.length > 6 || !(param[1] instanceof ItemStack)) return false;
			AutomationRecipes.addCmpRecipe((ItemStack)param[1], Arrays.copyOfRange(param, 2, param.length));
			return true;
		}
	}
	
	static class ThermalAssemblerHandler implements IRecipeHandler {
		@Override
		public boolean addRecipe(Object... param) {
			if (param.length < 4 || !(param[1] instanceof Object[] && param[2] instanceof Object[] && param[3] instanceof Double)) return false;
			FluidStack Fin = null;
			FluidStack Fout = null;
			ArrayList<Object> Iin = new ArrayList<Object>();
			ArrayList<ItemStack> Iout = new ArrayList<ItemStack>();
			for (Object o : (Object[])param[1]) {
				if (o instanceof FluidStack) Fin = (FluidStack)o;
				else Iin.add(o);
			}
			for (Object o : (Object[])param[2]) {
				if (o instanceof FluidStack) Fout = (FluidStack)o;
				else if (o instanceof ItemStack) Iout.add((ItemStack)o);
				else return false;
			}
			AutomationRecipes.addRecipe(new LFRecipe(Fin, Iin.isEmpty() ? null : Iin.toArray(new Object[Iin.size()]), Fout, Iout.isEmpty() ? null : Iout.toArray(new ItemStack[Iout.size()]), ((Double)param[3]).floatValue()));
			return true;
		}
	}
	
	static class ElectrolyserHandler implements IRecipeHandler {
		@Override
		public boolean addRecipe(Object... param) {
			if (param.length < 5 || !(param[4] instanceof Double)) return false;
			AutomationRecipes.addRecipe(new ElRecipe(param[1], param[2], param[3], ((Double)param[4]).floatValue()));
			return true;
		}
	}
	
	static class DecompCoolHandler implements IRecipeHandler {
		@Override
		public boolean addRecipe(Object... param) {
			if (param.length < 6 || !(param[5] instanceof Double)) return false;
			AutomationRecipes.addRecipe(new CoolRecipe(param[1], param[2], param[3], param[4], ((Double)param[5]).floatValue()));
			return true;
		}
	}
	
	static class GravitationalCondHandler implements IRecipeHandler {
		@Override
		public boolean addRecipe(Object... param) {
			if (param.length < 4 || !(param[1] instanceof ItemStack && param[2] instanceof ItemStack && param[3] instanceof Double)) return false;
			AutomationRecipes.addRecipe(new GCRecipe((ItemStack)param[1], (ItemStack)param[2], ((Double)param[3]).intValue()));
			return true;
		}
	}
	
	static class HeatRadiatorHandler implements IRecipeHandler {
		@Override
		public boolean addRecipe(Object... param) {
			if (param.length < 3 || !(param[1] instanceof FluidStack && param[2] instanceof FluidStack)) return false;
			AutomationRecipes.addRadiatorRecipe((FluidStack)param[1], (FluidStack)param[2]);
			return true;
		}
	}

}
