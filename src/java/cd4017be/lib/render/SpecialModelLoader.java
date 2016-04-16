package cd4017be.lib.render;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.statemap.IStateMapper;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelFluid;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.fluids.Fluid;

public class SpecialModelLoader implements ICustomModelLoader {

	public static final SpecialModelLoader instance = new SpecialModelLoader();
	public static final StateMapper stateMapper = new StateMapper();
	private static String mod = "";
	
	public static void setMod(String name) {
		mod = name;
		instance.mods.add(name);
	}
	
	public static void registerFluid(Fluid fluid) {
		ModelFluid model = new ModelFluid(fluid);
		instance.models.put(new ResourceLocation(mod, "models/block/" + fluid.getName()), model);
		ModelLoader.setCustomStateMapper(fluid.getBlock(), stateMapper);
	}
	
	public static void registerBlockModel(Block block, IModel model) {
		String[] name = block.getRegistryName().split(":");
		instance.models.put(new ResourceLocation(name[0], "models/block/" + name[1]), model);
		//ModelLoader.setCustomStateMapper(block, stateMapper);
	}
	
	public HashMap<ResourceLocation, IModel> models = new HashMap<ResourceLocation, IModel>();
	public HashSet<String> mods = new HashSet<String>();
	
	private SpecialModelLoader() {
		ModelLoaderRegistry.registerLoader(this);
	}
	
	@Override
	public void onResourceManagerReload(IResourceManager resourceManager) {}

	@Override
	public boolean accepts(ResourceLocation modelLocation) {
		return mods.contains(modelLocation.getResourceDomain()) && 
				models.containsKey(modelLocation);
	}

	@Override
	public IModel loadModel(ResourceLocation modelLocation) throws IOException {
		return models.get(modelLocation);
	}
	
	public static class StateMapper implements IStateMapper {
		@Override
		public Map<IBlockState, ModelResourceLocation> putStateModelLocations(Block block) {
			HashMap<IBlockState, ModelResourceLocation> map = new HashMap<IBlockState, ModelResourceLocation>();
			ModelResourceLocation loc = new ModelResourceLocation(block.getRegistryName());
			for (IBlockState state : block.getBlockState().getValidStates()) map.put(state, loc);
			return map;
		}
	}
}
