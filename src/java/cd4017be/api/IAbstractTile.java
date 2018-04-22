package cd4017be.api;

import javax.annotation.Nullable;

import cd4017be.lib.util.ICachableInstance;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

/**
 * 
 * @author CD4017BE
 */
public interface IAbstractTile extends ICapabilityProvider, ICachableInstance {
	/** 
	 * @param s side
	 * @return the neighbor 'TileEntity' on that side. Could theoretically be anything.
	 */
	public @Nullable ICapabilityProvider getTileOnSide(@Nullable EnumFacing s);

	/**@return whether this is a client side instance */
	public boolean isClient();
}
