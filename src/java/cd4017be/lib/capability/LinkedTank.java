package cd4017be.lib.capability;

import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraftforge.fluids.FluidStack;

/**
 * 
 * @author CD4017BE
 */
public class LinkedTank implements IFluidHandlerModifiable {

	private final Supplier<FluidStack> get;
	private final Consumer<FluidStack> set;
	public int cap;

	public LinkedTank(int cap, Supplier<FluidStack> get, Consumer<FluidStack> set) {
		this.cap = cap;
		this.get = get;
		this.set = set;
	}

	@Override
	public int getTanks() {
		return 1;
	}

	@Override
	public FluidStack getFluidInTank(int tank) {
		return get.get();
	}

	@Override
	public int getTankCapacity(int tank) {
		return cap;
	}

	@Override
	public void setFluidInTank(int tank, FluidStack stack) {
		set.accept(stack);
	}

	@Override
	public boolean isFluidValid(int tank, FluidStack stack) {
		return true;
	}

	@Override
	public int fill(FluidStack res, FluidAction action) {
		FluidStack fluid = get.get();
		if (fluid.isEmpty()) {
			int m = Math.min(res.getAmount(), cap);
			if (action.execute()) set.accept(new FluidStack(res, m));
			return m;
		} else if (fluid.isFluidEqual(res)) {
			int m = Math.min(res.getAmount(), cap - fluid.getAmount());
			if (m != 0 && action.execute()) {
				fluid.grow(m);
				set.accept(fluid);
			}
			return m;
		} else return 0;
	}

	@Override
	public FluidStack drain(FluidStack res, FluidAction action) {
		FluidStack fluid = get.get();
		if (fluid.isEmpty() || !fluid.isFluidEqual(res)) return FluidStack.EMPTY;
		int m = Math.min(res.getAmount(), fluid.getAmount());
		if (action.execute()) {
			fluid.shrink(m);
			set.accept(fluid);
		}
		return new FluidStack(fluid, m);
	}

	@Override
	public FluidStack drain(int m, FluidAction action) {
		FluidStack fluid = get.get();
		if (fluid.isEmpty()) return FluidStack.EMPTY;
		if (fluid.getAmount() < m) m = fluid.getAmount();
		if (action.execute()) {
			fluid.shrink(m);
			set.accept(fluid);
		}
		return new FluidStack(fluid, m);
	}

}
