package cd4017be.lib.network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.BitSet;
import java.util.function.*;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;

/**Utility class for annotation based automatic object
 * serialization and deserialization over GUI packets.
 * @author CD4017BE */
public class StateSyncAdv extends StateSynchronizer {

	public final Object[] holders;
	final Synchronizer<?>[] synchronizers;
	final int[] rawIdx;

	public static int[] array(int size, int count) {
		int[] indices = new int[count];
		for (int i = 0, j = size; i < count; i++, j += size)
			indices[i] = j;
		return indices;
	}

	public static int[] sequence(int... sizes) {
		int s = 0;
		for (int i = 0; i < sizes.length; i++)
			s = sizes[i] += s;
		return sizes;
	}

	/**@param client whether this is used on client side.
	 * @param elements Objects to synchronize via annotations.
	 * For client side these are just Classes, not the objects themselves!
	 * @return a new synchronizer for use in {@link AdvancedContainer}. */
	public static StateSyncAdv of(boolean client, Object... elements) {
		return of(client, new int[0], 0, elements);
	}

	/**@param client whether this is used on client side.
	 * @param rawIdx byte index table for custom raw data.
	 * @param objects number of extra objects to reserve for custom data.
	 * @param elements Objects to synchronize via annotations.
	 * For client side these are just Classes, not the objects themselves!
	 * @return a new synchronizer for use in {@link AdvancedContainer}. */
	public static StateSyncAdv of(boolean client, int[] rawIdx, int objects, Object... elements) {
		Synchronizer<?>[] synchronizers = new Synchronizer[elements.length];
		int[] ofsIdx = new int[elements.length];
		int[] ofsObj = new int[elements.length];
		int l = rawIdx.length;
		int bytes = l > 0 ? rawIdx[l - 1] : 0, indices = objects + l;
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
			client ? null : elements, synchronizers, rawIdx
		);
	}

	private StateSyncAdv(
		int count, int[] indices, int[] objIdx, int objCount, int rawSize,
		Object[] holders, Synchronizer<?>[] synchronizers, int[] rawIdx
	) {
		super(count, indices, objIdx, new Object[objCount], ByteBuffer.allocate(rawSize).order(ByteOrder.LITTLE_ENDIAN));
		this.holders = holders;
		this.synchronizers = synchronizers;
		this.rawIdx = rawIdx;
	}

	@Override
	public BitSet clear() {
		rawStates.clear();
		if (set.get(0)) {
			set.clear(0);
			set.set(1, count + 1);
		} else set.clear();
		return set;
	}

	@Override
	public BitSet read(ByteBuf buf) {
		rawStates.clear();
		return super.read(buf);
	}

	public void detectChanges() {
		rawStates.clear();
		if (rawIdx.length > 0) rawStates.position(rawIdx[rawIdx.length - 1]);
		for (int i = 0; i < synchronizers.length; i++)
			synchronizers[i].detectChanges(holders[i], rawStates, objStates, objIdx[i], set, indices[i]);
		rawStates.rewind();
	}

	public void writeChanges(FriendlyByteBuf pkt) {
		final int i1 = rawIdx.length + 1;
		for (int i = 1, j; i <= i1 && (j = set.nextSetBit(i)) > 0 && j < i1; i++) {
			if ((i = set.nextClearBit(j + 1)) > i1) i = i1;
			rawStates.limit(rawIdx[i - 2]);
			if (j >= 2) rawStates.position(rawIdx[j - 2]);
			pkt.writeBytes(rawStates);
		}
		rawStates.clear();
		if (i1 >= 2) rawStates.position(rawIdx[i1 - 2]);
		
		for (int i = 0; i < synchronizers.length; i++)
			synchronizers[i].writeChanged(pkt, rawStates, objStates, objIdx[i], set, indices[i]);
	}

	public void readChanges(FriendlyByteBuf pkt) throws IOException {
		final int i1 = rawIdx.length + 1;
		for (int i = 1, j; i <= i1 && (j = set.nextSetBit(i)) > 0 && j < i1; i++) {
			if ((i = set.nextClearBit(j + 1)) > i1) i = i1;
			rawStates.limit(rawIdx[i - 2]);
			if (j >= 2) rawStates.position(rawIdx[j - 2]);
			pkt.readBytes(rawStates);
		}
		rawStates.clear();
		if (i1 >= 2) rawStates.position(rawIdx[i1 - 2]);
		
		if (holders == null)
			for (int i = 0; i < synchronizers.length; i++)
				synchronizers[i].readChanges(pkt, rawStates, objStates, objIdx[i], set, indices[i]);
		else for (int i = 0; i < synchronizers.length; i++)
				synchronizers[i].updateChanges(holders[i], pkt, set, indices[i]);
	}

	public int objIdxOfs() {
		return 1 + rawIdx.length;
	}

	@SuppressWarnings("unchecked")
	public <T> T get(int i) {
		return (T)objStates[i];
	}

	public void set(int i, Object v) {
		set.set(i + objIdxOfs());
		objStates[i] = v;
	}

	public void setDouble(int i, double v) {setDouble(i, -8, v);}
	public void setFloat(int i, float v) {setFloat(i, -4, v);}
	public void setLong(int i, long v) {setLong(i, -8, v);}
	public void setInt(int i, int v) {setInt(i, -4, v);}
	public void setShort(int i, int v) {setShort(i, -2, v);}
	public void setByte(int i, int v) {setByte(i, -1, v);}

	public void setDouble(int i, int p, double v) {
		setLong(i, p, Double.doubleToRawLongBits(v));
	}

	public void setFloat(int i, int p, float v) {
		setInt(i, p, Float.floatToRawIntBits(v));
	}

	public void setLong(int i, int p, long v) {
		p += rawIdx[i];
		if (v != rawStates.getLong(p)) {
			rawStates.putLong(p, v);
			set.set(i + 1);
		}
	}

	public void setInt(int i, int p, int v) {
		p += rawIdx[i];
		if (v != rawStates.getInt(p)) {
			rawStates.putInt(p, v);
			set.set(i + 1);
		}
	}

	public void setShort(int i, int p, int v) {
		p += rawIdx[i];
		if ((short)v != rawStates.getShort(p)) {
			rawStates.putShort(p, (short)v);
			set.set(i + 1);
		}
	}

	public void setByte(int i, int p, int v) {
		p += rawIdx[i];
		if ((byte)v != rawStates.get(p)) {
			rawStates.put(p, (byte)v);
			set.set(i + 1);
		}
	}

	public ByteBuffer buffer() {
		return rawStates;
	}

	public int objIndex(Class<?> c, String name) {
		for (int i = 0; i < synchronizers.length; i++) {
			Synchronizer<?> s = synchronizers[i];
			if (c == null || s.clazz == c) {
				int j = s.varIndex(name);
				if (j >= 0)
					return objIdx[i] + j;
				else if (c != null)
					throw new IllegalArgumentException("variable '" + name + "' is not defined!");
			}
		}
		throw new IllegalArgumentException("class '" + c + "' is not listed!");
	}

	public long rawIndex(Class<?> c, String name) {
		int l = rawIdx.length, p = l > 0 ? rawIdx[l - 1] : 0;
		for (Synchronizer<?> s : synchronizers) {
			if (c == null || s.clazz == c) {
				int j = s.varIndex(name);
				if (j >= 0) {
					int i0 = s.indices[j];
					return (long)(p + i0) | (long)(s.indices[j+1] - i0) << 32;
				} else if (c != null)
					throw new IllegalArgumentException("variable '" + name + "' is not defined!");
			}
			p += s.rawSize();
		}
		throw new IllegalArgumentException(
			c != null ? "class '" + c + "' is not listed!"
				: "variable '" + name + "' is not defined!"
		);
	}

	public <U> Supplier<U> objGetter(String name) {
		return objGetter(null, name);
	}

	public <U> Supplier<U> objGetter(Class<?> c, String name) {
		return objGetter(objIndex(c, name));
	}

	@SuppressWarnings("unchecked")
	public <U> Supplier<U> objGetter(int i) {
		Object[] states = objStates;
		return ()-> (U)states[i];
	}

	public DoubleSupplier floatGetter(String name, boolean integer) {
		return floatGetter(null, name, integer);
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

	public IntSupplier intGetter(String name, boolean signed) {
		return intGetter(null, name, signed);
	}

	public IntSupplier intGetter(Class<?> c, String name, boolean signed) {
		long i = rawIndex(c, name);
		return intGetter((int)i, (int)(i >>> 32), signed);
	}

	public IntSupplier intGetter(int i, boolean signed) {
		int p = i == 0 ? 0 : rawIdx[i-1];
		return intGetter(p, rawIdx[i] - p, signed);
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

	public LongSupplier longGetter(String name, boolean signed) {
		return longGetter(null, name, signed);
	}

	public LongSupplier longGetter(Class<?> c, String name, boolean signed) {
		long i = rawIndex(c, name);
		return longGetter((int)i, (int)(i >>> 32), signed);
	}

	public LongSupplier longGetter(int p, int l, boolean signed) {
		ByteBuffer state = rawStates;
		switch(l) {
		case 8: return ()-> state.getLong(p);
		case 4: return signed ? ()-> state.getInt(p) : ()-> state.getInt(p) & 0xffffffffL;
		case 2: return signed ? ()-> state.getShort(p) : ()-> state.getShort(p) & 0xffffL;
		case 1: return signed ? ()-> state.get(p) : ()-> state.get(p) & 0xffL;
		default: throw new IllegalStateException();
		}
	}

	public BooleanSupplier boolGetter(String name) {
		return boolGetter(null, name);
	}

	public BooleanSupplier boolGetter(Class<?> c, String name) {
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
