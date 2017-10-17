package cd4017be.lib.render;

import org.lwjgl.opengl.GL11;

import cd4017be.api.automation.IOperatingArea;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 *
 * @author CD4017BE
 */
@SideOnly(Side.CLIENT)
public class SelectionRenderer extends TileEntitySpecialRenderer<TileEntity> {

	private void renderSelection(int[] area, double ofsX, double ofsY, double ofsZ) {
		if (area == null) return;
		//set state
		GlStateManager.bindTexture(0);
		GlStateManager.disableCull();
		GlStateManager.disableLighting(); 
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
		GlStateManager.depthMask(false);
		GlStateManager.color(1, 1, 1, 1);
		//render
		int density = 0x40;
		BufferBuilder t = Tessellator.getInstance().getBuffer();
		t.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
		t.setTranslation(ofsX, ofsY, ofsZ);
		//Bottom
		t.pos(area[0], area[1], area[2]).color(0xff, 0x00, 0x00, density).endVertex();
		t.pos(area[3], area[1], area[2]).color(0xff, 0x00, 0x00, density).endVertex();
		t.pos(area[3], area[1], area[5]).color(0xff, 0x00, 0x00, density).endVertex();
		t.pos(area[0], area[1], area[5]).color(0xff, 0x00, 0x00, density).endVertex();
		//Top
		t.pos(area[0], area[4], area[2]).color(0xff, 0x00, 0x00, density).endVertex();
		t.pos(area[3], area[4], area[2]).color(0xff, 0x00, 0x00, density).endVertex();
		t.pos(area[3], area[4], area[5]).color(0xff, 0x00, 0x00, density).endVertex();
		t.pos(area[0], area[4], area[5]).color(0xff, 0x00, 0x00, density).endVertex();
		//North
		t.pos(area[0], area[1], area[2]).color(0x00, 0xff, 0x00, density).endVertex();
		t.pos(area[3], area[1], area[2]).color(0x00, 0xff, 0x00, density).endVertex();
		t.pos(area[3], area[4], area[2]).color(0x00, 0xff, 0x00, density).endVertex();
		t.pos(area[0], area[4], area[2]).color(0x00, 0xff, 0x00, density).endVertex();
		//South
		t.pos(area[0], area[1], area[5]).color(0x00, 0xff, 0x00, density).endVertex();
		t.pos(area[3], area[1], area[5]).color(0x00, 0xff, 0x00, density).endVertex();
		t.pos(area[3], area[4], area[5]).color(0x00, 0xff, 0x00, density).endVertex();
		t.pos(area[0], area[4], area[5]).color(0x00, 0xff, 0x00, density).endVertex();
		//East
		t.pos(area[0], area[1], area[2]).color(0x00, 0x00, 0xff, density).endVertex();
		t.pos(area[0], area[4], area[2]).color(0x00, 0x00, 0xff, density).endVertex();
		t.pos(area[0], area[4], area[5]).color(0x00, 0x00, 0xff, density).endVertex();
		t.pos(area[0], area[1], area[5]).color(0x00, 0x00, 0xff, density).endVertex();
		//West
		t.pos(area[3], area[1], area[2]).color(0x00, 0x00, 0xff, density).endVertex();
		t.pos(area[3], area[4], area[2]).color(0x00, 0x00, 0xff, density).endVertex();
		t.pos(area[3], area[4], area[5]).color(0x00, 0x00, 0xff, density).endVertex();
		t.pos(area[3], area[1], area[5]).color(0x00, 0x00, 0xff, density).endVertex();
		//done
		t.setTranslation(0, 0, 0);
		Tessellator.getInstance().draw();
	}

	@Override
	public void render(TileEntity te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
		if (te instanceof IOperatingArea && IOperatingArea.Handler.renderArea((IOperatingArea)te))
			this.renderSelection(((IOperatingArea)te).getOperatingArea(), x - (double)te.getPos().getX(), y - (double)te.getPos().getY(), z - (double)te.getPos().getZ());
	}

}
