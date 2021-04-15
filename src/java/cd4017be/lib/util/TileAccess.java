package cd4017be.lib.util;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;

/**
 * 
 * @author CD4017BE
 */
public class TileAccess {

	public final TileEntity te;
	public final Direction side;

	public TileAccess(TileEntity te, Direction side) {this.te = te; this.side = side;}

}
