/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cd4017be.lib;

import cpw.mods.fml.common.FMLLog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.apache.commons.io.IOUtils;

import cd4017be.lib.util.Utils;

/**
 *
 * @author CD4017BE
 */
public class TooltipInfo 
{
    
    private static HashMap<String, String> toolTips = new HashMap<String, String>();
    
    /**
     * @param name
     * @return the tooltip registered with given name.
     */
    public static String getInfo(String name)
    {
        return toolTips.get(name);
    }
    
    /**
     * Registers a tooltip for given name.
     * @param name
     * @param info
     */
    public static void addInfo(String name, String info)
    {
        toolTips.put(name, info);
    }
    
    /**
     * Loads tooltips from a file.
     * @param resource
     */
    public static void loadInfoFile(ResourceLocation resource)
    {
        try {
            IResource r = Minecraft.getMinecraft().getResourceManager().getResource(resource);
            String k, v;
            int p;
            for (String s : IOUtils.readLines(r.getInputStream())) {
                if (!s.isEmpty() && s.charAt(0) != '#' && (p = s.indexOf('=')) > 0 && p < s.length() - 1) {
                    k = s.substring(0, p).trim();
                    v = s.substring(p + 1).trim().replace("\\n", "\n");
                    toolTips.put(k, v);
                }
            }
        } catch (IOException e) 
        {
            FMLLog.getLogger().warn("Failed to load Tool-tip-file of Mod " + resource.getResourceDomain(), e);
        }
    }
    
    public static void replaceReferences(ConfigurationFile cfg)
    {
    	configurations.add(cfg);
    	int p, q;
    	String id, repl;
    	for (Entry<String, String> e : toolTips.entrySet()) {
    		String s = e.getValue();
    		p = 0;
    		while ((q = s.indexOf("\\<", p)) >= p && (p = s.indexOf(">", q)) > q) {
    			id = s.substring(q + 2, p);
    			repl = formatReference(id, cfg);
    			s = s.replace("\\<" + id + ">", repl);
    			p = q + repl.length();
    		}
    		e.setValue(s);
    	}
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
    
}
