package cd4017be.api.energy;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.energy.IEnergyStorage;
import static net.minecraftforge.energy.CapabilityEnergy.*;
import cd4017be.api.energy.EnergyAPI.IEnergyAccess;
import cd4017be.api.energy.EnergyAPI.IEnergyHandler;
import static cd4017be.api.energy.EnergyAPI.RF_value;

public class EnergyForge implements IEnergyHandler {

	@Override
	public IEnergyAccess create(TileEntity te, EnumFacing s) {
		return te.hasCapability(ENERGY, s) ? new Energy(te, s) : null;
	}

	@Override
	public IEnergyAccess create(ItemStack item, int s) {
		EnumFacing side = s >= 0 && s < 6 ? EnumFacing.VALUES[s] : null;
		return item.hasCapability(ENERGY, side) ? new Energy(item, side) : null;
	}

	static class Energy implements IEnergyAccess {

		final ICapabilityProvider cp;
		final EnumFacing s;

		Energy(ICapabilityProvider cp, EnumFacing s) {
			this.cp = cp; this.s = s;
		}

		@Override
		public float getStorage() {
			IEnergyStorage h = cp.getCapability(ENERGY, s);
			return h != null ? (float)h.getEnergyStored() * RF_value : 0F;
		}

		@Override
		public float getCapacity() {
			IEnergyStorage h = cp.getCapability(ENERGY, s);
			return h != null ? (float)h.getMaxEnergyStored() * RF_value : 0F;
		}

		@Override
		public float addEnergy(float e) {
			IEnergyStorage h = cp.getCapability(ENERGY, s);
			if (h == null) return 0F;
			else if (e >= RF_value) return (float)h.receiveEnergy((int)Math.floor(e / RF_value), false) * RF_value;
			else if (e <= -RF_value) return (float)h.extractEnergy((int)Math.floor(e / -RF_value), false) * -RF_value;
			else return 0F;
		}

	}

}
