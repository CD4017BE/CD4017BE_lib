package cd4017be.lib.render.te;

import com.mojang.blaze3d.vertex.PoseStack;

import cd4017be.api.grid.IDynamicPart;
import cd4017be.lib.tileentity.Grid;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;


/**
 * @author CD4017BE */
public class GridTER implements BlockEntityRenderer<Grid> {

	public GridTER(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public void render(
		Grid te, float t, PoseStack ms, MultiBufferSource rtb, int light, int overlay
	) {
		IDynamicPart[] parts = te.dynamicParts;
		if (parts == null) return;
		te.onVisible();
		long o = te.opaque;
		for (IDynamicPart part : parts) part.render(ms, rtb, light, overlay, light, o);
	}

}
