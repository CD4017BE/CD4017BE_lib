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
    	
    	EnergyTile(IEnergyHandler tile) {
    		this.energy = tile;
    	}
    	
		@Override
		public double getStorage(int s) {
			return (double)energy.getEnergyStored(s < 0 || s >= 6 ? null : EnumFacing.VALUES[s]) * RF_value;
		}

		@Override
		public double getCapacity(int s) {
			return (double)energy.getMaxEnergyStored(s < 0 || s >= 6 ? null : EnumFacing.VALUES[s]) * RF_value;
		}

		@Override
		public double addEnergy(double e, int s) {
			if (e >= RF_value && energy instanceof IEnergyReceiver)
				return (double)((IEnergyReceiver)energy).receiveEnergy(s < 0 || s >= 6 ? null : EnumFacing.VALUES[s], (int)Math.floor(e / RF_value), false) * RF_value;
			else if (e <= -RF_value && energy instanceof IEnergyProvider)
				return (double)((IEnergyProvider)energy).extractEnergy(s < 0 || s >= 6 ? null : EnumFacing.VALUES[s], (int)Math.floor(e / -RF_value), false) * -RF_value;
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
		public double getStorage(int s) {
			return (double)energy.getEnergyStored(item) * RF_value;
		}

		@Override
		public double getCapacity(int s) {
			return (double)energy.getMaxEnergyStored(item) * RF_value;
		}

		@Override
		public double addEnergy(double e, int s) {
			if (e >= RF_value)
				return (double)energy.receiveEnergy(item, (int)Math.floor(e / RF_value), false) * RF_value;
			else if (e <= -RF_value && energy instanceof IEnergyProvider)
				return (double)energy.extractEnergy(item, (int)Math.floor(e / -RF_value), false) * -RF_value;
			else return 0;
		}
    	
    }

    @Override
    public IEnergyAccess create(TileEntity te) {
        return te instanceof IEnergyHandler ? new EnergyTile((IEnergyHandler)te) : null;
    }
    
	@Override
	public IEnergyAccess create(ItemStack item) {
		return item.getItem() instanceof IEnergyContainerItem ? new EnergyItem(item, (IEnergyContainerItem)item.getItem()) : null;
	}
    
}