/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cd4017be.lib;

import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;

import cd4017be.lib.render.SingleTextureDefinition;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemMeshDefinition;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/**
 *
 * @author CD4017BE
 */
public class BlockItemRegistry 
{
    public static HashMap<String, Short> blockItemIdMap = new HashMap<String, Short>();
    private static HashMap<String, Item> items = new HashMap<String, Item>();
    private static HashMap<String, Block> blocks = new HashMap<String, Block>();
    private static HashMap<String, ItemStack> stacks = new HashMap<String, ItemStack>();
    
    public static String currentMod = "";
    
    /**
     * Call this before registering Items or Blocks
     * @param name
     */
    public static void setMod(String name)
    {
        currentMod = name;
    }
    
    public static String texPath()
    {
        return currentMod.concat(":");
    }
    
    /**
     * Registers a Block with given Name-Id, ItemBlock-Class and parameters for the ItemBlock
     * @param block
     * @param id
     * @param item
     * @param par
     */
    public static void registerBlock(Block block, String id, Class<? extends ItemBlock> item, Object... par)
    {
        blocks.put(id /*block.getUnlocalizedName()*/, block);
        GameRegistry.registerBlock(block, item, id /*block.getUnlocalizedName()*/, par);
        if (item.equals(ItemBlock.class)) registerItemStack(new ItemStack(block), block.getUnlocalizedName());
    }
    
    /**
     * Registers an Item
     * @param item
     */
    public static void registerItem(Item item, String id)
    {
        items.put(id /*item.getUnlocalizedName()*/, item);
        GameRegistry.registerItem(item, id /*item.getUnlocalizedName()*/);
        if (!item.getHasSubtypes()) registerItemStack(new ItemStack(item), item.getUnlocalizedName());
    }
    
    /**
     * registers the .json model for the Item or optionally a specific meta type of it.
     * @param id name or "name:meta"
     */
    @SideOnly(Side.CLIENT)
    public static void registerItemRender(String id) {
    	int p = id.indexOf(':');
    	if (p <= 0) {
    		registerItemRender(getItem(id), new SingleTextureDefinition(texPath() + id));
    		return;
    	}
    	int m;
    	try {m = Integer.parseInt(id.substring(p + 1));} catch (NumberFormatException e) {m = 0;}
    	id = id.substring(0, p);
    	Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(getItem(id), m, new ModelResourceLocation(texPath() + id, "inventory" + (m != 0 ? "_" + m : "")));
    }
    
    @SideOnly(Side.CLIENT)
    public static void registerItemRender(Item item, ItemMeshDefinition def) {
    	Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(item, def);
    }
    
    /**
     * registers the .json model for the Block or optionally a specific meta type of it.
     * @param id name or "name:meta"
     */
    @SideOnly(Side.CLIENT)
    public static void registerBlockRender(String id)
    {
    	int p = id.indexOf(':');
    	if (p <= 0) {
    		registerBlockRender(id, new SingleTextureDefinition(texPath() + id));
    		return;
    	}
    	int m;
    	try {m = Integer.parseInt(id.substring(p + 1));} catch (NumberFormatException e) {m = 0;}
    	id = id.substring(0, p);
    	Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(Item.getItemFromBlock(getBlock(id)), m, new ModelResourceLocation(texPath() + id + (m != 0 ? "_" + m : ""), "inventory"));
    }
    
    @SideOnly(Side.CLIENT)
    public static void registerBlockRender(String id, ItemMeshDefinition def)
    {
    	Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(Item.getItemFromBlock(getBlock(id)), def);
    }
    
    @SideOnly(Side.CLIENT)
    public static void registerModels(String id, String... models) {
    	Item item = itemId(id);
    	if (item == null) item = Item.getItemFromBlock(blockId(id));
    	registerModels(item, models);
    }
    
    @SideOnly(Side.CLIENT)
    public static void registerModels(Block block, String... models) {
    	registerModels(Item.getItemFromBlock(block));
    }
    
    @SideOnly(Side.CLIENT)
    public static void registerModels(Item item, String... models) {
    	ResourceLocation[] locs = new ResourceLocation[models.length];
    	for (int i = 0; i < locs.length; i++) {
    		locs[i] = new ResourceLocation(currentMod, models[i]);
    	}
    	ModelBakery.registerItemVariants(item, locs);
    }
    
    /**
     * Registers a special ItemStack. Used for Items with sub types. 
     * @param item
     * @param name
     */
    public static void registerItemStack(ItemStack item, String name)
    {
        stacks.put(name, item);
    }
    
    /**
     * Registers multiple ItemStacks of the same Item for different damage values.
     * @param startItem 
     * @param names
     */
    public static void registerMetadataItemStacks(ItemStack startItem, String... names)
    {
        int s = startItem.getItemDamage();
        for (int i = 0; i < names.length; i++) {
            if (names[i] == null) continue;
            ItemStack item = startItem.copy();
            item.setItemDamage(s + i);
            stacks.put(names[i], item);
        }
    }
    
    
    public static Block getBlock(String name)
    {
        return blocks.get(name);
    }
    
    
    public static Item getItem(String name)
    {
        return items.get(name);
    }
    
    /**
     * @param name Block-name with "tile." prefix.
     * @return the Block registered for the given name.
     */
    public static Block blockId(String name)
    {
        return blocks.get(name);
    }
    
    /**
     * @param name Item-name.
     * @return the Item registered for the given name.
     */
    public static Item itemId(String name)
    {
        return items.get(name);
    }
    
    /**
     * @param name
     * @param n stacksize
     * @return ItemStack registered for the given name with given stacksize.
     */
    public static ItemStack stack(String name, int n)
    {
        ItemStack item = stacks.get(name);
        if (item == null) return null;
        ItemStack ret = item.copy();
        ret.stackSize = n;
        return ret;
    }
    
    /**
     * @param name
     * @param n stacksize
     * @param m damage
     * @return ItemStack registered for the given name with given stacksize and damage.
     */
    public static ItemStack stack(String name, int n, int m)
    {
        ItemStack item = stacks.get(name);
        if (item == null) return null;
        ItemStack ret = item.copy();
        ret.stackSize = n;
        ret.setItemDamage(m);
        return ret;
    }
    
}
