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
	/** bits[0-11 6*2]: sideCfg, bit[12,13]: internalIO, bit[14,15]: totalIO */
	public short con;

	@Override
	public boolean canConnect(byte side) {
		return (con >> (side * 2) & 3) == 0;
	}

	public void onStateChange() {
		World world = ((TileEntity)tile).getWorld();
		BlockPos pos = ((TileEntity)tile).getPos();
		for (int i = 0; i < 6; i++)
			if ((con >> (i * 2) & 3) == 2)
				world.notifyBlockOfStateChange(pos.offset(EnumFacing.VALUES[i]), Blocks.REDSTONE_TORCH);
	}

	public void updateInput() {
		World world = ((TileEntity)tile).getWorld();
		BlockPos pos = ((TileEntity)tile).getPos();
		int newIn = 0;
		for (byte i = 0; i < 6; i++) 
				if ((con >> (i * 2) & 3) == 1) {
					EnumFacing s = EnumFacing.VALUES[i];
					newIn |= world.getRedstonePower(pos.offset(s), s);
				}
		if (newIn != inputState) {
			inputState = newIn;
			network.updateState = true;
		}
	}
	
	public void readFromNBT(NBTTagCompound nbt) {
		con = nbt.getShort("con");
		inputState = nbt.getInteger("state");
	}
	
	public void writeToNBT(NBTTagCompound nbt) {
		nbt.setShort("con", con);
		nbt.setInteger("state", inputState); //inputState is saved to ensure blocks don't get incomplete redstone states after chunkload.
	}

	@Override
	public Capability<IntegerComp> getCap() {
		return Capabilities.RS_INTEGER_CAPABILITY;
	}

}
