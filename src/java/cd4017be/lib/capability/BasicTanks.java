package cd4017be.lib.capability;

import java.util.Arrays;

import net.minecraftforge.fluids.FluidStack;

/**
 * @author CD4017BE */
public class BasicTanks implements IFluidHandlerModifiable {

	public final FluidStack[] inv;
	public final int[] caps;

	public BasicTanks(int slots, int cap) {
		this.inv = new FluidStack[slots];
		this.caps = new int[slots];
		Arrays.fill(caps, cap);
	}

	public BasicTanks(int... caps) {
		this.inv = new FluidStack[caps.length];
		this.caps = caps;
	}

	@Override
	public int getTanks() {
		return inv.length;
	}

	@Override
	public FluidStack getFluidInTank(int tank) {
		return inv[tank];
	}

	@Override
	public int getTankCapacity(int tank) {
		return caps[tank];
	}

	@Override
	public boolean isFluidValid(int tank, FluidStack stack) {
		return true;
	}

	@Override
	public int fill(FluidStack resource, FluidAction action) {
		return 0;
	}

	@Override
	public FluidStack drain(FluidStack resource, FluidAction action) {
		return FluidStack.EMPTY;
	}

	@Override
	public FluidStack drain(int maxDrain, FluidAction action) {
		return FluidStack.EMPTY;
	}

	@Override
	public void setFluidInTank(int i, FluidStack stack) {
		inv[i] = stack;
	}

}
