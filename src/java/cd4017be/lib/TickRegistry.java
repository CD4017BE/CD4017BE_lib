package cd4017be.lib;

import java.util.ArrayList;

import net.minecraftforge.common.MinecraftForge;
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

	/**added entries will be processed next tick */
	public ArrayList<IUpdatable> updates = new ArrayList<IUpdatable>();
	private ArrayList<IUpdatable> swapList = new ArrayList<IUpdatable>();

	public TickRegistry() {
		MinecraftForge.EVENT_BUS.register(this);
	}

	@SubscribeEvent()
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

}
