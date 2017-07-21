package cd4017be.lib.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraftforge.fml.common.registry.GameRegistry;

/**
 *
 * @author CD4017BE
 */
public class BaseBlock extends Block {

	public BaseBlock(String id, Material m) {
		super(m);
		this.setRegistryName(id);
		GameRegistry.register(this);
		this.setUnlocalizedName("cd4017be." + id);
	}

}
