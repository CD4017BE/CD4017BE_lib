package cd4017be.lib.render.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.BufferBuilder;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelProperty;

/**A model that is baked "just in time" by for example
 * a TileEntity to render special static graphics.
 * @see TileEntityModel
 * @author CD4017BE */
public class JitBakedModel implements BakedModel {

	public static final ModelProperty<BakedModel> JIT_BAKED_MODEL = new ModelProperty<>();
	public static final int INNER = 6, AOC = 1, GUI3D = 2, LIT = 3;

	public static JitBakedModel make(IModelData data) {
		JitBakedModel jm = new JitBakedModel(0);
		data.setData(JIT_BAKED_MODEL, jm);
		return jm;
	}

	@SuppressWarnings("unchecked")
	public final ArrayList<BakedQuad>[] quads = new ArrayList[] {
		new ArrayList<BakedQuad>(), new ArrayList<BakedQuad>(),
		new ArrayList<BakedQuad>(), new ArrayList<BakedQuad>(),
		new ArrayList<BakedQuad>(), new ArrayList<BakedQuad>(),
		new ArrayList<BakedQuad>()
	};
	public TextureAtlasSprite particle;
	private final byte mode;

	public JitBakedModel(int mode) {
		this.mode = (byte)mode;
	}

	public JitBakedModel clear() {
		for (ArrayList<?> q : quads) q.clear();
		return this;
	}

	public ArrayList<BakedQuad> inner() {
		return quads[INNER];
	}

	@Override
	public List<BakedQuad> getQuads(BlockState state, Direction side, Random rand) {
		return quads[side == null ? INNER : side.ordinal()];
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
	public ItemOverrides getOverrides() {
		return ItemOverrides.EMPTY;
	}

	public void render(BufferBuilder vb, Pose mat, int light, int overlayColor) {
		for (ArrayList<BakedQuad> quads : this.quads)
			for (BakedQuad quad : quads)
				vb.putBulkData(mat, quad, 1, 1, 1, light, overlayColor, true);
	}

}
