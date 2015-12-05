/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cd4017be.lib.templates;

import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;

/**
 *
 * @author CD4017BE
 */
public interface IAutomatedInv extends ISidedInventory
{
    public boolean canInsert(ItemStack item, int cmp, int i);
    public boolean canExtract(ItemStack item, int cmp, int i);
    public boolean isValid(ItemStack item, int cmp, int i);
    public void slotChange(ItemStack oldItem, ItemStack newItem, int i);
}
