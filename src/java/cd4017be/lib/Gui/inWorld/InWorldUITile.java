package cd4017be.lib.Gui.inWorld;

import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.block.AdvancedBlock.IInteractiveTile;
import cd4017be.lib.block.BaseTileEntity;
import cd4017be.lib.util.Orientation;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
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
	protected abstract AxisAlignedBB[] getBoxes(); 
	protected abstract void onInteract(EntityPlayer player, ItemStack item, EnumHand hand, RayTraceResult hit, ClickType type);

	@Override
	public boolean onActivated(EntityPlayer player, EnumHand hand, ItemStack item, EnumFacing s, float X, float Y, float Z) {
		RayTraceResult hit = getAimTarget(player);
		if (hit == null) return false;
		if (!world.isRemote)
			onInteract(player, item, hand, hit, ClickType.use);
		return true;
	}

	@Override
	public void onClicked(EntityPlayer player) {
		if (world.isRemote) return;
		RayTraceResult hit = getAimTarget(player);
		if (hit != null)
			onInteract(player, player.getHeldItem(player.swingingHand), player.swingingHand, hit, ClickType.hit);
	}

	public RayTraceResult getAimTarget(Entity e) {
		Orientation o = getOrientation();
		Vec3d start = o.rotate(e.getPositionEyes(1).subtract(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
		Vec3d end = o.rotate(e.getLook(1).scale(VIEW_DIST)).add(start);
		RayTraceResult hit = null, h;
		AxisAlignedBB[] boxes = getBoxes();
		for (int i = 0; i < boxes.length; i++)
			if ((h = boxes[i].calculateIntercept(start, end)) != null) {
				hit = h;
				end = hit.hitVec;
				hit.subHit = i;
			}
		return hit;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public double getMaxRenderDistanceSquared() {
		return VIEW_DIST * VIEW_DIST;
	}

}
