package cd4017be.api.rs_ctr.port;

import java.util.HashMap;
import java.util.function.Function;
import javax.annotation.Nullable;
import cd4017be.lib.util.ItemFluidUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.INBTSerializable;

/**
 * Stores information about a connection between MountedSignalPorts and is mainly used client side to render the connection.<dl>
 * Implementations are registered via {@link #REGISTRY} using a unique String ID which must be written to tag name "id" when implementing {@link INBTSerializable#serializeNBT()}.
 * @author CD4017BE
 */
public abstract class Connector implements INBTSerializable<NBTTagCompound> {

	/**map of registered Connector types */
	public static final HashMap<String, Function<MountedPort, ? extends Connector>> REGISTRY = new HashMap<>();

	public final MountedPort port;

	public Connector(MountedPort port) {
		this.port = port;
	}

	/**
	 * @param port the port holding this connector
	 * @param linkID current signal link
	 * @return the additional tool-tip shown when the port is aimed.
	 */
	public String displayInfo(MountedPort port, int linkID) {
		return linkID != 0 ? "\n#" + linkID : "";
	}

	/**
	 * Perform special removal actions like dropping items and/or calling {@link Port#disconnect()}.
	 * @param player
	 */
	public abstract void onRemoved(@Nullable EntityPlayer player);

	/**
	 * called when the given port is loaded into the world.
	 * @param port the port holding this connector.
	 */
	public void onLoad() {}

	/**
	 * called when the port holding this connector is unloaded.
	 */
	public void onUnload() {}

	public void onLinkLoad(Port link) {}

	/**
	 * called when the port changed its position and/or orientation (for ex. when the block rotated).
	 * @param port the port holding this connector (with new position)
	 */
	public void onPortMove() {}

	protected abstract String id();

	/**
	 * convenience method to drop an item stack at the ports position
	 * @param stack item to drop
	 * @param player the optional player that should receive the item
	 */
	protected void dropItem(ItemStack stack, @Nullable EntityPlayer player) {
		if (player == null) ItemFluidUtil.dropStack(stack, port.getWorld(), port.getPos());
		else if (!player.isCreative()) ItemFluidUtil.dropStack(stack, player);
	}

	@Override
	public NBTTagCompound serializeNBT() {
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setString("id", id());
		return nbt;
	}

	/**
	 * @param nbt serialized data
	 * @return a deserialized connector instance or null if data invalid.
	 */
	public static Connector load(NBTTagCompound nbt, MountedPort port) {
		Function<MountedPort, ? extends Connector> c = REGISTRY.get(nbt.getString("id"));
		if (c == null) return null;
		Connector con = c.apply(port);
		con.deserializeNBT(nbt);
		return con;
	}

	/**
	 * implemented by {@link Item}s that want to interact with {@link MountedPort}s.
	 * @author cd4017be
	 */
	public interface IConnectorItem {

		/**
		 * Perform attachment of given connector item on given SignalPort by calling {@link MountedPort#setConnector(Connector, EntityPlayer)} and eventually {@link Port#connect(Port)}.
		 * @param stack the itemstack used
		 * @param port the port to interact with
		 * @param player the interacting player
		 */
		void doAttach(ItemStack stack, MountedPort port, EntityPlayer player);

	}

}
