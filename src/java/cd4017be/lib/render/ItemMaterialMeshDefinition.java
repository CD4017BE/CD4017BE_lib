package cd4017be.lib.render;

import cd4017be.lib.item.ItemMaterial;
import cd4017be.lib.item.ItemMaterial.Variant;
import net.minecraft.client.renderer.ItemMeshDefinition;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 
 * @author CD4017BE
 */
@SideOnly(Side.CLIENT)
public class ItemMaterialMeshDefinition implements ItemMeshDefinition {

	private final ModelResourceLocation defaultLoc;

	public ItemMaterialMeshDefinition(ItemMaterial item) {
		this.defaultLoc = new ModelResourceLocation(item.getRegistryName(), "inventory");
		ModelBakery.registerItemVariants(item, defaultLoc);
		ResourceLocation[] locs = new ResourceLocation[item.variants.size()];
		int n = 0;
		for (Variant name : item.variants.values())
			locs[n++] = name.model;
		ModelBakery.registerItemVariants(item, locs);
	}

	@Override
	public ModelResourceLocation getModelLocation(ItemStack stack) {
		ItemMaterial item = (ItemMaterial)stack.getItem();
		Variant name = item.variants.get(stack.getItemDamage());
		if (name != null) return new ModelResourceLocation(name.model, "inventory");
		return defaultLoc;
	}

}