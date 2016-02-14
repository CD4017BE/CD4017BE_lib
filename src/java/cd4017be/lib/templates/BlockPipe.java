/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cd4017be.lib.templates;

import java.util.List;

import cd4017be.lib.TileBlock;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemBlock;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 *
 * @author CD4017BE
 */
public class BlockPipe extends TileBlock
{
	public float size = 0.25F;
	
    public BlockPipe(String id, Material m, Class<? extends ItemBlock> item, int type)
    {
        super(id, m, item, type);
        this.setRenderType(2);
    }

	private boolean keepBB = false;
    
    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess world, BlockPos pos) 
    {
    	if (keepBB) return;
        TileEntity te = world.getTileEntity(pos);
        float f1 = (1F - size) / 2, f2 = (1F + size) / 2;
        float[] bb = new float[]{f1, f2, f1, f2, f1, f2};
        if (te != null && te instanceof IPipe)
        {
            IPipe pipe = (IPipe)te;
            IPipe.Cover cover = pipe.getCover();
            if (cover != null) {
                bb = new float[]{0, 1, 0, 1, 0, 1};
            } else for (byte s = 0; s < 6; s++)
                if (pipe.textureForSide(s) >= 0) bb[s] = ((s & 1) == 0 ? 0F : 1F);
        }
        this.setBlockBounds(bb[4], bb[0], bb[2], bb[5], bb[1], bb[3]);
    }
    
    @Override
    public AxisAlignedBB getCollisionBoundingBox(World world, BlockPos pos, IBlockState state) 
    {
        this.setBlockBoundsBasedOnState(world, pos);
        return super.getCollisionBoundingBox(world, pos, state);
    }

    @Override
    public AxisAlignedBB getSelectedBoundingBox(World world, BlockPos pos) 
    {
        return this.getCollisionBoundingBox(world, pos, world.getBlockState(pos));
    }

    @Override
	public void addCollisionBoxesToList(World world, BlockPos pos, IBlockState state, AxisAlignedBB area, List<AxisAlignedBB> list, Entity entity) 
    {
		TileEntity te = world.getTileEntity(pos);
		AxisAlignedBB box;
		int x = pos.getX(), y = pos.getY(), z = pos.getZ();
		if (te != null && te instanceof IPipe && ((IPipe)te).getCover() != null) {
			box = new AxisAlignedBB((double)x, (double)y, (double)z, (double)x + 1D, (double)y + 1D, (double)z + 1D);
			if (area.intersectsWith(box))list.add(box);
			return;
        }
		this.setBlockBoundsBasedOnState(world, pos);
		double d0 = (double)((1F - size) / 2F), d1 = (double)((1F + size) / 2F);
    	box = new AxisAlignedBB((double)x + this.minX, (double)y + d0, (double)z + d0, (double)x + this.maxX, (double)y + d1, (double)z + d1);
    	if (box.intersectsWith(area)) list.add(box);
    	if (this.minY < d0 || this.maxY > d1) {
    		box = new AxisAlignedBB((double)x + d0, (double)y + this.minY, (double)z + d0, (double)x + d1, (double)y + this.maxY, (double)z + d1);
    		if (box.intersectsWith(area)) list.add(box);
    	}
    	if (this.minZ < d0 || this.maxZ > d1) {
    		box = new AxisAlignedBB((double)x + d0, (double)y + d0, (double)z + this.minZ, (double)x + d1, (double)y + d1, (double)z + this.maxZ);
    		if (box.intersectsWith(area)) list.add(box);
    	}
	}

	@Override
	public MovingObjectPosition collisionRayTrace(World world, BlockPos pos, Vec3 v0, Vec3 v1) 
	{
		TileEntity te = world.getTileEntity(pos);
		if (te != null && te instanceof IPipe && ((IPipe)te).getCover() != null) return super.collisionRayTrace(world, pos, v0, v1);
		this.setBlockBoundsBasedOnState(world, pos);
		MovingObjectPosition pos0, pos1;
		try {keepBB = true;
		double d0 = (double)((1F - size) / 2F), d1 = (double)((1F + size) / 2F);
		double y0 = this.minY, y1 = this.maxY, z0 = this.minZ, z1 = this.maxZ;
		this.minY = d0; this.maxY = d1; this.minZ = d0; this.maxZ = d1;
		pos0 = super.collisionRayTrace(world, pos, v0, v1);
		this.minX = d0; this.maxX = d1; this.minY = y0; this.maxY = y1;
		pos1 = super.collisionRayTrace(world, pos, v0, v1);
		if (pos1 != null && (pos0 == null || pos0.hitVec.squareDistanceTo(v0) > pos1.hitVec.squareDistanceTo(v0))) pos0 = pos1;
		this.minY = d0; this.maxY = d1; this.minZ = z0; this.maxZ = z1;
		pos1 = super.collisionRayTrace(world, pos, v0, v1);
		keepBB = false;} catch (RuntimeException e) {keepBB = false; throw e;}
		return pos1 != null && (pos0 == null || pos0.hitVec.squareDistanceTo(v0) > pos1.hitVec.squareDistanceTo(v0)) ? pos1 : pos0;
	}

	@Override
	public boolean isNormalCube(IBlockAccess world, BlockPos pos) 
    {
    	TileEntity te = world.getTileEntity(pos);
        return te != null && te instanceof IPipe && ((IPipe)te).getCover() != null;
	}

    @Override
	public boolean isSideSolid(IBlockAccess world, BlockPos pos, EnumFacing side) {
        return this.isNormalCube(world, pos);
	}

	@Override
	public boolean isFullCube() {
		return false;
	}

	@Override
	public boolean isBlockSolid(IBlockAccess world, BlockPos pos, EnumFacing side) {
		return this.isNormalCube(world, pos);
	}

	@Override
	public boolean doesSideBlockRendering(IBlockAccess world, BlockPos pos, EnumFacing face) {
		TileEntity te = world.getTileEntity(pos);
        if (te != null && te instanceof IPipe) {
            IPipe.Cover cover = ((IPipe)te).getCover();
            if (cover != null) {
            	return cover.block.getBlock().isOpaqueCube();
            }
        }
        return false;
	}

	@Override
    public float getBlockHardness(World world, BlockPos pos) 
    {
        float h = this.blockHardness;
        TileEntity te = world.getTileEntity(pos);
        if (te != null && te instanceof IPipe) {
            IPipe.Cover cover = ((IPipe)te).getCover();
            if (cover != null) {
                Block block = cover.block.getBlock();
                if (block != null) {
                    float h1 = block.getBlockHardness(world, new BlockPos(0, -1, 0));
                	if (h1 < 0) h = -1;
                    else h += h1;
                }
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
	public float getAmbientOcclusionLightValue() {
		return 1.0F;
	}

	@Override
	public int getLightOpacity(IBlockAccess world, BlockPos pos) 
	{
		return this.isNormalCube(world, pos) ? 255 : 0;
	}
    
}
