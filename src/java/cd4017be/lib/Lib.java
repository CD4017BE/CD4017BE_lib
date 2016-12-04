package cd4017be.lib;

import cd4017be.api.Capabilities;
import cd4017be.api.computers.ComputerAPI;
import cd4017be.api.energy.EnergyAPI;
import cd4017be.api.recipes.RecipeAPI;
import cd4017be.api.recipes.RecipeScriptParser;
import cd4017be.lib.render.ItemMaterialMeshDefinition;
import cd4017be.lib.templates.ItemMaterial;
import cd4017be.lib.templates.TabMaterials;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.RecipeSorter;

@Mod(modid = "CD4017BE_lib", useMetadata = true)
public class Lib {
	@Instance
	public static Lib instance = new Lib();

	public static ItemMaterial materials;
	public static TabMaterials creativeTab;

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		Capabilities.register();
		EnergyAPI.init();
		RecipeSorter.register("cd4017be_lib:shapedNBT", NBTRecipe.class, RecipeSorter.Category.SHAPED, "after:minecraft:shaped before:minecraft:shapeless");
		creativeTab = new TabMaterials("cd4017be_lib");
		(materials = new ItemMaterial("m")).setCreativeTab(creativeTab);
		creativeTab.item = new ItemStack(materials);
		RecipeAPI.registerScript(event, "core.rcp", null);
	}

	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {
		ComputerAPI.register();
		RecipeAPI.executeScripts(RecipeAPI.INIT);
	}

	@Mod.EventHandler
	public void postInit(FMLPostInitializationEvent event) {
		RecipeAPI.executeScripts(RecipeAPI.POST_INIT);
		if (event.getSide().isClient()) clientPostInit();
	}

	@SideOnly(Side.CLIENT)
	private static void clientPostInit() {
		BlockItemRegistry.registerRender(materials);
		BlockItemRegistry.registerRender(materials, new ItemMaterialMeshDefinition(materials));
	}

	@Mod.EventHandler
	public void afterStart(FMLServerAboutToStartEvent event) {
		//remove unnecessary stuff from memory that was cached for other mods during loading phase.
		RecipeAPI.cache.clear();
		RecipeScriptParser.codeCache.clear();
		System.gc();//Why not
	}
}
