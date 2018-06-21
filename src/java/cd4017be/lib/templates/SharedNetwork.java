package cd4017be.lib.templates;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import cd4017be.lib.TickRegistry;
import cd4017be.lib.TickRegistry.IUpdatable;
import net.minecraft.util.math.BlockPos;

/**
 * Class template to share functionality across multi-block structures
 * @param <C> the type of components to use in this SharedNetwork.
 * @param <N> should be the class extending this, so that '(N)this' won't throw a ClassCastException.
 * @author CD4017BE
 */
@SuppressWarnings("unchecked")
public abstract class SharedNetwork<C extends NetworkNode<C, N, ?>, N extends SharedNetwork<C, N>> implements IUpdatable { 

	protected C core;
	public final HashMap<Long, C> components;
	public boolean update = false;

	/**
	 * creates a single component network out of the given component
	 * @param core
	 */
	public SharedNetwork(C core) {
		this.components = new HashMap<Long, C>();
		this.components.put(core.uid, core);
		this.core = core;
		core.network = (N)this;
	}

	protected SharedNetwork(HashMap<Long, C> comps) {
		components = comps;
	}

	/**
	 * when the SharedNetwork gets split this will be called for each recreated sub network
	 * @param comps the components contained in that sub network
	 * @return the new SharedNetwork instance
	 */
	public abstract N onSplit(HashMap<Long, C> comps);

	/**
	 * called when the SharedNetwork is merged with another one. 
	 * @param network the other network
	 */
	public void onMerged(N network) {
		for (C c : network.components.values()) c.network = (N)this;
		components.putAll(network.components);
		network.components.clear();
	}

	/**
	 * adds the component to this network and merges both networks together
	 * @param comp
	 */
	public void add(C comp) {
		if (comp.network == this || comp.invalid() || comp.tile.isClient()) return;
		if (components.size() >= comp.network.components.size()) onMerged(comp.network);
		else comp.network.onMerged((N)this);
	}

	/**
	 * called when a component is removed (broken, chunk unload, etc.)
	 * @param comp the component removed
	 */
	public void remove(C comp) {
		components.remove(comp.uid);
		comp.network = null;
		if (core == comp) core = null;
		if (!components.isEmpty()) markDirty();
	}

	/**
	 * removes the connection between two components.
	 * @param comp the component that disconnected
	 * @param side the side that disconnected
	 */
	public void onDisconnect(C comp, byte side) {
		if (comp.getNeighbor(side) != null && !comp.tile.isClient()) markDirty();
	}

	/**
	 * called to check if there are new neighboring blocks to connect with
	 * @param comp
	 * @deprecated use {@link NetworkNode#updateCons()} instead
	 */
	@Deprecated
	public void updateCompCon(C comp) {
		C obj;
		for (byte i : sides())
			if (comp.canConnect(i) && (obj = comp.getNeighbor(i)) != null)
				comp.network.add(obj);
		comp.updateCon = false;
	}

	/**
	 * @deprecated not needed anymore: network will update itself now
	 */
	@Deprecated
	public void updateTick(C comp) {
		if (comp.updateCon) updateCompCon(comp);
		if (core == null) core = comp;
		else if (comp != core) return;
		updatePhysics();
		if (update) {
			this.reassembleNetwork();
			update = false;
		}
	}

	@Override
	public void process() {
		if (update) {
			if (core == null)
				for (C c : components.values()) { core = c; break; }
			reassembleNetwork();
			update = false;
		}
	}

	public void markDirty() {
		if (!update) {
			update = true;
			TickRegistry.instance.updates.add(this);
		}
	}

	/**
	 * @deprecated use your own tick handling or modify {@link #process()}
	 */
	@Deprecated
	protected void updatePhysics() {}

	protected byte[] sides() {return defaultSides;}

	protected void reassembleNetwork() {
		ArrayList<C> queue = new ArrayList<C>();
		C obj, obj1;
		while (this.components.size() > 1) {
			HashMap<Long, C> comps = new HashMap<Long, C>();
			queue.clear();
			queue.add(core);
			while (!queue.isEmpty()) {
				obj = queue.remove(queue.size() - 1);
				if (comps.put(obj.uid, obj) == null)
					for (byte i : sides())
						if (obj.canConnect(i) && (obj1 = obj.getNeighbor(i)) != null && components.containsKey(obj1.uid))
							queue.add(obj1);
			}
			if (comps.size() == components.size()) return;
			N network = onSplit(comps);
			for (Entry<Long, C> e : comps.entrySet()) {
				e.getValue().network = network;
				components.remove(e.getKey());
			}
			for (C c : components.values()) { core = c; break; }
		}
	}

	private static final byte[] defaultSides = {0, 1, 2, 3, 4, 5};
	/** just a random big number to create chaotic values */
	private static final int spreader = 549568949;
	public static long ExtPosUID(BlockPos pos, int dimId) {
		dimId *= spreader;
		return pos.toLong() ^ (long)dimId << 32;
	}
	public static long SidedPosUID(long base, int side) {
		return base ^ (long)(side * spreader);
	}

}
