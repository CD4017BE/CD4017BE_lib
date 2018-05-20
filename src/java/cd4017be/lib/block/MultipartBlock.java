package cd4017be.lib.block;

import java.util.List;

import cd4017be.lib.property.PropertyWrapObj;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 
 * @author CD4017BE
 */
public abstract class MultipartBlock extends AdvancedBlock {

	public PropertyInteger baseState;
	public static final IUnlistedProperty<IModularTile> moduleRef = new PropertyWrapObj<IModularTile>("tile", IModularTile.class);

	public final int numModules;

	/**
	 * @see AdvancedBlock#AdvancedBlock(String, Material, SoundType, int, Class)
	 * @param mods number of modules handled by renderer
	 */
	public MultipartBlock(String id, Material m, SoundType sound, int flags, int mods, Class<? extends TileEntity> tile) {
		super(id, m, sound, flags, tile);
		this.numModules = mods;
	}

	protected abstract PropertyInteger createBaseState();

	@SideOnly(Side.CLIENT)
	public abstract String moduleVariant(int i);

	@SideOnly(Side.CLIENT)
	public abstract Class<?> moduleType(int i);

	@Override
	protected BlockStateContainer createBlockState() {
		baseState = createBaseState();
		return new ExtendedBlockState(this, baseState == null ? new IProperty[0] : new IProperty[] {baseState}, new IUnlistedProperty[] {moduleRef});
	}

	@Override
	public IBlockState getStateFromMeta(int meta) {
		return baseState == null ? getDefaultState() : blockState.getBaseState().withProperty(baseState, meta);
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		return baseState == null ? 0 : state.getValue(baseState);
	}

	@Override
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
		AxisAlignedBB box = boundingBox[0];
		if (box == FULL_BLOCK_AABB) return box;
		TileEntity te = world.getTileEntity(pos);
		if (te instanceof IModularTile) {
			IModularTile tile = (IModularTile) te;
			for (int i = 1; i < boundingBox.length; i++) {
				AxisAlignedBB box1 = boundingBox[i];
				if (box1 != NULL_AABB && tile.isModulePresent(i - 1)) {
					if (box1 == FULL_BLOCK_AABB) return box1;
					box = box.union(box1);
				}
			}
		}
		return box;
	}

	@Override
	public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> list, Entity entity, boolean b) {
		AxisAlignedBB box = boundingBox[0];
		addCollisionBoxToList(pos, entityBox, list, box);
		if (box == FULL_BLOCK_AABB) return;
		TileEntity te = world.getTileEntity(pos);
		if (te instanceof IModularTile) {
			IModularTile tile = (IModularTile) te;
			for (int i = 1; i < boundingBox.length; i++) {
				box = boundingBox[i];
				if (box != NULL_AABB && tile.isModulePresent(i - 1)) {
					addCollisionBoxToList(pos, entityBox, list, box);
					if (box == FULL_BLOCK_AABB) return;
				}
			}
		}
	}

	@Override
	public RayTraceResult collisionRayTrace(IBlockState state, World world, BlockPos pos, Vec3d start, Vec3d end) {
		start = start.subtract(pos.getX(), pos.getY(), pos.getZ());
		end = end.subtract(pos.getX(), pos.getY(), pos.getZ());
		int p = 0;
		AxisAlignedBB box = boundingBox[p];
		RayTraceResult collision = box.calculateIntercept(start, end);
		if (collision != null) end = collision.hitVec;
		if (box != FULL_BLOCK_AABB) {
			TileEntity te = world.getTileEntity(pos);
			if (te instanceof IModularTile) {
				IModularTile tile = (IModularTile) te;
				for (int i = 1; i < boundingBox.length; i++) {
					box = boundingBox[i];
					if (box != NULL_AABB && tile.isModulePresent(i - 1)) {
						RayTraceResult collision1 = box.calculateIntercept(start, end);
						if (collision1 != null) {
							collision = collision1;
							end = collision.hitVec;
							p = i;
						}
						if (box == FULL_BLOCK_AABB) break;
					}
				}
			}
		}
		if (collision != null) {
			collision = new RayTraceResult(end.addVector(pos.getX(), pos.getY(), pos.getZ()), collision.sideHit, pos);
			collision.subHit = p;
		}
		return collision; 
	}

	@Override
	public IBlockState getExtendedState(IBlockState state, IBlockAccess world, BlockPos pos) {
		IExtendedBlockState eState = (IExtendedBlockState)state;
		TileEntity te = world.getTileEntity(pos);
		if (te instanceof IModularTile)
			return eState.withProperty(moduleRef, (IModularTile)te);
		return eState;
	}

	public boolean renderMultilayer = false;

	public MultipartBlock setMultilayer() {
		renderMultilayer = true;
		return this;
	}

	@Override
	public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer) {
		return renderMultilayer || layer == getBlockLayer();
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
		@SideOnly(Side.CLIENT)
		public default boolean isOpaque() {return false;}
	}

}
