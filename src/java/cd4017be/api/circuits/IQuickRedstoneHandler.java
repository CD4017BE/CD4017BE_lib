package cd4017be.api.circuits;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

/**
 * TileEntities can implement this to improve performance by receiving redstone signals directly without block updates.
 * @author CD4017BE
 */
public interface IQuickRedstoneHandler {

	/**
	 * Can be called instead of a block update by another TileEntity to notify a redstone state change.
	 * @param side the side of this Block receiving the signal
	 * @param value the new state
	 * @param src the TileEntity calling this (may be null)
	 */
	public void onRedstoneStateChange(EnumFacing side, int value, TileEntity src);

}
