package cd4017be.api.rs_ctr.wire;

import java.lang.reflect.Array;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Consumer;

import cd4017be.api.rs_ctr.port.IConnector;
import cd4017be.api.rs_ctr.port.MountedPort;
import cd4017be.api.rs_ctr.port.Port;

/**
 * Parses a connected signal wire line from a given starting point.
 * @author cd4017be
 */
public class WireLine implements Collection<MountedPort> {

	/**start/end point of the connection (may be null if not a fully closed connection) */
	public final Port source, sink;
	/**list of all wire hooks in order source to sink */
	public final RelayPort[] hooks;

	/**
	 * Parses the connection of the given port.
	 * @param port
	 * @throws WireLoopException if the ports are connected in a loop
	 */
	public WireLine(MountedPort port) throws WireLoopException {
		ArrayDeque<RelayPort> list = new ArrayDeque<>();
		Port p0 = scan(port, list);
		Port p1 = port instanceof RelayPort ? scan(((RelayPort)port).opposite, list) : port;
		if (port.isMaster) {
			this.source = p1;
			this.sink = p0;
		} else {
			this.source = p0;
			this.sink = p1;
		}
		this.hooks = list.toArray(new RelayPort[list.size()]);
	}

	private static Port getLink(MountedPort port) {
		IConnector c = port.getConnector();
		if (!(c instanceof IWiredConnector)) return null;
		IWiredConnector con = (IWiredConnector)c;
		Port lp = con.getLinkPort(port);
		if (!lp.isMaster ^ port.isMaster) return null;
		if (lp instanceof MountedPort) {
			c = ((MountedPort)lp).getConnector();
			if (!(c instanceof IWiredConnector && ((IWiredConnector)c).isLinked(port))) return null;
		}
		return lp;
	}

	private static Port scan(MountedPort mport, ArrayDeque<RelayPort> list) throws WireLoopException {
		boolean dir = mport.isMaster;
		if (mport instanceof RelayPort && mport.getConnector() != null)
			if (dir) list.addLast((RelayPort)mport);
			else list.addFirst((RelayPort)mport);
		Port port;
		while ((port = getLink(mport)) instanceof RelayPort) {
			RelayPort sr = (RelayPort)port;
			if (list.contains(sr)) throw new WireLoopException();
			if (dir) list.addLast(sr);
			else list.addFirst(sr);
			mport = sr = sr.opposite;
			if (sr.getConnector() == null) return null;
			if (dir) list.addLast(sr);
			else list.addFirst(sr);
		}
		return port;
	}

	/**
	 * @return whether this signal line is in itself type compatible
	 */
	public boolean checkTypes() {
		Class<?> type = null;
		if (source != null) type = source.type;
		if (sink != null) {
			if (type == null) type = sink.type;
			else if (sink.type != type) return false;
		}
		if (type == null) return true;
		for (RelayPort port : hooks)
			if (!((IWiredConnector)port.getConnector()).isCompatible(type))
				return false;
		return true;
	}

	@Override
	public int size() {
		int n = hooks.length;
		if (source != null) n++;
		if (sink != null) n++;
		return n;
	}

	@Override
	public boolean isEmpty() {
		return source == null && sink == null && hooks.length == 0;
	}

	@Override
	public boolean contains(Object port) {
		if (port instanceof RelayPort) {
			for (RelayPort sr : hooks)
				if (sr == port) return true;
			return false;
		}
		return port == source || port == sink;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		for (Object o : c)
			if (!contains(o))
				return false;
		return true;
	}

	@Override
	public Iterator<MountedPort> iterator() {
		return new Iterator<MountedPort>() {
			int i = source instanceof MountedPort ? -1 : 0;
			int n = hooks.length + (sink instanceof MountedPort ? 1 : 0);

			@Override
			public boolean hasNext() {
				return i < n;
			}

			@Override
			public MountedPort next() {
				int j = i++;
				return j < 0 ? (MountedPort)source : j < hooks.length ? hooks[j] : (MountedPort)sink;
			}
		};
	}

	@Override
	public void forEach(Consumer<? super MountedPort> action) {
		if (source instanceof MountedPort)
			action.accept((MountedPort)source);
		for (RelayPort port : hooks)
			action.accept(port);
		if (sink instanceof MountedPort)
			action.accept((MountedPort)sink);
	}

	@Override
	public MountedPort[] toArray() {
		return toArray(new MountedPort[size()]);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T[] toArray(T[] a) {
		int n = size();
		if (a.length < n) a = (T[]) Array.newInstance(a.getClass().getComponentType(), n);
		int i = 0;
		if (source != null) a[i++] = (T) source;
		System.arraycopy(hooks, 0, a, i, hooks.length);
		if (source != null) a[n - 1] = (T) source;
		return null;
	}

	@Override
	public boolean add(MountedPort e) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(Collection<? extends MountedPort> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@SuppressWarnings("serial")
	public static class WireLoopException extends Exception {}

}
