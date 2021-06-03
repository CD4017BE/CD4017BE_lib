package cd4017be.lib.config;

import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.fml.config.ModConfig.Type;

/**@author CD4017BE */
public class LibServer extends ModConfig {

	public final BooleanValue canCutBlocks, canReplicate, fakePlayerAdvancements;
	public final DoubleValue serverSyncDst, clientSyncDst;

	public LibServer() {
		super(Type.SERVER);
		Builder b = new Builder();
		b.push("assembler");
		canCutBlocks = b
		.comment("Enables the Assembler's Block cutting feature that allows building with Block Bits.")
		.define("block_cutting", true);
		canReplicate = b
		.comment("Enables the Assembler's Microblock Structure replication feature.")
		.define("replicating", true);
		b.pop();
		clientSyncDst = b
		.comment("Distance below which players start receiving packets for fast TileEntity changes.")
		.defineInRange("min_sync_radius", 16.0, 1.0, 64.0);
		serverSyncDst = b
		.comment("Added on top of min_sync_radius to define the distance at which players stop receiving packets.")
		.defineInRange("ext_sync_radius", 4.0, 0.0, 64.0);
		
		fakePlayerAdvancements = b
		.comment("Whether Fake-Players can trigger advancements (should be disabled for performance reasons).")
		.define("fakeplayer_advancements", false);
		finish(b);
	}

}
