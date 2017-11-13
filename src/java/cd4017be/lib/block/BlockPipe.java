package cd4017be.lib.block;

import cd4017be.lib.property.PropertyByte;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.common.property.IUnlistedProperty;

/**
 * 
 * @author CD4017BE
 */
public abstract class BlockPipe extends MultipartBlock {

	public static final IUnlistedProperty<?>[] CON_PROPS = {
		new PropertyByte("cb"),
		new PropertyByte("ct"),
		new PropertyByte("cn"),
		new PropertyByte("cs"),
		new PropertyByte("cw"),
		new PropertyByte("ce")
	};

	public static BlockPipe create(String id, Material m, SoundType sound, Class<? extends TileEntity> tile, int states) {
		return new BlockPipe(id, m, sound, tile) {
			@Override
			protected PropertyInteger createBaseState() {
				return states > 1 ? PropertyInteger.create("type", 0, states - 1) : null;
			}
		};
	}

	protected BlockPipe(String id, Material m, SoundType sound, Class<? extends TileEntity> tile) {
		super(id, m, sound, 3, tile);
	}

	@Override
	protected IUnlistedProperty<?>[] createModules() {
		return CON_PROPS;
	}

	@Override
	public void getSubBlocks(Item item, CreativeTabs tab, NonNullList<ItemStack> list) {
		if (baseState != null)
			for (int i : baseState.getAllowedValues())
				list.add(new ItemStack(item, 1, i));
		else list.add(new ItemStack(item));
	}

	@Override
	public int damageDropped(IBlockState state) {
		return this.getMetaFromState(state);
	}

	public BlockPipe setSize(double size) {
		size /= 2.0;
		double min = 0.5 - size, max = 0.5 + size;
		boundingBox = new AxisAlignedBB[] {
			new AxisAlignedBB(min, min, min, max, max, max),
			new AxisAlignedBB(min, 0.0, min, max, min, max),
			new AxisAlignedBB(min, max, min, max, 1.0, max),
			new AxisAlignedBB(min, min, 0.0, max, max, min),
			new AxisAlignedBB(min, min, max, max, max, 1.0),
			new AxisAlignedBB(0.0, min, min, min, max, max),
			new AxisAlignedBB(max, min, min, 1.0, max, max),
		};
		return this;
	}

}
