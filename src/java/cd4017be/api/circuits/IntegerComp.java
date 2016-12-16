package cd4017be.api.circuits;

import cd4017be.api.Capabilities;
import cd4017be.api.IAbstractTile;
import cd4017be.lib.templates.MultiblockComp;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;

public class IntegerComp extends MultiblockComp<IntegerComp, SharedInteger> {

	public IntegerComp(IAbstractTile tile) {
		super(tile);
	}

	public int inputState;
	/** bits[0-13 8*(1+1)]: side*(in+out) */
	public short rsIO;

	public void onStateChange() {
		World world = ((TileEntity)tile).getWorld();
		BlockPos pos = ((TileEntity)tile).getPos();
		for (int i = 0; i < 6; i++)
			if ((rsIO >> (i * 2) & 2) != 0)
				world.notifyBlockOfStateChange(pos.offset(EnumFacing.VALUES[i]), Blocks.REDSTONE_TORCH);
	}

	public void updateInput() {
		World world = ((TileEntity)tile).getWorld();
		BlockPos pos = ((TileEntity)tile).getPos();
		int newIn = 0;
		for (byte i = 0; i < 6; i++) 
				if ((rsIO >> (i * 2) & 1) != 0) {
					EnumFacing s = EnumFacing.VALUES[i];
					newIn |= world.getRedstonePower(pos.offset(s), s);
				}
		if (newIn != inputState) {
			inputState = newIn;
			if (!network.updateState) {
				network.updateState = true;
				network.updatePhysics();
			}
		}
	}

	public void readFromNBT(NBTTagCompound nbt) {
		con = nbt.getByte("con");
		network.setIO(this, nbt.getShort("io"));
		inputState = nbt.getInteger("state");
	}

	public void writeToNBT(NBTTagCompound nbt) {
		nbt.setByte("con", con);
		nbt.setShort("io", rsIO);
		nbt.setInteger("state", inputState); //inputState is saved to ensure blocks don't get incomplete redstone states after chunkload.
	}

	@Override
	public Capability<IntegerComp> getCap() {
		return Capabilities.RS_INTEGER_CAPABILITY;
	}

}
