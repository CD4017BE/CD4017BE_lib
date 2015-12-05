/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cd4017be.lib;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.LanguageRegistry;

import java.util.HashMap;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

/**
 *
 * @author CD4017BE
 */
public class BlockItemRegistry 
{
    public static HashMap<String, Short> blockItemIdMap = new HashMap();
    private static HashMap<String, Item> items = new HashMap();
    private static HashMap<String, Block> blocks = new HashMap();
    private static HashMap<String, ItemStack> stacks = new HashMap();
    
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
    
    /*
    public static int getOrCreateBlockId(String s)
    {
        Short id = blockItemIdMap.get(s);
        if (id == null)
        {
            while(Block.getBlockById(DefBlockId) != Blocks.air) {DefBlockId++;}
            id = Short.valueOf(DefBlockId++);
            blockItemIdMap.put(s, id);
            configModified = true;
            System.out.println("BlockId " + id.intValue() + " for " + s + " created");
        }
        return id.intValue();
    }
    
    public static int getOrCreateItemId(String s)
    {
        Short id = blockItemIdMap.get(s);
        if (id == null)
        {
            while(Item.getItemById(DefItemId) != null) {DefItemId++;}
            id = Short.valueOf(DefItemId++);
            blockItemIdMap.put(s, id);
            configModified = true;
            System.out.println("ItemId " + id.intValue() + " for " + s + " created");
        }
        return id.intValue();
    }
    
    public static void loadIdConfig(FMLPreInitializationEvent event)
    {
        if (configDir != null) return;
        configDir = new File(event.getModConfigurationDirectory(), "CD4017BEmodsIdConfig.txt");
        try {try {
            InputStreamReader fr = new InputStreamReader(new FileInputStream(configDir));
            String s = "";
            boolean read = false;
            int c = 0;
            while ((c = fr.read()) != -1)
            {
                if (c == '<')
                {
                    s = "";
                    read = true;
                } else
                if (read && c == '>')
                {
                    addReadId(s);
                    read = false;
                } else
                if (read)
                {
                    s += (char)c;
                }
            }
            fr.close();
        } catch (FileNotFoundException e){
            System.out.println("CD4017BE-lib: Config File is missing");
        }} catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void saveIdConfig()
    {
        if (!configModified || configDir == null) return;
        configModified = false;
        try {
            if (configDir.createNewFile()) System.out.println("CD4017BE-lib: new Config File created");
            OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream(configDir));
            fw.write("If you have ID-conflicts with other mods simply change the default-Id-entries to beginnings of free areas of IDs and delete all other entries\n");
            fw.write("< defaultStartIdBlocks = " + DefBlockId + " >\n");
            fw.write("< defaultStartIdItems = " + DefItemId + " >\n");
            Entry<String, Short>[] list = blockItemIdMap.entrySet().toArray(new Entry[blockItemIdMap.size()]);
            Arrays.sort(list, new Comparator() {
                @Override
                public int compare(Object o1, Object o2) {
                    return ((Short)((Entry)o1).getValue()) - (((Short)((Entry)o2).getValue()));
                }
            });
            for (Entry<String, Short> e : list) {
                fw.write("< " + e.getKey() + " = " + e.getValue() + " >\n");
            }
            fw.close();
            System.out.println("CD4017BE-lib: Config changes saved");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static void addReadId(String s) throws IOException
    {
        int p = s.indexOf('=');
        if (p < 0) return;
        String name = s.substring(0, p).trim();
        short id = Short.parseShort(s.substring(p + 1).trim());
        if ("defaultStartIdBlocks".equals(name)) {
            DefBlockId = id;
        } else if ("defaultStartIdItems".equals(name)) {
            DefItemId = id;
        } else {
            blockItemIdMap.put(name, id);
        }
        
    }
    */
    
    /**
     * Registers a Block with given Name-Id, ItemBlock-Class and parameters for the ItemBlock
     * @param block
     * @param id
     * @param item
     * @param par
     */
    public static void registerBlock(Block block, String id, Class<? extends ItemBlock> item, Object... par)
    {
        blocks.put(block.getUnlocalizedName(), block);
        GameRegistry.registerBlock(block, item, block.getUnlocalizedName(), par);
        if (item.equals(ItemBlock.class)) registerItemStack(new ItemStack(block), block.getUnlocalizedName());
    }
    
    /**
     * Registers an Item
     * @param item
     */
    public static void registerItem(Item item)
    {
        items.put(item.getUnlocalizedName(), item);
        GameRegistry.registerItem(item, item.getUnlocalizedName());
        if (!item.getHasSubtypes()) registerItemStack(new ItemStack(item), item.getUnlocalizedName());
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
     * @param name Item-name with "item." prefix.
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
    
    /**
     * Registers a localized name for an Item or Block
     * @param item
     * @param name
     */
    public static void registerName(Object item, String name)
    {
        LanguageRegistry.addName(item, name);
    }
    
}
