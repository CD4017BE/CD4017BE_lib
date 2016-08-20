/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cd4017be.lib.templates;

import cd4017be.lib.util.Utils;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;


/**
 * Fully automated template for fluid tanks, where up to 4 of them can be "listed" to have side access and other automation properties.
 * @author CD4017BE
 */
public class TankContainer implements ITankContainer
{
	/**	bits[0-47 6*4*2]: side * tank * access, bits[48-51 4*1]: tank * locked */
	public long sideCfg = 0;
	public final FluidStack[] fluids;
	public final Tank[] tanks;

	/**
	 * @param l total amount of fluid slots
	 * @param t amount of slots that are "listed" tanks (max 4), use tank() to define them.
	 */
	public TankContainer(int l, int t) {
		if (t > 4 || t > l) throw new IllegalArgumentException("Too many tanks! " + t + " / " + (l < 4 ? l : 4));
		fluids = new FluidStack[l];
		tanks = new Tank[t];
	}

	/**
	 * Set the properties of a tank. You must call this method for all of them, otherwise you may get NullPointerExceptions!
	 * @param i tanks index to set (0...t-1)
	 * @param cap [mB] capacity
	 * @param dir preferred direction: -1 input, 0 none, 1 output
	 * @param in inventory slot index for input from containers or -1 for none
	 * @param out inventory slot index for output to containers or -1 for none
	 * @param types list of allowed fluid types, leave empty to allow any.
	 * @return this for construction convenience
	 */
	public TankContainer tank(int i, int cap, byte dir, int in, int out, Fluid... types) {
		tanks[i] = new Tank(i, cap, dir, in, out, types);
		if (types.length == 1) {
			fluids[i] = new FluidStack(tanks[i].types[0], 0);
			sideCfg |= 1 << (i + 48);
		}
		return this;
	}

	/**
	 * call each tick to update automation
	 * @param tile the TileEntity owning this
	 */
	public void update(AutomatedTile tile) {
		byte cfg;
		IFluidHandler access;
		FluidStack fluid;
		for (byte s = 0; s < 6; s++) {
			if((cfg = (byte)(sideCfg >> (s * 8))) == 0) continue;
			TileEntity te = Utils.getTileOnSide(tile, s);
			if (te == null || !te.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, EnumFacing.VALUES[s^1])) continue;
			access = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, EnumFacing.VALUES[s^1]);
			for (int t = 0; t < tanks.length; t++, cfg >>= 2) 
				if ((cfg & 3) == 1 && tanks[t].dir == -1) {
					if ((fluid = fluids[t]) != null) {
						if ((fluid = access.drain(new FluidStack(fluid, tanks[t].cap - fluid.amount), true)) != null) 
							fluids[t].amount += fluid.amount;
					} else if (tanks[t].types == null) {
						fluids[t] = access.drain(tanks[t].cap, true);
					} else for (Fluid f : tanks[t].types) {
						if ((fluids[t] = access.drain(new FluidStack(f, tanks[t].cap), true)) != null) break;
					}
				} else if ((cfg & 3) == 2 && tanks[t].dir == 1 && (fluid = fluids[t]) != null && fluid.amount > 0 && 
					(fluid.amount -= access.fill(fluid.copy(), true)) <= 0 && (sideCfg >> (t + 48) & 1) == 0) 
						fluids[t] = null;
		}
		if (tile.inventory != null) {
			ItemStack item;
			for (int t = 0; t < tanks.length; t++) {
				if (tanks[t].inSlot >= 0 && ((fluid = fluids[t]) == null || fluid.amount < tanks[t].cap) && (item = tile.inventory.items[tanks[t].inSlot]) != null && item.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null)) {
					access = item.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null);
					if (fluid != null) {
						if ((fluid = access.drain(new FluidStack(fluid, tanks[t].cap - fluid.amount), true)) != null) 
							fluids[t].amount += fluid.amount;
					} else if (tanks[t].types == null) {
						fluids[t] = access.drain(tanks[t].cap, true);
					} else for (Fluid f : tanks[t].types) {
						if ((fluids[t] = access.drain(new FluidStack(f, tanks[t].cap), true)) != null) break;
					}
				}
				if (tanks[t].outSlot >= 0 && (fluid = fluids[t]) != null && fluid.amount > 0 && (item = tile.inventory.items[tanks[t].outSlot]) != null && item.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null)) {
					access = item.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null);
					if ((fluid.amount -= access.fill(fluid.copy(), true)) <= 0 && (sideCfg >> (t + 48) & 1) == 0) fluids[t] = null;
				}
			}
		}
	}

	public void setFluid(int t, FluidStack fluid) {
		if (fluid != null && fluid.amount == 0) fluid = null;
		if (fluid == null && fluids[t] != null && (sideCfg >> (t + 48) & 1) != 0) fluids[t].amount = 0;
		else fluids[t] = fluid;
	}

	public int getAmount(int t) {
		return fluids[t] == null ? 0 : fluids[t].amount;
	}

	public int getSpace(int t) {
		return fluids[t] == null ? tanks[t].cap : tanks[t].cap - fluids[t].amount;
	}

	public int fill(int t, FluidStack fluid, boolean doFill) {
		if (!tanks[t].canFillFluidType(fluid)) return 0;
		FluidStack stack = fluids[t];
		int rem = tanks[t].cap - (stack == null ? 0 : stack.amount);
		if (rem > fluid.amount) rem = fluid.amount;
		if (doFill && rem > 0) {
			if (stack != null) stack.amount += rem;
			else fluids[t] = new FluidStack(fluid, rem);
		}
		return rem;
	}

	public FluidStack drain(int t, int amount, boolean doDrain) {
		FluidStack stack;
		if ((stack = fluids[t]) == null || stack.amount <= 0) return null;
		int rem = Math.min(amount, stack.amount);
		if (doDrain && (stack.amount -= rem) <= 0 && (TankContainer.this.sideCfg >> (t + 48) & 1) == 0)
			TankContainer.this.fluids[t] = null;
		return new FluidStack(stack, rem);
	}

	public byte getConfig(int s, int t) {
		return (byte)(sideCfg >> (8 * s + 2 * t) & 3);
	}

	public boolean canUnlock(int t) {
		return tanks[t].types == null || tanks[t].types.length != 1;
	}

	public boolean isLocked(int t) {
		return (sideCfg >> (48 + t) & 1) != 0;
	}

	public void readFromNBT(NBTTagCompound nbt, String name) {
		sideCfg = nbt.getLong(name + "Cfg");
		for (int f = 0; f < fluids.length; f++) {
			String tagName = name + Integer.toHexString(f);
			fluids[f] = nbt.hasKey(tagName, 10) ? FluidStack.loadFluidStackFromNBT(nbt.getCompoundTag(tagName)) : null;
		}
	}

	public void writeToNBT(NBTTagCompound nbt, String name) {
		nbt.setLong(name + "Cfg", sideCfg);
		for (int f = 0; f < fluids.length; f++)
			if (fluids[f] != null) {
				NBTTagCompound tag = new NBTTagCompound();
				fluids[f].writeToNBT(tag);
				nbt.setTag(name + Integer.toHexString(f), tag);
			}
	}

	/**
	 * Defines the properties of a "listed" Tank such as: capacity, preferred transport direction, allowed fluid types and item container slots.
	 * @author CD4017BE
	 */
	public class Tank implements IFluidTankProperties {

		public final int idx, cap, inSlot, outSlot;
		public final byte dir;
		public final Fluid[] types;

		private Tank(int idx, int cap, byte dir, int in, int out, Fluid... types) {
			this.idx = idx;
			this.cap = cap;
			this.types = types.length == 0 ? null : types;
			this.dir = dir;
			this.inSlot = in;
			this.outSlot = out;
		}

		@Override
		public FluidStack getContents() {
			return TankContainer.this.fluids[idx] == null ? null : TankContainer.this.fluids[idx].copy();
		}

		@Override
		public int getCapacity() {
			return cap;
		}

		@Override
		public boolean canFill() {
			return true;
		}

		@Override
		public boolean canDrain() {
			return true;
		}

		@Override
		public boolean canFillFluidType(FluidStack fluidStack) {
			if (TankContainer.this.fluids[idx] != null) return TankContainer.this.fluids[idx].isFluidEqual(fluidStack);
			if (types == null) return true;
			for (Fluid f : types) if (fluidStack.getFluid() == f) return true;
			return false;
		}

		@Override
		public boolean canDrainFluidType(FluidStack fluidStack) {
			return TankContainer.this.fluids[idx] != null && TankContainer.this.fluids[idx].isFluidEqual(fluidStack);
		}
	}

	/**
	 * Used to access the TankContainer from a side via the Forge capabilities system.
	 * @author CD4017BE
	 */
	public class Access implements IFluidHandler {
		/** side config */
		final byte accessIdx;

		public Access(EnumFacing s) {
			if (s == null) accessIdx = (byte)0xff;
			else accessIdx = (byte)(TankContainer.this.sideCfg >> (s.ordinal() * 8));
		}

		@Override
		public IFluidTankProperties[] getTankProperties() {
			int ml = TankContainer.this.tanks.length, n = 0;
			for (int i = 0, k = 3; i < ml; i++, k <<= 2) 
				if((accessIdx & k) != 0) n++;
			Tank[] prop = new Tank[n];
			for (int i = 0, j = 0, k = 3; j < n; i++, k <<= 2)
				if((accessIdx & k) != 0) prop[j++] = TankContainer.this.tanks[i];
			return prop;
		}

		@Override
		public int fill(FluidStack fluid, boolean doFill) {
			int am = fluid.amount, rem;
			FluidStack stack;
			for (int i = 0, k = 1; am > 0 && i < 4; i++, k <<= 2) 
				if ((accessIdx & k) != 0 && TankContainer.this.tanks[i].canFillFluidType(fluid)) {
					stack = TankContainer.this.fluids[i];
					rem = TankContainer.this.tanks[i].cap - (stack == null ? 0 : stack.amount);
					if (rem > am) {rem = am; am = 0;} else am -= rem;
					if (doFill && rem > 0) {
						if (stack != null) stack.amount += rem;
						else TankContainer.this.fluids[i] = new FluidStack(fluid, rem);
					}
				}
			return fluid.amount - am;
		}

		@Override
		public FluidStack drain(FluidStack fluid, boolean doDrain) {
			int am = fluid.amount, rem;
			FluidStack stack;
			for (int i = 0, k = 1; am > 0 && i < 4; i++, k <<= 2) 
				if ((accessIdx & k) != 0 && (stack = TankContainer.this.fluids[i]) != null && stack.amount > 0 && stack.isFluidEqual(fluid)) {
					rem = Math.min(am, stack.amount);
					am -= rem;
					if (doDrain && (stack.amount -= rem) <= 0 && (TankContainer.this.sideCfg >> (i + 48) & 1) == 0)
						TankContainer.this.fluids[i] = null;
				}
			return new FluidStack(fluid, fluid.amount - am);
		}

		@Override
		public FluidStack drain(int maxDrain, boolean doDrain) {
			FluidStack fluid = null, stack;
			int rem;
			for (int i = 0, k = 1; i < 4; i++, k <<= 2) 
				if ((accessIdx & k) != 0 && (stack = TankContainer.this.fluids[i]) != null && stack.amount > 0) {
					if (fluid == null) {
						rem = Math.min(maxDrain, stack.amount);
						fluid = new FluidStack(stack, rem);
					} else if (stack.isFluidEqual(fluid)) {
						rem = Math.min(maxDrain - fluid.amount, stack.amount);
						fluid.amount += rem;
					} else continue;
					if (doDrain && (stack.amount -= rem) <= 0 && (TankContainer.this.sideCfg >> (i + 48) & 1) == 0)
						TankContainer.this.fluids[i] = null;
					if (fluid.amount >= maxDrain) return fluid;
				}
			return fluid;
		}

	}

	@Override
	public FluidStack getTank(int i) {
		return fluids[i];
	}

	@Override
	public int getCapacity(int i) {
		return tanks[i].cap;
	}

	@Override
	public void setTank(int i, FluidStack fluid) {
		fluids[i] = fluid;
	}

	@Override
	public int getTanks() {
		return tanks.length;
	}

}
