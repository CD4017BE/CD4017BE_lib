/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cd4017be.lib.templates;

import java.util.ArrayList;
import java.util.List;

import cd4017be.lib.TileBlock;
import cd4017be.lib.templates.IPipe.Cover;
import cd4017be.lib.util.PropertyBlock;
import cd4017be.lib.util.PropertyByte;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;

/**
 *
 * @author CD4017BE
 */
@SuppressWarnings("rawtypes")
public class BlockPipe extends TileBlock
{
	public static final PropertyByte[] CONS = new PropertyByte[6];
	public static final PropertyByte CORE = new PropertyByte("core");
	public static final PropertyBlock COVER = new PropertyBlock("cover");
	public static final IUnlistedProperty[] RENDER_PROPS;
	static {
		ArrayList<IUnlistedProperty> list = new ArrayList<IUnlistedProperty>();
		list.add(CORE);
		for (int i = 0; i < 6; i++) {
			CONS[i] = new PropertyByte("con" + i);
			list.add(CONS[i]);
		}
		list.add(COVER);
		RENDER_PROPS = list.toArray(new IUnlistedProperty[list.size()]);
	}
	
	public float size = 0.25F;
	
	public BlockPipe(String id, Material m, SoundType sound, int type)
	{
		super(id, m, sound, type);
	}

	@Override
	protected BlockStateContainer createBlockState() {
		ArrayList<IProperty> main = new ArrayList<IProperty>();
		this.addProperties(main);
		return new ExtendedBlockState(this, main.toArray(new IProperty[main.size()]), RENDER_PROPS);
	}

	@Override
	public IBlockState getExtendedState(IBlockState oldState, IBlockAccess world, BlockPos pos) {
		IExtendedBlockState state = (IExtendedBlockState)oldState;
		TileEntity te = world.getTileEntity(pos);
		if (te != null && te instanceof IPipe) {
			IPipe pipe = (IPipe)te;
			state = state.withProperty(CORE, (byte)pipe.textureForSide((byte)-1));
			for (byte i = 0; i < 6; i++) 
				state = state.withProperty(CONS[i], (byte)pipe.textureForSide(i));
			Cover cover = pipe.getCover();
			if (cover != null)
				state = state.withProperty(COVER, cover.block.getBlock().getExtendedState(cover.block, world, pos));
			return state;
		} else return state.withProperty(CORE, (byte)this.getMetaFromState(oldState));
	}

	@Override
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
		TileEntity te = world.getTileEntity(pos);
		if (te != null && te instanceof IPipe && ((IPipe)te).getCover() == null)
			return this.outerBox((IPipe)te);
		else return FULL_BLOCK_AABB;
	}
	
	private AxisAlignedBB outerBox(IPipe pipe) {
		final double f1 = (1D - (double)size) / 2D, f2 = (1D + (double)size) / 2D;
		double[] bb = new double[]{f1, f2, f1, f2, f1, f2};
		for (byte s = 0; s < 6; s++)
			if (pipe.textureForSide(s) != -1) 
				bb[s] = (double)(s & 1);
		return new AxisAlignedBB(bb[4], bb[0], bb[2], bb[5], bb[1], bb[3]);
	}

	@Override
	public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB area, List<AxisAlignedBB> list, Entity entity) 
	{
		AxisAlignedBB box;
		TileEntity te = world.getTileEntity(pos);
		if (te == null || !(te instanceof IPipe) || ((IPipe)te).getCover() != null) {
			box = FULL_BLOCK_AABB.offset(pos);
			if (area.intersectsWith(box))list.add(box);
			return;
		}
		AxisAlignedBB box0 = this.outerBox((IPipe)te);
		double x = pos.getX(), y = pos.getY(), z = pos.getZ();
		final double d0 = (double)((1F - size) / 2F), d1 = (double)((1F + size) / 2F);
		box = new AxisAlignedBB(x + box0.minX, y + d0, z + d0, x + box0.maxX, y + d1, z + d1);
		if (box.intersectsWith(area)) list.add(box);
		if (box0.minY < d0 || box0.maxY > d1) {
			box = new AxisAlignedBB(x + d0, y + box0.minY, z + d0, x + d1, y + box0.maxY, z + d1);
			if (box.intersectsWith(area)) list.add(box);
		}
		if (box0.minZ < d0 || box0.maxZ > d1) {
			box = new AxisAlignedBB(x + d0, y + d0, z + box0.minZ, x + d1, y + d1, z + box0.maxZ);
			if (box.intersectsWith(area)) list.add(box);
		}
	}

	@Override
	public RayTraceResult collisionRayTrace(IBlockState state, World world, BlockPos pos, Vec3d v0, Vec3d v1) 
	{
		ArrayList<AxisAlignedBB> boxes = new ArrayList<AxisAlignedBB>();
		this.addCollisionBoxToList(state, world, pos, FULL_BLOCK_AABB.offset(pos), boxes, null);
		RayTraceResult rayTrace = null;
		for (AxisAlignedBB box : boxes) {
			rayTrace = box.calculateIntercept(v0, v1);
			if (rayTrace != null) return new RayTraceResult(rayTrace.hitVec, rayTrace.sideHit, pos);
		}
		return null;
	}

	@Override
	public boolean isNormalCube(IBlockState state) {
		return false;
	}

	@Override
	public boolean isNormalCube(IBlockState state, IBlockAccess world, BlockPos pos) 
	{
		TileEntity te = world.getTileEntity(pos);
		return te != null && te instanceof IPipe && ((IPipe)te).getCover() != null;
	}

	@Override
	public boolean isSideSolid(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
		if (redstone) return true;/*{
			TileEntity te = world.getTileEntity(pos);
			if (te != null && te instanceof IPipe) return ((IPipe)te).getCover() != null || ((IPipe)te).textureForSide((byte)side.ordinal()) >= 0;
		}*/
		return this.isNormalCube(state, world, pos);
	}

	@Override
	public boolean isFullCube(IBlockState state) {
		return false;
	}

	@Override
	public boolean isBlockSolid(IBlockAccess world, BlockPos pos, EnumFacing side) {
		return this.isNormalCube(world.getBlockState(pos), world, pos);
	}

	@Override
	public boolean isOpaqueCube(IBlockState state) {
		return false;
	}

	@Override
	public boolean isPassable(IBlockAccess world, BlockPos pos) {
		return false;
	}

	@Override
	public boolean doesSideBlockRendering(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing face) {
		TileEntity te = world.getTileEntity(pos);
		if (te != null && te instanceof IPipe) {
			IPipe.Cover cover = ((IPipe)te).getCover();
			if (cover != null) {
				return cover.block.isOpaqueCube();
			}
		}
		return false;
	}

	@Override
	public float getBlockHardness(IBlockState state, World world, BlockPos pos) 
	{
		float h = this.blockHardness;
		TileEntity te = world.getTileEntity(pos);
		if (te != null && te instanceof IPipe) {
			IPipe.Cover cover = ((IPipe)te).getCover();
			if (cover != null) {
				float h1 = cover.block.getBlockHardness(world, new BlockPos(0, -1, 0));
				if (h1 < 0) h = -1;
				else h += h1;
			}
		}
		return h;
	}

	@Override
	public float getExplosionResistance(World world, BlockPos pos, Entity ex, Explosion expl) 
	{
		float h = this.getExplosionResistance(ex);
		TileEntity te = world.getTileEntity(pos);
		if (te != null && te instanceof IPipe) {
			IPipe.Cover cover = ((IPipe)te).getCover();
			if (cover != null) {
				Block block = cover.block.getBlock();
				if (block != null) h += block.getExplosionResistance(ex);
			}
		}
		return h;
	}

	@Override
	public float getAmbientOcclusionLightValue(IBlockState state) {
		return 1.0F;
	}

	@Override
	public int getLightOpacity(IBlockState state, IBlockAccess world, BlockPos pos) 
	{
		return this.isNormalCube(state, world, pos) ? 255 : 0;
	}
	
}
