/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cd4017be.lib.render;

import org.lwjgl.opengl.GL11;

import cd4017be.api.automation.IOperatingArea;
import cd4017be.lib.ModTileEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 *
 * @author CD4017BE
 */
@SideOnly(Side.CLIENT)
public class SelectionRenderer extends TileEntitySpecialRenderer<ModTileEntity>
{
	
	private void renderSelection(int[] area, double ofsX, double ofsY, double ofsZ)//TODO use GlStateManager
	{
		if (area == null) return;
		//save state
		int Vbs = GL11.glGetInteger(GL11.GL_BLEND_SRC),
				Vbd = GL11.glGetInteger(GL11.GL_BLEND_DST);
		boolean Vc = GL11.glIsEnabled(GL11.GL_CULL_FACE),
				Vl = GL11.glIsEnabled(GL11.GL_LIGHTING),
				Vb = GL11.glIsEnabled(GL11.GL_BLEND);
		//set state
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
		GL11.glDisable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_LIGHTING); 
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glDepthMask(false);
		GL11.glColor4f(1, 1, 1, 1);
		//render
		int density = 0x40;
		VertexBuffer t = Tessellator.getInstance().getBuffer();
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
		//reset state
		GL11.glDepthMask(true);
		GL11.glBlendFunc(Vbs, Vbd);
		if (!Vb) GL11.glDisable(GL11.GL_BLEND);
		if (Vl) GL11.glEnable(GL11.GL_LIGHTING);
		if (Vc) GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, Minecraft.getMinecraft().getTextureMapBlocks().getGlTextureId());
	}

	@Override
	public void renderTileEntityAt(ModTileEntity te, double x, double y, double z, float partialTicks, int destroyStage) {
		if (te instanceof IOperatingArea && IOperatingArea.Handler.renderArea((IOperatingArea)te))
			this.renderSelection(((IOperatingArea)te).getOperatingArea(), x - (double)te.getPos().getX(), y - (double)te.getPos().getY(), z - (double)te.getPos().getZ());
	}
	
}
