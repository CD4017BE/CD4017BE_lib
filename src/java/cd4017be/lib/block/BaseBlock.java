package cd4017be.lib.block;

import cd4017be.lib.util.TooltipUtil;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

/**
 *
 * @author CD4017BE
 */
public class BaseBlock extends Block {

	/**
	 * create and register a basic block<br>
	 * with default hardness & explosion resistance of stone
	 * @param id the block's registry name
	 * @param m the block's material
	 */
	public BaseBlock(String id, Material m) {
		super(m);
		this.setRegistryName(id);
		this.setUnlocalizedName(TooltipUtil.unlocalizedNameFor(this));
		this.setHardness(1.5F);
		this.setResistance(10F);
	}

}
