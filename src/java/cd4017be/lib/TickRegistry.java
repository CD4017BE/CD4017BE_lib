package cd4017be.lib;

import java.util.ArrayList;
import cd4017be.lib.block.AdvancedBlock.INeighborAwareTile;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Handles single use update ticks for various things.
 * Mainly used for TileEntities that only react to certain events and don't need continuous ticks.
 * Also kind of a workaround for Forge not providing a useful initialization event in TileEntities that has save world access (save = not crashing with StackOverflowException by infinite recursive chunkloading).
 * @author CD4017BE
 */
public class TickRegistry {

	public static final TickRegistry instance = new TickRegistry();

	public static void register() {
		//already got registered when class loaded
	}

	/**added entries will be processed next tick */
	public ArrayList<IUpdatable> updates = new ArrayList<IUpdatable>();
	private ArrayList<IUpdatable> swapList = new ArrayList<IUpdatable>();

	private TickRegistry() {
		MinecraftForge.EVENT_BUS.register(this);
	}

	@SubscribeEvent
	public void tick(TickEvent.ServerTickEvent ev) {
		if (ev.phase == TickEvent.Phase.END && !updates.isEmpty()) {
			//swap lists to prevent adding more components while still processing them
			ArrayList<IUpdatable> list = updates;
			updates = swapList;
			swapList = list;
			//process updates
			for (IUpdatable update : list)
				update.process();
			list.clear();
		}
	}

	public interface IUpdatable {
		public void process();
	}

	/**
	 * Some TileEntities in my mods need to be notified when neighboring TileEntities appear or disappear (especially on multiblock structures).
	 * Unfortunately since 1.11 Minecraft won't let TileEntities notify their neighbors on Chunk load anymore, so I have to do it manually now.
	 */
	@SubscribeEvent
	public void chunkLoad(ChunkEvent.Load ev) {
		Chunk chunk = ev.getChunk();
		World world = chunk.getWorld();
		ChunkPos cp = chunk.getPos();
		boolean loadedN = world.isBlockLoaded(cp.getBlock(0, 0, -1)),
				loadedS = world.isBlockLoaded(cp.getBlock(0, 0, 16)),
				loadedW = world.isBlockLoaded(cp.getBlock(-1, 0, 0)),
				loadedE = world.isBlockLoaded(cp.getBlock(16, 0, 0));
		for (BlockPos pos : chunk.getTileEntityMap().keySet()) {
			//only check for TileEntitys that sit on the chunk border
			int x = pos.getX() & 15, z = pos.getZ() & 15;
			if (z == 0 && loadedN) notifyNeighborTile(world, pos, EnumFacing.NORTH);
			else if (z == 15 && loadedS) notifyNeighborTile(world, pos, EnumFacing.SOUTH);
			if (x == 0 && loadedW) notifyNeighborTile(world, pos, EnumFacing.WEST);
			else if (x == 15 && loadedE) notifyNeighborTile(world, pos, EnumFacing.EAST);
		}
	}

	private void notifyNeighborTile(World world, BlockPos pos, EnumFacing side) {
		TileEntity te = world.getTileEntity(pos.offset(side));
		//only do it for TileEntities that explicitly want notification to not screw up other mods
		if (te instanceof INeighborAwareTile)
			((INeighborAwareTile)te).neighborTileChange(pos);
	}

	/**
	 * since MC-1.11 TileEntities are by default not invalidated when their chunk unloads which causes some problems when working with references to other TileEntity instances. <br>
	 * So they are quietly invalidated here to fix that (in the hope it won't screw things up).
	 */
	@SubscribeEvent
	public void chunkUnload(ChunkEvent.Unload ev) {
		for (TileEntity te : ev.getChunk().getTileEntityMap().values())
			//set field directly instead of calling invalidate() as modders may have added additional logic to that method
			//which was eventually only intended for regular TileEntity removal.
			te.tileEntityInvalid = true;
	}

}
