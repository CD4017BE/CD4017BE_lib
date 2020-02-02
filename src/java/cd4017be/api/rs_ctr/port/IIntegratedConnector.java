package cd4017be.api.rs_ctr.port;

import cd4017be.api.rs_ctr.wire.IWiredConnector;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.Vec3d;

/**
 * A connector for a MountedPort that contains an integrated logic layer between its port and any wire that connects to it.
 * @author CD4017BE */
public interface IIntegratedConnector extends IWiredConnector, IPortProvider {

	/**@param link the MountedPort where the wire is coming from
	 * @return the internal port a wire actually connects to */
	Port getLinkedWith(MountedPort link);

	@Override
	default boolean isLinked(MountedPort to) {
		return getLinkedWith(to) != null;
	}

	boolean addLink(MountedPort link, Vec3d path, EntityPlayer player);
}
