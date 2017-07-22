package cd4017be.lib.block;

import cd4017be.lib.util.Orientation;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;

public class BaseTileEntity extends TileEntity {

	private IBlockState blockState;

	public BaseTileEntity() {}

	public BaseTileEntity(IBlockState state) {
		blockState = state;
	}

	public IBlockState getBlockState() {
		if (blockState == null) {
			blockState = world.getBlockState(pos);
			blockType = blockState.getBlock();
		}	
		return blockState;
	}

	public Orientation getOrientation() {
		getBlockState();
		if (blockType instanceof OrientedBlock)
			return blockState.getValue(((OrientedBlock)blockType).orientProp);
		else return Orientation.N;
	}

	@Override
	public NBTTagCompound getUpdateTag() {
		return this.writeToNBT(new NBTTagCompound());
	}

	/**
	 * Fire render(client) / data(server) update
	 */
	public void markUpdate() {
		getBlockState();
		world.notifyBlockUpdate(pos, blockState, blockState, 3);
	}

	@Override //just skip all the ugly hard-coding in superclass
	public AxisAlignedBB getRenderBoundingBox() {
		return new AxisAlignedBB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
	}

}
