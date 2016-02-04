/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cd4017be.lib.templates;

import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 *
 * @author CD4017BE
 */
public interface IPipe 
{
    public int textureForSide(byte s);
    public Cover getCover();
    
    public static class Cover {
        public ItemStack item;
        public IBlockState block;
        
        public Cover(IBlockState block)
        {
            this.block = block;
        }
        
        public static Cover create(ItemStack item) {
            if (item == null || !(item.getItem() instanceof ItemBlock)) return null;
            ItemBlock ib = (ItemBlock)item.getItem();
            Cover cover = new Cover(ib.block.getStateFromMeta(ib.getMetadata(item.getMetadata())));
            if (cover.block == null || cover.block.getBlock() instanceof BlockPipe || !cover.block.getBlock().isFullBlock()) return null;
            cover.item = item.copy();
            cover.item.stackSize = 1;
            return cover;
        }
        
        public static Cover read(NBTTagCompound nbt, String name) {
            if (!nbt.hasKey(name)) return null;
            else {
                ItemStack item = ItemStack.loadItemStackFromNBT(nbt.getCompoundTag(name));
                return create(item);
            }
        }
        
        public void write(NBTTagCompound nbt, String name)
        {
            NBTTagCompound tag = new NBTTagCompound();
            item.writeToNBT(tag);
            nbt.setTag(name, tag);
        }
        
    }
    
}
