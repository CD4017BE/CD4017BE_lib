package cd4017be.lib;

import cd4017be.api.Capabilities;
import cd4017be.api.computers.ComputerAPI;
import cd4017be.api.recipes.RecipeAPI;
import cd4017be.api.recipes.RecipeScriptParser;
import cd4017be.lib.render.ItemMaterialMeshDefinition;
import cd4017be.lib.templates.ItemMaterial;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;

@Mod(modid = "CD4017BE_lib", useMetadata = true)
public class Lib {
	@Instance
	public static Lib instance = new Lib();

	public static ItemMaterial materials;

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		Capabilities.register();
		materials = new ItemMaterial("m");
		RecipeAPI.registerScript(event, "core", null);
	}

	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {
		ComputerAPI.register();
		RecipeAPI.executeScripts(RecipeAPI.INIT);
	}

	@Mod.EventHandler
	public void postInit(FMLPostInitializationEvent event) {
		RecipeAPI.executeScripts(RecipeAPI.POST_INIT);
		if (event.getSide().isClient()) {
			BlockItemRegistry.registerRender(materials, new ItemMaterialMeshDefinition(materials));
		}
	}

	@Mod.EventHandler
	public void afterStart(FMLServerAboutToStartEvent event) {
		//remove unnecessary stuff from memory that was cached for other mods during loading phase.
		RecipeAPI.cache.clear();
		RecipeScriptParser.codeCache.clear();
		System.gc();//Why not
	}
}
