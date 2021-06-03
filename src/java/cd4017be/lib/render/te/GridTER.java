package cd4017be.lib.render.te;

import com.mojang.blaze3d.matrix.MatrixStack;

import cd4017be.api.grid.IDynamicPart;
import cd4017be.lib.tileentity.Grid;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;


/**
 * @author CD4017BE */
public class GridTER extends TileEntityRenderer<Grid> {

	public GridTER(TileEntityRendererDispatcher terd) {
		super(terd);
	}

	@Override
	public void render(
		Grid te, float t, MatrixStack ms, IRenderTypeBuffer rtb, int light, int overlay
	) {
		IDynamicPart[] parts = te.dynamicParts;
		if (parts == null) return;
		long o = te.opaque;
		for (IDynamicPart part : parts) part.render(ms, rtb, light, overlay, light, o);
	}

}
