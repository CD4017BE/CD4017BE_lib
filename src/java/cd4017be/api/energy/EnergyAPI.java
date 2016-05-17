/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cd4017be.api.energy;

import java.util.ArrayList;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

/**
 * This is the core class for energy exchange with other mods. 
 * To implement your own API just create a class that implements IEnergyHandler and 
 * add an instance of it to the ArrayList 'handlers' during any initialization phase.
 * @author CD4017BE
 */
public class EnergyAPI 
{
	/** list of available IEnergyHandlers */
	public static ArrayList<IEnergyHandler> handlers = new ArrayList<IEnergyHandler>();
	/** default IEnergyAccess instance for things that don't support energy. 
	 * It simply returns 0 for all methods.*/
	public static final IEnergyAccess NULL = new NullAccess();
	/** the IEnergyHandler instance for InductiveAutomation */
	public static EnergyAutomation main = new EnergyAutomation();
	/**[J] energy conversion factor for RedstoneFlux*/
	public static double RF_value = 100D;
	/**[J] energy conversion factor for IndustrialCraft's EU*/
	public static double EU_value = 400D;
	/**[J] energy conversion factor for OpenComputers*/
	public static double OC_value = 1000D;
	
	/** 
	 * This is a wrapper used to access energy in ItemStacks or TileEntities. 
	 * All methods take an access type as argument. Which is for TileEntities: 0-5 = block faces BTNSWE, -1 = internal.
	 * And for ItemStacks 0 = external, -1 = internal. Don't use any other values than listed as parameters unless you know what you're doing!
	 * Also don't expect internal access to be always supported.
	 * */
	public static interface IEnergyAccess {
		/** 
		 * @param s access type  
		 * @return [J] stored energy
		 */
		public double getStorage(int s);
		/**
		 * @param s access type
		 * @return [J] storage capacity
		 */
		public double getCapacity(int s);
		/**
		 * @param e [J] the amount energy to insert (e > 0) or extract (e < 0)
		 * @param s access type
		 * @return [J] the amount of energy actually inserted (> 0) or extracted (< 0)
		 */
		public double addEnergy(double e, int s);
	}
	
	/** This is used to support IEnergyAccess instances for TileEntities and ItemStacks */
	public static interface IEnergyHandler
	{
		/**
		 * @param te the TileEntity to create a wrapper for
		 * @return the wrapper or null if given TileEntity is not supported by this handler
		 */
		public IEnergyAccess create(TileEntity te);
		/**
		 * @param item the ItemStack to create a wrapper for
		 * @return the wrapper or null if given ItemStack is not supported by this handler
		 */
		public IEnergyAccess create(ItemStack item);
	}
	
	static {
		handlers.add(main);
		handlers.add(new EnergyRedstoneFlux());
		//registerAccess(EnergyIndustrialCraft.class); //TODO reimplement
	}
	
	/**
	 * @param te the TileEntity to get a valid wrapper for
	 * @return the wrapper instance. Never null: if no valid handler was found this is the default instance.
	 */
	public static IEnergyAccess get(TileEntity te) {
		if (te == null) return NULL;
		if (te instanceof IEnergyAccess) return (IEnergyAccess)te;
		IEnergyAccess e;
		for (IEnergyHandler c : handlers)
			if ((e = c.create(te)) != null)
				return e;
		return NULL;
	}
	
	/**
	 * @param te the ItemStack to get a valid wrapper for
	 * @return the wrapper instance. Never null: if no valid handler was found this is the default instance.
	 */
	public static IEnergyAccess get(ItemStack item) {
		if (item == null || item.getItem() == null || item.stackSize != 1) return NULL;
		IEnergyAccess e;
		for (IEnergyHandler c : handlers)
			if ((e = c.create(item)) != null)
				return e;
		return NULL;
	}
	
	static class NullAccess implements IEnergyAccess {
		
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
