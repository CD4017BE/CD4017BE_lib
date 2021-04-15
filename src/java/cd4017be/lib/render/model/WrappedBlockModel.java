package cd4017be.lib.render.model;

import java.util.*;

import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockModelShapes;
import net.minecraft.client.renderer.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.data.IModelData;

/**
 * @author CD4017BE */
@OnlyIn(Dist.CLIENT)
public class WrappedBlockModel implements IBakedModel {

	public static final BlockModelShapes MODELS
	= Minecraft.getInstance().getBlockRendererDispatcher().getBlockModelShapes();

	public final IBakedModel model;
	public final BlockState state;
	public IModelData data = EmptyModelData.INSTANCE;

	public WrappedBlockModel(IBakedModel model, BlockState state) {
		this.model = model;
		this.state = state;
	}

	public WrappedBlockModel(BlockState state) {
		this(MODELS.getModel(state), state);
	}

	public List<BakedQuad> getQuads(Direction side, Random rand) {
		return model.getQuads(state, side, rand, data);
	}

	public void updateData(IBlockDisplayReader world, BlockPos pos) {
		data = model.getModelData(world, pos, state, data);
	}

	@Override
	public List<BakedQuad> getQuads(BlockState state, Direction side, Random rand) {
		return model.getQuads(this.state, side, rand, this.data);
	}

	@Override
	public boolean isAmbientOcclusion() {
		return model.isAmbientOcclusion();
	}

	@Override
	public boolean isGui3d() {
		return model.isGui3d();
	}

	@Override
	public boolean isSideLit() {
		return model.isSideLit();
	}

	@Override
	public boolean isBuiltInRenderer() {
		return model.isBuiltInRenderer();
	}

	@Override
	public TextureAtlasSprite getParticleTexture() {
		return model.getParticleTexture(data);
	}

	@Override
	public ItemOverrideList getOverrides() {
		return model.getOverrides();
	}

	@Override
	public IModelData getModelData(
		IBlockDisplayReader world, BlockPos pos, BlockState state, IModelData tileData
	) {
		this.data = model.getModelData(world, pos, this.state, this.data);
		return tileData;
	}

}
