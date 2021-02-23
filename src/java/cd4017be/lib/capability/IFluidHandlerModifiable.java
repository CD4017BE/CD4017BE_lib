package cd4017be.lib.capability;

import cd4017be.lib.container.slot.SlotFluidHandler;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

/**The fluid equivalent of {@link IItemHandlerModifiable}, mainly used for {@link SlotFluidHandler} on client side.
 * @author CD4017BE */
public interface IFluidHandlerModifiable extends IFluidHandler {

	void setFluidInTank(int tank, FluidStack stack);

}
