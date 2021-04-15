package cd4017be.lib;

import java.util.ArrayList;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.util.Arrays;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;

/**
 * Handles single use update ticks for various things.
 * Mainly used for TileEntities that only react to certain events and don't need continuous ticks.
 * Also kind of a workaround for Forge not providing a useful initialization event in TileEntities that has save world access (save = not crashing with StackOverflowException by infinite recursive chunkloading).
 * @author CD4017BE
 */
public class TickRegistry {

	public static final Marker TICKS = MarkerManager.getMarker("Ticks");
	private static final boolean DEBUG = false;
	public static final TickRegistry instance = new TickRegistry();
	/**value switches between 2 and 3 every tick, used to distinguish which tick we are currently in */
	public static byte TICK = 2;

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
			TICK ^= 1;
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

}
