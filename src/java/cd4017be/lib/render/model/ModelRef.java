package cd4017be.lib.render.model;

import java.util.Collection;
import java.util.Set;
import java.util.function.Function;

import com.mojang.datafixers.util.Pair;

import net.minecraft.client.renderer.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModelConfiguration;
import net.minecraftforge.client.model.geometry.IModelGeometry;


public class ModelRef implements IModelGeometry<ModelRef> {

	final ResourceLocation loc;

	public ModelRef(String loc) {
		this.loc = new ResourceLocation(loc);
	}

	@Override
	public IBakedModel bake(
		IModelConfiguration owner, ModelBakery bakery,
		Function<RenderMaterial, TextureAtlasSprite> spriteGetter,
		IModelTransform modelTransform, ItemOverrideList overrides,
		ResourceLocation modelLocation
	) {
		return bakery.getBakedModel(loc, modelTransform, spriteGetter);
	}

	@Override
	public Collection<RenderMaterial> getTextures(
		IModelConfiguration owner, Function<ResourceLocation, IUnbakedModel> modelGetter,
		Set<Pair<String, String>> missingTextureErrors
	) {
		return modelGetter.apply(loc).getMaterials(modelGetter, missingTextureErrors);
	}

}
