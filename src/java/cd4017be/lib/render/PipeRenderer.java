/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cd4017be.lib.render;

import org.lwjgl.opengl.GL11;

import cd4017be.lib.ModTileEntity;
import cd4017be.lib.templates.IPipe;
import cd4017be.lib.templates.IPipe.Cover;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.util.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 *
 * @author CD4017BE
 */
@Deprecated //use ModelPipe instead 
public class PipeRenderer extends TileEntitySpecialRenderer<ModTileEntity>
{   
	
    private String modelRes;
    private String[] variants;
    
    public PipeRenderer(String base, String... variants)
    {
    	this.modelRes = base;
    	this.variants = variants;
    }
    
    private IBakedModel getModel(int i, boolean part) {
    	if (i < 0 || i >= variants.length) return null;
    	ModelResourceLocation loc = new ModelResourceLocation(modelRes + variants[i] + (part ? "_con" : "_core"), "inventory");//Item workaround for registering models
    	return Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes().getModelManager().getModel(loc);
    }
    
    private boolean renderCover(WorldRenderer renderer, IBlockAccess world, BlockPos pos, Cover cover)
    {
    	if (cover == null) return false;
    	IBlockState state;
    	try {state = cover.block.getBlock().getExtendedState(cover.block, world, pos);} catch (Throwable e) {state = cover.block;}
    	if (!Minecraft.getMinecraft().getBlockRendererDispatcher().renderBlock(state, pos, world, renderer)) return false;
    	return cover.block.getBlock().isOpaqueCube();
    }
    
    private void renderPipe(WorldRenderer renderer, IBlockAccess world, BlockPos pos, IPipe pipe)
    {
    	IBlockState state = world.getBlockState(pos);
    	IBakedModel m = this.getModel(pipe.textureForSide((byte)-1), false);
    	CombinedModel model = new CombinedModel(m).add(m);
    	for (byte i = 0; i < 6; i++) {
    		m = this.getModel(pipe.textureForSide(i), true);
    		if (m != null) model.addRotated(m, CombinedModel.Rotations[i]);
    	}
    	Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelRenderer().renderModel(world, model, state, pos, renderer, false);
    }

	@Override
	public void renderTileEntityAt(ModTileEntity te, double x, double y, double z, float partialTicks, int destroyStage) {
        if (!(te instanceof IPipe)) return;
        this.bindTexture(TextureMap.locationBlocksTexture);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.blendFunc(770, 771);
        GlStateManager.enableBlend();
        GlStateManager.enableCull();
        if (Minecraft.isAmbientOcclusionEnabled()) GlStateManager.shadeModel(7425);
        else GlStateManager.shadeModel(7424);
        WorldRenderer t = Tessellator.getInstance().getWorldRenderer();
        t.setTranslation(x - (double)te.getPos().getX(), y - (double)te.getPos().getY(), z - (double)te.getPos().getZ());
        t.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
        
        Cover cover = ((IPipe)te).getCover();
        if (!this.renderCover(t, te.getWorld(), te.getPos(), cover)) {
        	this.renderPipe(t, te.getWorld(), te.getPos(), (IPipe)te);
        }
        
        t.setTranslation(0.0D, 0.0D, 0.0D);
        Tessellator.getInstance().draw();
        RenderHelper.enableStandardItemLighting();
        
	}
    
}
