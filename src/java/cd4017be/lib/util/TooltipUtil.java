package cd4017be.lib.util;

import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.registries.IForgeRegistryEntry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import cd4017be.api.recipes.RecipeScriptContext;
import cd4017be.lib.script.Script;
import cd4017be.lib.script.ScriptFiles.Version;
import cd4017be.lib.script.obj.IOperand;

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

	@SideOnly(Side.CLIENT)
	public static boolean showShiftHint() {
		return overrideModifiers ? shiftOverride : GuiScreen.isShiftKeyDown();
	}

	public static String getAltHint() {
		if (AltHint == null){
			AltHint = I18n.translateToLocal("cd4017be.altHint");
			if (AltHint == "cd4017be.altHint") AltHint = "<ALT for extra>";
		}
		return AltHint;
	}

	@SideOnly(Side.CLIENT)
	public static boolean showAltHint() {
		return overrideModifiers ? altOverride : GuiScreen.isAltKeyDown();
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
				for (Entry<String, IOperand> var : m.variables.entrySet())
					addVar(var.getKey(), var.getValue().value());
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
	public static TooltipEditor editor;
	public static boolean shiftOverride = true, altOverride = false, overrideModifiers = FMLCommonHandler.instance().getSide() == Side.SERVER;

	/**
	 * @param s translation key
	 * @return localized text with config variable references resolved
	 */
	public static String getConfigFormat(String s) {
		if (s.equals(lastKey) && editor == null) return lastValue; //speed up tooltip rendering performance
		lastKey = s;
		String t = editor != null ? editor.getTranslation(s) : I18n.translateToLocal(s);
		if (t.equals(s)) {
			Matcher m = variantReplacement.matcher(s);
			if (!m.find()) return lastValue = s;
			String n = m.group(1);
			String s1 = s.substring(0, m.start(1)) + "i" + s.substring(m.end(1));
			t = editor != null ? editor.getTranslation(s1) : I18n.translateToLocal(s1);
			if (t.equals(s1)) return lastValue = s;
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

	/**
	 * @param s translation key or literal format string prefixed with '\'<br>
	 * Note: the special added pattern {@code "%u"} formats a floating point number in the SI unit scale system.<br>
	 * A number after a '.' specifies the significant digits count (default 3) and a number before a '.' the minimum digit count before the comma (default 1).
	 * @param args format arguments
	 * @return formatted localized text
	 */
	public static String format(String s, Object... args) {
		s = translate(s).trim().replace("\\n", "\n");
		try {
			Matcher m = numberFormat.matcher(s);
			String s1 = "";
			while (m.find()) {
				double val = 0;
				int exp = 0, n = 3;
				String g = m.group(1);
				if (g != null) val = ((Number)args[Integer.parseInt(g)]).doubleValue();
				else for (int i = 0; i < args.length; i++)
					if (args[i] instanceof Number) {
						val = ((Number)args[i]).doubleValue();
						Object[] nargs = new Object[args.length - 1];
						if (i > 0) System.arraycopy(args, 0, nargs, 0, i);
						if (i < nargs.length) System.arraycopy(args, i + 1, nargs, i, nargs.length - i);
						args = nargs;
						break;
					}
				if ((g = m.group(2)) != null && !g.isEmpty()) exp = Integer.parseInt(g);
				if ((g = m.group(3)) != null) n = Integer.parseInt(g);
				s1 += s.substring(0, m.start()) + formatNumber(val, n, exp);
				m.reset(s = s.substring(m.end()));
			}
			for (int i = 0; i < args.length; i++)
				if (args[i] instanceof Boolean)
					args[i] = translate((Boolean)args[i] ? "gui.yes" : "gui.no");
				else if (args[i] instanceof EnumFacing)
					args[i] = translate("enumfacing." + args[i]);
			return String.format(s1 + s, args);
		} catch (IllegalFormatException e) {
			return s + "\n" + e.toString();
		}
	}

	/**
	 * @param s translation key or literal string prefixed with '\'
	 * @return localized text
	 */
	public static String translate(String s) {
		return s.startsWith("\\") ? s.substring(1) : editor != null ? editor.getTranslation(s) : I18n.translateToLocal(s);
	}

	public static boolean hasTranslation(String s) {
		return I18n.canTranslate(s) || editor != null && editor.hasEdited(s);
	}

	private static final String[] DecScale =   {"a"  , "f"  , "p"  , "n" , "u" , "m" , "" , "k", "M", "G", "T" , "P" , "E" };
	public static final double[] ScaleUnits = {1e-18, 1e-15, 1e-12, 1e-9, 1e-6, 1e-3, 1e0, 1e3, 1e6, 1e9, 1e12, 1e15, 1e18, 1e21};
	public static final double[] exp10 = {1, 10, 100, 1e3, 1e4, 1e5, 1e6, 1e7, 1e8, 1e9, 1e10, 1e11, 1e12, 1e13, 1e14, 1e15};

	/**
	 * @param x number
	 * @param w significant digits
	 * @param c clip below exponent of 10
	 * @return formatted number
	 */
	public static String formatNumber(double x, int w, int c) {
		return formatNumber(x, w, c < 0 ? 1.0 / exp10[-c] : exp10[c], false, true);
	}

	/**
	 * @param x number
	 * @param w significant digits
	 * @param c minimum integral part
	 * @param sign include positive sign
	 * @param trim remove leading & trailing zeroes
	 * @return formatted number
	 */
	public static String formatNumber(double x, int w, double c, boolean sign, boolean trim) {
		if (w < 0 || w >= exp10.length) throw new IllegalArgumentException("invalid width " + w);
		if (Double.isNaN(x)) return "NaN";
		StringBuilder sb = new StringBuilder(w + 3);
		//sign
		if (x < 0) {
			sb.append('-');
			x = -x;
		} else if (sign)
			sb.append('+');
		int p0 = sb.length();
		//scale factor
		int i = Arrays.binarySearch(ScaleUnits, x / c);
		if (i < 0) i = -2 - i;
		if (i < 0) return sb.append('0').toString();
		if (i >= DecScale.length) return sb.append('\u221e').toString();
		x /= ScaleUnits[i];
		int exp = Arrays.binarySearch(exp10, x < 1.0 ? 1.0 / x : x);
		if (exp < 0) exp = -2 - exp;
		if (x < 1.0) exp = -exp;
		//integral digits
		int y = (int)Math.round(x * exp10[w - exp - 1]);
		int p = sb.append(y).length() - w + exp + 1;
		while(p > sb.length()) sb.append('0');
		if (trim)
			for (int j = sb.length() - 1; j >= p && sb.charAt(j) == '0'; j--)
				sb.deleteCharAt(j);
		if (p < sb.length()) {
			for (;p <= p0; p++) sb.insert(p0, '0');
			sb.insert(p, '.');
		}
		//scale unit
		return sb.append(DecScale[i]).toString();
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
