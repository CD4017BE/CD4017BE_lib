package cd4017be.lib.render.model;

import java.util.Collection;
import java.util.Set;
import java.util.function.Function;

import com.mojang.datafixers.util.Pair;

import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.IModelConfiguration;
import net.minecraftforge.client.model.geometry.IModelGeometry;


public class ModelRef implements IModelGeometry<ModelRef> {

	final ResourceLocation loc;

	public ModelRef(String loc) {
		this.loc = new ResourceLocation(loc);
	}

	@Override
	public BakedModel bake(
		IModelConfiguration owner, ModelBakery bakery,
		Function<Material, TextureAtlasSprite> spriteGetter,
		ModelState modelTransform, ItemOverrides overrides,
		ResourceLocation modelLocation
	) {
		return bakery.bake(loc, modelTransform, spriteGetter);
	}

	@Override
	public Collection<Material> getTextures(
		IModelConfiguration owner, Function<ResourceLocation, UnbakedModel> modelGetter,
		Set<Pair<String, String>> missingTextureErrors
	) {
		return modelGetter.apply(loc).getMaterials(modelGetter, missingTextureErrors);
	}

}
