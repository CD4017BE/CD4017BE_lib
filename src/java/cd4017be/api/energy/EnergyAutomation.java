/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cd4017be.api.energy;

import cd4017be.api.automation.IEnergy;
import cd4017be.api.automation.IEnergyStorage;
import cd4017be.api.automation.PipeEnergy;
import net.minecraft.tileentity.TileEntity;

/**
 *
 * @author CD4017BE
 */
public class EnergyAutomation implements IEnergyAccess
{
    private IEnergyStorage storage = null;
    private IEnergy energy = null;
    
    @Override
    public float getStorage(int s) 
    {
        if (storage != null) return (float)storage.getEnergy();
        else if (energy != null){
            PipeEnergy pipe = energy.getEnergy((byte)s);
            if (pipe != null) return (float)(pipe.Ucap * pipe.Ucap);
            else return 0;
        } else return 0;
    }

    @Override
    public float getCapacity(int s) 
    {
        if (storage != null) return (float)storage.getCapacity();
        else if (energy != null){
            PipeEnergy pipe = energy.getEnergy((byte)s);
            if (pipe != null) return (long)pipe.Umax * (long)pipe.Umax;
            else return 0;
        } else return 0;
    }

    @Override
    public float addEnergy(float e, int s) 
    {
        if (storage != null) return (float)storage.addEnergy(e);
        else if (energy != null){
            PipeEnergy pipe = energy.getEnergy((byte)s);
            if (pipe != null && pipe != PipeEnergy.empty) {
                double d = pipe.Ucap * pipe.Ucap;
                if (d + e < 0) {
                    e = -(float)d;
                    pipe.Ucap = 0;
                } else if (d + e > (long)pipe.Umax * (long)pipe.Umax) {
                    e = (long)pipe.Umax * (long)pipe.Umax - (float)d;
                    pipe.Ucap = pipe.Umax;
                } else pipe.addEnergy(e);
                return e;
            } else return 0;
        } else return 0;
    }

    @Override
    public boolean create(TileEntity te) 
    {
        if (te instanceof IEnergyStorage) {
            storage = (IEnergyStorage)te;
            return true;
        } else if (te instanceof IEnergy) {
            energy = (IEnergy)te;
            return true;
        } else return false;
    }
    
}
