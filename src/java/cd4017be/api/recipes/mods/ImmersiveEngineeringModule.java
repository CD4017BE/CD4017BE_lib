package cd4017be.api.recipes.mods;

import java.lang.reflect.Method;

import org.apache.logging.log4j.Level;

import cd4017be.api.recipes.RecipeModule;
import cd4017be.lib.script.Parameters;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.FMLLog;


/**
 * Provides wrapped config script access to ImmersiveEngineering's recipe API.
 * @author CD4017BE
 */
public class ImmersiveEngineeringModule extends RecipeModule {

	public ImmersiveEngineeringModule() {
		super("IE");
		mkRcp("AlloyRecipe", "Alloy");
		mkRcp("ArcFurnaceRecipe", "Arc");
		mkRcp("BlastFurnaceRecipe", "Blast");
		mkRcp("BottlingMachineRecipe", "Bottle");
		mkRcp("CokeOvenRecipe", "Coke");
		mkRcp("CrusherRecipe", "Crush");
		mkRcp("FermenterRecipe", "Ferment");
		mkRcp("MetalPressRecipe", "Press");
		mkRcp("MixerRecipe", "Mix");
		mkRcp("RefineryRecipe", "Refine");
		mkRcp("SqueezerRecipe", "Squeeze");
		FMLLog.log("CD4017BE_lib|RecipeAPI", Level.INFO, "added ImmersiveEngineering wrapper module");
	}

	private void mkRcp(String name, String id) {
		try {
			Class<?> c = Class.forName("blusunrize.immersiveengineering.api.crafting." + name);
			for (Method m : c.getDeclaredMethods())
				if (m.getName().equals("addRecipe"))
					methods.put("add" + id, new AddRecipe(m));
			Method rem = c.getMethod("removeRecipes", ItemStack.class);
			methods.put("rem" + id, (p)-> rem.invoke(null, p.get(0, ItemStack.class)));
		} catch (ClassNotFoundException | NoSuchMethodException e) {
			FMLLog.log("CD4017BE_lib|RecipeAPI", Level.WARN, "Problem occoured adding ImmersiveEngineering handler %s: %s", name, e.toString());
		}
	}

	private static class AddRecipe implements Handler {

		final Method m;

		AddRecipe(Method m) {
			this.m = m;
		}

		@Override
		public Object handle(Parameters param) throws Exception {
			Class<?>[] types = m.getParameterTypes();
			Object[] p = new Object[types.length];
			for (int i = 0; i < p.length; i++) {
				Class<?> t = types[i];
				if (t == int.class) p[i] = (int)param.getNumber(i);
				else if (t == Object[].class && i == p.length - 1) p[i] = param.param.length > i ? param.getArray(i) : null;
				else if (t == ItemStack.class && i < param.param.length && param.param[i] == null) p[i] = ItemStack.EMPTY;
				else p[i] = param.get(i, t);
			}
			return m.invoke(null, p);
		}

	}

}
