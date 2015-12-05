/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cd4017be.api.energy;

import net.minecraft.tileentity.TileEntity;

/**
 *
 * @author CD4017BE
 */
public interface IEnergyAccess 
{
    public float getStorage(int s);
    public float getCapacity(int s);
    public float addEnergy(float amount, int s);
    public boolean create(TileEntity te);
}
