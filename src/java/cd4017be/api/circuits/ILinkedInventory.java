package cd4017be.api.circuits;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

/**
 * 
 * @author CD4017BE
 */
public interface ILinkedInventory {
	public BlockPos getLinkPos();
	public EnumFacing getLinkDir();
	public TileEntity getLinkObj();
}
