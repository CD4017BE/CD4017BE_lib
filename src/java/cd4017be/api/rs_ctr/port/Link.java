package cd4017be.api.rs_ctr.port;

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Handles the connection between SignalPorts.
 * @author CD4017BE
 */
public class Link {

	public static final Logger LOG = LogManager.getLogger("rs_ctr API");
	private static final Int2ObjectMap<Link> links = new Int2ObjectOpenHashMap<>();
	private static int nextLinkID = 1;
	private static File file;
	private static boolean dirty;

	public static class SaveHandler {
		@SubscribeEvent
		public void onSave(WorldEvent.Save ev) {
			if (!dirty || file == null) return;
			NBTTagCompound nbt = new NBTTagCompound();
			nbt.setInteger("nextID", nextLinkID);
			try {
				CompressedStreamTools.write(nbt, file);
				LOG.info("Signal Link IDs sucessfully saved");
				dirty = false;
			} catch (IOException e) {
				LOG.error("failed to save Signal Link IDs: ", e);
			}
		}
	}

	static {
		MinecraftForge.EVENT_BUS.register(new SaveHandler());
	}

	private static int newLinkID() {
		dirty = true;
		return nextLinkID++;
	}

	public static void saveData() {
		LOG.info("{} active Links cleared during server unload.", links.size());
		links.clear();
	}

	public static void loadData(File savedir) {
		nextLinkID = 1;
		file = new File(savedir, "data/signalLinkIDs.dat");
		try {
			NBTTagCompound nbt = CompressedStreamTools.read(file);
			if (nbt == null) {
				LOG.info("Signal Link ID file not found: this must be a newly created world.");
				return;
			}
			int i = nbt.getInteger("nextID");
			if (i != 0) nextLinkID = i;
			LOG.info("Signal Link IDs sucessfully loaded: next = {}", nextLinkID);
		} catch (IOException e) {
			LOG.error("failed to load Signal Link IDs: ", e);
		}
	}

	public static Link of(int id) {
		return links.get(id);
	}

	public static void register(Port port) {
		Link link = links.get(port.linkID);
		if (link != null) link.load(port);
		else links.put(port.linkID, new Link(port));
	}

	public final int id;
	private Port source, sink;	

	private Link(Port port) {
		this.id = port.linkID;
		if (port.isMaster) source = port;
		else sink = port;
		if (id >= nextLinkID) {
			nextLinkID = id + 1;
			LOG.warn("It appears the used up Signal Link IDs info wasn't properly saved to disk. IDs may have been assigned duplicate!");
		}
	}

	public Link(Port source, Port sink) {
		if (!source.isMaster || sink.isMaster)
			throw new IllegalArgumentException("invalid port directions!");
		this.source = source;
		this.sink = sink;
		links.put(this.id = source.linkID = sink.linkID = newLinkID(), this);
		source.owner.setPortCallback(source.pin, sink.owner.getPortCallback(sink.pin));
		source.owner.onPortModified(source, IPortProvider.E_CONNECT);
		sink.owner.onPortModified(sink, IPortProvider.E_CONNECT);
	}

	public void load(Port port) {
		boolean link = false;
		if (port.isMaster) {
			if (source == null) {
				source = port;
				link = sink != null;
			}
		} else {
			if (sink == null) {
				sink = port;
				link = source != null;
			}
		}
		if (link) {
			source.onLinkLoad(sink);
			sink.onLinkLoad(source);
			if (!links.containsKey(id)) return;
			source.owner.setPortCallback(source.pin, sink.owner.getPortCallback(sink.pin));
		} else logLinkError("loaded duplicate", port);
	}

	public void unload(Port port) {
		if (port != (port.isMaster ? source : sink)) {
			logLinkError("unloaded invalid", port);
			if (port.isMaster) port.owner.setPortCallback(port.pin, null);
			return;
		}
		if (source != null)
			source.owner.setPortCallback(source.pin, null);
		if (port.isMaster) source = null;
		else sink = null;
		if (source == null && sink == null)
			links.remove(id);
	}

	public void disconnect() {
		if (source != null) {
			source.linkID = 0;
			source.owner.setPortCallback(source.pin, null);
			source.owner.onPortModified(source, IPortProvider.E_DISCONNECT);
		}
		if (sink != null) {
			sink.linkID = 0;
			sink.owner.onPortModified(sink, IPortProvider.E_DISCONNECT);
		}
		links.remove(id);
	}

	public Port source() {
		return source;
	}

	public Port sink() {
		return sink;
	}

	private static void logLinkError(String message, Port port) {
		LOG.warn(
			"{} {} port on ID {}, hosted on pin {} of {}",
			message, port.isMaster ? "master" : "slave",
			port.linkID, port.pin, port.owner.getClass().getName()
		);
	}
}