package cd4017be.lib;

import cd4017be.lib.util.TooltipUtil;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

/**
 *
 * @author CD4017BE
 */
public class DefaultFluid extends Fluid
{
	
	public DefaultFluid(String name, String tex)
	{
		super(name, new ResourceLocation(tex), new ResourceLocation(tex));
	}

	@Override
	public String getLocalizedName(FluidStack stack)
	{
		return TooltipUtil.translate(this.getUnlocalizedName() + ".name");
	}
	
}