/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cd4017be.api.energy;

import java.util.List;
import java.util.concurrent.Callable;

import cd4017be.api.automation.PipeEnergy;
import cd4017be.api.energy.EnergyAPI.IEnergyAccess;
import cd4017be.api.energy.EnergyAPI.IEnergyHandler;
import cd4017be.lib.TooltipInfo;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import static cd4017be.api.energy.EnergyAPI.IA_value;

/**
 *
 * @author CD4017BE
 */
public class EnergyAutomation implements IEnergyHandler
{
	
	public static interface IEnergyItem {
		public int getEnergyCap(ItemStack item);
		public int getChargeSpeed(ItemStack item);
		public String getEnergyTag();
	}
	
	public static class EnergyItem implements IEnergyAccess {
		private final ItemStack stack;
		private final int s;
		public final IEnergyItem item;
		/** [kJ] remaining fraction for use with precision mode */
		public float fractal = 0;
		
		/** @param s "access side": -2 = precision, -1 = unlimited, 0 = limited */
		public EnergyItem(ItemStack stack, IEnergyItem item, int s) {
			this.stack = stack;
			this.item = item;
			this.s = s;
			if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
		}
		
		public void addInformation(List list) {
			list.add(String.format("Energy: %d / %d %s", this.getStorageI(), item.getEnergyCap(stack), TooltipInfo.getEnergyUnit()));
		}
		/**
		 * Get Integer tag value
		 * @return [kJ] stored energy
		 */
		public int getStorageI() {
			return stack.getTagCompound().getInteger(item.getEnergyTag());
		}
		/**
		 * Add Energy directly to the Integer tag
		 * @param n [kJ] amount
		 * @return [kJ] actually added energy
		 */
		public int addEnergyI(int n) {
			if (n == 0) return n;
			int cap = item.getEnergyCap(stack);
			if (s >= 0) {
				int max = item.getChargeSpeed(stack);
				if (n > max) n = max;
				else if (n < -max) n = -max;
			}
			int e = stack.getTagCompound().getInteger(item.getEnergyTag()) + n;
			if (e < 0) {
				n -= e;
				e = 0;
			} else if (e > cap) {
				n -= e - cap;
				e = cap;
			}
			stack.getTagCompound().setInteger(item.getEnergyTag(), e);
			return n;
		}
		/**
		 * @return [J] stored energy
		 */
		@Override
		public float getStorage() {
			if (s == -2) return ((float)this.getStorageI() + fractal) * IA_value;
			else return (float)this.getStorageI() * IA_value;
		}
		/**
		 * @return [J] energy storage capacity
		 */
		@Override
		public float getCapacity() {
			return item.getEnergyCap(stack) * IA_value;
		}
		/**
		 * @param E [J] energy to add
		 * @return [J] actually added energy
		 */
		@Override
		public float addEnergy(float E) {
			E /= IA_value;
			if (s == -2) {
				fractal = E - this.addEnergyI((int)Math.floor(E + fractal)); 
				if (fractal < 0 || fractal >= 1) {
					float d = (float)Math.floor(fractal);
					fractal -= d;
					E -= d;
				}
				return E * IA_value;
			} else return (float)this.addEnergyI(E < 0 ? (int)Math.ceil(E) : (int)Math.floor(E)) * IA_value;
		}
	}
	
	@CapabilityInject(PipeEnergy.class)
    public static Capability<PipeEnergy> ELECTRIC_CAPABILITY = null;
	
	public EnergyAutomation() {
		CapabilityManager.INSTANCE.register(PipeEnergy.class, new Capability.IStorage<PipeEnergy>() {
			@Override
			public NBTBase writeNBT(Capability<PipeEnergy> cap, PipeEnergy pipe, EnumFacing s) {
				NBTTagCompound nbt = new NBTTagCompound();
				pipe.writeToNBT(nbt, "");
				return nbt;
			}
			@Override
			public void readNBT(Capability<PipeEnergy> cap, PipeEnergy pipe, EnumFacing s, NBTBase nbt) {
				pipe.readFromNBT((NBTTagCompound)nbt, "");
			}
		}, new Callable<PipeEnergy>() {
			@Override
			public PipeEnergy call() throws Exception {
				return new PipeEnergy(0, 0);
			}
		});
	}
	
	@Override
	public IEnergyAccess create(TileEntity te, EnumFacing s)  {
		return te.getCapability(ELECTRIC_CAPABILITY, s);
	}
	
	@Override
	public IEnergyAccess create(ItemStack item, int s) {
		return item != null && item.getItem() instanceof IEnergyItem ? new EnergyItem(item, (IEnergyItem)item.getItem(), s) : null;
	}
}
