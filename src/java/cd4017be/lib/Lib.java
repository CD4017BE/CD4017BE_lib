package cd4017be.lib;

import static net.minecraftforge.common.MinecraftForge.EVENT_BUS;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cd4017be.api.grid.Link;
import cd4017be.lib.config.LibClient;
import cd4017be.lib.config.LibCommon;
import cd4017be.lib.config.LibServer;
import cd4017be.lib.network.GuiNetworkHandler;
import cd4017be.lib.network.SyncNetworkHandler;
import cd4017be.lib.text.TooltipUtil;
import cd4017be.lib.tick.GateUpdater;
import cd4017be.lib.tileentity.SyncTileEntity;
import net.minecraft.item.*;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * @author CD4017BE */
@Mod(Lib.ID)
public class Lib {

	public static final String ID = "cd4017be_lib";
	/**whether we are in a modding development environment */
	public static final boolean DEV_DEBUG = !FMLLoader.isProduction();
	public static final Logger LOG = LogManager.getLogger(ID);
	public static final LibClient CFG_CLIENT = new LibClient();
	public static final LibCommon CFG_COMMON = new LibCommon();
	public static final LibServer CFG_SERVER = new LibServer();

	public static final ItemGroup CREATIVE_TAB = new ItemGroup(ID) {
		@Override
		public ItemStack makeIcon() {
			return new ItemStack(Content.assembler);
		}
	};

	public Lib() {
		FMLJavaModLoadingContext.get().getModEventBus().register(this);
		CFG_CLIENT.register(ID);
		CFG_COMMON.register(ID);
		CFG_SERVER.register(ID);
		EVENT_BUS.addListener(this::shutdown);
		EVENT_BUS.register(GateUpdater.class);
	}

	@SubscribeEvent
	void setup(FMLCommonSetupEvent event) {
		GuiNetworkHandler.register();
		SyncNetworkHandler.register();
	}

	@SubscribeEvent
	void onConfigLoad(ModConfigEvent event) {
		if (!event.getConfig().getModId().equals(ID)) return;
		double d = CFG_SERVER.clientSyncDst.get();
		SyncTileEntity.CLIENT_RANGE = d * d;
		d += CFG_SERVER.serverSyncDst.get();
		SyncTileEntity.SERVER_RANGE = d * d;
	}

	void shutdown(FMLServerStoppedEvent event) {
		Link.clear();
		if (TooltipUtil.editor != null)
			TooltipUtil.editor.save();
	}

	public static ResourceLocation rl(String path) {
		return new ResourceLocation(ID, path);
	}

}
