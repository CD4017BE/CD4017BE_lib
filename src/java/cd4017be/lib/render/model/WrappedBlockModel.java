package cd4017be.lib.render.model;

import java.util.*;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.data.IModelData;

/**
 * @author CD4017BE */
@OnlyIn(Dist.CLIENT)
public class WrappedBlockModel implements BakedModel {

	public static final BlockModelShaper MODELS
	= Minecraft.getInstance().getBlockRenderer().getBlockModelShaper();

	public final BakedModel model;
	public final BlockState state;
	public IModelData data = EmptyModelData.INSTANCE;

	public WrappedBlockModel(BakedModel model, BlockState state) {
		this.model = model;
		this.state = state;
	}

	public WrappedBlockModel(BlockState state) {
		this(MODELS.getBlockModel(state), state);
	}

	public List<BakedQuad> getQuads(Direction side, Random rand) {
		return model.getQuads(state, side, rand, data);
	}

	public void updateData(BlockAndTintGetter world, BlockPos pos) {
		data = model.getModelData(world, pos, state, data);
	}

	@Override
	public List<BakedQuad> getQuads(BlockState state, Direction side, Random rand) {
		return model.getQuads(this.state, side, rand, this.data);
	}

	@Override
	public boolean useAmbientOcclusion() {
		return model.useAmbientOcclusion();
	}

	@Override
	public boolean isGui3d() {
		return model.isGui3d();
	}

	@Override
	public boolean usesBlockLight() {
		return model.usesBlockLight();
	}

	@Override
	public boolean isCustomRenderer() {
		return model.isCustomRenderer();
	}

	@Override
	public TextureAtlasSprite getParticleIcon() {
		return model.getParticleIcon(data);
	}

	@Override
	public ItemOverrides getOverrides() {
		return model.getOverrides();
	}

	@Override
	public IModelData getModelData(
		BlockAndTintGetter world, BlockPos pos, BlockState state, IModelData tileData
	) {
		this.data = model.getModelData(world, pos, this.state, this.data);
		return tileData;
	}

}
