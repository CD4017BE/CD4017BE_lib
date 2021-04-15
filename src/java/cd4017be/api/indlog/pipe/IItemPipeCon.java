package cd4017be.api.indlog.pipe;

import net.minecraft.util.Direction;

/**
 * indicates that ItemPipes should automatically connect to the TileEntity implementing this
 * @author CD4017BE
 */
public interface IItemPipeCon {

	/**
	 * @param s side to connect from
	 * @return preferred transfer direction: 0 = none, 1 = in, 2 = out, 3 = both
	 */
	public byte getItemConnectDir(Direction s);

}
