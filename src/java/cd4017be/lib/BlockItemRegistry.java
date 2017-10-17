package cd4017be.lib;

import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;

import cd4017be.lib.render.SingleTextureDefinition;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.ItemMeshDefinition;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/**
 *
 * @author CD4017BE
 */
public class BlockItemRegistry {

	private static HashMap<String, ItemStack> stacks = new HashMap<String, ItemStack>();

	@SideOnly(Side.CLIENT)
	public static void registerRender(Item item, int m0, int m1) {
		String id = item.getRegistryName().getResourcePath();
		for (int m = m0; m <= m1; m++)
			ModelLoader.setCustomModelResourceLocation(item, m, new ModelResourceLocation(new ResourceLocation(item.getRegistryName().getResourceDomain(), m == 0 ? id : id + "_" + m), "inventory"));
	}

	@SideOnly(Side.CLIENT)
	public static void registerRenderBS(Block block, int m0, int m1) {
		Item item = Item.getItemFromBlock(block);
		ResourceLocation base = item.getRegistryName();
		for (int m = m0; m <= m1; m++)
			ModelLoader.setCustomModelResourceLocation(item, m, new ModelResourceLocation(base, "inventory" + (m == 0 ? "" : m)));
	}

	@SideOnly(Side.CLIENT)
	public static void registerRender(Item item, ItemMeshDefinition def) {
		if (def == null) def = new SingleTextureDefinition(item.getRegistryName().toString());
		ModelLoader.setCustomMeshDefinition(item, def);
	}

	@SideOnly(Side.CLIENT)
	public static void registerModels(Item item, String... models) {
		ResourceLocation[] locs = new ResourceLocation[models.length];
		for (int i = 0; i < locs.length; i++)
			locs[i] = new ResourceLocation(item.getRegistryName().getResourceDomain(), models[i]);
		ModelBakery.registerItemVariants(item, locs);
	}

	@SideOnly(Side.CLIENT)
	public static void registerRender(Item item) {registerRender(item, 0, 0);}
	@SideOnly(Side.CLIENT)
	public static void registerRender(Block block, int m0, int m1) {registerRender(Item.getItemFromBlock(block), m0, m1);}
	@SideOnly(Side.CLIENT)
	public static void registerRender(Block block, ItemMeshDefinition def) {registerRender(Item.getItemFromBlock(block), def);}
	@SideOnly(Side.CLIENT)
	public static void registerRender(Block block) {registerRender(block, 0, 0);}
	@SideOnly(Side.CLIENT)
	public static void registerModels(Block block, String... models) {registerModels(Item.getItemFromBlock(block), models);}

	/**
	 * Registers a special ItemStack. Used for Items with sub types. 
	 * @param item
	 * @param name
	 */
	public static void registerItemStack(ItemStack item, String name) {
		stacks.put(name, item);
	}

	/**
	 * Registers multiple ItemStacks of the same Item for different damage values.
	 * @param startItem 
	 * @param names
	 */
	public static void registerMetadataItemStacks(ItemStack startItem, String... names) {
		int s = startItem.getItemDamage();
		for (int i = 0; i < names.length; i++) {
			if (names[i] == null) continue;
			ItemStack item = startItem.copy();
			item.setItemDamage(s + i);
			stacks.put(names[i], item);
		}
	}

	/**
	 * @param name
	 * @param n stacksize
	 * @return ItemStack registered for the given name with given stacksize.
	 */
	public static ItemStack stack(String name, int n) {
		ItemStack item = stacks.get(name);
		if (item == null) return ItemStack.EMPTY;
		ItemStack ret = item.copy();
		ret.setCount(n);
		return ret;
	}

	/**
	 * @param name
	 * @param n stacksize
	 * @param m damage
	 * @return ItemStack registered for the given name with given stacksize and damage.
	 */
	public static ItemStack stack(String name, int n, int m) {
		ItemStack item = stacks.get(name);
		if (item == null) return ItemStack.EMPTY;
		ItemStack ret = item.copy();
		ret.setCount(n);
		ret.setItemDamage(m);
		return ret;
	}

}
