/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cd4017be.lib.templates;

import cd4017be.lib.TileBlock;
import cd4017be.lib.templates.IPipe.Cover;
import cd4017be.lib.util.Utils;
import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import cpw.mods.fml.client.registry.RenderingRegistry;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;

/**
 *
 * @author CD4017BE
 */
public class PipeRenderer implements ISimpleBlockRenderingHandler
{
    public final int renderId;
    
    private final float blockBounds = 0.25F;
    private final float maxBounds = 1.0F;
    private final float minBounds = 0.0F;
    
    public PipeRenderer() 
    {
        renderId = RenderingRegistry.getNextAvailableRenderId();
    }
    
    public void setRenderMachine(TileBlock block)
    {
        block.setRenderType(renderId);
    }
    
    @Override
    public void renderInventoryBlock(Block block, int metadata, int modelID, RenderBlocks renderer)
    {
        
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId, RenderBlocks renderer) 
    {
        TileEntity te = world.getTileEntity(x, y, z);
        if (te == null || !(te instanceof IPipe)) return false;
        Cover cover = ((IPipe)te).getCover();
        if (cover != null && this.renderCover(renderer, world, x, y, z, cover, block)) return true;
        boolean renderAllFaces = renderer.renderAllFaces;
        boolean flipTexture = renderer.flipTexture;
        int uvRotateBottom = renderer.uvRotateBottom;
        int uvRotateTop = renderer.uvRotateTop;
        int uvRotateEast = renderer.uvRotateEast;
        int uvRotateWest = renderer.uvRotateWest;
        int uvRotateNorth = renderer.uvRotateNorth;
        int uvRotateSouth = renderer.uvRotateSouth;
        renderer.renderAllFaces = true;
        renderer.flipTexture = false;
        float size = block instanceof BlockPipe ? ((BlockPipe)block).size : blockBounds;
        for (byte dir = 0; dir < 6; dir++)
        {
            int tex = ((IPipe)te).textureForSide(dir);
            if (tex >= 0)
            {
                setBlockBoundsForDir(renderer, dir, size);
                renderer.setOverrideBlockTexture(block.getIcon(0, tex));
                renderer.renderStandardBlock(block, x, y, z);
            }
        }
        renderer.clearOverrideBlockTexture();
        setBlockBoundsForDir(renderer, (byte)6, size);
        renderer.renderStandardBlock(block, x, y, z);
        
        renderer.unlockBlockBounds();
        renderer.renderAllFaces = renderAllFaces;
        renderer.flipTexture = flipTexture;
        renderer.uvRotateBottom = uvRotateBottom;
        renderer.uvRotateTop = uvRotateTop;
        renderer.uvRotateEast = uvRotateEast;
        renderer.uvRotateWest = uvRotateWest;
        renderer.uvRotateNorth = uvRotateNorth;
        renderer.uvRotateSouth = uvRotateSouth;
        return true;
    }
    
    private boolean renderCover(RenderBlocks renderer, IBlockAccess world, int x, int y, int z, Cover cover, Block block)
    {
        Block cBlock = cover.blockId;
        if (cBlock == null) return false;
        Tessellator tessellator = Tessellator.instance;
        renderer.enableAO = false;
        int l = cBlock.getRenderColor(cover.meta);
        float r = (float)(l >> 16 & 255) / 255.0F;
        float g = (float)(l >> 8 & 255) / 255.0F;
        float b = (float)(l & 255) / 255.0F;
        if (EntityRenderer.anaglyphEnable)
        {
            float f3 = (r * 30.0F + g * 59.0F + b * 11.0F) / 100.0F;
            float f4 = (r * 30.0F + g * 70.0F) / 100.0F;
            float f5 = (r * 30.0F + b * 70.0F) / 100.0F;
            r = f3;
            g = f4;
            b = f5;
        }
        float f3 = 0.5F;
        float f4 = 1.0F;
        float f5 = 0.8F;
        float f6 = 0.6F;
        float f7 = f4 * r;
        float f8 = f4 * g;
        float f9 = f4 * b;
        float f10 = f3 * r;
        float f11 = f5 * r;
        float f12 = f6 * r;
        float f13 = f3 * g;
        float f14 = f5 * g;
        float f15 = f6 * g;
        float f16 = f3 * b;
        float f17 = f5 * b;
        float f18 = f6 * b;
        tessellator.setBrightness(block.getMixedBrightnessForBlock(world, x, y - 1, z));
        tessellator.setColorOpaque_F(f10, f13, f16);
        renderer.renderFaceYNeg(block, (double)x, (double)y, (double)z, renderer.getBlockIconFromSideAndMetadata(cBlock, 0, cover.meta));
        tessellator.setBrightness(block.getMixedBrightnessForBlock(world, x, y + 1, z));
        tessellator.setColorOpaque_F(f7, f8, f9);
        renderer.renderFaceYPos(block, (double)x, (double)y, (double)z, renderer.getBlockIconFromSideAndMetadata(cBlock, 1, cover.meta));
        tessellator.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z - 1));
        tessellator.setColorOpaque_F(f11, f14, f17);
        renderer.renderFaceZNeg(block, (double)x, (double)y, (double)z, renderer.getBlockIconFromSideAndMetadata(cBlock, 2, cover.meta));
        tessellator.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z + 1));
        tessellator.setColorOpaque_F(f11, f14, f17);
        renderer.renderFaceZPos(block, (double)x, (double)y, (double)z, renderer.getBlockIconFromSideAndMetadata(cBlock, 3, cover.meta));
        tessellator.setBrightness(block.getMixedBrightnessForBlock(world, x - 1, y, z));
        tessellator.setColorOpaque_F(f12, f15, f18);
        renderer.renderFaceXNeg(block, (double)x, (double)y, (double)z, renderer.getBlockIconFromSideAndMetadata(cBlock, 4, cover.meta));
        tessellator.setBrightness(block.getMixedBrightnessForBlock(world, x + 1, y, z));
        tessellator.setColorOpaque_F(f12, f15, f18);
        renderer.renderFaceXPos(block, (double)x, (double)y, (double)z, renderer.getBlockIconFromSideAndMetadata(cBlock, 5, cover.meta));
        return true;
    }

    @Override
    public int getRenderId() 
    {
        return renderId;
    }
    
    private void setBlockBoundsForDir(RenderBlocks renderer, byte dir, float size)
    {
        float[] bb = new float[6];
        for (int i = 0; i < bb.length; i++) bb[i] = i < 3 ? (1F - size) / 2F : (1F + size) / 2F;
        if (dir == Utils.S_LX) {bb[3] = bb[0]; bb[0] = minBounds; renderer.uvRotateTop = 0; renderer.uvRotateBottom = 0; renderer.uvRotateEast = 0; renderer.uvRotateWest = 0;}
        else if (dir == Utils.S_HX) {bb[0] = bb[3]; bb[3] = maxBounds; renderer.uvRotateTop = 0; renderer.uvRotateBottom = 0; renderer.uvRotateEast = 0; renderer.uvRotateWest = 0;}
        else if (dir == Utils.S_LY) {bb[4] = bb[1]; bb[1] = minBounds; renderer.uvRotateEast = 1; renderer.uvRotateWest = 1; renderer.uvRotateNorth = 1; renderer.uvRotateSouth = 1;}
        else if (dir == Utils.S_HY) {bb[1] = bb[4]; bb[4] = maxBounds; renderer.uvRotateEast = 1; renderer.uvRotateWest = 1; renderer.uvRotateNorth = 1; renderer.uvRotateSouth = 1;}
        else if (dir == Utils.S_LZ) {bb[5] = bb[2]; bb[2] = minBounds; renderer.uvRotateTop = 1; renderer.uvRotateBottom = 1; renderer.uvRotateNorth = 0; renderer.uvRotateSouth = 0;}
        else if (dir == Utils.S_HZ) {bb[2] = bb[5]; bb[5] = maxBounds; renderer.uvRotateTop = 1; renderer.uvRotateBottom = 1; renderer.uvRotateNorth = 0; renderer.uvRotateSouth = 0;}
        renderer.overrideBlockBounds(bb[0], bb[1], bb[2], bb[3], bb[4], bb[5]);
    }

	@Override
	public boolean shouldRender3DInInventory(int modelId) {
		return false;
	}
    
}
