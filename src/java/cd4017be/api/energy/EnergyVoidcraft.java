package cd4017be.api.energy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import cd4017be.api.energy.EnergyAPI.IEnergyAccess;
import cd4017be.api.energy.EnergyAPI.IEnergyHandler;
import cd4017be.lib.util.TileAccess;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import static cd4017be.api.energy.EnergyAPI.VC_value;

/**
 * For interaction with VoidCrafts energy system.<br>
 * Note: currently only supports reading energy from TileEntities.
 * @author CD4017BE
 */
public class EnergyVoidcraft implements IEnergyHandler {

	@SuppressWarnings("rawtypes")
	private final Class teInterf;
	private final Method getEnergy, getCap;

	@SuppressWarnings("unchecked")
	public EnergyVoidcraft() throws ClassNotFoundException, NoSuchMethodException, SecurityException {
		this.teInterf = Class.forName("tamaized.voidcraft.common.voidicpower.IVoidicPower");
		this.getEnergy = teInterf.getMethod("getPowerAmount");
		this.getCap = teInterf.getMethod("getMaxPower");
	}

	@Override
	public IEnergyAccess create(TileEntity te, EnumFacing s) {
		return teInterf.isInstance(te) ? new Wrapper(te, s) : null;
	}

	@Override
	public IEnergyAccess create(ItemStack item, int s) {
		return null; //TileEntity only for now
	}

	class Wrapper extends TileAccess implements IEnergyAccess {

		public Wrapper(TileEntity te, EnumFacing side) {
			super(te, side);
		}

		@Override
		public float getStorage() {
			try {return VC_value * (float)getEnergy.invoke(te);}
			catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {e.printStackTrace(); return 0;}
		}

		@Override
		public float getCapacity() {
			try {return VC_value * (float)getCap.invoke(te);}
			catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {e.printStackTrace(); return 0;}
		}

		@Override
		public float addEnergy(float e) {
			return 0; //read only for now
		}

	}

}
