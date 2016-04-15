package cd4017be.lib.templates;

import java.util.List;

import cd4017be.lib.util.Obj2;

/**
 * This represents a component of a SharedNetwork.
 * @author CD4017BE
 * @param <C> This should be the class implementing this.
 * @param <N> The implementation of SharedNetwork this should operate with.
 */
public interface IComponent<C extends IComponent<C, N>, N extends SharedNetwork<C, N>> {

	/**
	 * @param network the network this component should be bound to
	 */
	public void setNetwork(N network);
	/**
	 * @return the network this component is bound to
	 */
	public N getNetwork();
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
