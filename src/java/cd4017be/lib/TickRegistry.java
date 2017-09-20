package cd4017be.lib;

import java.util.ArrayList;
import java.util.Map.Entry;

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
		for (Entry<BlockPos, TileEntity> e : chunk.getTileEntityMap().entrySet()) {
			BlockPos pos = e.getKey();
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

}
