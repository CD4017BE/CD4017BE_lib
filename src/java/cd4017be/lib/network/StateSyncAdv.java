package cd4017be.lib.network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.function.*;
import cd4017be.lib.Gui.AdvancedContainer;
import net.minecraft.network.PacketBuffer;

/**Utility class for annotation based automatic object
 * serialization and deserialization over GUI packets.
 * @author CD4017BE */
public class StateSyncAdv extends StateSynchronizer {

	final Object[] holders;
	final Synchronizer<?>[] synchronizers;
	final int ofsRaw;

	/**@param client whether this is used on client side.
	 * @param elements Objects to synchronize via annotations.
	 * For client side these are just Classes, not the objects themselves!
	 * @return a new synchronizer for use in {@link AdvancedContainer}. */
	public static StateSyncAdv of(boolean client, Object... elements) {
		return of(client, 0, 0, 0, elements);
	}

	/**@param client whether this is used on client side.
	 * @param indices number of extra indices to reserve for custom data.
	 * @param extraRaw number of extra bytes to reserve for custom data.
	 * @param objects number of extra objects to reserve for custom data.
	 * @param elements Objects to synchronize via annotations.
	 * For client side these are just Classes, not the objects themselves!
	 * @return a new synchronizer for use in {@link AdvancedContainer}. */
	public static StateSyncAdv of(boolean client, int indices, int bytes, int objects, Object... elements) {
		Synchronizer<?>[] synchronizers = new Synchronizer[elements.length];
		int[] ofsIdx = new int[elements.length];
		int[] ofsObj = new int[elements.length];
		int ofs = bytes;
		for (int i = 0; i < elements.length; i++) {
			Object o = elements[i];
			Synchronizer<?> s = client && o instanceof Class ?
				Synchronizer.of((Class<?>)o)
				: Synchronizer.of(o.getClass());
			ofsIdx[i] = indices + 1;
			ofsObj[i] = objects;
			indices += s.syncVariables();
			objects += s.objSize();
			bytes += s.rawSize();
			synchronizers[i] = s;
		}
		return new StateSyncAdv(
			indices, ofsIdx, ofsObj, objects, bytes,
			client ? null : elements, synchronizers, ofs
		);
	}

	private StateSyncAdv(int count, int[] indices, int[] objIdx, int objCount, int rawSize, Object[] holders, Synchronizer<?>[] synchronizers, int rawOfs) {
		super(count, indices, objIdx, new Object[objCount], ByteBuffer.allocate(rawSize));
		this.holders = holders;
		this.synchronizers = synchronizers;
		this.ofsRaw = rawOfs;
	}

	@Override
	public BitSet clear() {
		rawStates.clear();
		if (set.get(0)) {
			set.clear(0);
			set.set(1, count);
		} else set.clear();
		return set;
	}

	public void detectChanges() {
		for (int i = 0; i < synchronizers.length; i++)
			synchronizers[i].detectChanges(holders[i], rawStates, objStates, objIdx[i], set, indices[i]);
		rawStates.rewind();
	}

	public void writeChanges(PacketBuffer pkt) {
		for (int i = 0; i < synchronizers.length; i++)
			synchronizers[i].writeChanged(pkt, rawStates, objStates, objIdx[i], set, indices[i]);
	}

	public void readChanges(PacketBuffer pkt) throws IOException {
		if (holders == null)
			for (int i = 0; i < synchronizers.length; i++)
				synchronizers[i].readChanges(pkt, rawStates, objStates, objIdx[i], set, indices[i]);
		else for (int i = 0; i < synchronizers.length; i++)
				synchronizers[i].updateChanges(holders[i], pkt, set, indices[i]);
	}

	@SuppressWarnings("unchecked")
	public <T> T get(int i) {
		return (T)objStates[i];
	}

	public void set(int i, int oi, Object v) {
		set.set(i);
		objStates[oi] = v;
	}

	public ByteBuffer buffer() {
		return rawStates;
	}

	public int objIndex(Class<?> c, String name) {
		for (int i = 0; i < synchronizers.length; i++) {
			Synchronizer<?> s = synchronizers[i];
			if (s.clazz == c)
				return objIdx[i] + s.varIndex(name);
		}
		throw new IllegalArgumentException("class '" + c + "' is not listed!");
	}

	public long rawIndex(Class<?> c, String name) {
		int p = ofsRaw;
		for (Synchronizer<?> s : synchronizers)
			if (s.clazz == c) {
				int j = s.varIndex(name);
				int i0 = s.indices[j];
				return p + i0 | s.indices[j+1] - i0 << 32;
			} else p += s.rawSize();
		throw new IllegalArgumentException("class '" + c + "' is not listed!");
	}

	@SuppressWarnings("unchecked")
	public <U> Supplier<U> objGetter(int i) {
		Object[] states = objStates;
		return ()-> (U)states[i];
	}

	public <U> Supplier<U> objGetter(Class<?> c, String name) {
		return objGetter(objIndex(c, name));
	}

	public DoubleSupplier floatGetter(Class<?> c, String name, boolean integer) {
		long i = rawIndex(c, name);
		return floatGetter((int)i, (int)(i >>> 32), integer);
	}

	public DoubleSupplier floatGetter(int p, int l, boolean integer) {
		ByteBuffer state = rawStates;
		switch(l) {
		case 8: return integer ? ()-> state.getLong(p) : ()-> state.getDouble(p);
		case 4: return integer ? ()-> state.getInt(p) : ()-> state.getFloat(p);
		case 2: return ()-> state.getShort(p);
		case 1: return ()-> state.get(p);
		default: throw new IllegalArgumentException();
		}
	}

	public IntSupplier intGetter(Class<?> c, String name, boolean signed) {
		long i = rawIndex(c, name);
		return intGetter((int)i, (int)(i >>> 32), signed);
	}

	public IntSupplier intGetter(int p, int l, boolean signed) {
		ByteBuffer state = rawStates;
		switch(l) {
		case 8: return ()-> (int)state.getLong(p);
		case 4: return ()-> state.getInt(p);
		case 2: return signed ? ()-> state.getShort(p) : ()-> state.getShort(p) & 0xffff;
		case 1: return signed ? ()-> state.get(p) : ()-> state.get(p) & 0xff;
		default: throw new IllegalStateException();
		}
	}

	public BooleanSupplier intGetter(Class<?> c, String name) {
		long i = rawIndex(c, name);
		return boolGetter((int)i, (int)(i >>> 32));
	}

	public BooleanSupplier boolGetter(int p, int l) {
		ByteBuffer state = rawStates;
		switch(l) {
		case 8: return ()-> state.getLong(p) != 0;
		case 4: return ()-> state.getInt(p) != 0;
		case 2: return ()-> state.getShort(p) != 0;
		case 1: return ()-> state.get(p) != 0;
		default: throw new IllegalStateException();
		}
	}

}
