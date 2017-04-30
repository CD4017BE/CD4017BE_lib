package cd4017be.lib;

import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.translation.I18n;
import cd4017be.api.recipes.RecipeScriptContext;
import cd4017be.lib.script.Script;
import cd4017be.lib.script.ScriptFiles.Version;
import cd4017be.lib.util.Utils;

/**
 *
 * @author CD4017BE
 */
public class TooltipInfo {

	private static String ShiftHint, AltHint;
	private static String FluidDispUnit;
	private static String EnergyDispUnit;
	private static String PowerDispUnit;
	private static String LinkPosFormat;

	public static String getShiftHint() {
		if (ShiftHint == null){
			ShiftHint = I18n.translateToLocal("cd4017be.shiftHint");
			if (ShiftHint == "cd4017be.shiftHint") ShiftHint = "<SHIFT for info>";
		}
		return ShiftHint;
	}

	public static String getAltHint() {
		if (AltHint == null){
			AltHint = I18n.translateToLocal("cd4017be.altHint");
			if (AltHint == "cd4017be.altHint") AltHint = "<ALT for extra>";
		}
		return AltHint;
	}

	public static String getFluidUnit() {
		if (FluidDispUnit == null){
			FluidDispUnit = I18n.translateToLocal("cd4017be.fluidUnit");
			if (FluidDispUnit == "cd4017be.fluidUnit") FluidDispUnit = "B";
		}
		return FluidDispUnit;
	}

	public static String getEnergyUnit() {
		if (EnergyDispUnit == null){
			EnergyDispUnit = I18n.translateToLocal("cd4017be.energyUnit");
			if (EnergyDispUnit == "cd4017be.energyUnit") EnergyDispUnit = "kJ";
		}
		return EnergyDispUnit;
	}

	public static String getPowerUnit() {
		if (PowerDispUnit == null){
			PowerDispUnit = I18n.translateToLocal("cd4017be.powerUnit");
			if (PowerDispUnit == "cd4017be.powerUnit") PowerDispUnit = "kW";
		}
		return PowerDispUnit;
	}

	public static String[] sides = new String[]{"B", "T", "N", "S", "W", "E"};

	public static String formatLink(BlockPos pos, EnumFacing side) {
		if (LinkPosFormat == null) {
			LinkPosFormat = I18n.translateToLocal("cd4017be.linkPos");
			if (LinkPosFormat == "cd4017be.linkPos") LinkPosFormat = "Link: x=%d y=%d z=%d %s";
		}
		return String.format(LinkPosFormat, pos.getX(), pos.getY(), pos.getZ(), side != null ? sides[side.ordinal()] : "");
	}

	public static void addConfigReference(ConfigurationFile cfg) {
		for (Entry<String, Object> var : cfg.variables.entrySet()) {
			String id = var.getKey();
			Object val = var.getValue();
			String text;
			switch(id.charAt(0)) {
			case 'A':
				switch(id.charAt(1)) {
				case 'B': {
					boolean[] arr = (boolean[])val;
					for (int i = 0; i < arr.length; i++) variables.put(id + ":" + i, "" + arr[i]);
				} break;
				case 'W': {
					byte[] arr = (byte[])val;
					for (int i = 0; i < arr.length; i++) variables.put(id + ":" + i, "" + arr[i]);
				} break;
				case 'S': {
					short[] arr = (short[])val;
					for (int i = 0; i < arr.length; i++) variables.put(id + ":" + i, "" + arr[i]);
				} break;
				case 'I': {
					int[] arr = (int[])val;
					for (int i = 0; i < arr.length; i++) variables.put(id + ":" + i, "" + arr[i]);
				} break;
				case 'L': {
					long[] arr = (long[])val;
					for (int i = 0; i < arr.length; i++) variables.put(id + ":" + i, "" + arr[i]);
				} break;
				case 'F': {
					float[] arr = (float[])val;
					for (int i = 0; i < arr.length; i++)
						variables.put(id + ":" + i, Utils.formatNumber(arr[i], 3, 0));
				} break;
				case 'D': {
					double[] arr = (double[])val;
					for (int i = 0; i < arr.length; i++)
						variables.put(id + ":" + i, Utils.formatNumber(arr[i], 3, 0));
				} break;
				case 'T': {
					String[] arr = (String[])val;
					for (int i = 0; i < arr.length; i++) variables.put(id + ":" + i, arr[i]);
				} break;
				} continue;
			case 'F': text = Utils.formatNumber((Float)val, 3, 0); break;
			case 'D': text = Utils.formatNumber((Double)val, 3, 0); break;
			default: text = val.toString();
			}
			variables.put(id, text);
		}
	}

	public static void addScriptVariables() {
		RecipeScriptContext cont = RecipeScriptContext.instance;
		for (Version v : RecipeScriptContext.scriptRegistry) {
			Script m = (Script)cont.modules.get(v.name);
			if (m != null)
				for (Entry<String, Object> var : m.variables.entrySet())
					addVar(var.getKey(), var.getValue());
		}
	}

	private static void addVar(String name, Object o) {
		if (o instanceof Double) {
			variables.put(name, Utils.formatNumber((Double)o, 4, 0));
		} else if (o instanceof double[]) {
			double[] arr = (double[])o;
			for (int i = 0; i < arr.length; i++)
				variables.put(name + ":" + i, Utils.formatNumber(arr[i], 4, 0));
		} else if (o instanceof Object[]) {
			Object[] arr = (Object[])o;
			for (int i = 0; i < arr.length; i++)
				addVar(name + ":" + i, arr[i]);
		} else if (o instanceof ItemStack) {
			ItemStack item = (ItemStack)o;
			variables.put(name, (item.stackSize > 1 ? item.stackSize + "x " : "") + item.getDisplayName());
		} else if (o != null) variables.put(name, o.toString());
	}

	private static final HashMap<String, String> variables = new HashMap<String, String>();
	private static final Pattern varInsertion = Pattern.compile("\\\\<([\\w:]+)>");

	public static String getLocFormat(String s) {
		s = I18n.translateToLocal(s).trim().replace("\\n", "\n");
		Matcher m = varInsertion.matcher(s);
		String s1 = "";
		while (m.find()) {
			String var = variables.get(m.group(1));
			if (var != null) {
				s1 += s.substring(0, m.start()) + var;
				m.reset(s = s.substring(m.end()));
			}
		}
		return s1 + s;
		/*
		int p = 0, q, x;
		String id, repl;
		while ((q = s.indexOf("\\<", p)) >= p && (p = s.indexOf(">", q)) > q) {
			id = s.substring(q + 2, p);
			x = id.indexOf(":");
			repl = x <= 0 ? id : id.substring(0, x);
			for (ConfigurationFile cfg : configurations)
				if (cfg.getObject(repl) != null) {
					repl = formatReference(id, cfg);
					break;
				}
			s = s.replace("\\<" + id + ">", repl);
			p = q + repl.length();
		}
		return s;
		*/
	}

	public static String format(String s, Object... args) {
		s = I18n.translateToLocal(s).trim().replace("\\n", "\n");
		try {
			return String.format(s, args);
		} catch (IllegalFormatException e) {
			return s + "\n" + e.toString();
		}
	}

}
