package cd4017be.lib.render;

import java.io.IOException;

import cd4017be.lib.event.ModTextureStitchEvent;
import net.minecraft.client.renderer.texture.PngSizeInfo;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;


/**
 * 
 * @author CD4017BE
 */
public class RectangularSprite extends TextureAtlasSprite {

	static boolean STITCHING = true;

	static class EventHandler {
		@SubscribeEvent
		void preStitch(TextureStitchEvent.Pre event) {
			STITCHING = true;
		}
		@SubscribeEvent
		void postStitch(TextureStitchEvent.Post event) {
			STITCHING = false;
		}
		@SubscribeEvent
		void preStitch(ModTextureStitchEvent.Pre event) {
			STITCHING = true;
		}
		@SubscribeEvent
		void postStitch(ModTextureStitchEvent.Post event) {
			STITCHING = false;
		}
	}

	static {
		MinecraftForge.EVENT_BUS.register(new EventHandler());
	}

	boolean imgTransposed;

	/**
	 * @param spriteName
	 */
	public RectangularSprite(String spriteName) {
		super(spriteName);
	}

	@Override
	public void loadSprite(PngSizeInfo sizeInfo, boolean p_188538_2_) throws IOException {
		try {super.loadSprite(sizeInfo, p_188538_2_);}
		catch(RuntimeException e) {} //I want broken aspect ratios because they're awesome!
	}

	@Override
	public void initSprite(int inX, int inY, int originInX, int originInY, boolean rotatedIn) {
		int w = width, h = height;
		if (rotatedIn) {
			width = h; height = w;
			super.initSprite(inX, inY, originInX, originInY, rotatedIn);
			width = w; height = h;
		} else super.initSprite(inX, inY, originInX, originInY, rotatedIn);
		if (rotatedIn ^ imgTransposed) {
			int[][] arr = framesTextureData.get(0);
			for (int i = 0; i < arr.length; i++) {
				int[] img = arr[i], ri = new int[img.length];
				for (int j = 0, l = 0; j < w; j++)
					for (int k = 0; k < h; k++, l++)
						ri[l] = img[w * k + j];
				arr[i] = ri;
			}
			imgTransposed = rotatedIn;
		}
	}

	@Override
	public int getIconWidth() {
		return STITCHING && rotated ? height : width;
	}

	@Override
	public int getIconHeight() {
		return STITCHING && rotated ? width : height;
	}

	public boolean uvTransposed() {
		return imgTransposed;
	}

	/**
	 * @param u left [0-1]
	 * @param v top [0-1]
	 * @param w width [0-1]
	 * @param h height [0-1]
	 * @return the interpolated (u, v) coordinates of the four rectangle corners in anti-clockwise order, starting at top left
	 */
	public static float[] getInterpolatedUV(float[] a, TextureAtlasSprite tex, float u, float v, float w, float h) {
		if (a == null) a = new float[8];
		int t; float o, d;
		if (tex instanceof RectangularSprite && ((RectangularSprite)tex).imgTransposed) {
			t = 4;
			o = u; u = v; v = o;
			o = w; w = h; h = o;
		} else t = 0;
		o = tex.getMinU(); d = tex.getMaxU() - o;
		a[0] = u = u * d + o;  a[4] = w * d + o + u;
		o = tex.getMinV(); d = tex.getMaxV() - o;
		a[1] = v = v * d + o;  a[5] = h * d + o + v;
		a[2] = a[0^t];     a[6] = a[4^t];
		a[3] = a[5^t];     a[7] = a[1^t];
		return a;
	}

}
