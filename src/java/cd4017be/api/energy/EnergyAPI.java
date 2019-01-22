package cd4017be.api.energy;

import java.util.ArrayList;

import org.apache.logging.log4j.Level;

import cd4017be.api.recipes.RecipeScriptContext.ConfigConstants;
import cd4017be.lib.Lib;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
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
	public static float RF_value;
	/**[J] energy conversion factor for IndustrialCraft's EU*/
	public static float EU_value;
	/**[J] energy conversion factor for OpenComputers*/
	public static float OC_value;
	/**[J] energy conversion factor for VoidCraft*/
	public static float VC_value;

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
	public static interface IEnergyHandler {
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

	public static void init(ConfigConstants cfg) {
		if (Loader.isModLoaded("opencomputers")) {
			OC_value = (float) cfg.getNumber("energy_value_OC", 1000);
			if (!Float.isNaN(OC_value)) {
				handlers.add(new EnergyOpenComputers());
				Lib.LOG.info("added Open Computers Energy-API with {} J / OC-unit", OC_value);
			} else Lib.LOG.info("NOT added Open Computers Energy-API (disabled by config)");
		}
		if (true) {
			RF_value = (float) cfg.getNumber("energy_value_RF", 100);
			if (!Float.isNaN(RF_value)) {
				handlers.add(new EnergyForge());
				Lib.LOG.info("added Forge Energy-API with {} J / Flux", RF_value);
			} else Lib.LOG.info("NOT added Forge Energy-API (disabled by config)");
		}
		if (Loader.isModLoaded("ic2")) {
			EU_value = (float) cfg.getNumber("energy_value_EU", 400);
			if (!Float.isNaN(EU_value)) {
				handlers.add(new EnergyIndustrialCraft());
				Lib.LOG.info("added IC2 Energy-API with {} J / EU", EU_value);
			} else Lib.LOG.info("CD4017BE_lib", Level.INFO, "NOT added IC2 Energy-API (disabled by config)");
		}
		if (Loader.isModLoaded("voidcraft")) try {
			VC_value = (float) cfg.getNumber("energy_value_VC", 1000);
			if (!Float.isNaN(VC_value)) {
				handlers.add(new EnergyVoidcraft());
				Lib.LOG.info("added VoidCraft Energy-API with {} J / VC-unit", VC_value);
			} else Lib.LOG.info("NOT added VoidCraft Energy-API (disabled by config)");
		} catch(Exception e) {
			Lib.LOG.error("failed to add VoidCraft Energy-API!", e);
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
