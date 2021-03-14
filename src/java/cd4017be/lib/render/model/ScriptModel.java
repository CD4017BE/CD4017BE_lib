package cd4017be.lib.render.model;

import java.io.*;
import java.util.*;
import java.util.function.Function;
import javax.script.ScriptException;

import com.google.gson.*;
import com.mojang.datafixers.util.Pair;

import cd4017be.lib.render.model.ScriptModelBuilder.Quad;
import cd4017be.lib.script.Script;
import cd4017be.lib.script.ScriptLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.model.*;
import net.minecraft.client.renderer.model.SimpleBakedModel.Builder;
import net.minecraft.client.renderer.texture.MissingTextureSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.IResource;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.TransformationMatrix;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.IModelConfiguration;
import net.minecraftforge.client.model.IModelLoader;
import net.minecraftforge.client.model.geometry.IModelGeometry;

/**A model that generates its quads with script code.<br>
 * JSON Format:<pre>
 * "parent": "parent/model",
 * "textures": {
 *     "particle": "texture:path",
 *     "script_tex_name": "texture:path",
 *     ...
 * },
 * "loader": "cd4017be_lib:rcp",
 * "code": "script code"
 * "code": ["script", "code"]
 * </pre> The code will be run with <b>ARG</b> set to the
 * {@link ScriptModelBuilder} and <b>LOAD</b> set to a loader for
 * other scripts from .rcp files in <code>assets/model/</code>.
 * Scripts loaded with <code>LOAD:"namespace:script/path"</code>
 * will also have their <b>LOAD</b> variable set for loading more
 * scripts themselves.
 * @author CD4017BE */
@OnlyIn(Dist.CLIENT)
public class ScriptModel implements IModelGeometry<ScriptModel> {

	private final Quad[] quads;

	public ScriptModel(List<Quad> list) {
		this.quads = list.toArray(new Quad[list.size()]);
	}

	@Override
	public IBakedModel bake(
		IModelConfiguration owner, ModelBakery bakery,
		Function<RenderMaterial, TextureAtlasSprite> spriteGetter,
		IModelTransform modelTransform, ItemOverrideList overrides,
		ResourceLocation modelLocation
	) {
		TransformationMatrix transf = modelTransform.getRotation();
		Function<String, TextureAtlasSprite> resolver
		= spriteGetter.compose(owner::resolveTexture);
		
		Builder b = new Builder(owner, overrides);
		for (Quad q : quads) q.bake(b, transf, resolver);
		return b.setTexture(resolver.apply("particle")).build();
	}

	@Override
	public Collection<RenderMaterial> getTextures(
		IModelConfiguration owner, Function<ResourceLocation, IUnbakedModel> modelGetter,
		Set<Pair<String, String>> missingTextureErrors
	) {
		HashSet<String> names = new HashSet<>();
		for (Quad q : quads) names.add(q.tex);
		names.add("particle");
		ArrayList<RenderMaterial> list = new ArrayList<>(names.size());
		for (String n : names) {
			RenderMaterial rm = owner.resolveTexture(n);
			if (rm.getTextureLocation().equals(MissingTextureSprite.getLocation()))
				missingTextureErrors.add(Pair.of(n, owner.getModelName()));
			else list.add(rm);
		}
		return list;
	}

	@OnlyIn(Dist.CLIENT)
	public static class Loader extends ScriptLoader implements IModelLoader<ScriptModel> {

		public static final Loader INSTANCE = new Loader();
		private IResourceManager manager = Minecraft.getInstance().getResourceManager();
		private ScriptModelBuilder builder = new ScriptModelBuilder();

		@Override
		public void onResourceManagerReload(IResourceManager resourceManager) {
			manager = resourceManager;
			scripts.clear();
		}

		@Override
		public ScriptModel read(JsonDeserializationContext context, JsonObject modelContents) {
			JsonElement e = modelContents.get("code");
			if (e == null) throw new IllegalStateException("missing element 'code'");
			Reader r = e.isJsonArray()
				? new JsonArrayLineReader(e.getAsJsonArray())
				: new StringReader(e.getAsString());
			return compileModel(r, "code");
		}

		public ScriptModel compileModel(Reader r, String name) {
			try {
				return new ScriptModel(builder.run(compile(name, r)));
			} catch(ScriptException | IOException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		protected Script load(String name) {
			ResourceLocation rl = new ResourceLocation(name);
			rl = new ResourceLocation(rl.getNamespace(), "models/" + rl.getPath() + ".rcp");
			try(IResource r = manager.getResource(rl)) {
				return compile(name, new InputStreamReader(r.getInputStream()));
			} catch(IOException | ScriptException e) {
				e.printStackTrace();
				return null;
			}
		}
	}

}
