package cd4017be.lib;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.io.IOUtils;

public class ConfigurationFile 
{
	
	private final HashMap<String, Object> variables;
	
	public ConfigurationFile()
	{
		variables = new HashMap<String, Object>();
	}
	
	public void load(File file) throws IOException
	{
		load((InputStream)(new DataInputStream(new FileInputStream(file))));
	}
	
	public void load(InputStream in) throws IOException
	{
		String k, v;
        int p;
        for (String s : IOUtils.readLines(in)) {
            if (!s.isEmpty() && s.charAt(0) != '#' && (p = s.indexOf('=')) > 0 && p < s.length() - 1) {
                k = s.substring(0, p).trim();
                v = s.substring(p + 1).trim();
                Object obj = this.readObj(v, k);
                if (obj != null) this.variables.put(k, obj);
            }
        }
        in.close();
	}
	
	public void removeEntry(String... e)
	{
		for (String s : e) variables.remove(s);
	}
	
	public ArrayList<String> getVariables(String... pref)
	{
		ArrayList<String> list = new ArrayList<String>();
		for (String s : variables.keySet())
			for (String i : pref)
				if (s.startsWith(i)) {
					list.add(s);
					break;
				}
		return list;
	}
	
	private Object readObj(String s, String id)
	{
		try {
			if (id.startsWith("B.")) {
	        	return Boolean.parseBoolean(s);
	        } else if (id.startsWith("W.")) {
	        	return Byte.parseByte(s);
	        } else if (id.startsWith("S.")) {
	        	return Short.parseShort(s);
	        } else if (id.startsWith("I.")) {
	        	return Integer.parseInt(s);
	        } else if (id.startsWith("L.")) {
	        	return Long.parseLong(s);
	        } else if (id.startsWith("F.")) {
	        	return Float.parseFloat(s);
	        } else if (id.startsWith("D.")) {
	        	return Double.parseDouble(s);
	        } else if (id.startsWith("T.")) {
	        	return s;
	        } else if (id.startsWith("A")) {
	        	id = id.substring(1);
	        	String[] v = s.split(",", -1);
	        	if (id.startsWith("B.")) {
	        		boolean[] arr = new boolean[v.length];
	        		for (int i = 0; i < arr.length; i++) arr[i] = Boolean.parseBoolean(v[i].trim());
	        		return arr;
	        	} else if (id.startsWith("W.")) {
	        		byte[] arr = new byte[v.length];
	        		for (int i = 0; i < arr.length; i++) arr[i] = Byte.parseByte(v[i].trim());
	        		return arr;
	        	} else if (id.startsWith("S.")) {
	        		short[] arr = new short[v.length];
	        		for (int i = 0; i < arr.length; i++) arr[i] = Short.parseShort(v[i].trim());
	        		return arr;
	        	} else if (id.startsWith("I.")) {
	        		int[] arr = new int[v.length];
	        		for (int i = 0; i < arr.length; i++) arr[i] = Integer.parseInt(v[i].trim());
	        		return arr;
	        	} else if (id.startsWith("L.")) {
	        		long[] arr = new long[v.length];
	        		for (int i = 0; i < arr.length; i++) arr[i] = Long.parseLong(v[i].trim());
	        		return arr;
	        	} else if (id.startsWith("F.")) {
	        		float[] arr = new float[v.length];
	        		for (int i = 0; i < arr.length; i++) arr[i] = Float.parseFloat(v[i].trim());
	        		return arr;
	        	} else if (id.startsWith("D.")) {
	        		double[] arr = new double[v.length];
	        		for (int i = 0; i < arr.length; i++) arr[i] = Double.parseDouble(v[i].trim());
	        		return arr;
	        	} else if (id.startsWith("T.")) {
	        		for (int i = 0; i < v.length; i++) v[i] = v[i].trim();
	        		return v;
	        	} else return null;
	        } else return null;
		} catch (NumberFormatException e) {
			return null;
		}
	}
	
	public Object getObject(String id)
	{
		return this.variables.get(id);
	}
	
	public boolean getBoolean(String id, boolean def)
	{
		Object obj = this.variables.get("B." + id);
		if (obj == null || !(obj instanceof Boolean)) return def;
		else return (Boolean)obj;
	}
	
	public byte getByte(String id, byte def)
	{
		Object obj = this.variables.get("W." + id);
		if (obj == null || !(obj instanceof Byte)) return def;
		else return (Byte)obj;
	}
	
	public short getShort(String id, short def)
	{
		Object obj = this.variables.get("S." + id);
		if (obj == null || !(obj instanceof Short)) return def;
		else return (Short)obj;
	}
	
	public int getInt(String id, int def)
	{
		Object obj = this.variables.get("I." + id);
		if (obj == null || !(obj instanceof Integer)) return def;
		else return (Integer)obj;
	}
	
	public long getLong(String id, long def)
	{
		Object obj = this.variables.get("L." + id);
		if (obj == null || !(obj instanceof Long)) return def;
		else return (Long)obj;
	}
	
	public float getFloat(String id, float def)
	{
		Object obj = this.variables.get("F." + id);
		if (obj == null || !(obj instanceof Float)) return def;
		else return (Float)obj;
	}
	
	public double getDouble(String id, double def)
	{
		Object obj = this.variables.get("D." + id);
		if (obj == null || !(obj instanceof Double)) return def;
		else return (Double)obj;
	}
	
	public String getString(String id, String def)
	{
		Object obj = this.variables.get("T." + id);
		if (obj == null || !(obj instanceof String)) return def;
		else return (String)obj;
	}
	
	public boolean[] getBooleanArray(String id)
	{
		Object obj = this.variables.get("AB." + id);
		if (obj == null || !(obj instanceof boolean[])) return new boolean[0];
		else return (boolean[])obj;
	}
	
	public byte[] getByteArray(String id)
	{
		Object obj = this.variables.get("AW." + id);
		if (obj == null || !(obj instanceof byte[])) return new byte[0];
		else return (byte[])obj;
	}
	
	public short[] getShortArray(String id)
	{
		Object obj = this.variables.get("AS." + id);
		if (obj == null || !(obj instanceof short[])) return new short[0];
		else return (short[])obj;
	}
	
	public int[] getIntArray(String id)
	{
		Object obj = this.variables.get("AI." + id);
		if (obj == null || !(obj instanceof int[])) return new int[0];
		else return (int[])obj;
	}
	
	public long[] getLongArray(String id)
	{
		Object obj = this.variables.get("AL." + id);
		if (obj == null || !(obj instanceof long[])) return new long[0];
		else return (long[])obj;
	}
	
	public float[] getFloatArray(String id)
	{
		Object obj = this.variables.get("AF." + id);
		if (obj == null || !(obj instanceof float[])) return new float[0];
		else return (float[])obj;
	}
	
	public double[] getDoubleArray(String id)
	{
		Object obj = this.variables.get("AD." + id);
		if (obj == null || !(obj instanceof double[])) return new double[0];
		else return (double[])obj;
	}
	
	public String[] getStringArray(String id)
	{
		Object obj = this.variables.get("AT." + id);
		if (obj == null || !(obj instanceof String[])) return new String[0];
		else return (String[])obj;
	}
	
	public static void copyData(String resourcePath, File target) throws IOException
	{
		InputStream in = ConfigurationFile.class.getResourceAsStream(resourcePath);
		target.createNewFile();
		OutputStream out = new DataOutputStream(new FileOutputStream(target));
		IOUtils.copy(in, out);
	}
	
}
