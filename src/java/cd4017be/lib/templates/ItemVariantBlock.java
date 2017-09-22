package cd4017be.lib.templates;

import cd4017be.lib.DefaultItemBlock;
import net.minecraft.block.Block;

/**
 * 
 * @author CD4017BE
 */
public class ItemVariantBlock extends DefaultItemBlock {

	public ItemVariantBlock(Block id) {
		super(id);
		setHasSubtypes(true);
	}

	@Override
	public int getMetadata(int damage) {
		return damage;
	}

}
