package cd4017be.lib.capability;

import cd4017be.lib.Gui.ITankContainer;
import cd4017be.lib.util.ItemFluidUtil.StackedFluidAccess;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.FluidTankProperties;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

/**
 * IFluidHandler implementation for a single tank with integrated FluidContainer fill/drain mechanism.
 * @author cd4017be
 */
public class AdvancedTank extends AbstractInventory implements IFluidHandler, ITankContainer {
	/**owner */
	public final TileEntity tile;
	/**stored fluid */
	public FluidStack fluid;
	/**internal fluid container slot */
	public ItemStack cont;
	/**whether the fluid type is (permanently) hard-fixed */
	public final boolean fixed;
	/**true = fill, false = drain fluid containers */
	public boolean output;
	/**whether the fluid type is currently locked */
	public boolean lock;
	/**[mB] capacity of the tank */
	public int cap;
	/**fill state required to transpose fluid container */
	private int need;

	/**
	 * creates a tank that allows any fluid type
	 * @param cap capacity
	 * @param out whether this is considered as output tank
	 */
	public AdvancedTank(TileEntity tile, int cap, boolean out) {
		this(tile, cap, out, null);
	}

	/**
	 * creates a tank that is permanently locked to the given fluid type
	 * @param cap capacity
	 * @param out whether this is considered as output tank
	 * @param type fluid type to lock to
	 */
	public AdvancedTank(TileEntity tile, int cap, boolean out, Fluid type) {
		this.tile = tile;
		this.cap = cap;
		this.output = out;
		this.need = out ? Integer.MAX_VALUE : Integer.MIN_VALUE;
		this.fixed = type != null;
		this.lock = fixed;
		this.fluid = fixed ? new FluidStack(type, 0) : null;
		this.cont = ItemStack.EMPTY;
	}

	/**
	 * for changing the lock state during operation
	 * @param lock the new fluid type lock state
	 */
	public void setLock(boolean lock) {
		if (fixed) return;
		if (lock) this.lock = fluid != null;
		else {
			this.lock = false;
			if (fluid != null && fluid.amount == 0) fluid = null;
		}
	}

	/**
	 * for changing the I/O direction during operation
	 * @param out the new I/O direction for fluid containers
	 */
	public void setOut(boolean out) {
		if (out != output) {
			output = out;
			if (out) fillContainer();
			else drainContainer();
		}
	}

	@Override
	public void setStackInSlot(int slot, ItemStack stack) {
		cont = stack;
		if (!tile.hasWorld() || tile.getWorld().isRemote) return;
		if (output) fillContainer();
		else drainContainer();
		tile.markDirty();
	}

	@Override
	public int getSlots() {
		return 1;
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		return cont;
	}

	@Override
	public int getTanks() {
		return 1;
	}

	@Override
	public FluidStack getTank(int i) {
		return fluid;
	}

	@Override
	public int getCapacity(int i) {
		return cap;
	}

	@Override
	public void setTank(int i, FluidStack fluid) {
		this.fluid = fluid;
	}

	@Override
	public IFluidTankProperties[] getTankProperties() {
		return new IFluidTankProperties[] {new FluidTankProperties(fluid, cap)};
	}

	@Override
	public int fill(FluidStack res, boolean doFill) {
		if (fluid == null) {
			int m = Math.min(res.amount, cap);
			if (doFill) {
				fluid = new FluidStack(res, m);
				if (output && m >= need) fillContainer();
				tile.markDirty();
			}
			return m;
		} else if (fluid.isFluidEqual(res)) {
			int m = Math.min(res.amount, cap - fluid.amount);
			if (m != 0 && doFill) increment(m);
			return m;
		} else return 0;
	}

	@Override
	public FluidStack drain(FluidStack res, boolean doDrain) {
		if (fluid == null || fluid.amount <= 0 || !fluid.isFluidEqual(res)) return null;
		int m = Math.min(res.amount, fluid.amount);
		FluidStack ret = new FluidStack(fluid, m);
		if (doDrain) decrement(m);
		return ret;
	}

	@Override
	public FluidStack drain(int m, boolean doDrain) {
		if (fluid == null || fluid.amount <= 0) return null;
		if (fluid.amount < m) m = fluid.amount;
		FluidStack ret = new FluidStack(fluid, m);
		if (doDrain) decrement(m);
		return ret;
	}

	/**
	 * @return [mB] stored fluid amount
	 */
	public int amount() {
		return fluid == null ? 0 : fluid.amount;
	}

	/**
	 * @return [mB] remaining free capacity
	 */
	public int free() {
		return fluid == null ? cap : cap - fluid.amount;
	}

	/**
	 * WARNING: contained fluid must not be null!
	 * @param n [mB] amount to increment the contained fluid by
	 */
	public void increment(int n) {
		n = fluid.amount += n;
		if (output && n >= need) fillContainer();
		tile.markDirty();
	}

	/**
	 * WARNING: contained fluid must not be null!
	 * @param n [mB] amount to decrement the contained fluid by
	 */
	public void decrement(int n) {
		n = fluid.amount -= n;
		if (n <= 0 && !lock) fluid = null;
		if (!output && n <= need) drainContainer();
		tile.markDirty();
	}

	/**
	 * Fills the currently held fluid container from the tank.<br>
	 * This operation is automatically performed by the tank on changes.
	 */
	public void fillContainer() {
		need = Integer.MAX_VALUE;
		if (cont.getCount() == 0) return;
		if (fluid == null) {
			need = 0;
			return;
		}
		StackedFluidAccess acc = new StackedFluidAccess(cont);
		if (!acc.valid()) return;
		int n = fluid.amount -= acc.fill(fluid, true);
		int m = acc.fill(new FluidStack(fluid, cap), false);
		if (m > 0) need = m;
		if (n <= 0 && !lock) fluid = null;
		cont = acc.result();
	}

	/**
	 * Drains the currently held fluid container into the tank.<br>
	 * This operation is automatically performed by the tank on changes.
	 */
	public void drainContainer() {
		need = Integer.MIN_VALUE;
		if (cont.getCount() == 0) return;
		StackedFluidAccess acc = new StackedFluidAccess(cont);
		if (!acc.valid()) return;
		if (fluid == null) {
			fluid = acc.drain(cap, true);
			if (fluid == null) return;
		} else {
			FluidStack res = acc.drain(new FluidStack(fluid, cap - fluid.amount), true);
			if (res != null) fluid.amount += res.amount;
		}
		FluidStack res = acc.drain(new FluidStack(fluid, cap), false);
		if (res != null) need = cap - res.amount;
		cont = acc.result();
	}

	/**
	 * @return whether the tank currently tries to fill/drain a fluid container
	 */
	public boolean transposing() {
		return output ? need <= cap : need >= 0;
	}

	public void readNBT(NBTTagCompound nbt) {
		if (fixed) fluid.amount = nbt.getInteger("Amount");
		else {
			fluid = FluidStack.loadFluidStackFromNBT(nbt);
			lock = fluid != null && nbt.getBoolean("lock");
		}
		cont = new ItemStack(nbt);
		if (cont.getCount() > 0) need = output ? 0 : cap;
	}

	public NBTTagCompound writeNBT(NBTTagCompound nbt) {
		if (fluid != null) fluid.writeToNBT(nbt);
		else nbt.removeTag("FluidName");
		if (!cont.isEmpty()) cont.writeToNBT(nbt);
		else nbt.removeTag("id");
		nbt.setBoolean("lock", lock);
		return nbt;
	}

	public int getComparatorValue() {
		return fluid == null || fluid.amount <= 0 ? 0 : (int)((long)fluid.amount * 14 / cap) + 1;
	}

}
