/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cd4017be.lib;

import net.minecraft.util.IIcon;
import net.minecraftforge.fluids.Fluid;

/**
 *
 * @author CD4017BE
 */
public class ModFluid extends Fluid
{
    
    private String locName;
    
    public ModFluid(String name, String locname)
    {
        super(name);
        this.locName = locname;
    }

    @Override
    public String getLocalizedName() 
    {
        return this.locName;
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