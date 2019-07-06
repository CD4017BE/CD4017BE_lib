package cd4017be.api.rs_ctr.wire;

import java.util.List;
import java.util.function.BiFunction;

import org.apache.commons.lang3.tuple.Pair;

import cd4017be.api.rs_ctr.port.IConnector;
import cd4017be.api.rs_ctr.port.IPortProvider;
import cd4017be.api.rs_ctr.port.ITagableConnector;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.api.rs_ctr.port.Port;
import cd4017be.api.rs_ctr.wire.IWiredConnector.IWiredConnectorItem;
import cd4017be.api.rs_ctr.wire.WireLine.WireLoopException;
import cd4017be.lib.util.Orientation;
import cd4017be.lib.util.TooltipUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

/**
 * A double SignalPort that rather than providing a functional connection on it's own, only passes it further to other ports.<br>
 * Used to implement wire anchors.
 * @author cd4017be
 */
public abstract class RelayPort extends MountedPort {

	public static final float SIZE = MountedPort.SIZE / 4F;
	public static BiFunction<IHookAttachable, Integer, RelayPort> IMPLEMENTATION;

	public final RelayPort opposite;

	/**
	 * @param owner
	 * @param pin convention: 0x8??? for source, 0x9??? for sink
	 */
	protected RelayPort(IPortProvider owner, int pin) {
		super(owner, pin & 0xfff | 0x8000, null, true);
		this.opposite = createPair();
	}

	protected RelayPort(RelayPort opposite) {
		super(opposite.owner, opposite.pin ^ 0x1000, null, !opposite.isMaster);
		this.opposite = opposite;
	}

	protected abstract RelayPort createPair();

	@Override
	public Pair<Vec3d, EnumFacing> rayTrace(Vec3d start, Vec3d dir) {
		RayTraceResult rt = new AxisAlignedBB(pos.x - SIZE, pos.y - SIZE, pos.z - SIZE, pos.x + SIZE, pos.y + SIZE, pos.z + SIZE).calculateIntercept(start, start.add(dir));
		return rt == null ? null : Pair.of(rt.hitVec.subtract(start), rt.sideHit);
	}

	@Override
	public Pair<Vec3d, String> getDisplayText(Vec3d aim) {
		String s;
		if (this.connector != null) s = this.connector.displayInfo(this, linkID);
		else if (opposite.connector != null) s = opposite.connector.displayInfo(opposite, linkID);
		else s = TooltipUtil.translate(name);
		if (!s.isEmpty() && s.charAt(0) == '\n') s = s.substring(1);
		return Pair.of(pos, s);
	}

	public abstract void orient(Orientation o);

	@Override
	public <T> void addRenderComps(List<T> list, Class<T> type) {
		super.addRenderComps(list, type);
		if (type.isInstance(opposite.connector)) {
			list.add(type.cast(opposite.connector));
			opposite.connector.setPort(opposite);
		}
	}

	@Override
	public boolean onInteract(EntityPlayer player, boolean hit, EnumFacing side, Vec3d aim) {
		ItemStack stack = player.getHeldItemMainhand();
		if (hit || player.isSneaking() && stack.isEmpty()) {
			if (connector != null) setConnector(null, player);
			else if (opposite.connector != null) opposite.setConnector(null, player);
			else ((IHookAttachable)owner).removeHook(pin, player);
			return true;
		}
		if (stack.getItem() instanceof IWiredConnectorItem) {
			((IWiredConnectorItem)stack.getItem()).doAttach(stack, connector != null && opposite.connector == null ? opposite : this, player);
			return true;
		} 
		return false;
	}

	@Override
	public void connect(Port to) {
		WireLine line;
		try {line = new WireLine(this);}
		catch (WireLoopException e) {return;}
		if (line.source == null || line.sink == null || !line.contains(to) || !line.checkTypes()) return;
		
		String label = null;
		for (MountedPort p : line) {
			IConnector con = p.getConnector();
			label = con instanceof ITagableConnector ? ((ITagableConnector)con).getTag() : null;
			if (label != null) break;
		}
		String label_ = label;
		line.forEach((c)-> {
			IConnector cn = c.getConnector();
			if (cn instanceof ITagableConnector)
				((ITagableConnector)cn).setTag(c, label_);
		});
		
		line.source.connect(line.sink);
		int id = line.source.getLink();
		for (RelayPort rp : line.hooks) {
			rp.linkID = id;
			rp.owner.onPortModified(rp, IPortProvider.E_CONNECT);
		}
	}

	@Override
	public void disconnect() {
		WireLine line;
		try {line = new WireLine(this);}
		catch (WireLoopException e) {return;}
		if (line.source != null) line.source.disconnect();
		else if (line.sink != null) line.sink.disconnect();
		for (RelayPort rp : line.hooks) {
			rp.linkID = 0;
			rp.owner.onPortModified(rp, IPortProvider.E_DISCONNECT);
		}
	}

	@Override
	public void onLoad() {
		if (this.connector != null) this.connector.onLoad(this);
		if (opposite.connector != null) opposite.connector.onLoad(this);
	}

	@Override
	public void onUnload() {
		if (this.connector != null) this.connector.onUnload();
		if (opposite.connector != null) opposite.connector.onUnload();
	}

	public abstract ItemStack getDropped();

}
