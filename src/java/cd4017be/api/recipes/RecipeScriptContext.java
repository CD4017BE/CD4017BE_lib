package cd4017be.api.recipes;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

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
import cd4017be.lib.script.Script;
import cd4017be.lib.script.ScriptFiles;
import cd4017be.lib.script.ScriptFiles.Version;
import cd4017be.lib.util.OreDictStack;

public class RecipeScriptContext extends Context {

	private static final Function<Object[], Object>
		IT = (p) -> {
			ItemStack item = null;
			if (p[0] instanceof String) {
				String name = (String)p[0];
				if (name.indexOf(':') < 0) item = BlockItemRegistry.stack(name, 1);
				else if (name.startsWith("ore:")) {
					name = name.substring(4);
					List<ItemStack> list = OreDictionary.getOres(name);
					if (list.isEmpty()) throw new IllegalArgumentException("empty OreDictionary type: " + name);
					item = list.get(0).copy();
				} else item = new ItemStack(Item.getByNameOrId(name));
				if (item == null || item.getItem() == null) throw new IllegalArgumentException("invalid item name: " + name);
			} else if (p[0] instanceof ItemStack) item = ((ItemStack)p[0]).copy();
			else if (p[0] instanceof OreDictStack) {
				ItemStack[] arr = ((OreDictStack)p[0]).getItems();
				return Arrays.copyOf(arr, arr.length, Object[].class);
			}
			int n = 1;
			if (p.length > n && p[n] instanceof Double) item.stackSize = ((Double)p[n++]).intValue();
			if (p.length > n && p[n] instanceof Double) item.setItemDamage(((Double)p[n++]).intValue());
			if (p.length > n && p[n] instanceof NBTTagCompound) item.setTagCompound((NBTTagCompound)p[n++]);
			return item;
		}, FL = (p) -> {
			FluidStack fluid = null;
			if (p[0] instanceof String) {
				String name = (String)p[0];
				fluid = FluidRegistry.getFluidStack(name, 0);
				if (fluid == null) throw new IllegalArgumentException("invalid fluid name: " + name);
			} else if (p[0] instanceof FluidStack) fluid = ((FluidStack)p[0]).copy();
			int n = 1;
			if (p.length > n && p[n] instanceof Double) fluid.amount = ((Double)p[n++]).intValue();
			if (p.length > n && p[n] instanceof NBTTagCompound) fluid.tag = (NBTTagCompound)p[n++];
			return fluid;
		}, ORE = (p) -> {
			OreDictStack ore = null;
			if (p[0] instanceof String) {
				String name = (String)p[0];
				ore = new OreDictStack(name, 1);
			} else if (p[0] instanceof OreDictStack) ore = ((OreDictStack)p[0]).copy();
			if (p.length > 1 && p[1] instanceof Double) ore.stacksize = ((Double)p[1]).intValue();
			return ore;
		}, HASIT = (p) -> {
			for (Object o: p) {
				String name = (String)o;
				if (name.indexOf(':') < 0) {
					if (BlockItemRegistry.stack(name, 1) == null) return false;
				} else if (name.startsWith("ore:")) {
					if (OreDictionary.getOres(name.substring(4)).isEmpty()) return false;
				} else if (Item.getByNameOrId(name) == null) return false;
			}
			return true;
		}, HASFL = (p) -> {
			for (Object o: p)
				if (!FluidRegistry.isFluidRegistered((String)o)) return false;
			return true;
		}, ORES = (p) -> {
			if (p[0] instanceof String)
				return OreDictionary.getOres((String)p[0]).toArray();
			else return null;
		}, HASMOD = (p) -> {
			return p[0] instanceof String && Loader.isModLoaded((String)p[0]);
		}, ADD = (p) -> {
			IRecipeHandler h = RecipeAPI.Handlers.get(p[0]);
			if (h == null) FMLLog.log("cd4017be_lib", Level.WARN, "recipe Handler \"%s\" does'nt exist!", p[0]);
			if (!h.addRecipe(p)) FMLLog.log("cd4017be_lib", Level.WARN, "adding recipe failed: \n%s", p);
			return null;
		};

	public static final List<Version> scriptRegistry = new ArrayList<Version>();

	public RecipeScriptContext() {
		defFunc.put("it", IT);
		defFunc.put("fl", FL);
		defFunc.put("ore", ORE);
		defFunc.put("hasit", HASIT);
		defFunc.put("hasfl", HASFL);
		defFunc.put("ores", ORES);
		defFunc.put("hasmod", HASMOD);
		defFunc.put("add", ADD);
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
			for (Version v : versions.values())
				ConfigurationFile.copyData(v.fallback, new File(dir, v.name + ".scr"));
		} catch (IOException e) {
			scripts = null;
			FMLLog.log(Level.ERROR, e, "loading compiled config scripts failed!");
		}
		if (scripts == null) scripts = ScriptFiles.createCompiledPackage(new File(dir, "compiled.dat"));
		for (Script s : scripts) modules.put(s.fileName, s);
	}

	public void run(String ph) {
		for (Version v : scriptRegistry) {
			reset();
			String func = v.name + "." + ph;
			try {
				invoke(func);
			} catch (NoSuchMethodException e) {
				FMLLog.log("RECIPE_SCRIPT", Level.INFO, "skipped %s", func);
			} catch (ScriptException e) {
				FMLLog.log("RECIPE_SCRIPT", Level.ERROR, e, "script execution failed for %s", v.name);
			}
		}
	}

}
