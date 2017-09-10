package cd4017be.lib.block;

import cd4017be.lib.property.PropertyOrientation;
import cd4017be.lib.util.Orientation;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public abstract class OrientedBlock extends AdvancedBlock {

	public PropertyOrientation orientProp;

	protected OrientedBlock(String id, Material m, SoundType sound, int flags, Class<? extends TileEntity> tile, PropertyOrientation prop) {
		super(id, m, sound, flags, tile);
	}

	public static OrientedBlock create(String id, Material m, SoundType sound, int flags, Class<? extends TileEntity> tile, PropertyOrientation prop) {
		return new OrientedBlock(id, m, sound, flags, tile, prop) {
			@Override
			protected BlockStateContainer createBlockState() {
				orientProp = prop;
				return new BlockStateContainer(this, prop);
			}
		};
	}

	@Override
	public IBlockState getStateFromMeta(int meta) {
		return this.blockState.getBaseState().withProperty(orientProp, Orientation.values()[meta]);
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		return state.getValue(orientProp).ordinal();
	}

	@Override
	public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing s, float X, float Y, float Z, int m, EntityLivingBase placer, EnumHand hand) {
		int p = placer.rotationPitch > 45 ? 1 : placer.rotationPitch < -35 ? -1 : 0;
		int y = MathHelper.floor((double)(placer.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
		return blockState.getBaseState().withProperty(orientProp, orientProp.getPlacementState(placer.isSneaking(), y, p, s, X, Y, Z));
	}

	@Override
	public OrientedBlock setBlockBounds(AxisAlignedBB box) {
		boundingBox = new AxisAlignedBB[16];
		for (Orientation o : orientProp.getAllowedValues())
			boundingBox[o.ordinal()] = o.rotate(box);
		return this;
	}

	@Override
	public boolean rotateBlock(World world, BlockPos pos, EnumFacing axis) {
		Orientation o = world.getBlockState(pos).getValue(orientProp);
		Orientation no = orientProp.getRotatedState(o, axis);
		if (no == o) return false;
		world.setBlockState(pos, getDefaultState().withProperty(orientProp, no));
		return true;
	}

	@Override
	public EnumFacing[] getValidRotations(World world, BlockPos pos) {
		return orientProp.rotations();
	}

	@Override
	public IBlockState withRotation(IBlockState state, Rotation rot) {
		if (rot == Rotation.NONE) return state;
		int i = state.getValue(orientProp).ordinal();
		Orientation o = Orientation.values()[i & 12 | rot.rotate(i & 3, 4)];
		return orientProp.getAllowedValues().contains(o) ? state.withProperty(orientProp, o) : state;
	}

	@Override
	public IBlockState withMirror(IBlockState state, Mirror mirror) {
		if (mirror == Mirror.NONE) return state;
		int i = state.getValue(orientProp).ordinal();
		Orientation o = Orientation.values()[i & 12 | mirror.mirrorRotation(i & 3, 4)];
		return orientProp.getAllowedValues().contains(o) ? state.withProperty(orientProp, o) : state;
	}

}
