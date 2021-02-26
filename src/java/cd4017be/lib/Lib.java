package cd4017be.lib;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import cd4017be.lib.block.AdvancedBlock;
import cd4017be.lib.config.LibClient;
import cd4017be.lib.container.test.ContainerEnergySupply;
import cd4017be.lib.container.test.ContainerFluidSupply;
import cd4017be.lib.container.test.ContainerItemSupply;
import cd4017be.lib.item.*;
import cd4017be.lib.network.GuiNetworkHandler;
import cd4017be.lib.text.TooltipEditor;
import cd4017be.lib.text.TooltipUtil;
import cd4017be.lib.tileentity.test.EnergySupply;
import cd4017be.lib.tileentity.test.FluidSupply;
import cd4017be.lib.tileentity.test.ItemSupply;
import net.minecraft.block.AbstractBlock.Properties;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.*;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.event.RegistryEvent.Register;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 
 * @author CD4017BE
 */
@Mod(Lib.ID)
public class Lib {

	public static final String ID = "cd4017be_lib";
	/**whether we are in a modding development environment */
	public static final boolean DEV_DEBUG = !FMLLoader.isProduction();
	public static final Logger LOG = LogManager.getLogger(ID);
	public static final LibClient CFG_CLIENT = new LibClient();

	public static AdvancedBlock<EnergySupply> ENERGY_SUPP;
	public static AdvancedBlock<FluidSupply> FLUID_SUPP;
	public static AdvancedBlock<ItemSupply> ITEM_SUPP;
	public static Item energy_supp, item_supp, fluid_supp;
	public static TileEntityType<EnergySupply> T_ENERGY_SUPP;
	public static TileEntityType<FluidSupply> T_FLUID_SUPP;
	public static TileEntityType<ItemSupply> T_ITEM_SUPP;
	public static ContainerType<ContainerEnergySupply> C_ENERGY_SUPP;
	public static ContainerType<ContainerItemSupply> C_ITEM_SUPP;
	public static ContainerType<ContainerFluidSupply> C_FLUID_SUPP;
	public static ItemMaterial materials;
	public static DocumentedItem rrwi;

	public static final ItemGroup creativeTab = new ItemGroup(ID) {
		@Override
		public ItemStack createIcon() {
			return new ItemStack(rrwi);
		}
	};

	public Lib() {
		
		FMLJavaModLoadingContext.get().getModEventBus().register(this);
		CFG_CLIENT.register("lib");
		MinecraftForge.EVENT_BUS.addListener(this::shutdown);
	}

	@SubscribeEvent
	void setup(FMLCommonSetupEvent event) {
		
		GuiNetworkHandler.register();
		/*
		FileUtil.initConfigDir(event);
		RecipeSorter.register(ID + ":shapedNBT", NBTRecipe.class, RecipeSorter.Category.SHAPED, "after:minecraft:shaped before:minecraft:shapeless");
		RecipeScriptContext.instance = new RecipeScriptContext(LOG);
		RecipeScriptContext.instance.setup();
		RecipeScriptContext.instance.run(ConfigName + ".PRE_INIT");
		ConfigConstants cfg = new ConfigConstants(RecipeScriptContext.instance.modules.get(ConfigName));
		SyncNetworkHandler.register(cfg);
		EnergyAPI.init(cfg);
		ComputerAPI.register();
		*/
	}

	void shutdown(FMLServerStoppingEvent event) {
		TickRegistry.instance.clear();
		if (TooltipUtil.editor != null)
			TooltipUtil.editor.save();
	}

	@SubscribeEvent
	void registerBlocks(Register<Block> ev) {
		Properties p = Properties.create(Material.IRON).hardnessAndResistance(-1F, Float.POSITIVE_INFINITY).noDrops().sound(SoundType.ANVIL);
		ev.getRegistry().registerAll(
			ENERGY_SUPP = new AdvancedBlock<>("energy_supp", p, 0, EnergySupply.class),
			FLUID_SUPP = new AdvancedBlock<>("fluid_supp", p, 0, FluidSupply.class),
			ITEM_SUPP = new AdvancedBlock<>("item_supp", p, 0, ItemSupply.class)
		);
	}

	@SubscribeEvent
	void registerItems(Register<Item> ev) {
		Item.Properties p = new Item.Properties().group(creativeTab).rarity(Rarity.EPIC),
		p1 = new Item.Properties().group(creativeTab);
		ev.getRegistry().registerAll(
			energy_supp = new DocumentedBlockItem(ENERGY_SUPP, p),
			fluid_supp = new DocumentedBlockItem(FLUID_SUPP, p),
			item_supp = new DocumentedBlockItem(ITEM_SUPP, p),
			rrwi = new DocumentedItem("rrwi", p1)
		);
	}

	@SubscribeEvent
	void registerTileEntities(Register<TileEntityType<?>> ev) {
		ev.getRegistry().registerAll(
			T_ENERGY_SUPP = ENERGY_SUPP.createTileEntityType(),
			T_FLUID_SUPP = FLUID_SUPP.createTileEntityType(),
			T_ITEM_SUPP = ITEM_SUPP.createTileEntityType()
		);
	}

	@SubscribeEvent
	void registerContainers(Register<ContainerType<?>> ev) {
		ev.getRegistry().registerAll(
			(C_ENERGY_SUPP = IForgeContainerType.create(ContainerEnergySupply::new)).setRegistryName(rl("energy_supp")),
			(C_ITEM_SUPP = IForgeContainerType.create(ContainerItemSupply::new)).setRegistryName(rl("item_supp")),
			(C_FLUID_SUPP = IForgeContainerType.create(ContainerFluidSupply::new)).setRegistryName(rl("fluid_supp"))
		);
	}

	@SubscribeEvent
	@OnlyIn(Dist.CLIENT)
	void setupClient(FMLClientSetupEvent ev) {
		TooltipEditor.init();
		ScreenManager.registerFactory(C_ENERGY_SUPP, ContainerEnergySupply::setupGui);
		ScreenManager.registerFactory(C_ITEM_SUPP, ContainerItemSupply::setupGui);
		ScreenManager.registerFactory(C_FLUID_SUPP, ContainerFluidSupply::setupGui);
	}

	@OnlyIn(Dist.CLIENT)
	public void registerMaterialModels(ModelRegistryEvent ev) {
		
	}

	public static ResourceLocation rl(String path) {
		return new ResourceLocation(ID, path);
	}

}
