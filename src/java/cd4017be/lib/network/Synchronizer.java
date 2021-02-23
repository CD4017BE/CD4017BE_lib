package cd4017be.lib.network;

import java.lang.invoke.*;
import java.lang.reflect.*;
import static java.lang.invoke.MethodHandles.*;
import java.nio.ByteBuffer;
import static java.lang.invoke.MethodType.methodType;
import java.io.IOException;
import static cd4017be.lib.Lib.DEV_DEBUG;
import static java.lang.invoke.MethodHandleProxies.asInterfaceInstance;
import java.util.*;
import java.util.function.*;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import cd4017be.lib.Lib;
import cd4017be.lib.network.Encoders.BinObjEnc;
import cd4017be.lib.network.Sync.Type;
import net.minecraft.nbt.*;
import net.minecraft.network.PacketBuffer;

/** 
 * @author CD4017BE */
public class Synchronizer<T> {

	private static final Predicate<AccessibleObject> HAS_SYNC
	= o -> o.isAnnotationPresent(Sync.class);
	private static final Predicate<Method> IS_GETTER
	= m -> m.getReturnType() != void.class && m.getParameterTypes().length == 0;
	private static final HashMap<Class<?>, Synchronizer<?>> CACHE = new HashMap<>();

	/**get or create a Synchronizer for the given class. */
	@SuppressWarnings("unchecked")
	public static <T> Synchronizer<T> of(Class<T> o) {
		return (Synchronizer<T>)CACHE.computeIfAbsent(o, Synchronizer::new);
	}

	public final Class<T> clazz;
	public final Encoder<T>[] variables;
	final RawComparator[] rawComps;
	final int[] indices;
	final Function<T, ?>[] objGetters;
	final ObjReader<T>[] readers;
	@SuppressWarnings("rawtypes")
	final BinObjEnc[] encoders;

	@SuppressWarnings("unchecked")
	private Synchronizer(Class<T> c) {
		this.clazz = c;
		Triple<
			List<Encoder<T>>,
			List<Triple<RawComparator, ObjReader<T>, Integer>>,
			List<Triple<Function<T, ?>, BiConsumer<T, ?>, BinObjEnc<?>>>
		> lists = Stream.concat(
			Arrays.stream(c.getFields())
				.filter(HAS_SYNC)
				.map(StateVariable<T>::new),
			Arrays.stream(c.getMethods())
				.filter(HAS_SYNC).filter(IS_GETTER)
				.map(StateVariable<T>::new)
		).filter(StateVariable::valid).sorted()
		.collect(
			()-> Triple.of(new ArrayList<>(), new ArrayList<>(), new ArrayList<>()),
			(l, sv)-> sv.process(l),
			(a, b) -> {
				a.getLeft().addAll(b.getLeft());
				a.getMiddle().addAll(b.getMiddle());
				a.getRight().addAll(b.getRight());
			}
		);
		{
			List<Encoder<T>> list = lists.getLeft();
			this.variables = list.toArray((Encoder<T>[])new Encoder[list.size()]);
		} {
			List<Triple<RawComparator, ObjReader<T>, Integer>> list = lists.getMiddle();
			this.readers = new ObjReader[list.size() + lists.getRight().size()];
			this.rawComps = new RawComparator[list.size()];
			this.indices = new int[list.size() + 1];
			for (int j = 0, i = 0; i < rawComps.length; i++) {
				Triple<RawComparator, ObjReader<T>, Integer> e = list.get(i);
				rawComps[i] = e.getLeft();
				readers[i] = e.getMiddle();
				indices[i + 1] = j += e.getRight();
			}
		} {
			List<Triple<Function<T, ?>, BiConsumer<T, ?>, BinObjEnc<?>>> list = lists.getRight();
			this.objGetters = new Function[list.size()];
			this.encoders = new BinObjEnc[list.size()];
			for (int i = 0; i < objGetters.length; i++) {
				Triple<Function<T, ?>, BiConsumer<T, ?>, BinObjEnc<?>> e = list.get(i);
				Function<T, ?> get = e.getLeft();
				@SuppressWarnings("rawtypes")
				BiConsumer set = e.getMiddle();
				@SuppressWarnings("rawtypes")
				BinObjEnc enc = e.getRight();
				objGetters[i] = get;
				encoders[i] = enc;
				readers[i + rawComps.length] = reader(get, enc, set);
			}
		}
		if (DEV_DEBUG) {//It's easy to forget setting @Sync members public, so let's check for it.
			for (Field f : c.getDeclaredFields())
				if (!Modifier.isPublic(f.getModifiers()) && f.isAnnotationPresent(Sync.class))
					Lib.LOG.fatal("invalid @Sync {} in {}, must be public!", f.getName(), c.getName());
			for (Method m : c.getDeclaredMethods())
				if (!Modifier.isPublic(m.getModifiers()) && m.isAnnotationPresent(Sync.class))
					Lib.LOG.fatal("invalid @Sync {} in {}, must be public!", m.getName(), c.getName());
		}
	}

	static <T, U> ObjReader<T> reader(Function<T, U> get, BinObjEnc<U> enc, BiConsumer<T, U> set) {
		if (set != null) return (o, pkt)-> set.accept(o, enc.decode(get.apply(o), pkt));
		else return (o, pkt)-> enc.update(get.apply(o), pkt);
	}

	/**write all {@link Sync} annotated fields and getter methods in <b>o</b> to the given CompoundNBT.
	 * @param o object to encode
	 * @param nbt data tag to store in
	 * @param mode synchronization event bit-mask for {@link Sync#to()} */
	public void writeNBT(Object o, CompoundNBT nbt, int mode) {
		T obj = clazz.cast(o);
		for (Encoder<T> enc : variables)
			enc.writeNBT(obj, nbt, mode);
	}

	/**read all {@link Sync} annotated fields and setter methods in <b>o</b> from the given CompoundNBT.
	 * @param o object to decode
	 * @param nbt data tag load from
	 * @param mode synchronization event bit-mask for {@link Sync#to()} */
	public void readNBT(Object o, CompoundNBT nbt, int mode) {
		T obj = clazz.cast(o);
		for (Encoder<T> enc : variables)
			enc.readNBT(obj, nbt, mode);
	}

	public int rawSize() {
		return indices[indices.length - 1];
	}

	public int objSize() {
		return objGetters.length;
	}

	public ByteBuffer rawState() {
		return ByteBuffer.allocate(rawSize());
	}

	public Object[] objState() {
		return new Object[objGetters.length];
	}

	public int syncVariables() {
		return rawComps.length + objGetters.length;
	}

	public int varIndex(String name) {
		for (Encoder<T> var : variables) {
			if (var.tag.equals(name) && var.index >= 0)
				return var.index;
		}
		return -1;
	}

	/**Finds all variable states that differ from the given reference states
	 * and then updates the reference states to the new values.
	 * @param o object to monitor
	 * @param rawState reference state raw values. Buffer position must point to the
	 * first element prior call and will point behind the last element post call.
	 * @param objState reference state object values
	 * @param j0 index of the first element in <b>objState</b>
	 * @param changes records the set of variable indices that have changed
	 * @param i0 index of the first element in <b>changes</b> */
	@SuppressWarnings("unchecked")
	public void detectChanges(
		Object o, ByteBuffer rawState,
		Object[] objState, int j0,
		BitSet changes, int i0
	) {
		T obj = clazz.cast(o);
		for (RawComparator cmp : rawComps) {
			if (cmp.hasChanged(obj, rawState))
				changes.set(i0);
			i0++;
		}
		for (int i = 0; i < objGetters.length; i++, i0++, j0++)
			if (encoders[i].hasChanged(objGetters[i].apply(obj), objState, j0))
				changes.set(i0);
	}

	/**Writes all reference states marked as changed to the given update packet.
	 * @param pkt the state update packet to write to
	 * @param rawState reference state raw values. Buffer position must point to the
	 * first element prior call and will point behind the last element post call.
	 * @param objState reference state object values
	 * @param j0 index of the first element in <b>objState</b>
	 * @param changes set of changed variable indices to write
	 * @param i0 index of the first element in <b>changes</b> */
	@SuppressWarnings("unchecked")
	public void writeChanged(
		PacketBuffer pkt, ByteBuffer rawState,
		Object[] objState, int j0,
		BitSet changes, int i0
	) {
		int i1 = i0 + indices.length, p0 = rawState.position();
		for (int i = i0, j; i < i1 && (j = changes.nextSetBit(i)) >= 0; i++) {
			if ((i = changes.nextClearBit(j + 1)) >= i1) i = i1 - 1;
			rawState.limit(p0 + indices[i - i0]);
			rawState.position(p0 + indices[j - i0]);
			pkt.writeBytes(rawState);
		}
		rawState.clear().position(p0 + rawSize());
		for (int i = changes.nextSetBit(i1); i >= 0; i = changes.nextSetBit(i + 1)) {
			int j = i - i1;
			encoders[j].encode(objState[j0 + j], pkt);
		}
	}

	/**Reads all reference states marked as changed from the given update packet.
	 * @param pkt the state update packet to read from
	 * @param rawState reference state raw values. Buffer position must point to the
	 * first element prior call and will point behind the last element post call.
	 * @param objState reference state object values
	 * @param j0 index of the first element in <b>objState</b>
	 * @param changes set of changed variable indices to write
	 * @param i0 index of the first element in <b>changes</b> */
	@SuppressWarnings("unchecked")
	public void readChanges(
		PacketBuffer pkt, ByteBuffer rawState,
		Object[] objState, int j0,
		BitSet changes, int i0
	) throws IOException {
		int i1 = i0 + indices.length, p0 = rawState.position();
		for (int i = i0, j; i < i1 && (j = changes.nextSetBit(i)) >= 0; i++) {
			if ((i = changes.nextClearBit(j + 1)) >= i1) i = i1 - 1;
			rawState.limit(p0 + indices[i - i0]);
			rawState.position(p0 + indices[j - i0]);
			pkt.readBytes(rawState);
		}
		rawState.clear().position(p0 + rawSize());
		for (int i = changes.nextSetBit(i1); i >= 0; i = changes.nextSetBit(i + 1)) {
			int j = i - i1, k = j + j0;
			objState[k] = encoders[j].decode(objState[k], pkt);
		}
	}

	public void updateChanges(
		Object o, PacketBuffer pkt, BitSet changes, int i0
	) throws IOException {
		T obj = clazz.cast(o);
		int i1 = i0 + syncVariables();
		for (int i = changes.nextSetBit(i0); i >= 0 && i < i1; i = changes.nextSetBit(i + 1))
			readers[i - i0].read(obj, pkt);
	}

	static abstract class Encoder<T> {
		final int flags, index;
		final String tag;

		Encoder(int flags, int index, String tag) {
			this.flags = flags;
			this.index = index;
			this.tag = tag;
		}

		public abstract void writeNBT(T o, CompoundNBT nbt, int mode);
		public abstract void readNBT(T o, CompoundNBT nbt, int mode);
	}

	public static class EncoderNBT<T> extends Encoder<T> {
		final Function<T, INBT> encoder;
		final BiConsumer<T, INBT> decoder;

		public EncoderNBT(int flags, int index, String tag, Pair<Function<T, INBT>, BiConsumer<T, INBT>> enc_dec) {
			super(flags, index, tag);
			this.encoder = enc_dec.getLeft();
			this.decoder = enc_dec.getRight();
		}

		@Override
		public void writeNBT(T o, CompoundNBT nbt, int mode) {
			if ((flags & mode) == 0) return;
			INBT nb = encoder.apply(o);
			if (nb != null) nbt.put(tag, nb);
		}

		@Override
		public void readNBT(T o, CompoundNBT nbt, int mode) {
			if ((flags & mode) == 0) return;
			decoder.accept(o, nbt.get(tag));
		}

	}

	public static class EncoderChild<T, U> extends Encoder<T> {
		final Synchronizer<U> sync;
		final Function<T, U> getter;

		EncoderChild(int flags, int index, String tag, Synchronizer<U> s, Function<T, U> getter) {
			super(flags, index, tag);
			this.sync = s;
			this.getter = getter;
		}

		@Override
		public void writeNBT(T o, CompoundNBT nbt, int mode) {
			if ((flags & mode) == 0) return;
			CompoundNBT sub = new CompoundNBT();
			sync.writeNBT(getter.apply(o), sub, mode);
			nbt.put(tag, sub);
		}

		@Override
		public void readNBT(T o, CompoundNBT nbt, int mode) {
			if ((flags & mode) == 0) return;
			sync.readNBT(getter.apply(o), nbt.getCompound(tag), mode);
		}
	}

	@FunctionalInterface
	public interface RawComparator {
		boolean hasChanged(Object o, ByteBuffer rawState);
	}

	@FunctionalInterface
	public interface ObjReader<T> {
		void read(T o, PacketBuffer pkt) throws IOException;
	}

	private static class StateVariable<T> implements Comparable<StateVariable<T>> {
		final Sync annotation;
		MethodHandle getter, setter;
		Class<?> type;
		String name;

		public StateVariable(Field f) {
			this.annotation = f.getAnnotation(Sync.class);
			this.name = f.getName();
			this.type = f.getType();
			try {
				Lookup l = MethodHandles.publicLookup();
				this.getter = l.unreflectGetter(f);
				if (annotation.type() == Type.Fix) return;
				try {
					Method m = f.getDeclaringClass().getMethod(name, type);
					if (m.isAnnotationPresent(Sync.class))
						this.setter = l.unreflect(m);
				} catch (NoSuchMethodException e) {}
				if (this.setter == null && !Modifier.isFinal(f.getModifiers()))
					this.setter = l.unreflectSetter(f);
			} catch(IllegalAccessException e) {
				e.printStackTrace();
			}
		}

		public StateVariable(Method m) {
			this.annotation = m.getAnnotation(Sync.class);
			this.name = m.getName();
			this.type = m.getReturnType();
			try {
				Lookup l = MethodHandles.publicLookup();
				this.getter = l.unreflect(m);
				if (annotation.type() == Type.Fix) return;
				Class<?> c = m.getDeclaringClass();
				m = c.getMethod(name, type);
				this.setter = l.unreflect(m);
			} catch(NoSuchMethodException e) {}
			catch(IllegalAccessException | SecurityException e) {
				e.printStackTrace();
			}
		}

		@SuppressWarnings({"unchecked", "rawtypes"})
		public void process(Triple<
			List<Encoder<T>>,
			List<Triple<RawComparator, ObjReader<T>, Integer>>,
			List<Triple<Function<T, ?>, BiConsumer<T, ?>, BinObjEnc<?>>>
		> l) {
			int mask = annotation.to(), index = -1;
			Type t = annotation.type().actual(type);
			Pair<Function<T, INBT>, BiConsumer<T, INBT>> enc_dec;
			getter = explicitCastArguments(getter, methodType(t.inType, Object.class));
			if (setter != null) 
				setter = explicitCastArguments(setter, methodType(Void.class, Object.class, t.inType));
			if (t != Type.Obj) {
				enc_dec = Pair.of(
					asInterfaceInstance(Function.class, collectArguments(t.write, 0, getter)),
					setter == null ? null
						: asInterfaceInstance(BiConsumer.class, collectArguments(setter, 1, t.read))
				);
				if (mask < 0) {
					index = l.getMiddle().size();
					l.getMiddle().add(Triple.of(
						asInterfaceInstance(RawComparator.class, collectArguments(t.comp, 0, getter)),
						setter == null ? null
							: asInterfaceInstance(ObjReader.class, collectArguments(setter, 1, t.update)),
						t.size
					));
				}
			} else {
				Encoders enc = Encoders.of(type);
				Function<T, ?> get = asInterfaceInstance(Function.class, getter);
				if (enc == null) {
					Synchronizer<?> s = of(type);
					if (s.variables.length > 0)
						l.getLeft().add(new EncoderChild(mask, index, tag(), s, get));
						//TODO raw encoding ?
					else Lib.LOG.warn("{} of type {} has no registered serialization and no @Sync variables!", name, type);
					return;
				}
				BiConsumer<T, ?> set = setter == null ? null
					: asInterfaceInstance(BiConsumer.class, setter);
				enc_dec = enc.compose(get, set);
				if (mask < 0)
					if (enc.binary != null) {
						index = l.getRight().size();
						l.getRight().add(Triple.of(get, set, enc.binary));
					} else Lib.LOG.error("Failed to handle @Sync(on = GUI) {}:\nBinary serialization not supported for {}!", name, type);
			}
			if (enc_dec.getLeft() == null && mask != Sync.GUI)
				Lib.LOG.error("Failed to handle @Sync {}:\nNBT serialization not supported for {}!", name, type);
			else if (enc_dec.getRight() == null && mask != Sync.GUI)
				Lib.LOG.error("Failed to handle @Sync {}:\nField can't be final, in-place write not supported for {}!", name, type);
			else l.getLeft().add(new EncoderNBT<>(mask, index, tag(), enc_dec));
		}

		private String tag() {
			return annotation.tag().isEmpty() ? name : annotation.tag();
		}

		@Override
		public int compareTo(StateVariable<T> o) {
			return name.compareTo(o.name);
		}

		public boolean valid() {
			return getter != null;
		}
	}

}
