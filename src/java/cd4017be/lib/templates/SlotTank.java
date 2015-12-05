/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cd4017be.lib.templates;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.IFluidContainerItem;

/**
 *
 * @author CD4017BE
 */
public class SlotTank extends Slot
{
    
    public SlotTank(IInventory inventory, int slot, int x, int y)
    {
        super(inventory, slot, x, y);
    }

    @Override
    public boolean isItemValid(ItemStack item) 
    {
        return item.getItem() instanceof IFluidContainerItem || FluidContainerRegistry.isContainer(item);
    }
    
}
