/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cd4017be.lib.templates;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/**
 *
 * @author CD4017BE
 */
public class SlotItemSel extends Slot
{
    private final ItemStack[] itemList;
    private int selectedItem;
    
    public SlotItemSel(IInventory inventory, int id, int x, int y, ItemStack[] itemList)
    {
        super(inventory, id, x, y);
        this.itemList = itemList;
        this.selectedItem = 0;
    }
    
    @Override
    public boolean canTakeStack(EntityPlayer par1EntityPlayer) 
    {
        return false;
    }

    @Override
    public boolean isItemValid(ItemStack par1ItemStack) 
    {
        return false;
    }
    
    public void onClicked()
    {
        selectedItem++;
        if (selectedItem >= itemList.length) selectedItem = 0;
        this.putStack(itemList[selectedItem]);
    }
}
