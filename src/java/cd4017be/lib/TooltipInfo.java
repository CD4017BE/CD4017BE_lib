/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cd4017be.lib;

import java.util.ArrayList;
import java.util.IllegalFormatException;

import net.minecraft.util.StatCollector;
import cd4017be.lib.util.Utils;

/**
 *
 * @author CD4017BE
 */
public class TooltipInfo 
{
    private static String ShiftHint;
    private static String FluidDispUnit;
    private static String EnergyDispUnit;
    private static String PowerDispUnit;
    
    public static String getShiftHint() {
    	if (ShiftHint == null){
    		ShiftHint = StatCollector.translateToLocal("cd4017be.shiftHint");
    		if (ShiftHint == "cd4017be.shiftHint") ShiftHint = "<SHIFT for info>";
    	}
    	return ShiftHint;
    }
    
    public static String getFluidUnit() {
    	if (FluidDispUnit == null){
    		FluidDispUnit = StatCollector.translateToLocal("cd4017be.fluidUnit");
    		if (FluidDispUnit == "cd4017be.fluidUnit") FluidDispUnit = "B";
    	}
    	return FluidDispUnit;
    }
    
    public static String getEnergyUnit() {
    	if (EnergyDispUnit == null){
    		EnergyDispUnit = StatCollector.translateToLocal("cd4017be.energyUnit");
    		if (EnergyDispUnit == "cd4017be.energyUnit") EnergyDispUnit = "kJ";
    	}
    	return EnergyDispUnit;
    }
    
    public static String getPowerUnit() {
    	if (PowerDispUnit == null){
    		PowerDispUnit = StatCollector.translateToLocal("cd4017be.powerUnit");
    		if (PowerDispUnit == "cd4017be.powerUnit") PowerDispUnit = "kW";
    	}
    	return PowerDispUnit;
	}
	
    public static void addConfigReference(ConfigurationFile cfg)
    {
    	configurations.add(cfg);
    }
    
    private static String formatReference(String id, ConfigurationFile cfg)
    {
    	if (id.startsWith("A")) {
    		try {
    			int p = id.indexOf(":");
    			String k = id.substring(3, p);
    			int idx = Integer.parseInt(id.substring(p + 1));
    			if (id.startsWith("AB.")) return Boolean.toString(cfg.getBooleanArray(k)[idx]);
            	else if (id.startsWith("AW.")) return Byte.toString(cfg.getByteArray(k)[idx]);
            	else if (id.startsWith("AS.")) return Short.toString(cfg.getShortArray(k)[idx]);
            	else if (id.startsWith("AI.")) return Integer.toString(cfg.getIntArray(k)[idx]);
            	else if (id.startsWith("AL.")) return Long.toString(cfg.getLongArray(k)[idx]);
            	else if (id.startsWith("AF.")) return Utils.formatNumber(cfg.getFloatArray(k)[idx], 3, 0);
            	else if (id.startsWith("AD.")) return Utils.formatNumber(cfg.getDoubleArray(k)[idx], 3, 0);
            	else if (id.startsWith("AT.")) return cfg.getStringArray(k)[idx];
    		} catch(Exception e) {}
    	} else {
    		String k = id.substring(2);
    		if (id.startsWith("B.")) return Boolean.toString(cfg.getBoolean(k, false));
        	else if (id.startsWith("W.")) return Byte.toString(cfg.getByte(k, (byte)0));
        	else if (id.startsWith("S.")) return Short.toString(cfg.getShort(k, (short)0));
        	else if (id.startsWith("I.")) return Integer.toString(cfg.getInt(k, 0));
        	else if (id.startsWith("L.")) return Long.toString(cfg.getLong(k, 0L));
        	else if (id.startsWith("F.")) return Utils.formatNumber(cfg.getFloat(k, 0F), 3, 0);
        	else if (id.startsWith("D.")) return Utils.formatNumber(cfg.getDouble(k, 0D), 3, 0);
        	else if (id.startsWith("T.")) return cfg.getString(k, "");
    	}
    	return "";
    }
    
    private static final ArrayList<ConfigurationFile> configurations = new ArrayList<ConfigurationFile>();
    
    public static String getLocFormat(String s)
    {
    	s = StatCollector.translateToLocal(s).trim().replace("\\n", "\n");
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
    }
    
    public static String format(String s, Object... args)
    {
    	s = StatCollector.translateToLocal(s).trim().replace("\\n", "\n");
    	try {
    		return String.format(s, args);
    	} catch (IllegalFormatException e) {
    		return s;
    	}
    }
    
}
