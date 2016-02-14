/*
 * To change this template, choose Tools | Templates
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
public class EnergyItemHandler 
{
    
    public static interface IEnergyItem {
        public int getEnergyCap(ItemStack item);
        public int getChargeSpeed(ItemStack item);
        public String getEnergyTag(ItemStack item);
    }
    
    public static void addInformation(ItemStack item, List list)
    {
        if (isEnergyItem(item)) {
            list.add(String.format("Energy: %d / %d kJ", getEnergy(item), ((IEnergyItem)item.getItem()).getEnergyCap(item)));
        }
    }
    
    public static int getEnergy(ItemStack item)
    {
        if (item != null && item.getItem() instanceof IEnergyItem){
            IEnergyItem ei = (IEnergyItem)item.getItem();
            String tag = ei.getEnergyTag(item);
            createNBT(item, tag);
            return item.getTagCompound().getInteger(tag);
        } else return 0;
    }
    
    public static int addEnergy(ItemStack item, int n, boolean restrict)
    {
        if (item != null && item.getItem() instanceof IEnergyItem && item.stackSize == 1) {
            IEnergyItem ei = (IEnergyItem)item.getItem();
            String tag = ei.getEnergyTag(item);
            int cap = ei.getEnergyCap(item);
            createNBT(item, tag);
            if (restrict) {
                int max = ei.getChargeSpeed(item);
                if (n > max) n = max;
                else if (n < -max) n = -max;
            }
            int e = item.getTagCompound().getInteger(tag) + n;
            int r;
            int s;
            if (e < 0)
            {
                s = 0;
                r = n - e;
            } else
            if (e > cap)
            {
                s = cap;
                r = n - e + cap;
            } else
            {
                s = e;
                r = n;
            }
            item.getTagCompound().setInteger(tag, s);
            int d = item.getMaxDamage();
            item.setItemDamage(d - s * d / cap);
            return r;
        } else return 0;
    }
    
    public static boolean isEnergyItem(ItemStack item)
    {
        return item != null && item.getItem() instanceof IEnergyItem;
    }
    
    private static void createNBT(ItemStack item, String tag)
    {
        if (item.getTagCompound() == null) item.setTagCompound(new NBTTagCompound());
        if (!item.getTagCompound().hasKey(tag)) item.getTagCompound().setInteger(tag, 0);
    }
}
