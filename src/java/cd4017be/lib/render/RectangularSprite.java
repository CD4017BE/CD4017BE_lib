package cd4017be.lib.render;

import java.io.IOException;

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
}
