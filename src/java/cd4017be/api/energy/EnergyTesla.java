package cd4017be.api.energy;

import net.darkhax.tesla.api.ITeslaConsumer;
import net.darkhax.tesla.api.ITeslaHolder;
import net.darkhax.tesla.api.ITeslaProducer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import cd4017be.api.energy.EnergyAPI.IEnergyAccess;
import cd4017be.api.energy.EnergyAPI.IEnergyHandler;
import static net.darkhax.tesla.capability.TeslaCapabilities.*;
import static cd4017be.api.energy.EnergyAPI.RF_value;

public class EnergyTesla implements IEnergyHandler {

	@Override
	public IEnergyAccess create(TileEntity te, EnumFacing s) {
		return te.hasCapability(CAPABILITY_HOLDER, s) || 
				te.hasCapability(CAPABILITY_CONSUMER, s) || 
				te.hasCapability(CAPABILITY_PRODUCER, s) ? 
				new Energy(te, s) : null;
	}

	@Override
	public IEnergyAccess create(ItemStack item, int s) {
		EnumFacing side = s >= 0 && s < 6 ? EnumFacing.VALUES[s] : null;
		return item.hasCapability(CAPABILITY_HOLDER, side) || 
				item.hasCapability(CAPABILITY_CONSUMER, side) || 
				item.hasCapability(CAPABILITY_PRODUCER, side) ? 
				new Energy(item, side) : null;
	}

	static class Energy implements IEnergyAccess {

		final ICapabilityProvider cp;
		final EnumFacing s;

		Energy(ICapabilityProvider cp, EnumFacing s) {
			this.cp = cp; this.s = s;
		}

		@Override
		public float getStorage() {
			ITeslaHolder h = cp.getCapability(CAPABILITY_HOLDER, s);
			return h != null ? (float)h.getStoredPower() * RF_value : 0F;
		}

		@Override
		public float getCapacity() {
			ITeslaHolder h = cp.getCapability(CAPABILITY_HOLDER, s);
			return h != null ? (float)h.getCapacity() * RF_value : 0F;
		}

		@Override
		public float addEnergy(float e) {
			if (e >= RF_value) {
				ITeslaConsumer c = cp.getCapability(CAPABILITY_CONSUMER, s);
				return c != null ? (float)c.givePower((long)Math.floor(e / RF_value), false) * RF_value : 0F;
			} else if (e <= -RF_value) {
				ITeslaProducer p = cp.getCapability(CAPABILITY_PRODUCER, s);
				return p != null ? (float)p.takePower((long)Math.floor(e / -RF_value), false) * -RF_value : 0F;
			} else return 0F;
		}
		
	}
	
}
