/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cd4017be.api.energy;

import cd4017be.api.energy.EnergyAPI.IEnergyAccess;
import cofh.api.energy.IEnergyContainerItem;
import cofh.api.energy.IEnergyHandler;
import cofh.api.energy.IEnergyProvider;
import cofh.api.energy.IEnergyReceiver;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import static cd4017be.api.energy.EnergyAPI.RF_value;

/**
 *
 * @author CD4017BE
 */
public class EnergyRedstoneFlux implements EnergyAPI.IEnergyHandler
{
	
	static class EnergyTile implements IEnergyAccess {
		
		final IEnergyHandler energy;
		final EnumFacing s;
		
		EnergyTile(IEnergyHandler tile, EnumFacing s) {
			this.energy = tile;
			this.s = s;
		}
		
		@Override
		public float getStorage() {
			return (float)energy.getEnergyStored(s) * RF_value;
		}

		@Override
		public float getCapacity() {
			return (float)energy.getMaxEnergyStored(s) * RF_value;
		}

		@Override
		public float addEnergy(float e) {
			if (e >= RF_value && energy instanceof IEnergyReceiver)
				return (float)((IEnergyReceiver)energy).receiveEnergy(s, (int)Math.floor(e / RF_value), false) * RF_value;
			else if (e <= -RF_value && energy instanceof IEnergyProvider)
				return (float)((IEnergyProvider)energy).extractEnergy(s, (int)Math.floor(e / -RF_value), false) * -RF_value;
			else return 0;
		}
		
	}
	
	static class EnergyItem implements IEnergyAccess {
		
		final ItemStack item;
		final IEnergyContainerItem energy;
		
		EnergyItem(ItemStack item, IEnergyContainerItem energy) {
			this.item = item;
			this.energy = energy;
		}
		
		@Override
		public float getStorage() {
			return (float)energy.getEnergyStored(item) * RF_value;
		}

		@Override
		public float getCapacity() {
			return (float)energy.getMaxEnergyStored(item) * RF_value;
		}

		@Override
		public float addEnergy(float e) {
			if (e >= RF_value)
				return (float)energy.receiveEnergy(item, (int)Math.floor(e / RF_value), false) * RF_value;
			else if (e <= -RF_value && energy instanceof IEnergyProvider)
				return (float)energy.extractEnergy(item, (int)Math.floor(e / -RF_value), false) * -RF_value;
			else return 0;
		}
		
	}

	@Override
	public IEnergyAccess create(TileEntity te, EnumFacing s) {
		return te instanceof IEnergyHandler ? new EnergyTile((IEnergyHandler)te, s) : null;
	}
	
	@Override
	public IEnergyAccess create(ItemStack item, int s) {
		return item.getItem() instanceof IEnergyContainerItem ? new EnergyItem(item, (IEnergyContainerItem)item.getItem()) : null;
	}
	
}