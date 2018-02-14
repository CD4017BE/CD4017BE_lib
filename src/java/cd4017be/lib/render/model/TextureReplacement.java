package cd4017be.lib.render.model;

import java.util.ArrayList;
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
 * @author cd4017be
 */
public class TextureReplacement implements IModel {

	public final IModel parent;
	public final ResourceLocation texture;
	private ResourceLocation toReplace;

	public TextureReplacement(IModel parent, ResourceLocation texture) {
		this.parent = parent;
		this.texture = texture;
	}

	@Override
	public Collection<ResourceLocation> getDependencies() {
		return parent.getDependencies();
	}

	@Override
	public Collection<ResourceLocation> getTextures() {
		ArrayList<ResourceLocation> list = new ArrayList<ResourceLocation>(parent.getTextures());
		if (list.isEmpty()) list.add(texture);
		else toReplace = list.set(0, texture);
		return list;
	}

	@Override
	public IBakedModel bake(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
		return parent.bake(state, format, (res) -> bakedTextureGetter.apply(res.equals(toReplace) ? texture : res));
	}

	@Override
	public IModelState getDefaultState() {
		return parent.getDefaultState();
	}

}
