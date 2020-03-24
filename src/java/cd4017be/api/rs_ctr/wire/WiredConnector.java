package cd4017be.api.rs_ctr.wire;

import java.util.Objects;
import cd4017be.api.rs_ctr.port.Connector;
import cd4017be.api.rs_ctr.port.IIntegratedConnector;
import cd4017be.api.rs_ctr.port.IPortProvider;
import cd4017be.api.rs_ctr.port.ITagableConnector;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.api.rs_ctr.port.Port;
import cd4017be.api.rs_ctr.port.Connector.IConnectorItem;
import cd4017be.api.rs_ctr.wire.WireLine.WireLoopException;
import cd4017be.lib.util.DimPos;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.util.Constants.NBT;

/**
 * A connector that can be attached to wire anchors and connects between blocks in the world.
 * @author CD4017BE
 */
public abstract class WiredConnector extends Connector implements IPortProvider, ITagableConnector {

	/**internal port used to represent connection within individual wire segments */
	public final Port conBeacon;
	/**position of the linked connector for this wire segment */
	public DimPos conPos;
	/**port pin of the linked connector for this wire segment */
	public int conPin;

	protected String tag;

	public WiredConnector(MountedPort port) {
		super(port);
		this.conBeacon = new Port(this, 0, WiredConnector.class, port.isMaster);
	}

	@Override
	public void onLoad() {
		conBeacon.onLoad();
	}

	@Override
	public void onUnload() {
		conBeacon.onUnload();
	}

	@Override
	public Port getPort(int pin) {
		return conBeacon;
	}

	@Override
	public Object getPortCallback(int pin) {
		return this;
	}

	@Override
	public void setPortCallback(int pin, Object callback) {
		if (!(callback instanceof WiredConnector)) return;
		WiredConnector link = (WiredConnector)callback;
		boolean moved = false;
		DimPos pos = new DimPos(this.port.getPos(), this.port.getWorld());
		if (link.conPin != this.port.pin || !pos.equals(link.conPos)) {
			link.conPin = this.port.pin;
			link.conPos = pos;
			link.onPortModified(link.conBeacon, E_CON_UPDATE);
			moved = true;
		}
		pos = new DimPos(link.port.getPos(), link.port.getWorld());
		if (this.conPin != link.port.pin || !pos.equals(this.conPos)) {
			this.conPin = link.port.pin;
			this.conPos = pos;
			this.onPortModified(this.conBeacon, E_CON_UPDATE);
			moved = true;
		}
		if (moved) onPortMove();
	}

	@Override
	public void onPortModified(Port port, int event) {
		this.port.owner.onPortModified(this.port, E_CON_UPDATE);
	}

	/**
	 * when this or the linked port changes position/orientation or got teleported.
	 * @param link the port this is connected to via wire
	 */
	protected void onMoved(WiredConnector link) {}

	@Override
	public void onPortMove() {
		WiredConnector p = getLink();
		if (p == null) return;
		this.onMoved(p);
		p.onMoved(this);
	}

	@Override
	public NBTTagCompound serializeNBT() {
		NBTTagCompound nbt = super.serializeNBT();
		nbt.merge(conBeacon.serializeNBT());
		nbt.setInteger("lp", conPin);
		nbt.setInteger("lx", conPos.getX());
		nbt.setInteger("ly", conPos.getY());
		nbt.setInteger("lz", conPos.getZ());
		nbt.setInteger("ld", conPos.dimId);
		if (tag != null) nbt.setString("tag", tag);
		return nbt;
	}

	@Override
	public void deserializeNBT(NBTTagCompound nbt) {
		conBeacon.deserializeNBT(nbt);
		conPin = nbt.getInteger("lp");
		conPos = new DimPos(
			nbt.getInteger("lx"),
			nbt.getInteger("ly"),
			nbt.getInteger("lz"),
			nbt.getInteger("ld")
		);
		tag = nbt.hasKey("tag", NBT.TAG_STRING)
			? nbt.getString("tag") : null;
	}

	public WiredConnector getLink() {
		int lid = conBeacon.getLink();
		if (lid == 0 || conPos == null) return null;
		Port lp = IPortProvider.getPort(conPos.getWorldServer(), conPos, conPin);
		if (lp instanceof MountedPort) {
			Connector c = ((MountedPort)lp).getConnector();
			if (c instanceof IIntegratedConnector)
				return ((IIntegratedConnector)c).getLinkedWith(this);
			if (c instanceof WiredConnector && ((WiredConnector)c).conBeacon.getLink() == lid)
				return (WiredConnector)c;
		}
		return null;
	}

	public boolean isLinked(WiredConnector to) {
		int lid = conBeacon.getLink();
		return lid != 0 && lid == to.conBeacon.getLink();
	}

	/**
	 * when a wire from link connecting to host is removed (by a player).
	 * @param link the port the removed wire is attached on
	 * @param player the responsible actor
	 */
	protected void onWireRemoved(WiredConnector link, EntityPlayer player) {
		Connector con = port.getConnector();
		if (con == this) port.setConnector(null, player);
		else if (con instanceof IIntegratedConnector)
			((IIntegratedConnector)con).removeWire(this, player);
	}

	@Override
	public void onRemoved(EntityPlayer player) {
		disconnectLine: {
			WireLine line;
			try {line = new WireLine(this);}
			catch (WireLoopException e) {break disconnectLine;}
			Port p;
			if (line.source != null && (p = line.source.getComPort()) != null) p.disconnect();
			else if (line.sink != null && (p = line.sink.getComPort()) != null) p.disconnect();
			for (WiredConnector con : line.hooks)
				((RelayPort)con.port).disconnect();
		}
		WiredConnector con = getLink();
		if (con != null) con.onWireRemoved(this, player);
		conBeacon.disconnect();
	}

	public void connect(WiredConnector to) {
		conBeacon.connect(to.conBeacon);
		
		WireLine line;
		try {line = new WireLine(this);}
		catch (WireLoopException e) {return;}
		if (line.source == null || line.sink == null || !line.contains(to) || !line.checkTypes()) return;
		
		String label = null;
		for (WiredConnector con : line)
			if ((label = con.getTag()) != null)
				break;
		String label_ = label;
		line.forEach((con)-> con.setTag(label_));
		
		Port sp = line.source.getComPort();
		sp.connect(line.sink.getComPort());
		for (WiredConnector rp : line.hooks)
			((RelayPort)rp.port).connect(sp);
	}

	/**@return the actual port used for interaction.
	 * (only differs from {@link #port} if used as part of an {@link IIntegratedConnector}) */
	public Port getComPort() {
		Connector con = port.getConnector();
		if (con instanceof IIntegratedConnector)
			return ((IIntegratedConnector)con).getPort(this);
		return port;
	}

	/**
	 * @param type the interaction callback class
	 * @return whether this wire supports the given interaction type
	 */
	public abstract boolean isCompatible(Class<?> type);

	@Override
	public void setTag(String tag) {
		if (Objects.equals(this.tag, tag)) return;
		this.tag = tag;
		port.owner.onPortModified(port, IPortProvider.E_CON_UPDATE);
	}

	@Override
	public String getTag() {
		return tag;
	}

	@Override
	public String displayInfo(MountedPort port, int linkID) {
		return tag != null ? "\n\u00a7e" + tag : super.displayInfo(port, linkID);
	}


	/**
	 * implemented by {@link Item}s that want to interact with {@link RelayPort}s.
	 * @see IConnectorItem
	 * @author cd4017be
	 */
	public interface IWiredConnectorItem extends IConnectorItem {

		/**
		 * Perform attachment of given connector item on given RelayPort by calling {@link MountedPort#setConnector(Connector, EntityPlayer)} and eventually {@link RelayPort#connect(Port)}.
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

	public static double getDistance(MountedPort from, MountedPort to) {
		return Math.sqrt(
			new DimPos(from.getPos(), from.getWorld())
			.distanceSq(new DimPos(to.getPos(), to.getWorld()))
		);
	}

}
