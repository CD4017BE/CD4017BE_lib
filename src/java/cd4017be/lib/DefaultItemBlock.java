/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cd4017be.lib;

import java.util.Arrays;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import org.lwjgl.input.Keyboard;

/**
 *
 * @author CD4017BE
 */
public class DefaultItemBlock extends ItemBlock
{
    
    public DefaultItemBlock(Block id)
    {
        super(id);
        BlockItemRegistry.registerItemStack(new ItemStack(this), this.getUnlocalizedName());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
    public void addInformation(ItemStack item, EntityPlayer player, List list, boolean b) 
    {
        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
        	String s = this.getUnlocalizedName(item) + ".tip";
        	String s1 = TooltipInfo.getLocFormat(s);
        	if (s1.equals(s) && this.hasSubtypes) s1 = TooltipInfo.getLocFormat(s = this.block.getUnlocalizedName().replaceFirst("tile.", "tile.cd4017be.") + ".tip");
        	if (!s1.equals(s)) list.addAll(Arrays.asList(s1.split("\n")));
        } else list.add(TooltipInfo.getShiftHint());
        super.addInformation(item, player, list, b);
    }

    @Override
	public String getUnlocalizedName(ItemStack item) 
	{
		String s = this.block.getUnlocalizedName().replaceFirst("tile.", "tile.cd4017be.");
    	if (this.hasSubtypes) s += ":" + item.getItemDamage();
		return s;
	}

	@Override
	public String getItemStackDisplayName(ItemStack item) 
	{
		return StatCollector.translateToLocal(this.getUnlocalizedName(item) + ".name");
	}
    
}
