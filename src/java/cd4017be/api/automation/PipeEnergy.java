/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cd4017be.api.automation;

import cd4017be.lib.ModTileEntity;
import cd4017be.lib.TooltipInfo;
import cd4017be.lib.util.Utils;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

/**
 *
 * @author CD4017BE
 */
public class PipeEnergy
{
    public static final PipeEnergy empty = new PipeEnergy(0, 0);
    public boolean update;
    public byte con;
    public double Ucap;
    public double[] Iind;
    public final int Umax;
    public final float Rcond;
    
    public PipeEnergy(int Umax, float Rcond)
    {
    	con = 0;
        Ucap = 0F;
        Iind = new double[]{0, 0, 0};
        this.Umax = Umax;
        this.Rcond = Rcond;
    }
    
    public PipeEnergy connect(byte c)
    {
    	con = c;
    	return this;
    }
    
    public void addEnergy(double E)
    {
        this.Ucap = Math.sqrt(this.Ucap * this.Ucap + E);
        if (Double.isNaN(Ucap)) this.Ucap = 0F;
    }
    
    public double getEnergy(double U, double R)
    {
        if (R < 1) R = 1;
        return (Ucap * Ucap - U * U) / R;
    }
    
    public void readFromNBT(NBTTagCompound nbt, String name) 
    {
        this.con = nbt.getByte("con");
    	this.Ucap = nbt.getDouble(name.concat("Ucap"));
        this.Iind[0] = nbt.getDouble(name.concat("IindY"));
        this.Iind[1] = nbt.getDouble(name.concat("IindZ"));
        this.Iind[2] = nbt.getDouble(name.concat("IindX"));
    }

    public void writeToNBT(NBTTagCompound nbt, String name) 
    {
    	nbt.setByte("con", con);
        nbt.setDouble(name.concat("Ucap"), Ucap);
        nbt.setDouble(name.concat("IindY"), Iind[0]);
        nbt.setDouble(name.concat("IindZ"), Iind[1]);
        nbt.setDouble(name.concat("IindX"), Iind[2]);
    }

    public boolean isConnected(int s)
    {
    	return (con >> s & 1) == 0;
    }
    
    public void update(ModTileEntity tile) 
    {
    	if (!(tile instanceof IEnergy)) return;
        IEnergy src = (IEnergy)tile;
        int s;
    	for (int i = 0; i < Iind.length; i++) {
    		s = i << 1;
            if (src.getEnergy((byte)s) != this || !this.isConnected(s)) continue;
            TileEntity te = Utils.getTileOnSide(tile, (byte)s);
            PipeEnergy energy = te != null && te instanceof IEnergy ? ((IEnergy)te).getEnergy((byte)(s | 1)) : null;
            if (energy != null && energy.Umax > 0 && energy.isConnected(s | 1)) {
                double uc = energy.Ucap;
                double ii = (Ucap - uc) / (1D + Rcond);
                double ud = (ii + Iind[i] * (1D - Rcond)) / 2D;
                Ucap -= ud;
                energy.Ucap += ud;
                Iind[i] = ii;
            }
        }
        if (Ucap > Umax) Ucap = Umax;
    }
    
    public static String[] getEnergyInfo(float U1, float U0, float R)
    {
    	float I = (U1 - U0) / R;
    	float P = (U1 + U0) * I;
    	return TooltipInfo.format("gui.cd4017be.energyFlow", P / 1000F, I).split("\n");
    	//return new String[]{"Power:", String.format("%.1f kW", P / 1000F), String.format("@ %.0f A", I)};
    }
    
}
