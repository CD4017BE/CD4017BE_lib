package cd4017be.lib.util;

import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.fml.common.registry.IForgeRegistryEntry;
import cd4017be.api.recipes.RecipeScriptContext;
import cd4017be.lib.script.Script;
import cd4017be.lib.script.ScriptFiles.Version;

/**
 *
 * @author CD4017BE
 */
@SuppressWarnings("deprecation")
public class TooltipUtil {

	public static String CURRENT_DOMAIN = "";

	public static String unlocalizedNameFor(IForgeRegistryEntry.Impl<?> obj) {
		ResourceLocation loc = obj.getRegistryName();
		return (loc.getResourceDomain().equals(CURRENT_DOMAIN) ? CURRENT_DOMAIN : "cd4017be") + '.' + loc.getResourcePath();
	}

	private static String ShiftHint, AltHint;
	private static String FluidDispUnit;
	private static String EnergyDispUnit;
	private static String PowerDispUnit;
	private static String LinkPosFormat;
	private static String LinkPosFormat1;

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

	public static String formatLink(BlockPos pos, EnumFacing side, int dim) {
		if (LinkPosFormat1 == null) {
			LinkPosFormat1 = I18n.translateToLocal("cd4017be.linkPos1");
			if (LinkPosFormat1 == "cd4017be.linkPos1") LinkPosFormat1 = "Link: x=%d y=%d z=%d %s @dim %d";
		}
		return String.format(LinkPosFormat1, pos.getX(), pos.getY(), pos.getZ(), side != null ? sides[side.ordinal()] : "", dim);
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
			variables.put(name, formatNumber((Double)o, 4, 0));
		} else if (o instanceof double[]) {
			double[] arr = (double[])o;
			for (int i = 0; i < arr.length; i++)
				variables.put(name + ":" + i, formatNumber(arr[i], 4, 0));
		} else if (o instanceof Object[]) {
			Object[] arr = (Object[])o;
			for (int i = 0; i < arr.length; i++)
				addVar(name + ":" + i, arr[i]);
		} else if (o instanceof ItemStack) {
			ItemStack item = (ItemStack)o;
			variables.put(name, (item.getCount() > 1 ? item.getCount() + "x " : "") + item.getDisplayName());
		} else if (o != null) variables.put(name, o.toString());
	}

	private static final HashMap<String, String> variables = new HashMap<String, String>();
	private static final Pattern varInsertion = Pattern.compile("\\\\<([\\w:]+)>");
	private static final Pattern variantReplacement = Pattern.compile("\\:(\\d+)");
	private static final Pattern numberFormat = Pattern.compile("%(?:(\\d+)\\$)?(?:(-?\\d?)\\.(\\d+))?u");
	private static String lastKey, lastValue;

	public static String getConfigFormat(String s) {
		if (s.equals(lastKey)) return lastValue; //speed up tooltip rendering performance
		lastKey = s;
		String t = I18n.translateToLocal(s);
		if (t.equals(s)) {
			Matcher m = variantReplacement.matcher(s);
			if (!m.find()) return lastValue = s;
			String n = m.group(1);
			s = s.substring(0, m.start(1)) + "i" + s.substring(m.end(1));
			t = I18n.translateToLocal(s);
			if (t.equals(s)) return lastValue = s;
			t = t.replace("\\i", n);
		}
		s = t.trim().replace("\\n", "\n");
		Matcher m = varInsertion.matcher(s);
		String s1 = "";
		while (m.find()) {
			String var = variables.get(m.group(1));
			if (var != null) {
				s1 += s.substring(0, m.start()) + var;
				m.reset(s = s.substring(m.end()));
			}
		}
		return lastValue = s1 + s;
	}

	public static String format(String s, Object... args) {
		s = I18n.translateToLocal(s).trim().replace("\\n", "\n");
		try {
			Matcher m = numberFormat.matcher(s);
			String s1 = "";
			while (m.find()) {
				double val = 0;
				int exp = 0, n = 3;
				String g = m.group(1);
				if (g != null) val = (Double)args[Integer.parseInt(g)];
				else for (Object o : args)
					if (o instanceof Double) {
						val = (Double)o;
						break;
					}
				if ((g = m.group(2)) != null && !g.isEmpty()) exp = Integer.parseInt(g);
				if ((g = m.group(3)) != null) n = Integer.parseInt(g);
				s1 += s.substring(0, m.start()) + formatNumber(val, n, exp);
				m.reset(s = s.substring(m.end()));
			}
			return String.format(s1 + s, args);
		} catch (IllegalFormatException e) {
			return s + "\n" + e.toString();
		}
	}

	public static String translate(String s) {
		return I18n.translateToLocal(s);
	}

	public static boolean hasTranslation(String s) {
		return I18n.canTranslate(s);
	}

	private static final String[] DecScale  = {"a", "f", "p", "n", "u", "m", "", "k", "M", "G", "T", "P", "E"};
	private static final int ofsDecScale = 6;

	/**
	 * @param x number
	 * @param w significant digits
	 * @param c clip below exponent of 10
	 * @return formatted number
	 */
	public static String formatNumber(double x, int w, int c)
	{
		double s = Math.signum(x);
		if (x == 0 || Double.isNaN(x) || Double.isInfinite(x)) return "" + x;
		int o = (int)Math.floor(Math.log10(x * s)) + 3 * ofsDecScale;
		int p = (o + c) / 3;
		int n = w - o + p * 3 - 1;
		if (p < 0) return "0";
		else if (p > DecScale.length) return "" + (s == -1 ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY);
		x *= Math.pow(0.001, p - ofsDecScale);
		String tex = String.format("%." + n + "f", x);
		String ds = "" + DecimalFormatSymbols.getInstance().getDecimalSeparator();
		if (tex.contains(ds)) {
			while(tex.endsWith("0")) tex = tex.substring(0, tex.length() - 1);
			if (tex.endsWith(ds)) tex = tex.substring(0, tex.length() - 1);
		}
		return tex + DecScale[p];
	}

	/**
	 * @param x number
	 * @param w max fractal digits
	 * @return formatted number
	 */
	public static String formatNumber(double x, int w) {
		String tex = String.format("%." + w + "f", x);
		String ds = "" + DecimalFormatSymbols.getInstance().getDecimalSeparator();
		if (tex.contains(ds)) {
			while(tex.endsWith("0")) tex = tex.substring(0, tex.length() - 1);
			if (tex.endsWith(ds)) tex = tex.substring(0, tex.length() - 1);
		}
		return tex;
	}

}
