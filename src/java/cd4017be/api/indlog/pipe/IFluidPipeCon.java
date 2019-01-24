package cd4017be.api.indlog.pipe;

import net.minecraft.util.EnumFacing;

/**
 * indicates that FluidPipes should automatically connect to the TileEntity implementing this
 * @author CD4017BE
 */
public interface IFluidPipeCon {

	/**
	 * @param s side to connect from
	 * @return preferred transfer direction: 0 = none, 1 = in, 2 = out, 3 = both
	 */
	public byte getFluidConnectDir(EnumFacing s);

}
