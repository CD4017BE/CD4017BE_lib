package cd4017be.lib.templates;

import cd4017be.api.IAbstractTile;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

/**
 * @author CD4017BE
 * @param <C> should be the class extending this, so that '(C)this' won't throw a ClassCastException.
 * @param <N> the implementation of SharedNetwork this should operate with.
 */
@SuppressWarnings("unchecked")
public abstract class MultiblockComp<C extends MultiblockComp<C, N>, N extends SharedNetwork<C, N>> {

	public N network;
	public final IAbstractTile tile;
	/** the unique identifier of this component. This should be calculated from it's coordinates and never change within one instance. */
	protected long uid;
	public byte con = 0x3f;

	public MultiblockComp(IAbstractTile tile) {
		this.tile = tile;
	}

	public long getUID() {
		return uid;
	}
	
	public void setUID(long uid) {
		if (this.uid != 0) return;
		this.uid = uid;
		if (network != null) {
			network.components.remove(0L);
			network.components.put(uid, (C)this);
		}
	}

	/**
	 * @param side usually EnumFacing index
	 * @return true if it can connect to given side
	 */
	public boolean canConnect(byte side) {
		return (con >> side & 1) != 0;
	}

	public void setConnect(byte side, boolean c) {
		if (!c && canConnect(side)) network.update = true;//network.onDisconnect((C)this, side);
		if (c) con |= 1 << side;
		else con &= ~(1 << side);
	}

	/**
	 * @param side usually EnumFacing index
	 * @return the neighbor component at given side or null if none
	 */
	public C getNeighbor(byte side) {
		ICapabilityProvider te = tile.getTileOnSide(EnumFacing.VALUES[side]);
		if (te == null) return null;
		C comp = te.getCapability(getCap(), EnumFacing.VALUES[side^1]);
		return comp != null && comp.canConnect((byte)(side^1)) ? comp : null;
	}

	public abstract Capability<C> getCap();

}
