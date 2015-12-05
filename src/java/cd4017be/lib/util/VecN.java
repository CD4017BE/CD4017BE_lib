/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cd4017be.lib.util;

import net.minecraft.nbt.NBTTagCompound;

/**
 *
 * @author CD4017BE
 */
public class VecN 
{
    public final double[] x;
    
    /**
     * Creates a new empty Vector
     * @param n the dimension of the Vector
     */
    public VecN(int n)
    {
        this.x = new double[n];
    }
    
    /**
     * Creates a new Vector out of parameters
     * @param x the individual values
     */
    public VecN(double... x)
    {
        this.x = x;
    }
    
    public VecN(VecN a, double... b)
    {
        this.x = new double[a.x.length + b.length];
        System.arraycopy(a.x, 0, x, 0, a.x.length);
        System.arraycopy(b, 0, x, a.x.length, b.length);
    }
    
    /**
     * Vector square length
     * @return |v|²
     */
    public double sq()
    {
        return this.scale(this);
    }
    
    /**
     * Vector length
     * @return |v|
     */
    public double l()
    {
        return Math.sqrt(this.scale(this));
    }
    
    public boolean isNaN()
    {
        for (double a : x) if (Double.isNaN(a)) return true;
        return false;
    }
    
    /**
     * Creates a copy of this Vector
     * @return v
     */
    public VecN copy()
    {
        VecN vec = new VecN(x.length);
        System.arraycopy(x, 0, vec.x, 0, x.length);
        return vec;
    }
    
    public VecN sub(int o, int n)
    {
        VecN vec = new VecN(n);
        System.arraycopy(x, o, vec.x, 0, n);
        return vec;
    }
    
    /**
     * Inverted Vector
     * @return -v
     */
    public VecN neg()
    {
        VecN vec = new VecN(x.length);
        for (int i = 0; i < x.length; i++) vec.x[i] = -x[i];
        return vec;
    }
    
    /**
     * Normalized Vector with lenght = 1
     * @return v / |v|
     */
    public VecN norm()
    {
        double d = l();
        VecN vec = new VecN(x.length);
        for (int i = 0; i < x.length; i++) vec.x[i] = x[i] / d;
        return vec;
    }
    
    /**
     * Adds coordinates to this Vector
     * @param x 
     * @return  
     */
    public VecN add(double... x)
    {
        VecN vec = new VecN(x.length);
        for (int i = 0; i < x.length; i++) vec.x[i] = this.x[i] + x[i];
        return vec;
    }
    
    /**
     * Scales this Vector
     * @param x 
     * @return  
     */
    public VecN scale(double... x)
    {
        VecN vec = new VecN();
        for (int i = 0; i < x.length; i++) vec.x[i] = this.x[i] * x[i];
        return vec;
    }
    
    /**
     * 
     * @param a
     * @return v + a
     */
    public VecN add(VecN a)
    {
        VecN vec = new VecN(a.x.length > x.length ? a.x.length : x.length);
        int n = a.x.length < x.length ? a.x.length : x.length;
        for (int i = 0; i < n; i++) vec.x[i] = x[i] + a.x[i];
        return vec;
    }
    
    /**
     * 
     * @param a
     * @return v - a
     */
    public VecN diff(VecN a)
    {
        VecN vec = new VecN(a.x.length > x.length ? a.x.length : x.length);
        int n = a.x.length < x.length ? a.x.length : x.length;
        for (int i = 0; i < n; i++) vec.x[i] = x[i] - a.x[i];
        return vec;
    }
    
    /**
     * 
     * @param s
     * @return v * s
     */
    public VecN scale(double s)
    {
        VecN vec = new VecN(x.length);
        for (int i = 0; i < x.length; i++) vec.x[i] = x[i] * s;
        return vec;
    }
    
    /**
     * 
     * @param a
     * @return v * a
     */
    public double scale(VecN a)
    {
        if (a.x.length != x.length) throw new IllegalArgumentException("dot-product with Vectors of different dimensions");
        double r = 0;
        for (int i = 0; i < x.length; i++) r += x[i] * a.x[i];
        return r;
    }
    
    public void writeToNBT(NBTTagCompound nbt, String name)
    {
        int[] v = new int[x.length];
        for (int i = 0; i < x.length; i++) v[i] = Float.floatToRawIntBits((float)x[i]);
        nbt.setIntArray(name, v);
    }
    
    public static VecN readFromNBT(NBTTagCompound nbt, String name)
    {
        int[] v = nbt.getIntArray(name);
        VecN vec = new VecN(v.length);
        for (int i = 0; i < vec.x.length; i++) vec.x[i] = Float.intBitsToFloat(v[i]);
        return vec;
    }
    
}
