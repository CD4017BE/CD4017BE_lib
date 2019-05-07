package cd4017be.lib;

import java.util.ArrayList;
import java.util.Map.Entry;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.util.Arrays;

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

	public static final Marker TICKS = MarkerManager.getMarker("Ticks");
	public static boolean DEBUG = false;
	public static final TickRegistry instance = new TickRegistry();

	public static void register() {
		//already got registered when class loaded
	}

	/**added entries will be processed next tick */
	public ArrayList<IUpdatable> updates = new ArrayList<IUpdatable>();
	private ArrayList<IUpdatable> swapList = new ArrayList<IUpdatable>();
	private ITickReceiver[] tickList = new ITickReceiver[8];
	private int tickers = 0, added;

	private TickRegistry() {
		MinecraftForge.EVENT_BUS.register(this);
	}

	@SubscribeEvent
	public void tick(TickEvent.ServerTickEvent ev) {
		if (ev.phase != TickEvent.Phase.END) return;
		String msg = "";
		if (!updates.isEmpty()) {
			//swap lists to prevent adding more components while still processing them
			ArrayList<IUpdatable> list = updates;
			updates = swapList;
			swapList = list;
			//process updates
			if (DEBUG) msg += String.format("%d singleTicks ", list.size());
			for (IUpdatable update : list)
				update.process();
			list.clear();
		}
		if (tickers > 0) {
			int i = 0;
			for (int j = 0; j < tickers; j++) {
				ITickReceiver tick = tickList[j];
				if (tick.tick()) {
					if (i < j) tickList[i] = tick;
					i++;
				}
			}
			if (i < tickers) {
				Arrays.fill(tickList, i, tickers, null);
				if (DEBUG) msg += String.format("%d ", i - tickers);
				tickers = i;
			}
		}
		if (DEBUG) {
			if (added > 0) msg += String.format("+%d ", added);
			if (!msg.isEmpty()) {
				msg += String.format("%d contTicks", tickers);
				Lib.LOG.info(TICKS, msg);
			}
		}
		added = 0;
	}

	/**
	 * clear the list of continuous tick receivers to prevent memory leaks
	 */
	public void clear() {
		if (DEBUG) Lib.LOG.info(TICKS, "-{} contTicks due to server shutdown", tickers);
		Arrays.fill(tickList, 0, tickers, null);
		tickers = 0;
		updates.clear();
	}

	/** @param tick will receive update ticks */
	public void add(ITickReceiver tick) {
		if (Thread.currentThread().getName().startsWith("Client")) throw new IllegalStateException("Adding ITickReceivers not allowed on client side!");
		if (tickers == tickList.length) {
			ITickReceiver[] arr = new ITickReceiver[tickers << 1];
			System.arraycopy(tickList, 0, arr, 0, tickers);
			tickList = arr;
		}
		tickList[tickers++] = tick;
		added++;
	}

	public static void schedule(IUpdatable updatable) {
		instance.updates.add(updatable);
	}

	public interface IUpdatable {
		public void process();
	}

	public interface ITickReceiver {
		/** @return whether to continue sending ticks to this */
		public boolean tick();
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
			//only check for TileEntitys that sit on the chunk border
			BlockPos pos = e.getKey();
			int x = pos.getX() & 15, z = pos.getZ() & 15;
			if (z == 0 && loadedN) notifyNeighborTile(world, pos, EnumFacing.NORTH, e.getValue());
			else if (z == 15 && loadedS) notifyNeighborTile(world, pos, EnumFacing.SOUTH, e.getValue());
			if (x == 0 && loadedW) notifyNeighborTile(world, pos, EnumFacing.WEST, e.getValue());
			else if (x == 15 && loadedE) notifyNeighborTile(world, pos, EnumFacing.EAST, e.getValue());
		}
	}

	/**
	 * Some TileEntities in my mods need to be notified when neighboring TileEntities appear or disappear (especially on multiblock structures).
	 * Unfortunately since 1.11 Minecraft won't let TileEntities notify their neighbors on Chunk unload anymore, so I have to do it manually now.
	 */
	@SubscribeEvent
	public void chunkUnload(ChunkEvent.Unload ev) {
		Chunk chunk = ev.getChunk();
		World world = chunk.getWorld();
		ChunkPos cp = chunk.getPos();
		boolean loadedN = world.isBlockLoaded(cp.getBlock(0, 0, -1)),
				loadedS = world.isBlockLoaded(cp.getBlock(0, 0, 16)),
				loadedW = world.isBlockLoaded(cp.getBlock(-1, 0, 0)),
				loadedE = world.isBlockLoaded(cp.getBlock(16, 0, 0));
		if (loadedN | loadedS | loadedW | loadedE)
			for (BlockPos pos : chunk.getTileEntityMap().keySet()) {
				int x = pos.getX() & 15, z = pos.getZ() & 15;
				if (z == 0 && loadedN) notifyNeighborTile(world, pos, EnumFacing.NORTH, null);
				else if (z == 15 && loadedS) notifyNeighborTile(world, pos, EnumFacing.SOUTH, null);
				if (x == 0 && loadedW) notifyNeighborTile(world, pos, EnumFacing.WEST, null);
				else if (x == 15 && loadedE) notifyNeighborTile(world, pos, EnumFacing.EAST, null);
			}
	}

	private void notifyNeighborTile(World world, BlockPos pos, EnumFacing side, TileEntity newTe) {
		TileEntity te = world.getTileEntity(pos.offset(side));
		//only do it for TileEntities that explicitly want notification to not screw up other mods
		if (te instanceof INeighborAwareTile)
			((INeighborAwareTile)te).neighborTileChange(newTe, side.getOpposite());
	}

}
