package cd4017be.lib.objectNetworks;

import java.util.List;

import cd4017be.lib.util.Obj2;

public interface IComponent {

	/**
	 * @param network the network this component should be bound to
	 */
	public <C extends IComponent, P extends IPhysics<C>> void setNetwork(SharedNetwork<C, P> network);
	/**
	 * @return the network this component is bound to
	 */
	public <C extends IComponent, P extends IPhysics<C>> SharedNetwork<C, P> getNetwork();
	/**
	 * @return the unique identifier of this component. This should be calculated from it's coordinates and never change within one instance.
	 */
	public long getUID();
	/**
	 * @param side the side attempted to connect
	 * @return true if it can connect to it
	 */
	public boolean canConnect(byte side);
	/**
	 * @return a list of valid connections. Long objA should be the unique identifier of the connected component returned by getUID(). Byte objB should be the side attempted to connect on the neighbor component and will be also used in calling canConnect().
	 */
	public List<Obj2<Long, Byte>> getConnections();
	
}
