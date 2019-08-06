package cd4017be.lib.block;

import cd4017be.lib.property.PropertyWrapObj;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.property.IUnlistedProperty;

/**
 * 
 * @author CD4017BE
 */
public abstract class MultipartBlock extends AdvancedBlock implements IMultipartBlock {

	public PropertyInteger baseState;
	public static final IUnlistedProperty<IModularTile> moduleRef = PropertyWrapObj.MULTIPART;

	public final int numModules;

	/**
	 * @see AdvancedBlock#AdvancedBlock(String, Material, SoundType, int, Class)
	 * @param mods number of modules handled by renderer
	 */
	public MultipartBlock(String id, Material m, SoundType sound, int flags, int mods, Class<? extends TileEntity> tile) {
		super(id, m, sound, flags, tile);
		this.numModules = mods;
	}

	@Override
	public IProperty<?> getBaseState() {
		return baseState;
	}

	@Override
	public int moduleCount() {
		return numModules;
	}

	@Override
	public boolean renderMultilayer() {
		return getBlockLayer() == null;
	}

	protected abstract PropertyInteger createBaseState();

	@Override
	protected BlockStateContainer createBlockState() {
		L_PROPERTIES.add(baseState = createBaseState());
		UL_PROPERTIES.add(moduleRef);
		return super.createBlockState();
	}

	@Override
	public IBlockState getStateFromMeta(int meta) {
		return baseState == null ? getDefaultState() : blockState.getBaseState().withProperty(baseState, meta);
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		return baseState == null ? 0 : state.getValue(baseState);
	}

	public MultipartBlock setMultilayer() {
		setBlockLayer(null);
		return this;
	}

	public interface IModularTile {
		/**
		 * @param m module property index
		 * @return state for given module
		 */
		public <T> T getModuleState(int m);
		/**
		 * @param m module property index
		 * @return whether given module exists (= has collision box)
		 */
		public boolean isModulePresent(int m);
		/**
		 * @return whether the current state is a full opaque block (to render only sided quads)
		 */
		public default boolean isOpaque() {return false;}
	}

}
