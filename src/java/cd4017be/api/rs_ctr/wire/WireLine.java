package cd4017be.api.rs_ctr.wire;

import java.lang.reflect.Array;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Consumer;

import cd4017be.api.rs_ctr.port.Connector;
import cd4017be.api.rs_ctr.port.MountedPort;

/**
 * Parses a connected signal wire line from a given starting point.
 * @author cd4017be
 */
public class WireLine implements Collection<WiredConnector> {

	/**start/end point of the connection (may be null if not a fully closed connection) */
	public final WiredConnector source, sink;
	/**list of all wire hooks in order source to sink */
	public final WiredConnector[] hooks;

	/**
	 * Parses the connection of the given port.
	 * @param port
	 * @throws WireLoopException if the ports are connected in a loop
	 */
	public WireLine(WiredConnector con) throws WireLoopException {
		ArrayDeque<WiredConnector> list = new ArrayDeque<>();
		WiredConnector p0 = scan(con, list);
		WiredConnector p1 = con.port instanceof RelayPort ? scan((WiredConnector)((RelayPort)con.port).opposite.getConnector(), list) : con;
		if (con.conBeacon.isMaster) {
			this.source = p1;
			this.sink = p0;
		} else {
			this.source = p0;
			this.sink = p1;
		}
		this.hooks = list.toArray(new WiredConnector[list.size()]);
	}

	private static WiredConnector scan(WiredConnector con, ArrayDeque<WiredConnector> list) throws WireLoopException {
		if (con == null) return null;
		boolean dir = con.conBeacon.isMaster;
		if (con.port instanceof RelayPort)
			if (dir) list.addLast(con);
			else list.addFirst(con);
		WiredConnector con1;
		while ((con1 = con.getLink()) != null && con1.port instanceof RelayPort) {
			if (list.contains(con1)) throw new WireLoopException();
			if (dir) list.addLast(con1);
			else list.addFirst(con1);
			RelayPort sr = ((RelayPort)con1.port).opposite;
			Connector c = sr.getConnector();
			if (!(c instanceof WiredConnector)) return null;
			con = (WiredConnector)c;
			if (dir) list.addLast(con);
			else list.addFirst(con);
		}
		return con1;
	}

	/**
	 * @return whether this signal line is in itself type compatible
	 */
	public boolean checkTypes() {
		Class<?> type = null;
		if (source != null) type = source.getComPort().type;
		if (sink != null) {
			if (type == null) type = sink.getComPort().type;
			else if (sink.getComPort().type != type) return false;
		}
		if (type == null) return true;
		for (WiredConnector con : hooks)
			if (!con.isCompatible(type))
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
		if (port instanceof WiredConnector) {
			for (WiredConnector sr : hooks)
				if (sr == port) return true;
			return port == source || port == sink;
		}
		return false;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		for (Object o : c)
			if (!contains(o))
				return false;
		return true;
	}

	@Override
	public Iterator<WiredConnector> iterator() {
		return new Iterator<WiredConnector>() {
			int i = source != null ? -1 : 0;
			int n = hooks.length + (sink != null ? 1 : 0);

			@Override
			public boolean hasNext() {
				return i < n;
			}

			@Override
			public WiredConnector next() {
				int j = i++;
				return j < 0 ? source : j < hooks.length ? hooks[j] : sink;
			}
		};
	}

	@Override
	public void forEach(Consumer<? super WiredConnector> action) {
		if (source != null) action.accept(source);
		for (WiredConnector con : hooks)
			action.accept(con);
		if (sink != null) action.accept(sink);
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
	public boolean add(WiredConnector e) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(Collection<? extends WiredConnector> c) {
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
