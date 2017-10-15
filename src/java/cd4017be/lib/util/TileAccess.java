package cd4017be.lib.util;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

/**
 * 
 * @author CD4017BE
 */
public class TileAccess {

	public final TileEntity te;
	public final EnumFacing side;

	public TileAccess(TileEntity te, EnumFacing side) {this.te = te; this.side = side;}

}
