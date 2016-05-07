/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cd4017be.lib;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

/**
 *
 * @author CD4017BE
 */
public class ModFluid extends Fluid
{
    
    public ModFluid(String name, String tex)
    {
        super(name, new ResourceLocation(tex), new ResourceLocation(tex));
    }

    @Override
    public String getLocalizedName(FluidStack stack)
    {
        return I18n.translateToLocal(this.getUnlocalizedName() + ".name");
    }
    
}