package cd4017be.lib.objectNetworks;

import java.util.HashMap;

public interface IPhysics<C extends IComponent> {
	/**
	 * called on network initialization
	 * @param network the network this is bound to
	 */
	public void setNetwork(SharedNetwork<C, ? extends IPhysics<C>> network);
	/**
	 * called when the SharedNetwork is merged with another one. 
	 * @param network the other network
	 */
	public void onMerged(SharedNetwork<C, ? extends IPhysics<C>> network);
	/**
	 * when the SharedNetwork gets split this will be called for each recreated sub network
	 * @param comps the components contained in that sub network
	 * @return the new IPhysics instance to be used for the new network
	 */
	public <P extends IPhysics<C>> P onSplit(HashMap<Long, C> comps);
	/**
	 * called when a component is removed (broken, chunk unload, etc.)
	 * @param comp the component removed
	 */
	public void onRemove(C comp);
	/**
	 * called every tick
	 */
	public void updateTick();
}
