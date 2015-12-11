/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cd4017be.lib;

import java.util.Arrays;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import org.lwjgl.input.Keyboard;

/**
 *
 * @author CD4017BE
 */
public class DefaultItem extends Item
{
    
    public DefaultItem(String id)
    {
        super();
        this.setUnlocalizedName(id);
        BlockItemRegistry.registerItem(this);
        this.init();
    }
    
    protected void init()
    {
        
    }

    @Override
    public void addInformation(ItemStack item, EntityPlayer player, List list, boolean b) 
    {
        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
        	String s = this.getUnlocalizedName(item) + ".tip";
        	String s1 = TooltipInfo.getLocFormat(s);
            if (!s1.equals(s)) list.addAll(Arrays.asList(s1.split("\n")));
        } else list.add("<SHIFT for info>");
        super.addInformation(item, player, list, b);
    }
    
	@Override
	public String getUnlocalizedName(ItemStack item) 
	{
		String s = super.getUnlocalizedName(item).replaceFirst("item.", "item.cd4017be.");
		if (this.hasSubtypes) s += ":" + item.getItemDamage();
		return s;
	}

	@Override
	public String getItemStackDisplayName(ItemStack item) 
	{
		return StatCollector.translateToLocal(this.getUnlocalizedName(item) + ".name");
	}
    
}
