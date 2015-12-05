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
        	String s = TooltipInfo.getInfo(this.getUnlocalizedName(item) + ":" + item.getItemDamage());
            if (s == null) s = TooltipInfo.getInfo(this.getUnlocalizedName(item));
            if (s != null) list.addAll(Arrays.asList(s.split("\n")));
        } else list.add("<SHIFT for info>");
        super.addInformation(item, player, list, b);
    }
    
}
