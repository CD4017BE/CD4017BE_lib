package cd4017be.api.recipes;

import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.Logger;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import cd4017be.lib.script.Context;
import cd4017be.lib.script.Module;
import cd4017be.lib.script.Parameters;
import cd4017be.lib.script.ScriptFiles.Version;
import cd4017be.lib.script.obj.IOperand;
import cd4017be.lib.script.obj.Nil;
import cd4017be.lib.script.obj.Number;
import cd4017be.lib.script.obj.ObjWrapper;
import cd4017be.lib.script.obj.Text;
import cd4017be.lib.script.obj.Vector;

/**
 * 
 * @author CD4017BE
 */
public class RecipeScriptContext extends Context {

	/*
	private static final Function<Parameters, IOperand>
		IT = (p) -> {
			ItemStack item = null;
			Object o = p.get(0);
			if (o instanceof String) {
				String name = (String)o;
				if (name.indexOf(':') < 0) item = BlockItemRegistry.stack(name, 1);
				else if (name.startsWith("ore:")) {
					name = name.substring(4);
					List<ItemStack> list = OreDictionary.getOres(name, false);
					if (!list.isEmpty()) item = list.get(0).copy();
				} else item = new ItemStack(Item.getByNameOrId(name));
				if (item == null || item.getItem() == null) return Nil.NIL;
			} else if (o instanceof ItemStack) item = ((ItemStack)o).copy();
			else if (o instanceof OreDictStack) {
				ItemStack[] arr = ((OreDictStack)o).getItems();
				int l = arr.length;
				Array a = new Array(l);
				IOperand[] ops = a.array;
				for (int i = l - 1; i >= 0; i--)
					ops[i] = new ItemOperand(arr[i]).onCopy();
				return a;
			} else throw new IllegalArgumentException("expected String, ItemStack or OreDictStack @ 0");
			switch(p.param.length) {
			case 4: item.setTagCompound(p.get(3, NBTTagCompound.class));
			case 3: item.setItemDamage((int)p.getNumber(2));
			case 2: item.setCount((int)p.getNumber(1));
			default: return new ItemOperand(item);
			}
		}, FL = (p) -> {
			FluidStack fluid = null;
			Object o = p.get(0);
			if (o instanceof String) {
				String name = (String)o;
				fluid = FluidRegistry.getFluidStack(name, 0);
				if (fluid == null) return Nil.NIL;
			} else if (o instanceof FluidStack) fluid = ((FluidStack)o).copy();
			else throw new IllegalArgumentException("expected String or FluidStack @ 0");
			switch(p.param.length) {
			case 3: fluid.tag = p.get(2, NBTTagCompound.class);
			case 2: fluid.amount = (int)p.getNumber(1);
			default: return new FluidOperand(fluid);
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
			for (int i = p.param.length - 1; i >= 0; i--) {
				String name = p.getString(i);
				if (name.indexOf(':') < 0) {
					if (BlockItemRegistry.stack(name, 1) == null) return Number.FALSE;
				} else if (name.startsWith("ore:")) {
					if (OreDictionary.getOres(name.substring(4), false).isEmpty()) return Number.FALSE;
				} else if (Item.getByNameOrId(name) == null) return Number.FALSE;
			}
			return Number.TRUE;
		}, HASFL = (p) -> {
			for (int i = p.param.length - 1; i >= 0; i--)
				if (!FluidRegistry.isFluidRegistered(p.getString(i))) return Number.FALSE;
			return Number.TRUE;
		}, HASMOD = (p) -> Loader.isModLoaded(p.getString(0)) ? Number.TRUE : Number.FALSE
		, ADD = (p) -> {
			IRecipeHandler h = RecipeAPI.Handlers.get(p.getString(0));
			if (h == null) throw new IllegalArgumentException(String.format("recipe Handler \"%s\" does'nt exist!", p.param[0]));
			h.addRecipe(p);
			return null;
		}, LISTORE = (p) -> {
			Pattern filter = Pattern.compile(p.getString(0));
			ArrayList<IOperand> list = new ArrayList<>();
			for (String name : OreDictionary.getOreNames()) {
				Matcher m = filter.matcher(name);
				if (!m.matches()) continue;
				int n = m.groupCount();
				if (n == 0) list.add(new Text(name));
				else {
					IOperand[] arr = new IOperand[n + 1];
					for (int i = 0; i <= n; i++)
						arr[i] = new Text(m.group(i));
					list.add(new Array(arr));
				}
			}
			return new Array(list.toArray(new IOperand[list.size()]));
		}, LIST = (p) -> {
			IRecipeList l = RecipeAPI.Lists.get(p.getString(0));
			if (l == null) throw new IllegalArgumentException(String.format("recipe List \"%s\" does'nt exist!", p.param[0]));
			return l.list(p);
		}, NBT = (p) -> {
			if (!p.has(0)) return new NBTWrapper();
			Object o = p.get(0);
			if (o instanceof ItemStack && ((ItemStack)o).hasTagCompound())
				return new NBTWrapper(((ItemStack)o).getTagCompound());
			else if (o instanceof FluidStack && ((FluidStack)o).tag != null)
				return new NBTWrapper(((FluidStack)o).tag);
			else return Nil.NIL;
		};
*/
	public static final List<Version> scriptRegistry = new ArrayList<Version>();
	static {
		//scriptRegistry.add(new Version(Lib.ConfigName, "/assets/" + Lib.ID + "/config/core.rcp"));
	}
	public static RecipeScriptContext instance;

	public RecipeScriptContext(Logger log) {
		super(log);
		/*
		defFunc.put("it", IT);
		defFunc.put("fl", FL);
		defFunc.put("ore", ORE);
		defFunc.put("hasit", HASIT);
		defFunc.put("hasfl", HASFL);
		defFunc.put("hasmod", HASMOD);
		defFunc.put("add", ADD);
		defFunc.put("listore", LISTORE);
		defFunc.put("list", LIST);
		defFunc.put("nbt", NBT);
		*/
	}

	public void setup() {
		/*
		RecipeAPI.addModModules(this);
		
		File dir = Config.CONFIG_DIR;
		File comp = new File(dir, "compiled.dat");
		HashMap<String, Version> versions = new HashMap<String, Version>();
		for (Version v : scriptRegistry)
			if (v.fallback != null) {
				v.checkVersion();
				versions.put(v.name, v);
			}
		Script[] scripts;
		boolean reload = true, existingCheck = false;
		try {
			scripts = ScriptFiles.loadPackage(comp, versions, true);
		} catch (FileNotFoundException e) {
			scripts = null; reload = false;
			existingCheck = true;
			LOG.info("No compiled config scripts found! This is probably the first startup.");
		} catch (IOException e) {
			scripts = null; reload = false;
			LOG.error("loading compiled config scripts failed!", e);
		}
		for (Version v : versions.values())
			try {
				File dst = new File(dir, v.name + ".rcp");
				if (existingCheck && dst.exists() && v.getFileVersion(new FileInputStream(dst)) >= v.version)
					continue;
				FileUtil.copyData(v.fallback, dst);
			} catch (IOException e) {
				LOG.error("copying script preset failed!", e);
			}
		if (scripts == null) {
			scripts = ScriptFiles.createCompiledPackage(comp);
			if (scripts == null && reload) try {
				LOG.info("Falling back to old scripts");
				scripts = ScriptFiles.loadPackage(comp, versions, false);
			} catch (IOException e) { LOG.error("loading compiled config scripts failed!", e); }
		}
		if (scripts != null) for (Script s : scripts) add(s);*/
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
			LOG.info(SCRIPT, "skipped {}", name);
		} catch (Exception e) {
			LOG.error(SCRIPT, "script execution failed for " + name, e);
		}
	}

	public static class ItemMatcher {
		private final ItemStack ref;
		private final boolean ignDmg, ignAm;
		public ItemMatcher(ItemStack stack) {
			ref = stack;
			ignDmg = stack.getDamageValue() == -1;
			ignAm = stack.getCount() <= 0;
		}
		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof ItemStack)) return false;
			ItemStack item = (ItemStack)obj;
			return item.getItem() == ref.getItem() && (ignDmg || item.getDamageValue() == ref.getDamageValue()) && (ignAm || item.getCount() == ref.getCount());
		}
	}

	public static class FluidMatcher {
		private final FluidStack ref;
		private final boolean ignAm;
		public FluidMatcher(FluidStack stack) {
			ref = stack;
			ignAm = stack.getAmount() <= 0;
		}
		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof FluidStack)) return false;
			FluidStack fluid = (FluidStack)obj;
			return fluid.getFluid() == ref.getFluid() && (ignAm || fluid.getAmount() == ref.getAmount());
		}
	}

	public static class ConfigConstants {
		private final Module m;
		public ConfigConstants(Module m) {this.m = m;}

		public double getNumber(String name, double fallback) {
			if (m == null) return fallback;
			IOperand o = m.read(name);
			if (o instanceof Number) return ((Number)o).value;
			m.assign(name, new Number(fallback));
			return fallback;
		}

		public double[] getVect(String name, double[] pre) {
			if (m == null) return pre;
			IOperand o = m.read(name);
			if (o instanceof Vector) {
				double[] vec = ((Vector)o).value;
				int n = Math.min(vec.length, pre.length);
				System.arraycopy(vec, 0, pre, 0, n);
				if (n < pre.length) {
					Vector v = new Vector(pre.length);
					m.assign(name, v);
					System.arraycopy(pre, 0, v.value, 0, pre.length);
				}
				return pre;
			}
			Vector vec = new Vector(pre.length);
			System.arraycopy(pre, 0, vec.value, 0, pre.length);
			m.assign(name, vec);
			return pre;
		}

		public int[] getVect(String name, int[] pre) {
			if (m == null) return pre;
			IOperand o = m.read(name);
			if (o instanceof Vector) {
				double[] vec = ((Vector)o).value;
				int n = Math.min(vec.length, pre.length);
				for (int i = 0; i < n; i++) pre[i] = (int)vec[i];
				if (n < pre.length) {
					Vector v = new Vector(n = pre.length);
					vec = v.value;
					m.assign(name, v);
					for (int i = 0; i < n; i++) vec[i] = pre[i];
				}
				return pre;
			}
			Vector vec = new Vector(pre.length);
			double[] a = vec.value;
			for (int i = pre.length - 1; i >= 0; i--)
				a[i] = pre[i];
			m.assign(name, vec);
			return pre;
		}

		public float[] getVect(String name, float[] pre) {
			if (m == null) return pre;
			IOperand o = m.read(name);
			if (o instanceof Vector) {
				double[] vec = ((Vector)o).value;
				int n = Math.min(vec.length, pre.length);
				for (int i = 0; i < n; i++) pre[i] = (float)vec[i];
				if (n < pre.length) {
					Vector v = new Vector(n = pre.length);
					vec = v.value;
					m.assign(name, v);
					for (int i = 0; i < n; i++) vec[i] = pre[i];
				}
				return pre;
			}
			Vector vec = new Vector(pre.length);
			double[] a = vec.value;
			for (int i = pre.length - 1; i >= 0; i--)
				a[i] = pre[i];
			m.assign(name, vec);
			return pre;
		}

		public <T> T get(String name, Class<T> type, T fallback) {
			if (m == null) return fallback;
			Object o = m.read(name).value();
			if (type.isInstance(o)) return type.cast(o);
			IOperand op;
			if (fallback instanceof IOperand) op = (IOperand)fallback;
			else if (fallback instanceof String) op = new Text((String)fallback);
			else if (fallback instanceof ItemStack) op = new ItemOperand((ItemStack)fallback);
			else if (fallback instanceof FluidStack) op = new FluidOperand((FluidStack)fallback);
			else if (fallback == null) op = Nil.NIL;
			else op = new ObjWrapper(fallback);
			m.assign(name, op);
			return fallback;
		}
/*
		public Object[] getArray(String name, int size) {
			if (m == null) return new Object[size];
			IOperand o = m.read(name);
			if (o instanceof Array) {
				Object[] vec = (Object[])o.value();
				if (vec.length < size) {
					IOperand[] x = new IOperand[size - vec.length];
					Arrays.fill(x, Nil.NIL);
					m.assign(name, o.addR(new Array(x)));
					vec = Arrays.copyOf(vec, size);
				}
				return vec;
			}
			IOperand[] x = new IOperand[size];
			Arrays.fill(x, Nil.NIL);
			m.assign(name, new Array(x));
			return new Object[size];
		}*/
	}

}
