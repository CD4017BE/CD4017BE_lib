package cd4017be.lib.block;

import java.util.ArrayList;
import java.util.List;

import cd4017be.lib.Gui.inWorld.ClickType;
import cd4017be.lib.Gui.inWorld.IInWorldUITile;
import cd4017be.lib.Gui.inWorld.UIcomp;
import cd4017be.lib.util.Obj2;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

public class WorldUITile extends BaseTileEntity implements IInWorldUITile {

	public static final double RANGE = 4;
	protected ArrayList<UIcomp> uiComps = new ArrayList<UIcomp>();

	@Override
	public List<UIcomp> UIComponents() {
		return uiComps;
	}

	@Override
	public Obj2<UIcomp, RayTraceResult> getSelectedComp(Entity entity, float t) {
		Vec3d start = entity.getLook(t).subtract(pos.getX(), pos.getY(), pos.getZ());
		Vec3d end = entity.getPositionEyes(t).scale(RANGE).add(start);
		RayTraceResult rtr = null, rt;
		UIcomp comp = null;
		for (UIcomp c : uiComps)
			if (c.bounds != null && (rt = c.bounds.calculateIntercept(start, end)) != null) {
				rtr = rt;
				end = rtr.hitVec;
				comp = c;
			}
		return comp == null ? null : new Obj2<UIcomp, RayTraceResult>(comp, rtr);
	}

	@Override
	public boolean onActivated(EntityPlayer player, EnumHand hand, ItemStack item, EnumFacing s, float X, float Y, float Z) {
		Obj2<UIcomp, RayTraceResult> sel = getSelectedComp(player, 1);
		return sel != null && sel.objA.interact(sel.objB, player, ClickType.use);
	}

	@Override
	public void onClicked(EntityPlayer player) {
		Obj2<UIcomp, RayTraceResult> sel = getSelectedComp(player, 1);
		if (sel != null) sel.objA.interact(sel.objB, player, ClickType.hit);
	}

	@Override
	public double getMaxRenderDistanceSquared() {
		return RANGE * RANGE;
	}

}
