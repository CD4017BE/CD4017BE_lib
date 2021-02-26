package cd4017be.lib.container.slot;

import cd4017be.lib.container.AdvancedContainer;
import net.minecraftforge.fluids.FluidStack;

/**Used to represent Fluid Tanks as Slots in {@link AdvancedContainer}.
 * @author CD4017BE */
public interface IFluidSlot {

	/**@return displayed capacity of the tank */
	int getCapacity();
	/**@return the fluid contained in the slot */
	FluidStack getFluid();
	/**@param stack the new fluid content received from server */
	void putFluid(FluidStack stack);

}
