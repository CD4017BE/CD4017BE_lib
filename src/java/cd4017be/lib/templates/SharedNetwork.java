package cd4017be.lib.templates;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import net.minecraft.util.BlockPos;
import cd4017be.lib.util.Obj2;

/**
 * 
 * @author CD4017BE
 * @param <C> This is the type of components to use in this SharedNetwork.
 * @param <N> This should be the class implementing this.
 */
@SuppressWarnings("unchecked")
public abstract class SharedNetwork<C extends IComponent<C, N>, N extends SharedNetwork<C, N>> { 
	
	protected C core;
	public final HashMap<Long, C> components;
	protected boolean update = false;
	/**
	 * creates a single component network out of the given component
	 * @param core
	 */
	public SharedNetwork(C core) {
		this.components = new HashMap<Long, C>();
		this.components.put(core.getUID(), core);
		this.core = core;
		this.core.setNetwork((N)this);
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
		for (C c : network.components.values()) c.setNetwork((N)this);
		components.putAll(network.components);
	}
	
	/**
	 * adds the component to this network and merges both networks together
	 * @param comp
	 */
	public void add(C comp) {
		N network = comp.getNetwork();
		if (network == this) return;
		if (components.size() >= network.components.size()) {
			onMerged(network);
		} else {
			network.onMerged((N)this);
			for (C c : components.values()) c.setNetwork(network);
			network.components.putAll(components);
		}
	}
	
	/**
	 * called when a component is removed (broken, chunk unload, etc.)
	 * @param comp the component removed
	 */
	public void remove(C comp) {
		components.remove(comp.getUID());
		if (core == comp) core = null;
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
		if (obj != null && obj.canConnect(side)) update = true;
	}
	
	/**
	 * should be called by each component every tick
	 * @param comp
	 */
	public void updateTick(C comp) {
		if (core == null) core = comp;
		else if (comp != core) return;
		updatePhysics();
		if (update) {
			this.reassembleNetwork();
			update = false;
		}
	}
	
	/**
	 * called every tick
	 */
	protected void updatePhysics() {}

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
				for (Obj2<Long, Byte> e : (List<Obj2<Long, Byte>>)obj.getConnections()) 
					if ((obj1 = components.get(e.objA)) != null && !comps.containsKey(e.objA) && obj1.canConnect(e.objB))
						queue.add(obj1);
			}
			if (comps.size() == components.size()) return;
			N network = onSplit(comps);
			for (Entry<Long, C> e : comps.entrySet()) {
				e.getValue().setNetwork(network);
				components.remove(e.getKey());
			}
			for (C c : components.values()) { core = c; break; }
		}
	}
	
	private static final int spreader = 549568949; //just a random big number to create chaotic values
	public static long ExtPosUID(BlockPos pos, int dimId) {
		dimId *= spreader;
		return pos.toLong() ^ (long)dimId << 32;
	}
	public static long SidedPosUID(long base, int side) {
		return base ^ (long)(side * spreader);
	}
	
}
