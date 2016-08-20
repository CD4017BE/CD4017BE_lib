package cd4017be.lib.templates;

import java.util.HashMap;
import java.util.List;

import cd4017be.lib.BlockItemRegistry;
import cd4017be.lib.DefaultItem;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * 
 * @author CD4017BE
 */
public class ItemMaterial extends DefaultItem {

	public HashMap<Integer, String> variants = new HashMap<Integer, String>();

	public ItemMaterial(String id) {
		super(id);
		this.setHasSubtypes(true);
	}

	@Override
	protected void init() {}

	@Override
	public String getUnlocalizedName(ItemStack item) {
		String name = variants.get(item.getItemDamage());
		return this.getUnlocalizedName() + (name == null ? "" : ":" + name);
	}

	@Override
	public void getSubItems(Item item, CreativeTabs tab, List<ItemStack> subItems) {
		for (int i : variants.keySet())
			subItems.add(new ItemStack(item, 1, i));
	}

	public void addMaterial(int id, String name) {
		if (variants.containsKey(id)) throw new IllegalArgumentException("Id already occupied!");
		variants.put(id, name);
		BlockItemRegistry.registerItemStack(new ItemStack(this, 1, id), this.getRegistryName().getResourcePath() + "." + name);
	}

}
