package cd4017be.lib.render;

import net.minecraft.client.renderer.ItemMeshDefinition;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 
 * @author CD4017BE
 *
 */
@SideOnly(Side.CLIENT)
public class SingleTextureDefinition implements ItemMeshDefinition {
	public final String path;
	public SingleTextureDefinition(String path) {
		this.path = path;
	}
	@Override
	public ModelResourceLocation getModelLocation(ItemStack stack) {
		return new ModelResourceLocation(path, "inventory");
	}
}
