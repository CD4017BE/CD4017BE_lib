package cd4017be.api.recipes.mods;

import java.lang.reflect.Method;

import cd4017be.api.recipes.RecipeModule;
import cd4017be.api.recipes.RecipeScriptContext;
import cd4017be.lib.script.Parameters;
import cd4017be.lib.script.obj.IOperand;
import cd4017be.lib.script.obj.Nil;
import net.minecraft.item.ItemStack;


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
		RecipeScriptContext.instance.LOG.info(RecipeScriptContext.SCRIPT, "added ImmersiveEngineering wrapper module");
	}

	private void mkRcp(String name, String id) {
		try {
			Class<?> c = Class.forName("blusunrize.immersiveengineering.api.crafting." + name);
			for (Method m : c.getDeclaredMethods())
				if (m.getName().equals("addRecipe"))
					methods.put("add" + id, new AddRecipe(m));
			Method rem = c.getMethod("removeRecipes", ItemStack.class);
			methods.put("rem" + id, (p)-> {
				rem.invoke(null, p.get(0, ItemStack.class));
				return Nil.NIL;
			});
		} catch (ClassNotFoundException | NoSuchMethodException e) {
			RecipeScriptContext.instance.LOG.warn(RecipeScriptContext.SCRIPT, "Problem occoured adding ImmersiveEngineering handler {}: {}", name, e.toString());
		}
	}

	private static class AddRecipe implements Handler {

		final Method m;

		AddRecipe(Method m) {
			this.m = m;
		}

		@Override
		public IOperand handle(Parameters param) throws Exception {
			Class<?>[] types = m.getParameterTypes();
			Object[] p = new Object[types.length];
			for (int i = 0; i < p.length; i++) {
				Class<?> t = types[i];
				if (t == int.class) p[i] = param.getIndex(i);
				else if (t == float.class) p[i] = (float)param.getNumber(i);
				else if (t == double.class) p[i] = param.getNumber(i);
				else if (t == Object[].class && i == p.length - 1) p[i] = param.getArrayOrAll(i);
				else if (t == ItemStack.class && param.has(i) && param.param[i] == Nil.NIL) p[i] = ItemStack.EMPTY;
				else p[i] = param.get(i, t);
			}
			m.invoke(null, p);
			return Nil.NIL;
		}

	}

}
