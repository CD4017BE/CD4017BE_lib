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
public class SlotHolo extends Slot
{
    
    private final boolean locked;
    private final boolean stack;
    
    public SlotHolo(IInventory inv, int id, int x, int y, boolean locked, boolean stack)
    {
        super(inv, id, x, y);
        this.locked = locked;
        this.stack = stack;
    }

    @Override
    public boolean canTakeStack(EntityPlayer par1EntityPlayer) 
    {
        return !locked;
    }

    @Override
    public boolean isItemValid(ItemStack par1ItemStack) 
    {
        return !locked;
    }

    @Override
    public ItemStack decrStackSize(int par1) 
    {
        super.decrStackSize(par1);
        return null;
    }

    @Override
    public int getSlotStackLimit() 
    {
        return stack ? 64 : 1;
    }
    
}
