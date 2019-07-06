package cd4017be.api.rs_ctr.wire;

import cd4017be.api.rs_ctr.port.IConnector;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.api.rs_ctr.port.Port;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

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

}
