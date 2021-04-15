package cd4017be.lib.templates;

import cd4017be.lib.TickRegistry;
import cd4017be.lib.TickRegistry.IUpdatable;
import cd4017be.lib.util.ICachableInstance;
import cd4017be.lib.util.Utils;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;

/**
 * 
 * @param <C> should be the class extending this, so that {@code (C)this} won't throw a ClassCastException.
 * @param <N> the type of {@link SharedNetwork} this operates with.
 * @param <T> the type of {@link IAbstractTile} (usually a TileEntity) providing instances of this
 * @author CD4017BE
 * @deprecated not fully implemented
 */
@SuppressWarnings("unchecked")
public abstract class NetworkNode<C extends NetworkNode<C, N, T>, N extends SharedNetwork<C, N>, T extends TileEntity> implements ICachableInstance {

	public N network;
	public final T tile;
	/** the unique identifier of this component. This should be calculated from it's coordinates and never change within one instance. */
	protected long uid;
	public byte con = 0x3f;
	public boolean updateCon;

	public NetworkNode(T tile) {
		this.tile = tile;
	}

	public long getUID() {
		return uid;
	}

	public void setUID(long uid) {
		markDirty();
		if (this.uid == uid) return;
		if (network != null) {
			C c = network.components.remove(this.uid);
			if (c != this)
				throw new IllegalStateException(String.format("Changing uid of %s into %d, but network had the wrong component %s stored under old uid", this, uid, c));
			network.components.put(uid, (C)this);
		}
		this.uid = uid;
	}

	/**
	 * call this on block updates or similar events to make this component auto connect with surrounding structures
	 */
	public void updateCons() {
		if (network == null) return;
		C obj;
		for (byte i : network.sides())
			if (canConnect(i) && (obj = getNeighbor(i)) != null)
				network.add(obj);
		updateCon = false;
	}

	public void markDirty() {
		if (!updateCon && tile instanceof IUpdatable && tile.hasWorld() && !tile.getWorld().isRemote) TickRegistry.instance.updates.add((IUpdatable)tile);
		updateCon = true;
	}

	/**
	 * @param side usually Direction index
	 * @return true if it can connect to given side
	 */
	public boolean canConnect(byte side) {
		return (con >> side & 1) != 0;
	}

	/**
	 * @param side usually Direction index
	 * @param c whether to connect there
	 */
	public void setConnect(byte side, boolean c) {
		boolean c0 = canConnect(side);
		if (!c && c0) {
			network.onDisconnect((C)this, side);
			con &= ~(1 << side);
		} else if (c && !c0) {
			markDirty();
			con |= 1 << side;
		}
	}

	/**
	 * @param side usually Direction index
	 * @return the neighbor component at given side or null if none
	 */
	public C getNeighbor(byte side) {
		C comp = Utils.neighborCapability(tile, Direction.values()[side], getCap());
		return comp != null && comp.canConnect((byte)(side^1)) ? comp : null;
	}

	/** @return forge capability of this component */
	public abstract Capability<C> getCap();

	public boolean invalid() {
		return network == null || tile.isRemoved();
	}

	@Override
	public String toString() {
		return "MultiblockComp [network=" + (network == null ? "none" : network.components.size()) + ", tile=" + (tile.isRemoved() ? "invalid " : "") + (tile instanceof TileEntity ? ((TileEntity)tile).getPos() : tile) + ", uid=" + uid + ", con=" + con
				+ ", update=" + updateCon + "]";
	}

}
