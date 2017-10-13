package cd4017be.lib.Gui.inWorld;

import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.block.AdvancedBlock.IInteractiveTile;
import cd4017be.lib.block.BaseTileEntity;
import cd4017be.lib.util.Orientation;
import cd4017be.lib.util.Utils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.AxisDirection;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public abstract class InWorldUITile extends BaseTileEntity implements IInteractiveTile {

	public static double VIEW_DIST = 4.0;
	protected static final byte SEND_INTERVAL = 10;
	protected byte packetTimer;

	public InWorldUITile() {
	}

	public InWorldUITile(IBlockState state) {
		super(state);
	}

	protected void sendPacket(boolean force) {
		if (!force && ++packetTimer < SEND_INTERVAL) return;
		packetTimer = 0;
		PacketBuffer data = BlockGuiHandler.getPacketTargetData(pos);
		getPacketData(data);
		BlockGuiHandler.sendPacketToAllNear(this, VIEW_DIST * 2, data);
	}

	protected abstract void getPacketData(PacketBuffer buff);
	protected abstract UIElement[] getBoxes(); 
	protected abstract void onInteract(EntityPlayer player, ItemStack item, EnumHand hand, RayTraceResult hit, ClickType type);

	@Override
	public boolean onActivated(EntityPlayer player, EnumHand hand, ItemStack item, EnumFacing s, float X, float Y, float Z) {
		RayTraceResult hit = getAimTarget(player, 1);
		if (hit == null) return false;
		if (!world.isRemote)
			onInteract(player, item, hand, hit, ClickType.use);
		return true;
	}

	@Override
	public void onClicked(EntityPlayer player) {
		if (world.isRemote) return;
		RayTraceResult hit = getAimTarget(player, 1);
		if (hit != null)
			onInteract(player, player.getHeldItem(player.swingingHand), player.swingingHand, hit, ClickType.hit);
	}

	public RayTraceResult getAimTarget(Entity e, float t) {
		Orientation o = getOrientation();
		Vec3d start = o.invRotate(e.getPositionEyes(t).subtract(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)).addVector(0.5, 0.5, 0.5);
		Vec3d end = o.invRotate(e.getLook(t).scale(VIEW_DIST)).add(start);
		RayTraceResult hit = null, h;
		UIElement[] boxes = getBoxes();
		for (int i = 0; i < boxes.length; i++)
			if ((h = boxes[i].intercept(start, end)) != null) {
				hit = h;
				end = hit.hitVec;
			}
		return hit;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public double getMaxRenderDistanceSquared() {
		return VIEW_DIST * VIEW_DIST;
	}

	public static class UIElement {
		public final AxisAlignedBB box;
		public final int id;
		public byte accSides;
		public UIElement(int id, double x0, double y0, double z0, double x1, double y1, double z1, int sides) {
			this.id = id;
			this.box = new AxisAlignedBB(x0 / 16.0, y0 / 16.0, z0 / 16.0, x1 / 16.0, y1 / 16.0, z1 / 16.0);
			this.accSides = (byte)sides;
		}
		public RayTraceResult intercept(Vec3d start, Vec3d end) {
			RayTraceResult res = box.calculateIntercept(start, end);
			if (res == null || (accSides >> res.sideHit.ordinal() & 1) == 0) return null;
			res.subHit = id;
			return res;
		}
		public double interpolate(Vec3d hit, EnumFacing dir) {
			double p = Utils.coord(hit.x, hit.y, hit.z, dir);
			double a = Utils.coord(box.minX, box.minY, box.minZ, dir);
			double b = Utils.coord(box.maxX, box.maxY, box.maxZ, dir);
			return (dir.getAxisDirection() == AxisDirection.POSITIVE ? (p - a) : (b - p)) / (b - a);
		}
	}

}
