package cd4017be.lib;

import static cd4017be.lib.Lib.CREATIVE_TAB;
import static cd4017be.lib.Lib.rl;
import static cd4017be.lib.block.BlockTE.flags;
import cd4017be.api.grid.GridPart;
import cd4017be.lib.block.BlockGrid;
import cd4017be.lib.block.BlockTE;
import cd4017be.lib.container.*;
import cd4017be.lib.item.*;
import cd4017be.lib.render.GridModels;
import cd4017be.lib.render.model.ScriptModel;
import cd4017be.lib.render.model.TileEntityModel;
import cd4017be.lib.render.te.GridTER;
import cd4017be.lib.text.TooltipEditor;
import cd4017be.lib.tileentity.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.Material;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.event.RegistryEvent.Register;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.ObjectHolder;

/**Registers all added game content.
 * Elements that belong to the same feature typically use the same registry name
 * so they are differentiated by case to avoid variable name conflicts:<br>
 * Block -> ALL_UPPERCASE<br>
 * Item -> all_lowercase<br>
 * TileEntity -> stored in {@link BlockTE#tileType}<br>
 * MenuType -> fIRST_LOWERCASE_REMAINING_UPPERCASE
 * @author CD4017BE */
@EventBusSubscriber(modid = Lib.ID, bus = Bus.MOD)
@ObjectHolder(value = Lib.ID)
public class Content {

	public static final BlockTE<EnergySupply> ENERGY_SUPP = null;
	public static final BlockTE<FluidSupply> FLUID_SUPP = null;
	public static final BlockTE<ItemSupply> ITEM_SUPP = null;
	public static final BlockTE<Assembler> ASSEMBLER = null;
	public static final BlockGrid GRID = null, GRID1 = null;

	public static final Item energy_supp = null, item_supp = null, fluid_supp = null, assembler = null;
	public static final GridHostItem grid = null;
	public static final MicroBlockItem microblock = null;

	/** alternate BlockEntityType for Grid to enable dynamic rendering */
	public static final BlockEntityType<Grid> GRID_TER = null;

	public static final MenuType<ContainerEnergySupply> eNERGY_SUPP = null;
	public static final MenuType<ContainerItemSupply> iTEM_SUPP = null;
	public static final MenuType<ContainerFluidSupply> fLUID_SUPP = null;
	public static final MenuType<ContainerAssembler> aSSEMBLER = null;
	public static final MenuType<ContainerGrid> gRID = null;

	@SubscribeEvent
	public static void registerBlocks(Register<Block> ev) {
		Properties pc = Properties.of(Material.METAL)
		.strength(-1F, Float.POSITIVE_INFINITY)
		.noDrops().sound(SoundType.ANVIL);
		Properties p = Properties.of(Material.STONE).strength(1.5F);
		ev.getRegistry().registerAll(
			new BlockTE<>(pc, flags(EnergySupply.class)).setRegistryName(rl("energy_supp")),
			new BlockTE<>(pc, flags(FluidSupply.class)).setRegistryName(rl("fluid_supp")),
			new BlockTE<>(pc, flags(ItemSupply.class)).setRegistryName(rl("item_supp")),
			new BlockTE<>(p, flags(Assembler.class)).setRegistryName(rl("assembler")),
			new BlockGrid(
				Properties.of(Material.STONE).strength(1.25F).noOcclusion().dynamicShape()
			).setRegistryName(rl("grid")),
			new BlockGrid(
				Properties.of(Material.STONE).strength(1.5F).dynamicShape()
			).setRegistryName(rl("grid1"))
		);
	}

	@SubscribeEvent
	public static void registerItems(Register<Item> ev) {
		Item.Properties pc = new Item.Properties().tab(CREATIVE_TAB).rarity(Rarity.EPIC),
		p = new Item.Properties().tab(CREATIVE_TAB);
		ev.getRegistry().registerAll(
			new DocumentedBlockItem(ENERGY_SUPP, pc),
			new DocumentedBlockItem(FLUID_SUPP, pc),
			new DocumentedBlockItem(ITEM_SUPP, pc),
			new DocumentedBlockItem(ASSEMBLER, p),
			new GridHostItem(GRID, p),
			new MicroBlockItem(p).setRegistryName(rl("microblock"))
		);
	}

	@SubscribeEvent
	public static void registerTileEntities(Register<BlockEntityType<?>> ev) {
		ev.getRegistry().registerAll(
			ENERGY_SUPP.makeTEType(EnergySupply::new),
			FLUID_SUPP.makeTEType(FluidSupply::new),
			ITEM_SUPP.makeTEType(ItemSupply::new),
			ASSEMBLER.makeTEType(Assembler::new),
			BlockTE.makeTEType(Grid::new, GRID, GRID1),
			BlockEntityType.Builder.of((pos, state) -> new Grid(GRID_TER, pos, state), GRID, GRID1)
				.build(null).setRegistryName(rl("grid_ter"))
		);
		GridPart.GRID_HOST_BLOCK = GRID.defaultBlockState();
	}

	@SubscribeEvent
	public static void registerContainers(Register<MenuType<?>> ev) {
		ev.getRegistry().registerAll(
			IForgeContainerType.create(ContainerEnergySupply::new).setRegistryName(rl("energy_supp")),
			IForgeContainerType.create(ContainerItemSupply::new).setRegistryName(rl("item_supp")),
			IForgeContainerType.create(ContainerFluidSupply::new).setRegistryName(rl("fluid_supp")),
			IForgeContainerType.create(ContainerAssembler::new).setRegistryName(rl("assembler")),
			IForgeContainerType.create(ContainerGrid::new).setRegistryName(rl("grid"))
		);
	}

	@SubscribeEvent
	@OnlyIn(Dist.CLIENT)
	public static void setupClient(FMLClientSetupEvent ev) {
		TooltipEditor.init();
		MenuScreens.register(eNERGY_SUPP, ContainerEnergySupply::setupGui);
		MenuScreens.register(iTEM_SUPP, ContainerItemSupply::setupGui);
		MenuScreens.register(fLUID_SUPP, ContainerFluidSupply::setupGui);
		MenuScreens.register(aSSEMBLER, ContainerAssembler::setupGui);
		MenuScreens.register(gRID, ContainerGrid::setupGui);
		//ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.CONFIGGUIFACTORY, Lib.CFG_CLIENT);
		BlockEntityRenderers.register(GRID_TER, GridTER::new);
	}

	@SubscribeEvent
	@OnlyIn(Dist.CLIENT)
	public static void registerModels(ModelRegistryEvent ev) {
		ModelLoaderRegistry.registerLoader(rl("te"), TileEntityModel.Loader.INSTANCE);
		ModelLoaderRegistry.registerLoader(rl("rcp"), ScriptModel.Loader.INSTANCE);
		for (ResourceLocation loc : GridModels.PORTS)
			ModelLoader.addSpecialModel(loc);
	}

}
