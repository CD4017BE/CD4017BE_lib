package cd4017be.api.indlog.filter;

import javax.annotation.Nullable;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

/**
 * Implemented by Items that should act as filter for fluids.
 * @author cd4017be
 */
public interface FluidFilterProvider {

	/**
	 * @param stack the actual stack representing the filter or null if created directly from NBT
	 * @return a new filter instance provided by this item
	 */
	PipeFilter<FluidStack, IFluidHandler> getFluidFilter(@Nullable ItemStack stack);

	public static PipeFilter<FluidStack, IFluidHandler> load(NBTTagCompound nbt) {
		Item item;
		if (nbt.hasKey("id", NBT.TAG_STRING)) item = Item.getByNameOrId(nbt.getString("id"));
		else item = Item.getByNameOrId("indlog:fluid_filter"); //backward compatibility
		if (item instanceof FluidFilterProvider) {
			PipeFilter<FluidStack, IFluidHandler> filter = ((FluidFilterProvider)item).getFluidFilter(null);
			if (filter != null) filter.deserializeNBT(nbt);
			return filter;
		} else return null;
	}

}
