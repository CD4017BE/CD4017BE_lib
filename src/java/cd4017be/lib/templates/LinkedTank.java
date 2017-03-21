package cd4017be.lib.templates;

import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.FluidTankProperties;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

public class LinkedTank implements IFluidHandler {

	private final Supplier<FluidStack> get;
	private final Consumer<FluidStack> set;
	public final int cap;

	public LinkedTank(int cap, Supplier<FluidStack> get, Consumer<FluidStack> set) {
		this.cap = cap;
		this.get = get;
		this.set = set;
	}

	@Override
	public IFluidTankProperties[] getTankProperties() {
		return new IFluidTankProperties[]{new FluidTankProperties(get.get(), cap)};
	}

	@Override
	public int fill(FluidStack res, boolean doFill) {
		FluidStack fluid = get.get();
		if (fluid == null) {
			int m = Math.min(res.amount, cap);
			if (doFill) set.accept(new FluidStack(res, m));
			return m;
		} else if (fluid.isFluidEqual(res)) {
			int m = Math.min(res.amount, cap - fluid.amount);
			if (doFill) {
				fluid.amount += m;
				set.accept(fluid);
			}
			return m;
		} else return 0;
	}

	@Override
	public FluidStack drain(FluidStack res, boolean doDrain) {
		FluidStack fluid = get.get();
		if (fluid == null || !fluid.isFluidEqual(res)) return null;
		int m = Math.min(res.amount, fluid.amount);
		if (doDrain) set.accept((fluid.amount -= m) > 0 ? fluid : null);
		return new FluidStack(fluid, m);
	}

	@Override
	public FluidStack drain(int m, boolean doDrain) {
		FluidStack fluid = get.get();
		if (fluid == null) return null;
		if (fluid.amount < m) m = fluid.amount;
		if (doDrain) set.accept((fluid.amount -= m) > 0 ? fluid : null);
		return new FluidStack(fluid, m);
	}

}
