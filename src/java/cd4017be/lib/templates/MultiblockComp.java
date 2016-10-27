package cd4017be.lib.templates;

import cd4017be.api.IAbstractTile;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

/**
 * 
 * @param <C> should be the class extending this, so that {@code (C)this} won't throw a ClassCastException.
 * @param <N> the implementation of {@link SharedNetwork} this should operate with.
 * @author CD4017BE
 */
@SuppressWarnings("unchecked")
public abstract class MultiblockComp<C extends MultiblockComp<C, N>, N extends SharedNetwork<C, N>> {

	public N network;
	public final IAbstractTile tile;
	/** the unique identifier of this component. This should be calculated from it's coordinates and never change within one instance. */
	protected long uid;
	public byte con = 0x3f;
	public boolean updateCon = true;

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

	/**
	 * @param side usually EnumFacing index
	 * @param c whether to connect there
	 */
	public void setConnect(byte side, boolean c) {
		boolean c0 = canConnect(side);
		if (!c && c0) {
			network.onDisconnect((C)this, side);
			con &= ~(1 << side);
		} else if (c && !c0) {
			updateCon = true;
			con |= 1 << side;
		}
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

	/** @return forge capability of this component */
	public abstract Capability<C> getCap();

}
