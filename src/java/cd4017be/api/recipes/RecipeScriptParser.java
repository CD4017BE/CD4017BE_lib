package cd4017be.api.recipes;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.logging.log4j.Level;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.oredict.OreDictionary;
import cd4017be.api.recipes.RecipeAPI.IRecipeHandler;
import cd4017be.lib.BlockItemRegistry;
import cd4017be.lib.ConfigurationFile;
import cd4017be.lib.util.OreDictStack;
import cd4017be.lib.util.ScriptCompiler;

public class RecipeScriptParser extends ScriptCompiler {

	public static HashMap<ResourceLocation, String> codeCache = new HashMap<ResourceLocation, String>(); 
	private static final String[] functions = {"it", "fl", "ore", "hasit", "hasfl", "nore"};
	private static final String[] methods = {"add"};
	
	public RecipeScriptParser(HashMap<String, Object> vars) {
		super(vars);
	}

	@Override
	protected String[] methods() {
		return methods;
	}

	@Override
	protected String[] functions() {
		return functions;
	}

	@Override
	protected void runMethod(int i, Object[] param, int line) {
		IRecipeHandler h = RecipeAPI.Handlers.get(param[0]);
		if (h == null) FMLLog.log("cd4017be_lib", Level.WARN, "recipe Handler \"%s\" does'nt exist!", param[0]);
		if (!h.addRecipe(param)) FMLLog.log("cd4017be_lib", Level.WARN, "adding recipe failed: \n%s", param);
	}

	@Override
	protected Object runFunction(int i, Object[] param, int line) throws CompileException {
		int n;
		switch(i) {
		case 0: {
			ItemStack item = null;
			if (param[0] instanceof String) {
				String name = (String)param[0];
				if (name.indexOf(':') < 0) item = BlockItemRegistry.stack(name, 1);
				else if (name.startsWith("ore:")) {
					name = name.substring(4);
					List<ItemStack> list = OreDictionary.getOres(name);
					if (list.isEmpty()) throw new CompileException("empty OreDictionary type:", name, line);
					else return list.get(0);
				}
				else item = new ItemStack(Item.getByNameOrId(name));
				if (item == null || item.getItem() == null) throw new CompileException("invalid item name:", name, line);
			} else if (param[0] instanceof ItemStack) item = ((ItemStack)param[0]).copy();
			else if (param[0] instanceof OreDictStack) {
				ItemStack[] arr = ((OreDictStack)param[0]).getItems();
				return Arrays.copyOf(arr, arr.length, Object[].class);
			}
			n = 1;
			if (param.length > n && param[n] instanceof Double) item.stackSize = ((Double)param[n++]).intValue();
			if (param.length > n && param[n] instanceof Double) item.setItemDamage(((Double)param[n++]).intValue());
			if (param.length > n && param[n] instanceof NBTTagCompound) item.setTagCompound((NBTTagCompound)param[n++]);
			return item;
		} case 1: {
			FluidStack fluid = null;
			if (param[0] instanceof String) {
				String name = (String)param[0];
				fluid = FluidRegistry.getFluidStack(name, 0);
				if (fluid == null) throw new CompileException("invalid fluid name:", name, line);
			} else if (param[0] instanceof FluidStack) fluid = ((FluidStack)param[0]).copy();
			n = 1;
			if (param.length > n && param[n] instanceof Double) fluid.amount = ((Double)param[n++]).intValue();
			if (param.length > n && param[n] instanceof NBTTagCompound) fluid.tag = (NBTTagCompound)param[n++];
			return fluid;
		} case 2: {
			OreDictStack ore = null;
			if (param[0] instanceof String) {
				String name = (String)param[0];
				ore = new OreDictStack(name, 1);
			} else if (param[0] instanceof OreDictStack) ore = ((OreDictStack)param[0]).copy();
			if (param.length > 1 && param[1] instanceof Double) ore.stacksize = ((Double)param[1]).intValue();
			return ore;
		} case 3:
			for (Object o: param) {
				String name = (String)o;
				if (name.indexOf(':') < 0) {
					if (BlockItemRegistry.stack(name, 1) == null) return 0D;
				} else if (name.startsWith("ore:")) {
					if (OreDictionary.getOres(name.substring(4)).isEmpty()) return 0D;
				} else if (Item.getByNameOrId(name) == null) return 0D;
			}
			return 1D;
		case 4:
			for (Object o: param)
				if (!FluidRegistry.isFluidRegistered((String)o)) return 0D;
			return 1D;
		case 5:
			if (param[0] instanceof String)
				return (double)OreDictionary.getOres((String)param[0]).size();
			else return 0D;
		}
		return null;
	}

	@Override
	protected Object indexArray(Object array, String index, int recLimit, int line) throws CompileException {
		return null;
	}

	@Override
	public ScriptCompiler extScript(HashMap<String, Object> var, String filename, int recLimit) throws CompileException {
		ResourceLocation res = new ResourceLocation(filename);
		String code = codeCache.get(res);
		if (code == null) try {
			code = ConfigurationFile.readTextFile(ConfigurationFile.getStream(filename));
		} catch(IOException e) {
			throw CompileException.of(e, filename, 0);
		}
		SubMethod method = new SubMethod(code, res);
		RecipeScriptParser script = new RecipeScriptParser(var);
		script.run(method, recLimit);
		return script;
	}

}
