package cd4017be.lib.render;

import static cd4017be.api.grid.GridPart.FACES;
import static cd4017be.api.grid.GridPart.step;
import static cd4017be.api.grid.GridPart.vec;
import static cd4017be.lib.render.Util.RGBtoBGR;
import static cd4017be.lib.render.model.JitBakedModel.INNER;
import static cd4017be.lib.render.model.JitBakedModel.LAYERS;
import static cd4017be.lib.render.model.WrappedBlockModel.MODELS;
import static cd4017be.math.Linalg.*;
import static cd4017be.math.MCConv.intBitsToVec;
import static cd4017be.math.MCConv.vecToIntBits;
import static java.lang.Float.floatToRawIntBits;
import static net.minecraft.client.renderer.RenderTypeLookup.canRenderInLayer;
import static net.minecraftforge.client.ForgeHooksClient.setRenderLayer;
import static net.minecraftforge.client.MinecraftForgeClient.getRenderLayer;

import java.util.*;

import cd4017be.lib.render.model.JitBakedModel;
import cd4017be.lib.render.model.TileEntityModel;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.EmptyModelData;

/**@author CD4017BE */
@OnlyIn(Dist.CLIENT)
public class MicroBlockFace {

	private final BakedQuad quad;
	private final float[] u, v;
	private final int ax, rt, color;

	public MicroBlockFace(BakedQuad quad, int rt, int color) {
		this.quad = quad;
		this.ax = quad.getDirection().getAxis().ordinal();
		this.rt = rt;
		int[] data = quad.getVertices();
		this.color = quad.isTinted() ? RGBtoBGR(color | 0xff000000) : data[3];
		//Normally stride = 8 but Optifine might append additional entries for shader data.
		int stride = data.length >> 2;
		float[][] mat = new float[3][5];
		for (int i = 0; i < 3; i++) {
			float[] row = mat[i];
			intBitsToVec(3, row, 0, data, i * stride);
			intBitsToVec(2, row, 3, data, i * stride + 4);
			row[ax] = 1F;
		}
		solveGauss(mat, 3, 5);
		this.u = col(3, new float[3], mat, 3);
		this.v = col(3, new float[3], mat, 4);
	}

	public static void drawVoxels(JitBakedModel model, Object key, long b, long opaque) {
		MicroBlockFace[] faces = facesOf(key);
		opaque |= b;
		for (int i = 0; i < 6; i++) {
			MicroBlockFace face = faces[i];
			if (face == null) continue;
			int s = step(i);
			long f = (i & 1) != 0 ? b & ~(opaque >>> s) : b >>> s & ~opaque;
			long m = FACES[i & 6];
			for (int j = 1; j < 4; j++, f >>>= s)
				face.addFaces(model, INNER, f & m, j - (i & 1));
			if ((f = b & FACES[i]) != 0) {
				if ((i & 1) != 0) f >>>= s * 3;
				face.addFaces(model, i, f, (i & 1) * 3);
			}
		}
	}

	public void addFaces(JitBakedModel model, int side, long mask, float layer) {
		ArrayList<BakedQuad> quads = model.quads(side | rt);
		int su = ax == 0 ? 4 : 1, sv = ax == 2 ? 4 : 16, s;
		while ((s = Long.numberOfTrailingZeros(mask)) < 64) {
			int i = (s | sv - 1) + 1, e;
			long strip = 1L << s;
			for (e = s + su; e < i; e += su) {
				if ((mask >>> e & 1) == 0) break;
				strip |= 1L << e;
			}
			mask &= ~strip;
			strip <<= sv;
			long not = s + sv > i && e < i ? strip << 1 | strip >>> 1 : 0L;
			while((strip & ~mask) == 0 && (not & ~mask) != 0) {
				mask &= ~strip;
				strip <<= sv;
				not <<= sv;
				e += sv;
			}
			e -= s + su;
			float[] p0 = vec(s);
			float[] size = dadd(3, vec(e), 1);
			p0[ax] += layer;
			sca(3, p0, .25F);
			sca(3, size, .25F);
			quads.add(makeRect(p0, size));
		}
	}

	public BakedQuad makeRect(float[] p0, float[] size) {
		float[] vec = new float[3];
		int[] data = quad.getVertices().clone();
		int stride = data.length >> 2;
		for (int j = 0, i = 0; j < 4; j++, i += stride) {
			intBitsToVec(3, vec, 0, data, i);
			mul(3, vec, size);
			add(3, vec, p0);
			vecToIntBits(3, vec, data, i);
			vec[ax] = 1F;
			data[i+3] = color;
			data[i+4] = floatToRawIntBits(dot(3, vec, u));
			data[i+5] = floatToRawIntBits(dot(3, vec, v));
		}
		return new BakedQuad(
			data, -1, quad.getDirection(),
			quad.getSprite(), quad.isShade()
		);
	}

	private static final HashMap<Object, MicroBlockFace[]> MODEL_CACHE = new HashMap<>();
	static {
		TileEntityModel.registerCacheInvalidate(MODEL_CACHE::clear);
	}

	/**@param key must be a BlockState or ResourceLocation
	 * @return faces for the given model */
	public static MicroBlockFace[] facesOf(Object key) {
		return MODEL_CACHE.computeIfAbsent(key, MicroBlockFace::create);
	}

	private static MicroBlockFace[] create(Object key) {
		Random rand = new Random();
		BlockState block = key instanceof BlockState ? (BlockState)key : null;
		IBakedModel model = block != null ? MODELS.getBlockModel(block)
			: MODELS.getModelManager().getModel((ResourceLocation)key);
		BlockColors colors = Minecraft.getInstance().getBlockColors();
		
		RenderType old = getRenderLayer();
		try {
			MicroBlockFace[] faces = new MicroBlockFace[6];
			for (Direction d : Direction.values())
				for (int i = 0; i < LAYERS.length; i++) {
					RenderType rt = LAYERS[i];
					if (block != null && !canRenderInLayer(block, rt)) continue;
					setRenderLayer(rt);
					rand.setSeed(42L);
					List<BakedQuad> quads = model.getQuads(block, d, rand, EmptyModelData.INSTANCE);
					if (quads.isEmpty()) continue;
					BakedQuad q = quads.get(0);
					int c = block == null ? -1 : colors.getColor(block, null, null, q.getTintIndex());
					faces[d.ordinal()] = new MicroBlockFace(q, i << 3, c);
					break;
				}
			return faces;
		} finally {
			setRenderLayer(old);
		}
	}

}