package cd4017be.lib.render.model;

import java.util.Collection;

import java.util.function.Function;

import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.common.model.IModelState;

/**
 * 
 * @author CD4017BE
 */
public class ModelVariant implements IModel {

	private final IModel parent;
	private final IModelState variant;

	public ModelVariant(IModel model, IModelState state) {
		parent = model;
		variant = state;
	}

	@Override
	public Collection<ResourceLocation> getDependencies() {
		return parent.getDependencies();
	}

	@Override
	public Collection<ResourceLocation> getTextures() {
		return parent.getTextures();
	}

	@Override
	public IBakedModel bake(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
		return parent.bake(variant, format, bakedTextureGetter);
	}

	@Override
	public IModelState getDefaultState() {
		return variant;
	}

}
