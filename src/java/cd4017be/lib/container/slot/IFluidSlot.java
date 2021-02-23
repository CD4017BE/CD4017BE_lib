package cd4017be.lib.container.slot;

import net.minecraftforge.fluids.FluidStack;

public interface IFluidSlot {

	FluidStack getFluid();
	void putFluid(FluidStack stack);
	int getCapacity();

}
