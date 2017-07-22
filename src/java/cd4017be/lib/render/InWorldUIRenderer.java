package cd4017be.lib.render;

import java.util.List;

import cd4017be.lib.Gui.inWorld.IInWorldUITile;
import cd4017be.lib.Gui.inWorld.UIcomp;
import cd4017be.lib.util.Obj2;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.RayTraceResult;

public class InWorldUIRenderer extends TileEntitySpecialRenderer<TileEntity> {

	public RenderItem itemRenderer;

	@Override
	public void renderTileEntityAt(TileEntity te, double x, double y, double z, float t, int destroyStage) {
		IInWorldUITile ui = (IInWorldUITile)te;
		Util.moveAndOrientToBlock(x, y, z, ui.getOrientation());
		List<UIcomp> comps = ui.UIComponents();
		for (UIcomp c : comps) c.draw(this);
		if (rendererDispatcher.cameraHitResult != null && te.getPos().equals(rendererDispatcher.cameraHitResult.getBlockPos())) {
			Obj2<UIcomp, RayTraceResult> sel = ui.getSelectedComp(rendererDispatcher.entity, t);
			if (sel != null) sel.objA.drawOverlay(this, sel.objB, Minecraft.getMinecraft().player);
		}
		//TODO cleanup render state
	}

}
