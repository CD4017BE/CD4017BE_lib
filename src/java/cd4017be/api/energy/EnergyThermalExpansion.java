/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cd4017be.api.energy;

import org.apache.logging.log4j.Level;

import cd4017be.lib.ModTileEntity;
import cd4017be.lib.util.Utils;
import cofh.api.energy.IEnergyContainerItem;
import cofh.api.energy.IEnergyHandler;
import cofh.api.energy.IEnergyStorage;
import cpw.mods.fml.common.FMLLog;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

/**
 *
 * @author CD4017BE
 */
public class EnergyThermalExpansion implements IEnergyAccess
{
    public static float E_Factor = 100F;
    private IEnergyHandler handler = null;
    private IEnergyStorage storage = null;
    
    @Override
    public float getStorage(int s) 
    {
        try {
	    	if (handler != null && handler.canConnectEnergy(ForgeDirection.getOrientation(s))) return (float)handler.getEnergyStored(ForgeDirection.getOrientation(s)) * E_Factor;
	        else if (storage != null) return (float)storage.getEnergyStored() * E_Factor;
	        else return 0;
        } catch (Exception e) {
	    	FMLLog.log(Level.ERROR, e, "Energy API Error by %s", handler != null ? handler.getClass().getName() : storage.getClass().getName());
	    	return 0;
	    }
    }

    @Override
    public float getCapacity(int s) 
    {
    	try {
	        if (handler != null && handler.canConnectEnergy(ForgeDirection.getOrientation(s))) return (float)handler.getMaxEnergyStored(ForgeDirection.getOrientation(s)) * E_Factor;
	        else if (storage != null) return (float)storage.getMaxEnergyStored() * E_Factor;
	        else return 0;
	    } catch (Exception e) {
	    	FMLLog.log(Level.ERROR, e, "Energy API Error by %s", handler != null ? handler.getClass().getName() : storage.getClass().getName());
	    	return 0;
	    }
    }

    @Override
    public float addEnergy(float e, int s) 
    {
        if (handler != null && handler.canConnectEnergy(ForgeDirection.getOrientation(s))) {
            if (e > 0) return (float)handler.receiveEnergy(ForgeDirection.getOrientation(s), (int)Math.floor(e / E_Factor), false) * E_Factor;
            else return (float)handler.extractEnergy(ForgeDirection.getOrientation(s), (int)Math.floor(e / -E_Factor), false) * -E_Factor;
        } else if (storage != null) {
            if (e > 0) return (float)storage.receiveEnergy((int)Math.floor(e / E_Factor), false) * E_Factor;
            else return (float)storage.extractEnergy((int)Math.floor(e / -E_Factor), false) * -E_Factor;
        } else return 0;
    }

    @Override
    public boolean create(TileEntity te) 
    {
        if (te instanceof IEnergyHandler) handler = (IEnergyHandler)te;
        if (te instanceof IEnergyStorage) storage = (IEnergyStorage)te;
        return handler != null || storage != null;
    }
    
    public static float outputEnergy(ModTileEntity tile, float e, int sides)
    {
        for (byte s = 0; s < 6 && e >= E_Factor; s++) {
            if ((sides >> s & 1) == 0) continue;
            TileEntity te = Utils.getTileOnSide(tile, s);
            if (te != null && te instanceof IEnergyHandler) {
                e -= (float)((IEnergyHandler)te).receiveEnergy(ForgeDirection.getOrientation(s^1), (int)Math.floor(e / E_Factor), false) * E_Factor;
            }
        }
        return e;
    }
    
    public static float addEnergy(ItemStack item, float e)
    {
    	if (e == 0 || item == null || !(item.getItem() instanceof IEnergyContainerItem)) return 0;
    	IEnergyContainerItem cont = (IEnergyContainerItem)item.getItem();
    	if (e > 0) {
    		return cont.receiveEnergy(item, (int)Math.floor(e / E_Factor), false) * E_Factor;
    	} else {
    		return cont.extractEnergy(item, (int)Math.floor(e / -E_Factor), false) * -E_Factor;
    	}
    }
    
}