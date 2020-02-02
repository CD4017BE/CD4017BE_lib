package cd4017be.api.rs_ctr.wire;

import cd4017be.api.rs_ctr.port.IConnector;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.api.rs_ctr.port.Port;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;

/**
 * A connector that can be attached to wire anchors and connects between blocks in the same world.
 * @see IConnector
 * @author cd4017be
 */
public interface IWiredConnector extends IConnector {

	/**
	 * @param from the port this is attached on
	 * @return the port this is connected to
	 */
	Port getLinkPort(MountedPort from);

	/**
	 * @param to the port to test
	 * @return whether this is actually connected to given port
	 */
	boolean isLinked(MountedPort to);

	/**
	 * @param type the interaction callback class
	 * @return whether this connector supports the given interaction type
	 */
	boolean isCompatible(Class<?> type);

	/**
	 * when a wire from link connecting to host is removed (by a player).
	 * @param host the port this is attached on
	 * @param link the port the removed wire is attached on
	 * @param player the responsible actor
	 */
	default void onWireRemoved(MountedPort host, MountedPort link, EntityPlayer player) {
		if (isLinked(link))
			host.setConnector(null, player);
	}

	@Override
	default void onRemoved(MountedPort port, EntityPlayer player) {
		Port p = getLinkPort(port);
		if (p instanceof MountedPort) {
			IConnector c = ((MountedPort)p).getConnector();
			if (c instanceof IWiredConnector)
				((IWiredConnector)c).onWireRemoved(((MountedPort)p), port, player);
		}
	}

	/**
	 * when the linked port changes position/orientation.
	 * @param link the port this is connected to via wire
	 */
	default void onLinkMove(MountedPort host, MountedPort link) {}

	@Override
	default void onPortMove(MountedPort port) {
		Port p = getLinkPort(port);
		if (!(p instanceof MountedPort)) return;
		MountedPort lp = (MountedPort)p;
		onLinkMove(port, lp);
		IConnector c = lp.getConnector();
		if (c instanceof IWiredConnector)
			((IWiredConnector)c).onLinkMove(lp, port);
	}

	/**
	 * implemented by {@link Item}s that want to interact with {@link RelayPort}s.
	 * @see IConnectorItem
	 * @author cd4017be
	 */
	public interface IWiredConnectorItem extends IConnectorItem {

		/**
		 * Perform attachment of given connector item on given RelayPort by calling {@link MountedPort#setConnector(IConnector, EntityPlayer)} and eventually {@link RelayPort#connect(Port)}.
		 * @param stack the itemstack used
		 * @param port the port to interact with
		 * @param player the interacting player
		 */
		default void doAttach(ItemStack stack, RelayPort port, EntityPlayer player) {
			doAttach(stack, (MountedPort)port, player);
		}

	}

	/**
	 * @return the direct distance between given port's connectors (for drawing wires)
	 */
	public static Vec3d getPath(MountedPort from, MountedPort to) {
		Vec3d path = new Vec3d(to.getPos().subtract(from.getPos())).add(to.pos.subtract(from.pos));
		if (!(from instanceof RelayPort)) path = path.subtract(new Vec3d(from.face.getDirectionVec()).scale(0.125));
		if (!(to instanceof RelayPort)) path = path.add(new Vec3d(to.face.getDirectionVec()).scale(0.125));
		return path;
	}

}
