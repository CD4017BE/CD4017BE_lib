/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cd4017be.lib;

import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.Fluid;

/**
 *
 * @author CD4017BE
 */
public class ModFluid extends Fluid
{
    
    public ModFluid(String name)
    {
        super(name);
    }

    @Override
    public String getLocalizedName()
    {
        return StatCollector.translateToLocal("fluid." + this.unlocalizedName + ".name");
    }
    
    @Override
    public IIcon getStillIcon() 
    {
        return this.getBlock().getIcon(0, 0);
    }
    
    public String getTexName()
    {
        return this.unlocalizedName;
    }
    
}