package cd4017be.api.grid;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.WorldData;
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess;
import net.minecraftforge.fmllegacy.WorldPersistenceHooks;
import net.minecraftforge.fmllegacy.WorldPersistenceHooks.WorldPersistenceHook;

/**Utility for connecting ports from {@link IPortHolder}s via unique Link-IDs.
 * @see #load(IPortHolder, int, int)
 * @see #unload(IPortHolder, int, int)
 * @see #newId()
 * @author CD4017BE */
public class Link {

	private IPortHolder provider, master;
	private int providerPort, masterPort;

	private Link(int id) {}

	private void load(IPortHolder obj, int port) {
		if (obj.isMaster(port)) {
			master = obj;
			masterPort = port;
		} else {
			provider = obj;
			providerPort = port;
		}
		if (provider != null && master != null)
			master.setHandler(masterPort, provider.getHandler(providerPort));
	}

	private boolean unload(IPortHolder obj, int port) {
		if (obj.isMaster(port)) {
			obj.setHandler(port, null);
			if (master == obj && masterPort == port) {
				master = null;
				if (provider == null) return true;
			}
		} else if (provider == obj && providerPort == port) {
			provider = null;
			if (master == null) return true;
			master.setHandler(masterPort, null);
		}
		return false;
	}

	private static final Int2ObjectOpenHashMap<Link> LINKS = new Int2ObjectOpenHashMap<>();
	private static int NEXT_ID;
	static {
		WorldPersistenceHooks.addHook(new WorldPersistenceHook() {

			@Override
			public String getModId() {
				return "cd4017be_lib";
			}

			@Override
			public CompoundTag getDataForWriting(
				LevelStorageAccess levelSave, WorldData serverInfo
			) {
				CompoundTag nbt = new CompoundTag();
				nbt.putInt("nextLink", NEXT_ID);
				return nbt;
			}

			@Override
			public void readData(
				LevelStorageAccess levelSave, WorldData serverInfo, CompoundTag tag
			) {
				NEXT_ID = tag.getInt("nextLink");
			}
		});
	}
	/** recursion depth limits */
	public static int REC_FLUID = 8, REC_ITEM = 8, REC_POWER = 2, REC_DATA = 4, REC_BLOCK = 4;

	/**Clear all links for server shutdown. */
	public static void clear() {
		LINKS.clear();
		NEXT_ID = 0;
	}

	/**@return a new unique Link-ID */
	public static int newId() {
		return NEXT_ID++;
	}

	/**Register a given port. Once both ports of a link are loaded, they connect.
	 * @param obj
	 * @param port
	 * @param linkId */
	public static void load(IPortHolder obj, int port, int linkId) {
		LINKS.computeIfAbsent(linkId, Link::new).load(obj, port);
	}

	/**Unregister a given port (will be disconnected).
	 * @param obj
	 * @param port
	 * @param linkId */
	public static void unload(IPortHolder obj, int port, int linkId) {
		Link l = LINKS.get(linkId);
		if (l != null && l.unload(obj, port))
			LINKS.remove(linkId);
	}

}
