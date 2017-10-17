package cd4017be.lib.item;

import net.minecraft.block.Block;

/**
 * 
 * @author CD4017BE
 */
public class ItemVariantBlock extends BaseItemBlock {

	public ItemVariantBlock(Block id) {
		super(id);
		setHasSubtypes(true);
	}

	@Override
	public int getMetadata(int damage) {
		return damage;
	}

}
