package cd4017be.api.rs_ctr.wire;

import java.util.List;
import java.util.function.BiFunction;

import org.apache.commons.lang3.tuple.Pair;

import cd4017be.api.rs_ctr.port.IPortProvider;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.api.rs_ctr.port.Port;
import cd4017be.api.rs_ctr.wire.WiredConnector.IWiredConnectorItem;
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
		if (type.isInstance(opposite.connector))
			list.add(type.cast(opposite.connector));
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
		linkID = to.getLink();
		owner.onPortModified(this, IPortProvider.E_CONNECT);
	}

	@Override
	public void disconnect() {
		linkID = 0;
		owner.onPortModified(this, IPortProvider.E_DISCONNECT);
	}

	@Override
	public void onLoad() {
		if (this.connector != null) this.connector.onLoad();
	}

	@Override
	public void onUnload() {
		if (this.connector != null) this.connector.onUnload();
	}

	public abstract ItemStack getDropped();

}
