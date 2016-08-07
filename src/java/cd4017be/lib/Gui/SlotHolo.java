/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cd4017be.lib.Gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 *
 * @author CD4017BE
 */
public class SlotHolo extends SlotItemHandler
{
    private final boolean locked, stack;
    
    public SlotHolo(IItemHandler inv, int id, int x, int y, boolean locked, boolean stack) {
        super(inv, id, x, y);
        this.locked = locked;
        this.stack = stack;
    }

    @Override
    public boolean canTakeStack(EntityPlayer player) {
        return !locked;
    }

    @Override
    public boolean isItemValid(ItemStack par1ItemStack) {
        return !locked;
    }

    @Override
    public int getSlotStackLimit() {
        return stack ? 64 : 1;
    }
    
}
