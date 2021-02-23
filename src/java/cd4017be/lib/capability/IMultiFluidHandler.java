package cd4017be.lib.capability;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

/**This {@link IFluidHandler} provides additional fill and drain access to specific internal tanks and
 * comes with all the fill/drain methods default implemented based on {@link IFluidHandlerModifiable}.
 * @author CD4017BE */
public interface IMultiFluidHandler extends IFluidHandlerModifiable {

	/**@param tank
	 * @return whether the given tank should be used in
	 *  {@link #fill(FluidStack, FluidAction)} */
	boolean shouldFill(int tank);

	/**@param tank
	 * @return whether the given tank should be used in
	 *  {@link #drain(FluidStack, FluidAction)} and {@link #drain(int, FluidAction)}
	 */
	boolean shouldDrain(int tank);

	@Override
	default int fill(FluidStack resource, FluidAction action) {
		for (int i = 0, l = getTanks(); i < l; i++) {
			if (!shouldFill(i)) continue;
			int n = fill(i, resource, action);
			if (n > 0) return n;
		}
		return 0;
	}

	@Override
	default FluidStack drain(FluidStack resource, FluidAction action) {
		for (int i = 0, l = getTanks(); i < l; i++) {
			if (!(shouldDrain(i) && resource.isFluidEqual(getFluidInTank(i)))) continue;
			FluidStack stack = drain(i, resource.getAmount(), action);
			if (!stack.isEmpty()) return stack;
		}
		return FluidStack.EMPTY;
	}

	@Override
	default FluidStack drain(int maxDrain, FluidAction action) {
		for (int i = 0, l = getTanks(); i < l; i++) {
			if (!shouldDrain(i)) continue;
			FluidStack stack = drain(i, maxDrain, action);
			if (!stack.isEmpty()) return stack;
		}
		return FluidStack.EMPTY;
	}

	/**Fills fluid into a specific internal tank.
	 * @param tank Tank slot to fill
	 * @param resource FluidStack representing the Fluid and maximum amount of fluid to be filled.
	 * @param action   If SIMULATE, fill will only be simulated.
	 * @return Amount of resource that was (or would have been, if simulated) filled. */
	default int fill(int tank, FluidStack resource, FluidAction action) {
		FluidStack stack = getFluidInTank(tank);
		if (stack.isEmpty()) {
			if (!isFluidValid(tank, resource)) return 0;
			int max = Math.min(resource.getAmount(), getTankCapacity(tank));
			if (action.execute()) setFluidInTank(tank, new FluidStack(resource, max));
			return max;
		} else if (stack.isFluidEqual(resource)) {
			int max = Math.min(resource.getAmount(), getTankCapacity(tank) - stack.getAmount());
			if (max <= 0) return 0;
			if (action.execute()) {
				stack.grow(max);
				setFluidInTank(tank, stack);
			}
			return max;
		} else return 0;
	}

	/**Drains fluid out of a specific internal tank.
	 * @param tank Tank slot to drain
	 * @param resource FluidStack representing the Fluid and maximum amount of fluid to be drained.
	 * @param action   If SIMULATE, drain will only be simulated.
	 * @return FluidStack representing the Fluid and amount that was (or would have been, if
	 * simulated) drained. */
	default FluidStack drain(int tank, int maxDrain, FluidAction action) {
		FluidStack stack = getFluidInTank(tank);
		maxDrain = Math.min(maxDrain, stack.getAmount());
		if (maxDrain <= 0) return FluidStack.EMPTY;
		FluidStack out = new FluidStack(stack, maxDrain);
		if (action.execute()) {
			stack.shrink(maxDrain);
			setFluidInTank(tank, stack);
		}
		return out;
	}

	/**@param tank the internal tank to access
	 * @return a fluid sub-handler representing the given internal tank */
	default IFluidHandler accessTank(int tank) {
		return new IFluidHandler() {
			@Override
			public boolean isFluidValid(int t, FluidStack stack) {
				return IMultiFluidHandler.this.isFluidValid(tank, stack);
			}
			
			@Override
			public int getTanks() {
				return 1;
			}
			
			@Override
			public int getTankCapacity(int t) {
				return IMultiFluidHandler.this.getTankCapacity(tank);
			}
			
			@Override
			public FluidStack getFluidInTank(int t) {
				return IMultiFluidHandler.this.getFluidInTank(tank);
			}
			
			@Override
			public int fill(FluidStack resource, FluidAction action) {
				return IMultiFluidHandler.this.fill(tank, resource, action);
			}
			
			@Override
			public FluidStack drain(int maxDrain, FluidAction action) {
				return IMultiFluidHandler.this.drain(tank, maxDrain, action);
			}
			
			@Override
			public FluidStack drain(FluidStack resource, FluidAction action) {
				if (resource.isFluidEqual(IMultiFluidHandler.this.getFluidInTank(tank)))
					return IMultiFluidHandler.this.drain(tank, resource.getAmount(), action);
				else return FluidStack.EMPTY;
			}
		};
	}

}
