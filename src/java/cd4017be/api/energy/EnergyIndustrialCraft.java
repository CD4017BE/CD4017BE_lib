package cd4017be.api.energy;

import ic2.api.item.IElectricItem;
import ic2.api.tile.IEnergyStorage;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import cd4017be.api.energy.EnergyAPI.IEnergyAccess;
import cd4017be.api.energy.EnergyAPI.IEnergyHandler;
import static cd4017be.api.energy.EnergyAPI.EU_value;
import static ic2.api.item.ElectricItem.manager;

/**
 * 
 * @author CD4017BE
 */
public class EnergyIndustrialCraft implements IEnergyHandler {

	@Override
	public IEnergyAccess create(TileEntity te, EnumFacing s) {
		return te instanceof IEnergyStorage ? new EnergyTile((IEnergyStorage)te) : null;
	}

	@Override
	public IEnergyAccess create(ItemStack item, int s) {
		return item.getItem() instanceof IElectricItem ? new EnergyItem(item, s < 0) : null;
	}

	static class EnergyTile implements IEnergyAccess {

		final IEnergyStorage t;

		EnergyTile(IEnergyStorage s) {
			this.t = s;
		}

		@Override
		public float getStorage() {
			return (float)t.getStored() * EU_value;
		}

		@Override
		public float getCapacity() {
			return (float)t.getCapacity() * EU_value;
		}

		@Override
		public float addEnergy(float e) {
			int s = t.getStored(), c = t.getCapacity();
			if (e >= EU_value && s < c) {
				int a = (int)Math.floor(e / EU_value);
				if (s + a > c) {
					e = (float)(c - s) * EU_value;
					t.setStored(c);
				} else t.setStored(s + a);
			} else if (e <= -EU_value && s > 0) {
				int a = (int)Math.floor(e / -EU_value);
				if (a > s) {
					e = (float)s * -EU_value;
					t.setStored(0);
				} else t.setStored(s - a);
			} else e = 0;
			return e;
		}

	}

	class EnergyItem implements IEnergyAccess {

		final ItemStack item;
		final boolean intern;

		EnergyItem(ItemStack item, boolean intern) {
			this.item = item; this.intern = intern;
		}

		@Override
		public float getStorage() {
			return (float)manager.getCharge(item) * EU_value;
		}

		@Override
		public float getCapacity() {
			return item.getItem() instanceof IElectricItem ? (float)((IElectricItem)item.getItem()).getMaxCharge(item) * EU_value : 0F;
		}

		@Override
		public float addEnergy(float e) {
			if (e > 0) return (float)manager.charge(item, e / EU_value, Integer.MAX_VALUE, intern, false) * EU_value;
			else if (e < 0) return (float)manager.discharge(item, e / -EU_value, Integer.MAX_VALUE, intern, !intern, false) * -EU_value;
			else return 0;
		}

	}

}
