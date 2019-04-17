package cd4017be.lib.render;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.client.model.animation.FastTESR;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


/**
 * This is basically the same as {@link FastTESR} but more flexible.
 * @author CD4017BE
 */
@SideOnly(Side.CLIENT)
public abstract class HybridFastTESR<T extends TileEntity> extends TileEntitySpecialRenderer<T> {

	@Override
	public void render(T te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		this.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		RenderHelper.disableStandardItemLighting();
		GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GlStateManager.enableBlend();
		GlStateManager.disableCull();
		if (Minecraft.isAmbientOcclusionEnabled())
			GlStateManager.shadeModel(GL11.GL_SMOOTH);
		else
			GlStateManager.shadeModel(GL11.GL_FLAT);

		buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
		renderTileEntityFast(te, x, y, z, partialTicks, destroyStage, alpha, buffer);
		buffer.setTranslation(0, 0, 0);
		tessellator.draw();

		renderSpecialPart(te, x, y, z, partialTicks, destroyStage, alpha);

		RenderHelper.enableStandardItemLighting();
	}

	/**
	 * Render the stuff that requires fancy OpenGL state modification
	 * @param te the TileEntity to render
	 * @param x render pos x
	 * @param y render pos y
	 * @param z render pos z
	 * @param t render time (partial tick)
	 * @param destroy
	 * @param alpha
	 */
	protected abstract void renderSpecialPart(T te, double x, double y, double z, float t, int destroy, float alpha);

	public abstract void renderTileEntityFast(T te, double x, double y, double z, float t, int destroy, float alpha, BufferBuilder buffer);

	/**
	 * @param te the TileEntity
	 * @return whether the player currently aims at the given TileEntity
	 */
	public static boolean isAimedAt(TileEntity te) {
		RayTraceResult rts = Minecraft.getMinecraft().objectMouseOver;
		return rts != null && te.getPos().equals(rts.getBlockPos());
	}

	/**
	 * @param te the TileEntity
	 * @param range maximum distance in blocks
	 * @return whether given TileEntity is within given distance to the camera
	 */
	public static boolean isWithinRange(TileEntity te, double range) {
		RenderManager rm = Minecraft.getMinecraft().getRenderManager();
		return te.getDistanceSq(rm.viewerPosX, rm.viewerPosY, rm.viewerPosZ) < range * range;
	}

}
