/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cd4017be.api.energy;

import ic2.api.energy.tile.IEnergySink;
import ic2.api.energy.tile.IEnergySource;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

/**
 *
 * @author CD4017BE
 */
public class EnergyIndustrialCraft implements IEnergyAccess
{
    public static float E_Factor = 400F;
    IEnergySink storage = null;
    IEnergySource source = null;
    
    @Override
    public float getStorage(int s) 
    {
        if (source != null) return (float)source.getOfferedEnergy() * E_Factor;
        else return 0;
    }

    @Override
    public float getCapacity(int s) 
    {
        if (storage != null) return (float)storage.getDemandedEnergy() * E_Factor;
        else return 0;
    }

    @Override
    public float addEnergy(float e, int s) 
    {
        if (e > 0 && storage != null) {
        	return e - (float)storage.injectEnergy(ForgeDirection.getOrientation(s), e / E_Factor, 32) * E_Factor;
        }
        else if (e < 0 && source != null) {
        	float r = Math.min(e / -E_Factor, (float)source.getOfferedEnergy());
        	source.drawEnergy(r);
        	return r * E_Factor;
        }
        else return 0;
    }

    @Override
    public boolean create(TileEntity te) 
    {
        if (te instanceof IEnergySink) storage = (IEnergySink)te;
        if (te instanceof IEnergySource) source = (IEnergySource)te;
        return storage != null || source != null;
    }
    
}
