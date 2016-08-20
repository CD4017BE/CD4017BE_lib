package cd4017be.lib.render;

import cd4017be.lib.templates.ItemMaterial;
import net.minecraft.client.renderer.ItemMeshDefinition;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public class ItemMaterialMeshDefinition implements ItemMeshDefinition {

	public ItemMaterialMeshDefinition(ItemMaterial item) {
		ResourceLocation[] locs = new ResourceLocation[item.variants.size()];
    	String path = item.getRegistryName().toString() + "/";
		int n = 0;
		for (String name : item.variants.values())
    		locs[n++] = new ResourceLocation(path + name);
    	ModelBakery.registerItemVariants(item, locs);
	}

	@Override
	public ModelResourceLocation getModelLocation(ItemStack stack) {
		ItemMaterial item = (ItemMaterial)stack.getItem();
		String name = item.variants.get(stack.getItemDamage());
		if (name != null) return new ModelResourceLocation(item.getRegistryName().toString() + "/" + name, "inventory");
		return null;
	}
}
