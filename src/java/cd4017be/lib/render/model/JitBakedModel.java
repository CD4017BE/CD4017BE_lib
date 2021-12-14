package cd4017be.lib.render.model;

import java.util.*;

import com.mojang.blaze3d.matrix.MatrixStack.Entry;
import com.mojang.blaze3d.vertex.IVertexBuilder;

import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Direction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.*;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelProperty;

/**A model that is baked "just in time" by for example
 * a TileEntity to render special static graphics.
 * @see TileEntityModel
 * @author CD4017BE */
@OnlyIn(Dist.CLIENT)
public class JitBakedModel implements IBakedModel {

	public static final ModelProperty<IBakedModel> JIT_BAKED_MODEL = new ModelProperty<>();
	public static final int INNER = 6, AOC = 1, GUI3D = 2, LIT = 3, LAYERED = 4;
	public static final int L_SOLID = 0, L_CUTOUT = 8, L_CUTMIP = 16, L_TRANSP = 24;
	public static final RenderType[] LAYERS = {
		RenderType.solid(), RenderType.cutout(),
		RenderType.cutoutMipped(), RenderType.translucent()
	};

	public static JitBakedModel make(IModelData data, int mode) {
		JitBakedModel jm = new JitBakedModel(mode);
		data.setData(JIT_BAKED_MODEL, jm);
		return jm;
	}

	public static JitBakedModel make(IModelData data) {
		return make(data, 0);
	}

	public static int renderTypeIdx(RenderType t) {
		for(int i = 0; i < LAYERS.length; i++)
			if(LAYERS[i] == t) return i << 3;
		return 0;
	}

	/** {0..5: cull-face quads, 6: inner quads, 7: padding} x
	 * {{@link #L_SOLID}, {@link #L_CUTOUT}, {@link #L_CUTMIP}, {@link #L_TRANSP}} <br>
	 * To save memory, the ArrayLists are lazily allocated. */
	@SuppressWarnings("unchecked")
	private final ArrayList<BakedQuad>[] quads = new ArrayList[32];
	public TextureAtlasSprite particle;
	private final byte mode;

	public JitBakedModel(int mode) {
		this.mode = (byte)mode;
	}

	public JitBakedModel clear() {
		for (ArrayList<?> q : quads)
			if (q != null) q.clear();
		return this;
	}

	public ArrayList<BakedQuad> inner() {
		return quads(INNER);
	}

	public ArrayList<BakedQuad> inner(int layer) {
		return quads(INNER | layer);
	}

	public ArrayList<BakedQuad> face(Direction side, int layer) {
		return quads(side.ordinal() | layer);
	}

	public ArrayList<BakedQuad> quads(int i) {
		ArrayList<BakedQuad> fq = quads[i];
		return fq != null ? fq : (quads[i] = new ArrayList<>());
	}

	private List<BakedQuad> getQuads(int i) {
		return quads[i] == null ? Collections.emptyList() : quads[i];
	}

	@Override
	public List<BakedQuad> getQuads(BlockState state, Direction side, Random rand) {
		int s = side == null ? INNER : side.ordinal();
		if ((mode & LAYERED) == 0) return getQuads(s);
		RenderType t = MinecraftForgeClient.getRenderLayer();
		if (t != null) return getQuads(renderTypeIdx(t) | s);
		List<BakedQuad> q = null;
		for (boolean mod = false; s < quads.length; s+=8)
			if (q == null) q = quads[s];
			else if (quads[s] != null) {
				(mod ? q : (q = new ArrayList<>(q))).addAll(quads[s]);
				mod = true;
			}
		return q == null ? Collections.emptyList() : q;
	}

	@Override
	public boolean useAmbientOcclusion() {
		return (mode & AOC) != 0;
	}

	@Override
	public boolean isGui3d() {
		return (mode & GUI3D) != 0;
	}

	@Override
	public boolean usesBlockLight() {
		return (mode & LIT) != 0;
	}

	@Override
	public boolean isCustomRenderer() {
		return false;
	}

	@Override
	public TextureAtlasSprite getParticleIcon() {
		return particle;
	}

	@Override
	public ItemOverrideList getOverrides() {
		return ItemOverrideList.EMPTY;
	}

	public void render(IRenderTypeBuffer rtb, Entry mat, int light, int overlayColor) {
		for (int i = 0; i < LAYERS.length; i++) {
			IVertexBuilder vb = rtb.getBuffer(LAYERS[i]);
			for (int j = 0; j <= INNER; j++) {
				ArrayList<BakedQuad> q = quads[i << 3 | j];
				if (q == null) continue;
				for (BakedQuad quad : q)
					vb.addVertexData(mat, quad, 1, 1, 1, light, overlayColor, true);
			}
		}
	}

}
