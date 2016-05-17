package cd4017be.api.energy;

import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Environment;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import cd4017be.api.energy.EnergyAPI.IEnergyAccess;
import cd4017be.api.energy.EnergyAPI.IEnergyHandler;
import static cd4017be.api.energy.EnergyAPI.OC_value;

public class EnergyOpenComputers implements IEnergyHandler {

	static class OCAccess implements IEnergyAccess {
		final Connector energy;
		
		OCAccess(Connector con) {
			this.energy = con;
		}
		
		@Override
		public double getStorage(int s) {
			return energy.globalBuffer() * OC_value;
		}

		@Override
		public double getCapacity(int s) {
			return energy.globalBufferSize() * OC_value;
		}

		@Override
		public double addEnergy(double e, int s) {
			if (e <= 0) return 0;
			else return e - energy.changeBuffer(e / OC_value) * OC_value;
		}
		
	}

	@Override
	public IEnergyAccess create(TileEntity te) {
		if (!(te instanceof Environment)) return null;
		Environment env  = (Environment)te;
		if (env.node() instanceof Connector) return new OCAccess((Connector)env.node());
		else return null;
	}

	@Override
	public IEnergyAccess create(ItemStack item) {
		return null;
	}

}
