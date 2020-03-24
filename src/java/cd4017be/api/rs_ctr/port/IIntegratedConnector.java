package cd4017be.api.rs_ctr.port;

import cd4017be.api.rs_ctr.wire.WiredConnector;
import net.minecraft.entity.player.EntityPlayer;

/**
 * A connector for a MountedPort that contains an integrated logic layer between its port and any wire that connects to it.
 * @author CD4017BE */
public interface IIntegratedConnector extends IPortProvider {

	/**@param link the WiredConnector where the wire is coming from
	 * @return the internal port a wire actually connects to */
	WiredConnector getLinkedWith(WiredConnector link);

	boolean addWire(WiredConnector con, EntityPlayer player, boolean sim);

	/**
	 * @param wiredConnector
	 */
	void removeWire(WiredConnector con, EntityPlayer player);

	/**
	 * @param wiredConnector
	 * @return
	 */
	Port getPort(WiredConnector con);
}
