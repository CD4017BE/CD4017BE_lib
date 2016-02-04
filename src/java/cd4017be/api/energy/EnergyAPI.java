/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cd4017be.api.energy;

import java.util.ArrayList;

import net.minecraft.tileentity.TileEntity;

/**
 *
 * @author CD4017BE
 */
public class EnergyAPI 
{
    private static ArrayList<Class<?extends IEnergyAccess>> list = new ArrayList<Class<? extends IEnergyAccess>>();
    
    static {
        registerAccess(EnergyAutomation.class);
        //registerAccess(EnergyThermalExpansion.class); //TODO reimplement
        //registerAccess(EnergyIndustrialCraft.class); //TODO reimplement
    }
    
    public static IEnergyAccess getAccess(TileEntity te)
    {
        for (Class<?extends IEnergyAccess> c : list) {
            try {
                IEnergyAccess e = c.newInstance();
                if (e.create(te)) return e;
            } catch (Exception e) {System.out.println(e);}
        }
        return new NullAccess();
    }
    
    public static void registerAccess(Class<?extends IEnergyAccess> c) 
    {
        list.add(c);
    }
    
    private static class NullAccess implements IEnergyAccess {

        @Override
        public float getStorage(int s) 
        {
            return 0;
        }

        @Override
        public float getCapacity(int s) 
        {
            return 0;
        }

        @Override
        public float addEnergy(float amount, int s) 
        {
            return 0;
        }

        @Override
        public boolean create(TileEntity te) 
        {
            return true;
        }
        
    }
    
}
