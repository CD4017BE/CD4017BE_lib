package cd4017be.lib.render;

import static cd4017be.lib.Lib.rl;
import static cd4017be.math.Linalg.*;
import static cd4017be.math.Orient.*;

import java.util.*;

import com.mojang.blaze3d.matrix.MatrixStack.Entry;
import com.mojang.blaze3d.vertex.IVertexBuilder;

import cd4017be.api.grid.GridPart;
import cd4017be.api.grid.IGridHost;
import cd4017be.lib.render.model.JitBakedModel;
import cd4017be.lib.util.Orientation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.model.*;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.data.EmptyModelData;

/**
 * @author CD4017BE */
public class GridModels {

	private static final Random RAND = new Random();
	public static final ModelManager MODELS = Minecraft.getInstance().getModelManager();
	public static final ResourceLocation[] PORTS;
	static {
		ResourceLocation obj_p = rl("part/obj_p"), obj_u = rl("part/obj_u");
		PORTS = new ResourceLocation[] {
			rl("part/data_in"), rl("part/data_out"),
			rl("part/power_p"), rl("part/power_u"),
			rl("part/item_p"), rl("part/item_u"),
			rl("part/fluid_p"), rl("part/fluid_u"),
			obj_p, obj_u, obj_p, obj_u, obj_p, obj_u, obj_p, obj_u
		};
	}

	private static final int[] ORIENTS = {
		Orientation.W12.o, Orientation.E12.o,
		Orientation.DS.o, Orientation.UN.o,
		Orientation.N12.o, Orientation.S12.o
	};

	public static void drawPort(JitBakedModel model, short port, boolean master, long b, long opaque) {
		opaque |= b;
		int p = IGridHost.posOfport(port);
		int q = IGridHost.posOfport(port - 0x111);
		int o = Integer.numberOfTrailingZeros(0x111 & ~port) >> 1 & 6;
		if (o >= 6 || p >= 0 && q >= 0 && (opaque >> p & opaque >> q & 1) != 0) return;
		boolean outer = p < 0 || q < 0;
		if (p < 0 || (b >> p & 1) == 0) {
			if (q < 0 || (b >> q & 1) == 0) return;
			p = q;
			o |= 1;
		}
		o = ORIENTS[o];
		float[] vec = dadd(3, sca(3, vec(p & 3, p >> 2 & 3, p >> 4 & 3), .25F), -.375F);
		orient(inv(o), vec);
		origin(o, dadd(3, vec, .375F), 0.5F, 0.5F, 0.5F);
		addOriented(
			outer ? model.quads[orient(o, 3)] : model.inner(),
			MODELS.getModel(PORTS[port >> 11 & 14 | (master ? 1:0)]),
			null, o, vec
		);
	}

	public static void putCube(ResourceLocation key, JitBakedModel model, long b, long opaque, int ofs, int orient) {
		IBakedModel faces = MODELS.getModel(key);
		float[] v = originOf(orient, ofs);
		if ((b & ~opaque) != 0)
			addOriented(model.inner(), faces, null, orient, v);
		opaque = ~(opaque | b);
		for (int i = 0; i < 6; i++) {
			int j = orient(orient, i);
			ArrayList<BakedQuad> quads;
			if ((b & GridPart.FACES[j]) != 0) quads = model.quads[j];
			else {
				int s = GridPart.step(j);
				if ((((j & 1) != 0 ? b << s : b >>> s) & opaque) == 0) continue;
				quads = model.inner();
			}
			addOriented(quads, faces, Direction.from3DDataValue(i), orient, v);
		}
	}

	public static void addOriented(
		ArrayList<BakedQuad> dest, IBakedModel model, Direction face, int o, float[] v
	) {
		for (BakedQuad quad : model.getQuads(null, face, RAND, EmptyModelData.INSTANCE))
			dest.add(orient(o, quad, v));
	}

	private static float[] originOf(int orient, int ofs) {
		return origin(orient, sca(3, vec(ofs & 3, ofs >> 2 & 3, ofs >> 4 & 3), .25F), .5F, .5F, .5F);
	}

	private static final float _255 = 1F/255F;

	public static void draw(ResourceLocation key, Entry mat, IVertexBuilder vb, int color, int light, int overlay) {
		float r = (color >> 16 & 0xff) * _255,
		      g = (color >> 8  & 0xff) * _255,
		      b = (color       & 0xff) * _255,
		      a = (color >>> 24      ) * _255;
		for (BakedQuad quad : MODELS.getModel(key).getQuads(null, null, RAND, EmptyModelData.INSTANCE))
			vb.addVertexData(mat, quad, r, g, b, a, light, overlay);
	}

}
