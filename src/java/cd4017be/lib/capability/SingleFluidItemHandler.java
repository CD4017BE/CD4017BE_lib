package cd4017be.lib.capability;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;

/**
 * 
 * @author CD4017BE
 */
public class SingleFluidItemHandler extends FluidHandlerItemStack {

	private final String tag;
	private final Fluid type;

	public SingleFluidItemHandler(ItemStack item, int cap, Fluid type, String tag) {
		super(item, cap);
		this.tag = tag;
		this.type = type;
	}

	@Override
	public FluidStack getFluid() {
		return new FluidStack(type, container.hasTagCompound() ? container.getTagCompound().getInteger(tag) : 0);
	}

	@Override
	protected void setFluid(FluidStack fluid) {
		int n = fluid == null ? 0 : fluid.amount;
		NBTTagCompound nbt = container.getTagCompound();
		if (nbt == null) container.setTagCompound(nbt = new NBTTagCompound());
		nbt.setInteger(tag, n);
	}

	@Override
	public boolean canFillFluidType(FluidStack fluid) {
		return fluid.getFluid() == type;
	}

	@Override
	protected void setContainerToEmpty() {
		if (container.hasTagCompound()) container.getTagCompound().setInteger(tag, 0);
	}

}
