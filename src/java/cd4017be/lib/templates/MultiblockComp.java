package cd4017be.lib.templates;

import cd4017be.api.IAbstractTile;
import cd4017be.lib.util.ICachableInstance;
import net.minecraft.tileentity.TileEntity;
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
public abstract class MultiblockComp<C extends MultiblockComp<C, N>, N extends SharedNetwork<C, N>> implements ICachableInstance {

	public N network;
	public final IAbstractTile tile;
	/** the unique identifier of this component. This should be calculated from it's coordinates and never change within one instance. */
	protected long uid;
	public byte con = 0x3f;
	public boolean updateCon;

	public MultiblockComp(IAbstractTile tile) {
		this.tile = tile;
	}

	public long getUID() {
		return uid;
	}
	
	public void setUID(long uid) {
		if (this.uid == uid) return;
		if (network != null) {
			C c = network.components.remove(this.uid);
			if (c != this)
				throw new IllegalStateException(String.format("Changing uid of %s into %d, but network had the wrong component %s stored under old uid", this, uid, c));
			network.components.put(uid, (C)this);
		}
		this.uid = uid;
		updateCon = true;
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

	public boolean invalid() {
		return network == null || tile.invalid();
	}

	@Override
	public String toString() {
		return "MultiblockComp [network=" + (network == null ? "none" : network.components.size()) + ", tile=" + (tile.invalid() ? "invalid " : "") + (tile instanceof TileEntity ? ((TileEntity)tile).getPos() : tile) + ", uid=" + uid + ", con=" + con
				+ ", update=" + updateCon + "]";
	}

}
