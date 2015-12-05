/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cd4017be.api.automation;

import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 *
 * @author CD4017BE
 */
public class AntimatterItemHandler 
{
    public static interface IAntimatterItem {
        public int getAmCapacity(ItemStack item);
        public String getAntimatterTag(ItemStack item);
    }
    
    public static void addInformation(ItemStack item, List list)
    {
        if (isAntimatterItem(item)) {
            list.add(String.format("Antimatter: %d / %d ng", getAntimatter(item), ((IAntimatterItem)item.getItem()).getAmCapacity(item)));
        }
    }
    
    public static int getAntimatter(ItemStack item)
    {
        if (item != null && item.getItem() instanceof IAntimatterItem){
            IAntimatterItem ei = (IAntimatterItem)item.getItem();
            String tag = ei.getAntimatterTag(item);
            createNBT(item, tag);
            return item.stackTagCompound.getInteger(tag);
        } else return 0;
    }
    
    public static int addAntimatter(ItemStack item, int n)
    {
        if (item != null && item.getItem() instanceof IAntimatterItem) {
            IAntimatterItem ei = (IAntimatterItem)item.getItem();
            String tag = ei.getAntimatterTag(item);
            createNBT(item, tag);
            int cap = ei.getAmCapacity(item), e = item.stackTagCompound.getInteger(tag) + n, r, s;
            if (e < 0) {
                s = 0;
                r = n - e;
            } else if (e > cap) {
                s = cap;
                r = n - e + cap;
            } else {
                s = e;
                r = n;
            }
            item.stackTagCompound.setInteger(tag, s);
            return r;
        } else return 0;
    }
    
    public static boolean isAntimatterItem(ItemStack item)
    {
        return item != null && item.getItem() instanceof IAntimatterItem;
    }
    
    private static void createNBT(ItemStack item, String tag)
    {
        if (item.stackTagCompound == null) item.stackTagCompound = new NBTTagCompound();
        if (!item.stackTagCompound.hasKey(tag)) item.stackTagCompound.setInteger(tag, 0);
    }
}
