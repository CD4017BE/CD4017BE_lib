package cd4017be.lib.block;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.NonNullList;

/**
 * 
 * @author CD4017BE
 */
public abstract class VariantBlock extends AdvancedBlock {

	/**
	 * @param id registry name
	 * @param m material
	 * @param sound
	 * @param flags 2 = nonOpaque, 1 = noFullBlock, 4 = don't open GUI
	 * @param variants amount of metadata subtypes to create
	 * @param tile optional TileEntity for the block
	 * @return new VariantBlock instance
	 */
	public static VariantBlock create(String id, Material m, SoundType sound, int flags, int variants, Class<? extends TileEntity> tile) {
		return new VariantBlock(id, m, sound, flags, tile) {
			@Override
			protected PropertyInteger createVariantState() {
				return PropertyInteger.create("type", 0, variants - 1);
			}
		};
	}

	public PropertyInteger prop;

	private VariantBlock(String id, Material m, SoundType sound, int flags, Class<? extends TileEntity> tile) {
		super(id, m, sound, flags, tile);
	}

	protected abstract PropertyInteger createVariantState();

	@Override
	protected BlockStateContainer createBlockState() {
		prop = createVariantState();
		return new BlockStateContainer(this, prop);
	}

	@Override
	public IBlockState getStateFromMeta(int meta) {
		return getDefaultState().withProperty(prop, meta);
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		return state.getValue(prop);
	}

	@Override
	public int damageDropped(IBlockState state) {
		return getMetaFromState(state);
	}

	@Override
	public void getSubBlocks(Item item, CreativeTabs tab, NonNullList<ItemStack> list) {
		for (int i : prop.getAllowedValues())
			list.add(new ItemStack(item, 1, i));
	}

}
