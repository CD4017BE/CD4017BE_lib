package cd4017be.lib.templates;

import cd4017be.api.IAbstractTile;
import cd4017be.lib.util.ItemFluidUtil.StackedFluidAccess;
import cd4017be.lib.util.Utils;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

/**
 * Fully automated template for fluid tanks, where up to 4 of them can be "listed" to have side access and other automation properties.
 * @author CD4017BE
 */
@Deprecated
public class TankContainer implements ITankContainer {

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
			sideCfg |= 1L << (i + 48);
		}
		return this;
	}

	/**
	 * call each tick to update automation
	 * @param tile the TileEntity owning this
	 */
	public void update(IAbstractTile tile, Inventory inv) {
		byte cfg;
		IFluidHandler access;
		FluidStack fluid;
		for (EnumFacing s : EnumFacing.VALUES) {
			if((cfg = (byte)(sideCfg >> (s.ordinal() * 8))) == 0) continue;
			ICapabilityProvider te = tile.getTileOnSide(s);
			if (te == null || !te.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, s.getOpposite())) continue;
			access = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, s.getOpposite());
			for (Tank t : tanks)  {
				int c = cfg & 3; cfg >>= 2;
				if (c == 1 && t.dir == Utils.IN) {
					if ((fluid = fluids[t.idx]) != null) {
						if ((fluid = access.drain(new FluidStack(fluid, t.cap - fluid.amount), true)) != null) 
							fluids[t.idx].amount += fluid.amount;
					} else if (t.types == null) {
						fluids[t.idx] = access.drain(t.cap, true);
					} else for (Fluid f : t.types) {
						if ((fluids[t.idx] = access.drain(new FluidStack(f, t.cap), true)) != null) break;
					}
				} else if (c == 2 && t.dir == Utils.OUT && (fluid = fluids[t.idx]) != null && fluid.amount > 0 && 
					(fluid.amount -= access.fill(fluid.copy(), true)) <= 0 && (sideCfg >> (t.idx + 48) & 1) == 0) 
						fluids[t.idx] = null;
			}
		}
		if (inv != null) {
			StackedFluidAccess acc;
			for (Tank t : tanks) {
				if (t.inSlot >= 0 && ((fluid = fluids[t.idx]) == null || fluid.amount < t.cap) && (acc = new StackedFluidAccess(inv.items[t.inSlot])).valid()) {
					if (fluid != null) {
						if ((fluid = acc.drain(new FluidStack(fluid, t.cap - fluid.amount), true)) != null) 
							fluids[t.idx].amount += fluid.amount;
					} else if (t.types == null) {
						fluids[t.idx] = acc.drain(t.cap, true);
					} else for (Fluid f : t.types) {
						if ((fluids[t.idx] = acc.drain(new FluidStack(f, t.cap), true)) != null) break;
					}
					inv.items[t.inSlot] = acc.result();
				}
				if (t.outSlot >= 0 && (fluid = fluids[t.idx]) != null && fluid.amount > 0 && (acc = new StackedFluidAccess(inv.items[t.outSlot])).valid()) {
					if ((fluid.amount -= acc.fill(fluid.copy(), true)) <= 0 && (sideCfg >> (t.idx + 48) & 1) == 0) fluids[t.idx] = null;
					inv.items[t.outSlot] = acc.result();
				}
			}
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
		FluidStack stack = fluids[t];
		if (!(stack != null ? stack.isFluidEqual(fluid) : tanks[t].acceptsType(fluid.getFluid()))) return 0;
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
		if (s >= 0) return (byte)(sideCfg >> (8 * s + 2 * t) & 3);
		else if (t < 0 || t >= tanks.length) return 0;
		byte dir = tanks[t].dir;
		return dir < 0 ? (byte)1 : dir > 0 ? (byte)2 : (byte)3;
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
	public class Tank {

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

		public boolean acceptsType(Fluid type) {
			if (types == null) return true;
			for (Fluid f : types) if (type == f) return true;
			return false;
		}

	}

	class TankAccess implements IFluidTankProperties {

		final int idx, dir;

		TankAccess(int id, int dir) {
			this.idx = id; this.dir = dir;
		}

		@Override
		public FluidStack getContents() {
			return fluids[idx] == null ? null : fluids[idx].copy();
		}

		@Override
		public int getCapacity() {
			return tanks[idx].cap;
		}

		@Override
		public boolean canFill() {
			return (dir & 1) != 0;
		}

		@Override
		public boolean canDrain() {
			return (dir & 2) != 0;
		}

		@Override
		public boolean canFillFluidType(FluidStack fluidStack) {
			return (dir & 1) != 0 && tanks[idx].acceptsType(fluidStack.getFluid());
		}

		@Override
		public boolean canDrainFluidType(FluidStack fluidStack) {
			return (dir & 2) != 0 && tanks[idx].acceptsType(fluidStack.getFluid());
		}

	}

	/**
	 * Used to access the TankContainer from a side via the Forge capabilities system.
	 * @author CD4017BE
	 */
	public class Access implements IFluidHandler {

		final TankAccess[] accs;

		public Access(EnumFacing s) {
			int ml = tanks.length;
			if (s == null) {
				accs = new TankAccess[ml];
				for (int i = 0; i < ml; i++) 
					accs[i] = new TankAccess(i, 3);
			} else {
				int c = (int)(sideCfg >> (s.ordinal() * 8));
				int n = Integer.bitCount((c | c >> 1) & 0x55);
				accs = new TankAccess[n];
				for (int i = 0, j = 0, k; j < n; i++)
					if((k = (c >> (i * 2) & 3)) != 0) accs[j++] = new TankAccess(i, k);
			}
		}

		@Override
		public IFluidTankProperties[] getTankProperties() {
			return accs;
		}

		@Override
		public int fill(FluidStack fluid, boolean doFill) {
			int am = fluid.amount, rem;
			FluidStack stack;
			for (TankAccess acc : accs)
				if ((acc.dir & 1) != 0 && ((stack = fluids[acc.idx]) != null ? stack.isFluidEqual(fluid) : tanks[acc.idx].acceptsType(fluid.getFluid()))) {
					rem = tanks[acc.idx].cap - (stack == null ? 0 : stack.amount);
					if (rem > am) {rem = am; am = 0;} else am -= rem;
					if (doFill && rem > 0) {
						if (stack != null) stack.amount += rem;
						else fluids[acc.idx] = new FluidStack(fluid, rem);
					}
					if (am <= 0) return fluid.amount;
				}
			return fluid.amount - am;
		}

		@Override
		public FluidStack drain(FluidStack fluid, boolean doDrain) {
			int am = fluid.amount, rem;
			FluidStack stack;
			for (TankAccess acc : accs)
				if ((acc.dir & 2) != 0 && (stack = fluids[acc.idx]) != null && stack.amount > 0 && stack.isFluidEqual(fluid)) {
					stack = fluids[acc.idx];
					rem = Math.min(am, stack.amount);
					am -= rem;
					if (doDrain && (stack.amount -= rem) <= 0 && (sideCfg >> (acc.idx + 48) & 1) == 0)
						fluids[acc.idx] = null;
					if (am <= 0) return fluid.copy();
				}
			return am == fluid.amount ? null : new FluidStack(fluid, fluid.amount - am);
		}

		@Override
		public FluidStack drain(int maxDrain, boolean doDrain) {
			FluidStack fluid = null, stack;
			int rem;
			for (TankAccess acc : accs) 
				if ((acc.dir & 2) != 0 && (stack = fluids[acc.idx]) != null && stack.amount > 0) {
					if (fluid == null) {
						rem = Math.min(maxDrain, stack.amount);
						fluid = new FluidStack(stack, rem);
					} else if (stack.isFluidEqual(fluid)) {
						rem = Math.min(maxDrain - fluid.amount, stack.amount);
						fluid.amount += rem;
					} else continue;
					if (doDrain && (stack.amount -= rem) <= 0 && (sideCfg >> (acc.idx + 48) & 1) == 0)
						fluids[acc.idx] = null;
					if (fluid.amount >= maxDrain) return fluid;
				}
			return fluid;
		}

	}

}
