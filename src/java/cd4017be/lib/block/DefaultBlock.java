/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cd4017be.lib.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.fml.common.registry.GameRegistry;

/**
 *
 * @author CD4017BE
 * @deprecated replaced by BaseBlock
 */
@Deprecated
public class DefaultBlock extends Block
{
	
	public DefaultBlock(String id, Material m) {
		super(m);
		this.setRegistryName(id);
		GameRegistry.register(this);
		this.setUnlocalizedName("cd4017be." + id);
	}

	@Override
	public String getLocalizedName() {
		return I18n.translateToLocal(this.getUnlocalizedName() + ".name");
	}
	
}
