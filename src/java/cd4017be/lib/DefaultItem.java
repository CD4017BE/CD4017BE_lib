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
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.fml.common.registry.GameRegistry;

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
        this.setRegistryName(id);
        this.setUnlocalizedName("cd4017be." + id);
        GameRegistry.register(this);
        this.init();
    }
    
    protected void init() {
    	BlockItemRegistry.registerItemStack(new ItemStack(this), "item." + this.getRegistryName().getResourcePath());
    }

	@Override
    public void addInformation(ItemStack item, EntityPlayer player, List<String> list, boolean b) 
    {
        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
        	String s = this.getUnlocalizedName(item) + ".tip";
        	String s1 = TooltipInfo.getLocFormat(s);
            if (!s1.equals(s)) list.addAll(Arrays.asList(s1.split("\n")));
        } else list.add(TooltipInfo.getShiftHint());
        super.addInformation(item, player, list, b);
    }
    
	@Override
	public String getUnlocalizedName(ItemStack item) {
		String s = super.getUnlocalizedName(item);
		return this.hasSubtypes ? s + ":" + item.getItemDamage() : s;
	}

	@Override
	public String getItemStackDisplayName(ItemStack item) {
		return I18n.translateToLocal(this.getUnlocalizedName(item) + ".name");
	}
    
}
