package cd4017be.lib.capability;

import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluid;
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
		return new FluidStack(type, container.hasTag() ? container.getTag().getInt(tag) : 0);
	}

	@Override
	protected void setFluid(FluidStack fluid) {
		int n = fluid == null ? 0 : fluid.getAmount();
		CompoundTag nbt = container.getTag();
		if (nbt == null) container.setTag(nbt = new CompoundTag());
		nbt.putInt(tag, n);
	}

	@Override
	public boolean canFillFluidType(FluidStack fluid) {
		return fluid.getFluid() == type;
	}

	@Override
	protected void setContainerToEmpty() {
		if (container.hasTag()) container.getTag().putInt(tag, 0);
	}

}
