package cd4017be.lib.block;

import cd4017be.lib.util.Orientation;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public abstract class OrientedBlock extends AdvancedBlock {

	public final IProperty<Orientation> orientProp;

	protected OrientedBlock(String id, Material m, SoundType sound, int flags, Class<? extends TileEntity> tile, IProperty<Orientation> prop) {
		super(id, m, sound, flags, tile);
		orientProp = prop;
	}

	public static OrientedBlock create(String id, Material m, SoundType sound, int flags, Class<? extends TileEntity> tile, IProperty<Orientation> prop) {
		return new OrientedBlock(id, m, sound, flags, tile, prop) {
			@Override
			protected BlockStateContainer createBlockState() {
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
		Orientation dir = null;
		if (placer.isSneaking()) {
			if (s.getAxis().isVertical()) {
				int i = Z + X > 1F ? (Z > X ? 1 : 3) : (Z < X ? 0 : 2);
				for (Orientation o : orientProp.getAllowedValues()) {
					int j = o.ordinal();
					if ((j & 3) == i) {
						dir = o;
						if (o.front == s.getOpposite()) break;
					} else if (dir == null && o.front == s.getOpposite()) dir = o;
				}
			} else {
				int i = s.getHorizontalIndex();
				for (Orientation o : orientProp.getAllowedValues()) {
					int j = o.ordinal();
					if ((j & 3) == i) {
						if ((j & 4) == 0) {
							dir = o;
							break;
						} else if (Y < 0.5F && o.front == EnumFacing.DOWN) {
							dir = o;
							if (Y < 0.25F) break;
						} else if (Y >= 0.5F && o.front == EnumFacing.UP) {
							dir = o;
							if (Y >= 0.75F) break;
						} else if (dir == null) dir = o;
					}
				}
			}
		} else {
			int h = placer.rotationPitch > 40 ? 3 : placer.rotationPitch < -35 ? 1 : 0;
			int i = MathHelper.floor((double)(placer.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
			for (Orientation o : orientProp.getAllowedValues()) {
				int j = o.ordinal();
				if ((j & 3) == i) {
					dir = o;
					if (j >> 2 == h || h == 0 && (j & 12) == 8) break;
				} else if (dir == null && (j >> 2 == h || h == 0 && (j & 12) == 8)) {
					dir = o;
				}
			}
		}
		return dir == null ? blockState.getBaseState() : blockState.getBaseState().withProperty(orientProp, dir);
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
		// TODO Auto-generated method stub
		return super.rotateBlock(world, pos, axis);
	}

	@Override
	public EnumFacing[] getValidRotations(World world, BlockPos pos) {
		// TODO Auto-generated method stub
		return super.getValidRotations(world, pos);
	}

}
