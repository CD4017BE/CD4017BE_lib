package cd4017be.lib.templates;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class TabMaterials extends CreativeTabs {

	public ItemStack item;

	public TabMaterials(String label) {
		super(label);
	}

	@Override
	public ItemStack getIconItemStack() {
		return item;
	}

	@Override
	public Item getTabIconItem() {
		return item.getItem();
	}

}
