package cd4017be.lib.objectNetworks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import net.minecraft.util.BlockPos;
import cd4017be.lib.util.Obj2;

public class SharedNetwork<C extends IComponent, P extends IPhysics<C>> { 
	
	protected C core;
	public final HashMap<Long, C> components;
	public final P physics;
	protected boolean update = false;
	/**
	 * creates a single component network out of the given component and physics
	 * @param physics
	 * @param core
	 */
	public SharedNetwork(P physics, C core) {
		this.components = new HashMap<Long, C>();
		this.components.put(core.getUID(), core);
		this.physics = physics;
		this.physics.setNetwork(this);
		this.core = core;
		this.core.setNetwork(this);
	}
	
	protected SharedNetwork(P oldPhysics, HashMap<Long, C> comps) {
		physics = oldPhysics.onSplit(comps);
		components = comps;
		physics.setNetwork(this);
	}
	/**
	 * adds the component to this network and merges both networks together
	 * @param comp
	 */
	public void add(C comp) {
		SharedNetwork<C, P> network = comp.getNetwork();
		if (components.size() >= network.components.size()) {
			physics.onMerged(network);
			for (C c : network.components.values()) c.setNetwork(this);
			components.putAll(network.components);
		} else {
			network.physics.onMerged(this);
			for (C c : components.values()) c.setNetwork(network);
			network.components.putAll(components);
		}
	}
	/**
	 * removes a component from this network. This method should be called on block removal and chunk unload.
	 * @param comp
	 */
	public void remove(C comp) {
		physics.onRemove(comp);
		components.remove(comp.getUID());
		update = true;
	}
	/**
	 * removes the connection between two components.
	 * @param comp the component that disconnected
	 * @param side the side that disconnected
	 * @param neighbor the UID of the neighbor to disconnect
	 */
	public void onDisconnect(C comp, byte side, long neighbor) {
		C obj = components.get(neighbor);
		if (obj == null) return;
		for (Obj2<Long, Byte> e : obj.getConnections()) {
			if (e.objB != side && components.containsKey(e.objA)) {
				update = true;
				return;
			}
		}
		HashMap<Long, C> comps = new HashMap<Long, C>();
		components.remove(obj.getUID());
		comps.put(obj.getUID(), obj);
		SharedNetwork<C, P> network = new SharedNetwork<C, P>(physics, comps);
		obj.setNetwork(network);
	}
	/**
	 * should be called by each component every tick
	 * @param comp
	 */
	public void updateTick(C comp) {
		if (core == null) core = comp;
		else if (comp != core) return;
		physics.updateTick();
		if (update) {
			this.reassembleNetwork();
			update = false;
		}
	}

	private void reassembleNetwork() {
		ArrayList<C> queue = new ArrayList<C>();
		C obj, obj1;
		while (this.components.size() > 1) {
			HashMap<Long, C> comps = new HashMap<Long, C>();
			queue.clear();
			queue.add(core);
			while (!queue.isEmpty()) {
				obj = queue.remove(queue.size() - 1);
				comps.put(obj.getUID(), obj);
				for (Obj2<Long, Byte> e : obj.getConnections()) 
					if ((obj1 = components.get(e.objA)) != null && !comps.containsKey(e.objA) && obj1.canConnect(e.objB))
						queue.add(obj1);
			}
			if (comps.size() == components.size()) return;
			SharedNetwork<C, P> network = new SharedNetwork<C, P>(physics, comps);
			for (Entry<Long, C> e : comps.entrySet()) {
				e.getValue().setNetwork(network);
				components.remove(e.getKey());
			}
			for (C c : components.values()) { core = c; break; }
		}
	}
	
	private static final int spreader = 549568949; //just a random big number to create chaotic values
	public static long ExtPosUID(BlockPos pos, int dimId, int side) {
		dimId *= spreader;
		side *= spreader;
		return pos.toLong() ^ (long)dimId ^ (long)side << 32;
	}
	
}
