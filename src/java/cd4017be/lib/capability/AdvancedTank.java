package cd4017be.lib.capability;

import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

import static net.minecraftforge.fluids.capability.CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY;
import static net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE;
import static net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE;

import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;

/**
 * IFluidHandler implementation for a single tank with integrated FluidContainer fill/drain mechanism.
 * @author cd4017be
 */
public class AdvancedTank extends AbstractInventory implements IFluidHandlerModifiable {
	/**owner */
	public final BlockEntity tile;
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
	public AdvancedTank(BlockEntity tile, int cap, boolean out) {
		this(tile, cap, out, null);
	}

	/**
	 * creates a tank that is permanently locked to the given fluid type
	 * @param cap capacity
	 * @param out whether this is considered as output tank
	 * @param type fluid type to lock to
	 */
	public AdvancedTank(BlockEntity tile, int cap, boolean out, Fluid type) {
		this.tile = tile;
		this.cap = cap;
		this.output = out;
		this.need = out ? Integer.MAX_VALUE : Integer.MIN_VALUE;
		this.fixed = type != null;
		this.lock = fixed;
		this.fluid = fixed ? new FluidStack(type, 0) : FluidStack.EMPTY;
		this.cont = ItemStack.EMPTY;
	}

	/**
	 * for changing the lock state during operation
	 * @param lock the new fluid type lock state
	 */
	public void setLock(boolean lock) {
		if (fixed) return;
		if (lock) this.lock = !fluid.isEmpty();
		else {
			this.lock = false;
			if (fluid.isEmpty())
				fluid = FluidStack.EMPTY;
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

	@SuppressWarnings("resource")
	@Override
	public void setStackInSlot(int slot, ItemStack stack) {
		cont = stack;
		if (!tile.hasLevel() || tile.getLevel().isClientSide) return;
		if (output) fillContainer();
		else drainContainer();
		tile.setChanged();
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
	public FluidStack getFluidInTank(int tank) {
		return fluid;
	}

	@Override
	public int getTankCapacity(int tank) {
		return cap;
	}

	@Override
	public void setFluidInTank(int tank, FluidStack stack) {
		this.fluid = stack;
	}

	@Override
	public boolean isFluidValid(int tank, FluidStack stack) {
		return !fixed || fluid.isFluidEqual(stack);
	}

	@Override
	public int fill(FluidStack res, FluidAction action) {
		if (fluid.getRawFluid() == Fluids.EMPTY) {
			int m = Math.min(res.getAmount(), cap);
			if (action.execute()) {
				fluid = new FluidStack(res, m);
				if (output && m >= need) fillContainer();
				tile.setChanged();
			}
			return m;
		} else if (fluid.isFluidEqual(res)) {
			int m = Math.min(res.getAmount(), cap - fluid.getAmount());
			if (m != 0 && action.execute()) increment(m);
			return m;
		} else return 0;
	}

	@Override
	public FluidStack drain(FluidStack res, FluidAction action) {
		if (fluid.isEmpty() || !fluid.isFluidEqual(res))
			return FluidStack.EMPTY;
		int m = Math.min(res.getAmount(), fluid.getAmount());
		FluidStack ret = new FluidStack(fluid, m);
		if (action.execute()) decrement(m);
		return ret;
	}

	@Override
	public FluidStack drain(int m, FluidAction action) {
		if (fluid.isEmpty()) return FluidStack.EMPTY;
		if (fluid.getAmount() < m) m = fluid.getAmount();
		FluidStack ret = new FluidStack(fluid, m);
		if (action.execute()) decrement(m);
		return ret;
	}

	/**
	 * @return [mB] stored fluid amount
	 */
	public int amount() {
		return fluid.getAmount();
	}

	/**
	 * @return [mB] remaining free capacity
	 */
	public int free() {
		return cap - fluid.getAmount();
	}

	/**
	 * WARNING: contained fluid must not be null!
	 * @param n [mB] amount to increment the contained fluid by
	 */
	public void increment(int n) {
		fluid.setAmount(n += fluid.getAmount());
		if (output && n >= need) fillContainer();
		tile.setChanged();
	}

	/**
	 * WARNING: contained fluid must not be null!
	 * @param n [mB] amount to decrement the contained fluid by
	 */
	public void decrement(int n) {
		fluid.setAmount(n = fluid.getAmount() - n);
		if (n <= 0 && !lock) fluid = FluidStack.EMPTY;
		if (!output && n <= need) drainContainer();
		tile.setChanged();
	}

	/**
	 * Fills the currently held fluid container from the tank.<br>
	 * This operation is automatically performed by the tank on changes.
	 */
	public void fillContainer() {
		need = Integer.MAX_VALUE;
		if (cont.getCount() == 0) return;
		if (fluid.getRawFluid() == Fluids.EMPTY) {
			need = 0;
			return;
		}
		cont.getCapability(FLUID_HANDLER_ITEM_CAPABILITY).ifPresent(acc -> {
			fluid.shrink(acc.fill(fluid, EXECUTE));
			int n = fluid.getAmount();
			int m = acc.fill(new FluidStack(fluid, cap), SIMULATE);
			if (m > 0) need = m;
			if (n <= 0 && !lock) fluid = FluidStack.EMPTY;
			cont = acc.getContainer();
		});
	}

	/**
	 * Drains the currently held fluid container into the tank.<br>
	 * This operation is automatically performed by the tank on changes.
	 */
	public void drainContainer() {
		need = Integer.MIN_VALUE;
		cont.getCapability(FLUID_HANDLER_ITEM_CAPABILITY).ifPresent(acc -> {
			if (fluid.getRawFluid() == Fluids.EMPTY) {
				fluid = acc.drain(cap, EXECUTE);
				if (fluid.isEmpty()) return;
			} else {
				FluidStack res = acc.drain(new FluidStack(fluid, cap - fluid.getAmount()), EXECUTE);
				if (!res.isEmpty()) fluid.grow(res.getAmount());
			}
			FluidStack res = acc.drain(new FluidStack(fluid, cap), SIMULATE);
			if (!res.isEmpty()) need = cap - res.getAmount();
			cont = acc.getContainer();
		});
	}

	/**
	 * @return whether the tank currently tries to fill/drain a fluid container
	 */
	public boolean transposing() {
		return output ? need <= cap : need >= 0;
	}

	public void readNBT(CompoundTag nbt) {
		if (fixed) fluid.setAmount(nbt.getInt("Amount"));
		else {
			fluid = FluidStack.loadFluidStackFromNBT(nbt);
			lock = nbt.getBoolean("lock") && fluid.getRawFluid() != Fluids.EMPTY;
		}
		cont = ItemStack.of(nbt);
		if (cont.getCount() > 0) need = output ? 0 : cap;
	}

	public CompoundTag writeNBT(CompoundTag nbt) {
		if (fluid.getRawFluid() != Fluids.EMPTY) fluid.writeToNBT(nbt);
		else nbt.remove("FluidName");
		if (!cont.isEmpty()) cont.save(nbt);
		else nbt.remove("id");
		nbt.putBoolean("lock", lock);
		return nbt;
	}

	public int getComparatorValue() {
		int n = fluid.getAmount();
		return n <= 0 ? 0 : (int)((long)n * 14 / cap) + 1;
	}

}
