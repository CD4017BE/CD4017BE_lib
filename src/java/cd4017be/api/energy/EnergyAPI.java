/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cd4017be.api.energy;

import java.util.ArrayList;

import org.apache.logging.log4j.Level;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.Loader;

/**
 * This is the core class for energy exchange with other mods. 
 * To implement your own API just create a class that implements IEnergyHandler and 
 * add an instance of it to the ArrayList 'handlers' during any initialization phase.
 * @author CD4017BE
 */
public class EnergyAPI {
	/** list of available IEnergyHandlers */
	public static ArrayList<IEnergyHandler> handlers = new ArrayList<IEnergyHandler>();
	/** default IEnergyAccess instance for things that don't support energy. 
	 * It simply returns 0 for all methods.*/
	public static final IEnergyAccess NULL = new NullAccess();
	/**[J] energy conversion factor for Item Charge*/
	public static final float IA_value = 1000F;
	/**[J] energy conversion factor for RedstoneFlux*/
	public static float RF_value = 100F;
	/**[J] energy conversion factor for IndustrialCraft's EU*/
	public static float EU_value = 400F;
	/**[J] energy conversion factor for OpenComputers*/
	public static float OC_value = 1000F;
	
	/** 
	 * This is a wrapper used to access energy in ItemStacks or TileEntities. 
	 */
	public static interface IEnergyAccess {
		/** 
		 * @return [J] stored energy
		 */
		public float getStorage();
		/**
		 * @return [J] storage capacity
		 */
		public float getCapacity();
		/**
		 * @param e [J] the amount energy to insert (e > 0) or extract (e < 0)
		 * @return [J] the amount of energy actually inserted (> 0) or extracted (< 0)
		 */
		public float addEnergy(float e);
	}
	
	/** This is used to support IEnergyAccess instances for TileEntities and ItemStacks */
	public static interface IEnergyHandler
	{
		/**
		 * @param te the TileEntity to create a wrapper for
		 * @param s access side (null for internal)
		 * @return the wrapper or null if given TileEntity is not supported by this handler
		 */
		public IEnergyAccess create(TileEntity te, EnumFacing s);
		/**
		 * @param item the ItemStack to create a wrapper for
		 * @param s access type: 0 = external, -1 = internal
		 * @return the wrapper or null if given ItemStack is not supported by this handler
		 */
		public IEnergyAccess create(ItemStack item, int s);
	}
	
	public static void init() {
		if (Loader.isModLoaded("Automation")) {
			handlers.add(new EnergyAutomation());
			FMLLog.log("CD4017BE_lib", Level.INFO, "added Inductive Automation Energy-API");
		}
		if (Loader.isModLoaded("OpenComputers")) {
			handlers.add(new EnergyOpenComputers());
			FMLLog.log("CD4017BE_lib", Level.INFO, "added Open Computers Energy-API");
		}
		if (true) {
			handlers.add(new EnergyForge());
			FMLLog.log("CD4017BE_lib", Level.INFO, "added Forge Energy-API");
		}
		if (Loader.isModLoaded("IC2")) {
			handlers.add(new EnergyIndustrialCraft());
			FMLLog.log("CD4017BE_lib", Level.INFO, "added IC2 Energy-API");
		}
	}
	
	/**
	 * @param te the TileEntity to get a valid wrapper for
	 * @param s access side (null for internal)
	 * @return the wrapper instance. Never null: if no valid handler was found this is the default instance.
	 */
	public static IEnergyAccess get(TileEntity te, EnumFacing s) {
		if (te == null) return NULL;
		if (te instanceof IEnergyAccess) return (IEnergyAccess)te;
		IEnergyAccess e;
		for (IEnergyHandler c : handlers)
			if ((e = c.create(te, s)) != null)
				return e;
		return NULL;
	}
	
	/**
	 * @param te the ItemStack to get a valid wrapper for
	 * @param s access type: 0 = external, -1 = internal. Don't use any other values than listed as parameters unless you know what you're doing!
	 * @return the wrapper instance. Never null: if no valid handler was found this is the default instance.
	 */
	public static IEnergyAccess get(ItemStack item, int s) {
		if (item == null || item.getItem() == null || item.getCount() != 1) return NULL;
		IEnergyAccess e;
		for (IEnergyHandler c : handlers)
			if ((e = c.create(item, s)) != null)
				return e;
		return NULL;
	}
	
	static class NullAccess implements IEnergyAccess {
		
		@Override
		public float getStorage() {
			return 0;
		}
		
		@Override
		public float getCapacity() {
			return 0;
		}
		
		@Override
		public float addEnergy(float e) {
			return 0;
		}
	}
	
}
