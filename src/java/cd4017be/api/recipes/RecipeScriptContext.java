package cd4017be.api.recipes;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

import javax.script.ScriptException;

import org.apache.logging.log4j.Level;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.oredict.OreDictionary;
import cd4017be.api.recipes.RecipeAPI.IRecipeHandler;
import cd4017be.lib.BlockItemRegistry;
import cd4017be.lib.ConfigurationFile;
import cd4017be.lib.script.Context;
import cd4017be.lib.script.Parameters;
import cd4017be.lib.script.Script;
import cd4017be.lib.script.ScriptFiles;
import cd4017be.lib.script.ScriptFiles.Version;
import cd4017be.lib.util.OreDictStack;

public class RecipeScriptContext extends Context {

	private static final Function<Parameters, Object>
		IT = (p) -> {
			ItemStack item = null;
			Object o = p.get(0);
			if (o instanceof String) {
				String name = (String)o;
				if (name.indexOf(':') < 0) item = BlockItemRegistry.stack(name, 1);
				else if (name.startsWith("ore:")) {
					name = name.substring(4);
					List<ItemStack> list = OreDictionary.getOres(name);
					if (list.isEmpty()) throw new IllegalArgumentException("empty OreDictionary type: " + name);
					item = list.get(0).copy();
				} else item = new ItemStack(Item.getByNameOrId(name));
				if (item == null || item.getItem() == null) throw new IllegalArgumentException("invalid item name: " + name);
			} else if (o instanceof ItemStack) item = ((ItemStack)o).copy();
			else if (o instanceof OreDictStack) {
				ItemStack[] arr = ((OreDictStack)o).getItems();
				return Arrays.copyOf(arr, arr.length, Object[].class);
			} else throw new IllegalArgumentException("expected String, ItemStack or OreDictStack @ 0");
			switch(p.param.length) {
			case 4: item.setTagCompound(p.get(3, NBTTagCompound.class));
			case 3: item.setItemDamage((int)p.getNumber(2));
			case 2: item.stackSize = (int)p.getNumber(1);
			default: return item;
			}
		}, FL = (p) -> {
			FluidStack fluid = null;
			Object o = p.get(0);
			if (o instanceof String) {
				String name = (String)o;
				fluid = FluidRegistry.getFluidStack(name, 0);
				if (fluid == null) throw new IllegalArgumentException("invalid fluid name: " + name);
			} else if (o instanceof FluidStack) fluid = ((FluidStack)o).copy();
			else throw new IllegalArgumentException("expected String or FluidStack @ 0");
			switch(p.param.length) {
			case 3: fluid.tag = p.get(2, NBTTagCompound.class);
			case 2: fluid.amount = (int)p.getNumber(1);
			default: return fluid;
			}
		}, ORE = (p) -> {
			OreDictStack ore = null;
			Object o = p.get(0);
			if (o instanceof String) {
				String name = (String)o;
				ore = new OreDictStack(name, 1);
			} else if (o instanceof OreDictStack) ore = ((OreDictStack)o).copy();
			else throw new IllegalArgumentException("expected String or OreDictStack @ 0");
			if (p.param.length == 2) ore.stacksize = (int)p.getNumber(1);
			return ore;
		}, HASIT = (p) -> {
			for (Object o: p.param) {
				String name = (String)o;
				if (name.indexOf(':') < 0) {
					if (BlockItemRegistry.stack(name, 1) == null) return false;
				} else if (name.startsWith("ore:")) {
					if (OreDictionary.getOres(name.substring(4)).isEmpty()) return false;
				} else if (Item.getByNameOrId(name) == null) return false;
			}
			return true;
		}, HASFL = (p) -> {
			for (Object o: p.param)
				if (!FluidRegistry.isFluidRegistered((String)o)) return false;
			return true;
		}, ORES = (p) -> OreDictionary.getOres(p.getString(0)).toArray()
		, HASMOD = (p) -> Loader.isModLoaded(p.getString(0))
		, ADD = (p) -> {
			IRecipeHandler h = RecipeAPI.Handlers.get(p.getString(0));
			if (h == null) throw new IllegalArgumentException(String.format("recipe Handler \"%s\" does'nt exist!", p.param[0]));
			h.addRecipe(p);
			return null;
		}, LISTORE = (p) -> {
			Pattern filter = Pattern.compile(p.getString(0));
			ArrayList<String> list = new ArrayList<String>();
			for (String name : OreDictionary.getOreNames())
				if (filter.matcher(name).matches()) list.add(name);
			return list.toArray();
		};

	public static final List<Version> scriptRegistry = new ArrayList<Version>();
	static {
		scriptRegistry.add(new Version("core"));
	}
	public static RecipeScriptContext instance;

	public RecipeScriptContext() {
		defFunc.put("it", IT);
		defFunc.put("fl", FL);
		defFunc.put("ore", ORE);
		defFunc.put("hasit", HASIT);
		defFunc.put("hasfl", HASFL);
		defFunc.put("ores", ORES);
		defFunc.put("hasmod", HASMOD);
		defFunc.put("add", ADD);
		defFunc.put("listore", LISTORE);
	}

	public void setup(FMLPreInitializationEvent event) {
		File dir = new File(event.getModConfigurationDirectory(), "cd4017be");
		HashMap<String, Version> versions = new HashMap<String, Version>();
		for (Version v : scriptRegistry)
			if (v.fallback != null)
				versions.put(v.name, v);
		Script[] scripts;
		try {
			scripts = ScriptFiles.loadPackage(new File(dir, "compiled.dat"), versions);
		} catch (IOException e) {
			scripts = null;
			FMLLog.log(Level.ERROR, e, "loading compiled config scripts failed!");
		}
		for (Version v : versions.values())
			try {
				ConfigurationFile.copyData(v.fallback, new File(dir, v.name + ".rcp"));
			} catch (IOException e) {
				FMLLog.log(Level.ERROR, e, "copying script preset failed!");
			}
		if (scripts == null) scripts = ScriptFiles.createCompiledPackage(new File(dir, "compiled.dat"));
		if (scripts != null) for (Script s : scripts) add(s);
	}

	public void runAll(String p) {
		for (Version v : scriptRegistry)
			run(v.name + "." + p);
	}

	public void run(String name) {
		reset();
		try {
			invoke(name, new Parameters());
		} catch (NoSuchMethodException e) {
			FMLLog.log("RECIPE_SCRIPT", Level.INFO, "skipped %s", name);
		} catch (ScriptException e) {
			FMLLog.log("RECIPE_SCRIPT", Level.ERROR, e, "script execution failed for %s", name);
		}
	}

}
