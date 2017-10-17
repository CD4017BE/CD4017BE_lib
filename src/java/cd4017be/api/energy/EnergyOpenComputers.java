package cd4017be.api.energy;

import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Environment;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import cd4017be.api.energy.EnergyAPI.IEnergyAccess;
import cd4017be.api.energy.EnergyAPI.IEnergyHandler;
import static cd4017be.api.energy.EnergyAPI.OC_value;

/**
 * 
 * @author CD4017BE
 */
public class EnergyOpenComputers implements IEnergyHandler {

	static class OCAccess implements IEnergyAccess {

		final Connector energy;

		OCAccess(Connector con) {
			this.energy = con;
		}

		@Override
		public float getStorage() {
			return (float)energy.globalBuffer() * OC_value;
		}

		@Override
		public float getCapacity() {
			return (float)energy.globalBufferSize() * OC_value;
		}

		@Override
		public float addEnergy(float e) {
			if (e <= 0) return 0;
			else return e - (float)energy.changeBuffer(e / OC_value) * OC_value;
		}

	}

	@Override
	public IEnergyAccess create(TileEntity te, EnumFacing s) {
		if (!(te instanceof Environment)) return null;
		Environment env  = (Environment)te;
		if (env.node() instanceof Connector) return new OCAccess((Connector)env.node());
		else return null;
	}

	@Override
	public IEnergyAccess create(ItemStack item, int s) {
		return null;
	}

}
