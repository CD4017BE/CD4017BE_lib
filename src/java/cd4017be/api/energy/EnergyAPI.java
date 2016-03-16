/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cd4017be.api.energy;

import java.util.ArrayList;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

/**
 *
 * @author CD4017BE
 */
public class EnergyAPI 
{
	public static ArrayList<IEnergyHandler> handlers = new ArrayList<IEnergyHandler>();
	
	public static interface IEnergyAccess 
	{
		public double getStorage(int s);
		public double getCapacity(int s);
		public double addEnergy(double e, int s);
	}
	
	public static interface IEnergyHandler
	{
		public IEnergyAccess create(TileEntity te);
		public IEnergyAccess create(ItemStack item);
	}
	
	static {
		handlers.add(new EnergyAutomation());
		//registerAccess(EnergyThermalExpansion.class); //TODO reimplement
		//registerAccess(EnergyIndustrialCraft.class); //TODO reimplement
	}
	
	public static IEnergyAccess get(TileEntity te) {
		if (te instanceof IEnergyAccess) return (IEnergyAccess)te;
		IEnergyAccess e;
		for (IEnergyHandler c : handlers) {
			e = c.create(te);
			if (e != null) return e;
		}
		return new NullAccess();
	}
	
	public static IEnergyAccess get(ItemStack item) {
		IEnergyAccess e;
		for (IEnergyHandler c : handlers) {
			e = c.create(item);
			if (e != null) return e;
		}
		return new NullAccess();
	}
	
	private static class NullAccess implements IEnergyAccess {
		
		@Override
		public double getStorage(int s) {
			return 0;
		}
		
		@Override
		public double getCapacity(int s) {
			return 0;
		}
		
		@Override
		public double addEnergy(double e, int s) {
			return 0;
		}
	}
}
