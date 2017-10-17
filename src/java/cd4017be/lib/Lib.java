package cd4017be.lib;

import cd4017be.api.Capabilities;
import cd4017be.api.computers.ComputerAPI;
import cd4017be.api.energy.EnergyAPI;
import cd4017be.api.recipes.RecipeScriptContext;
import cd4017be.lib.item.ItemMaterial;
import cd4017be.lib.render.ItemMaterialMeshDefinition;
import cd4017be.lib.templates.NBTRecipe;
import cd4017be.lib.templates.TabMaterials;
import cd4017be.lib.util.FileUtil;
import cd4017be.lib.util.TooltipUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.RecipeSorter;

/**
 * 
 * @author CD4017BE
 */
@Mod(modid = Lib.ID, useMetadata = true)
public class Lib {

	public static final String ID = "cd4017be_lib";

	@Instance
	public static Lib instance;

	public static ItemMaterial materials;

	public static final TabMaterials creativeTab = new TabMaterials(ID);

	public Lib() {
		MinecraftForge.EVENT_BUS.register(this);
	}

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		FileUtil.initConfigDir(event);
		BlockGuiHandler.register();
		Capabilities.register();
		EnergyAPI.init();
		(materials = new ItemMaterial("m")).setCreativeTab(creativeTab);
		creativeTab.item = new ItemStack(materials);
		RecipeSorter.register(ID + ":shapedNBT", NBTRecipe.class, RecipeSorter.Category.SHAPED, "after:minecraft:shaped before:minecraft:shapeless");
		RecipeScriptContext.instance = new RecipeScriptContext();
		RecipeScriptContext.instance.setup();
		RecipeScriptContext.instance.run("core.PRE_INIT");
	}

	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {
		ComputerAPI.register();
		RecipeScriptContext.instance.runAll("INIT");
	}

	@Mod.EventHandler
	public void postInit(FMLPostInitializationEvent event) {
		RecipeScriptContext.instance.runAll("POST_INIT");
		TooltipUtil.addScriptVariables();
		if (event.getSide().isClient()) clientPostInit();
	}

	@SideOnly(Side.CLIENT)
	private static void clientPostInit() {
		BlockItemRegistry.registerRender(materials);
		BlockItemRegistry.registerRender(materials, new ItemMaterialMeshDefinition(materials));
	}

	@Mod.EventHandler
	public void afterStart(FMLServerAboutToStartEvent event) {
		//trash stuff that's not needed anymore
		RecipeScriptContext.instance = null;
		System.gc();
	}

	@SubscribeEvent
	public void registerItems(RegistryEvent.Register<Item> ev) {
		ev.getRegistry().register(materials);
	}

}
