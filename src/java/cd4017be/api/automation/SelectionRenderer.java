/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cd4017be.api.automation;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;

/**
 *
 * @author CD4017BE
 */
public class SelectionRenderer extends TileEntitySpecialRenderer 
{
    
    private void renderSelection(int[] area, double ofsX, double ofsY, double ofsZ)
    {
        if (area == null) return;
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDepthMask(false);
        GL11.glColor4f(1, 1, 1, 1);
        int density = 0x40;
        Tessellator t = Tessellator.instance;
        t.setTranslation(ofsX, ofsY, ofsZ);
        t.startDrawingQuads();
        t.setColorRGBA(0xff, 0x00, 0x00, density);
        t.addVertex(area[0], area[1], area[2]);
        t.addVertex(area[3], area[1], area[2]);
        t.addVertex(area[3], area[1], area[5]);
        t.addVertex(area[0], area[1], area[5]);
        t.addVertex(area[0], area[4], area[2]);
        t.addVertex(area[3], area[4], area[2]);
        t.addVertex(area[3], area[4], area[5]);
        t.addVertex(area[0], area[4], area[5]);
        t.setColorRGBA(0x00, 0xff, 0x00, density);
        t.addVertex(area[0], area[1], area[2]);
        t.addVertex(area[3], area[1], area[2]);
        t.addVertex(area[3], area[4], area[2]);
        t.addVertex(area[0], area[4], area[2]);
        t.addVertex(area[0], area[1], area[5]);
        t.addVertex(area[3], area[1], area[5]);
        t.addVertex(area[3], area[4], area[5]);
        t.addVertex(area[0], area[4], area[5]);
        t.setColorRGBA(0x00, 0x00, 0xff, density);
        t.addVertex(area[0], area[1], area[2]);
        t.addVertex(area[0], area[4], area[2]);
        t.addVertex(area[0], area[4], area[5]);
        t.addVertex(area[0], area[1], area[5]);
        t.addVertex(area[3], area[1], area[2]);
        t.addVertex(area[3], area[4], area[2]);
        t.addVertex(area[3], area[4], area[5]);
        t.addVertex(area[3], area[1], area[5]);
        t.draw();
        t.setTranslation(0, 0, 0);
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
    }
    
    @Override
    public void renderTileEntityAt(TileEntity te, double d0, double d1, double d2, float f) 
    {
        if (te instanceof IOperatingArea && IOperatingArea.Handler.renderArea((IOperatingArea)te))
            this.renderSelection(((IOperatingArea)te).getOperatingArea(), d0 - te.xCoord, d1 - te.yCoord, d2 - te.zCoord);
    }
    
}
