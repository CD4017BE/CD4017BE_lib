/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cd4017be.lib.templates;

import cd4017be.lib.util.Utils;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/**
 *
 * @author CD4017BE
 */
public class SlotItemType extends Slot
{
    
    private final ItemStack[] allowed;
    
    public SlotItemType(IInventory inv, int id, int x, int y, ItemStack... allowed)
    {
        super(inv, id, x, y);
        this.allowed = allowed;
    }

    @Override
    public boolean isItemValid(ItemStack item) 
    {
        if (item == null) return true;
        for (ItemStack comp : allowed)
        {
            if (comp == null) continue;
            else if (Utils.itemsEqual(comp, item)) return true;
            else if (!item.getHasSubtypes() && item.getItem() == comp.getItem()) return true;
        }
        return false;
    }
    
}
