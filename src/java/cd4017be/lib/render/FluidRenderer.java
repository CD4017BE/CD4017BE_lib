package cd4017be.lib.render;

import java.util.HashMap;

import org.lwjgl.opengl.GL11;

import cd4017be.lib.Lib;
import cd4017be.lib.render.IModeledTESR;
import cd4017be.lib.render.SpecialModelLoader;
import cd4017be.lib.render.model.IntArrayModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.GlStateManager.Profile;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Render helper for fluid boxes
 * @author CD4017BE
 */
@SideOnly(Side.CLIENT)
public class FluidRenderer implements IModeledTESR {

	private static final String name = "fluid_block";
	public static FluidRenderer instance;
	private IntArrayModel baseModel = new IntArrayModel(0);
	private HashMap<Fluid, IntArrayModel> fluidModels = new HashMap<Fluid, IntArrayModel>();
	private HashMap<Fluid, Integer> fluidColors = new HashMap<Fluid, Integer>();

	/** Call this during {@link net.minecraftforge.client.event.ModelRegistryEvent ModelRegistryEvent} */
	public static void register() {
		if (instance == null)
			SpecialModelLoader.instance.tesrs.add(instance = new FluidRenderer());
	}

	/**
	 * @param fluid
	 * @return a model for rendering a 1x1x1 cube with the fluid's texture.
	 */
	public IntArrayModel getFor(Fluid fluid) {
		IntArrayModel m = fluidModels.get(fluid);
		if (m != null) return m;
		ResourceLocation res;
		if ((res = fluid.getStill()) == null && (res = fluid.getFlowing()) == null) return null;
		TextureAtlasSprite tex = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(res.toString());
		if (tex == null) return null;
		fluidModels.put(fluid, m = baseModel.withTexture(tex));
		return m;
	}

	/**
	 * @param fluid
	 * @return the effective (extracted from texture) color of the given fluid in 0xRRGGBB format.
	 */
	public int fluidColor(Fluid fluid) {
		Integer c = fluidColors.get(fluid);
		if (c != null) return c;
		int fc = fluid.getColor();
		ResourceLocation res;
		if ((res = fluid.getStill()) == null && (res = fluid.getFlowing()) == null) return fc;
		TextureAtlasSprite tex = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(res.toString());
		if (tex == null) return fc;
		int r = 0, g = 0, b = 0, n = 0;
		for (int i = 0; i < tex.getFrameCount(); i++)
			for (int[] arr : tex.getFrameTextureData(i)) {
				for (int k : arr) {
					int a = k >> 24 & 0xff;
					n += a;
					r += (k >> 16 & 0xff) * a;
					g += (k >> 8 & 0xff) * a;
					b += (k & 0xff) * a;
				}
			}
		r = r / n * (fc >> 16 & 0xff) / 255 & 0xff;
		g = g / n * (fc >> 8 & 0xff) / 255 & 0xff;
		b = b / n * (fc & 0xff) / 255 & 0xff;
		fc = r << 16 | g << 8 | b;
		fluidColors.put(fluid, fc);
		return fc;
	}

	@Override
	public void bakeModels(IResourceManager manager) {
		try {
			baseModel = SpecialModelLoader.loadTESRModel(Lib.ID, name);
			fluidModels.clear();
			fluidColors.clear();
		} catch (Exception e) {
			Lib.LOG.error("FluidRenderer failed to load fluid model: " + name, e);
		}
	}

	/**
	 * @param c color in 1: 0xAARRGGBB or 2: 0xAABBGGRR format
	 * @return the given color converted from format 1 to 2 or vice versa.
	 */
	public static int RGBtoBGR(int c) {
		return c & 0xff00ff00 | c << 16 & 0xff0000 | c >> 16 & 0xff;
	}

	/**
	 * Render a fluid box
	 * @param stack FluidStack to render
	 * @param te the TileEntity rendering it
	 * @param x render coord X
	 * @param y render coord Y
	 * @param z render coord Z
	 * @param dxz horizontal size
	 * @param dy vertical size
	 */
	public void render(FluidStack stack, TileEntity te, double x, double y, double z, double dxz, double dy) {
		Fluid fluid = stack.getFluid();
		IntArrayModel m = getFor(fluid);
		GlStateManager.disableLighting();
		Profile.TRANSPARENT_MODEL.apply();
		m.setColor(RGBtoBGR(fluid.getColor(stack)));
		m.setBrightness(te.getWorld().getCombinedLight(te.getPos(), fluid.getLuminosity(stack)));
		Minecraft.getMinecraft().renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		GlStateManager.pushMatrix();
		GlStateManager.translate(x + 0.5D, y, z + 0.5D);
		GlStateManager.scale(dxz, dy, dxz);
		BufferBuilder t = Tessellator.getInstance().getBuffer();
		t.begin(GL11.GL_QUADS, IntArrayModel.FORMAT);
		t.addVertexData(m.vertexData);
		Tessellator.getInstance().draw();
		GlStateManager.popMatrix();
		Profile.TRANSPARENT_MODEL.clean();
	}

}
