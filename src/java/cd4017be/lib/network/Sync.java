package cd4017be.lib.network;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.nio.ByteBuffer;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.*;

/**This is a marker for public fields that should get automatic state synchronization / persistence with NBT data.
 * For use in classes implementing {@link INBTSynchronized}.
 * <br>To customize data encoding, the annotation may also be applied on a getter and setter method pair.
 * For that, both methods must have the same name (no get/set prefix!),
 * the getter must have no arguments and return the data to encode
 * and the setter must return void and have a single argument that receives the data to decode.
 * @author CD4017BE */
@Retention(RUNTIME)
@Target({FIELD, METHOD})
public @interface Sync {

	/**data to store in world save */
	int SAVE = 1;
	/**data to send to client (bulk update) */
	int CLIENT = 2;
	/**data to send to client (freq update) */
	int SYNC = 4;
	/**data to store in world save and send with all client updates */
	int ALL = SAVE | CLIENT | SYNC;
	/**data to store in a container used to spawn the object */
	int SPAWN = 8;
	/**data to synchronize with GUI */
	int GUI = 0x80000000;

	/**@return bit flags defining which data synchronization events to participate in.
	 * Default is only on {@link #SAVE SAVE}.
	 * @see {@link Synchronizer#readNBT}, {@link Synchronizer#writeNBT} */
	int to() default SAVE;

	/**@return optional alternative data type to use for NBT storage.
	 * Defaults to {@link Type#Obj} which keeps the declared Java type.
	 * Non primitive type values must always use {@link Type#Obj} or {@link Type#Fix}! */
	Type type() default Type.Obj;

	/**@return optional alternative nbt tag name.
	 * By default the field/method name is used as tag name.*/
	String tag() default "";

	public enum Type {
		/** keep Java type*/
		Obj(Object.class, -1),
		/** don't override value */
		Fix(Object.class, -1),
		/** 1-bit (boolean)*/
		I1(boolean.class, 1),
		/** 8-bit signed integer (byte)*/
		I8(byte.class, 1),
		/** 16-bit signed integer (short)*/
		I16(short.class, 2),
		/** 32-bit signed integer (int)*/
		I32(int.class, 4),
		/** 64-bit signed integer (long)*/
		I64(long.class, 8),
		/** 32-bit floating point (float)*/
		F32(float.class, 4),
		/** 64-bit floating point (double)*/
		F64(double.class, 8),
		/** 8-bit encoded non null enum */
		Enum(Enum.class, 1),
		/** 8-bit encoded nullable enum */
		Enum0(Enum.class, 1);

		final Class<?> inType;
		final MethodHandle read, write, comp, update;
		final int size;

		private Type(Class<?> inType, int size) {
			this.inType = inType;
			this.size = size;
			try {
				if (inType.isPrimitive()) {
					Lookup l = MethodHandles.lookup();
					this.read = l.unreflect(Type.class.getDeclaredMethod("read" + name(), INBT.class));
					this.write = l.unreflect(Type.class.getDeclaredMethod("write" + name(), inType));
					this.comp = l.unreflect(Type.class.getDeclaredMethod("check" + name(), inType, ByteBuffer.class));
					String name = inType.getName();
					this.update = l.unreflect(ByteBuf.class.getDeclaredMethod(
						"read" + Character.toUpperCase(name.charAt(0)) + name.substring(1)
					));
				} else if (inType == Enum.class) {
					Lookup l = MethodHandles.lookup();
					this.read = l.unreflect(Type.class.getDeclaredMethod("read" + name(), INBT.class, Enum[].class));
					this.write = l.unreflect(Type.class.getDeclaredMethod("write" + name(), inType));
					this.comp = l.unreflect(Type.class.getDeclaredMethod("check" + name(), inType, ByteBuffer.class));
					this.update = l.unreflect(Type.class.getDeclaredMethod("read" + name(), ByteBuf.class, Enum[].class));
				} else {
					this.read = null;
					this.write = null;
					this.comp = null;
					this.update = null;
				}
			} catch(IllegalAccessException | NoSuchMethodException | SecurityException e) {
				throw new RuntimeException(e);
			}
		}

		public Type actual(Class<?> type) {
			//if (type.isArray()) type = type.getComponentType();
			if (type.isEnum())
				if (inType == Enum.class) return this;
				else return Enum;
			if (!type.isPrimitive()) return Obj;
			if (inType.isPrimitive()) return this;
			for (Type t : values())
				if (t.inType == type)
					return t;
			return this;
		}

		public MethodHandle read(Object[] enums) {
			return enums == null ? read
				: MethodHandles.insertArguments(read, 1, (Object)enums);
		}

		public MethodHandle update(Object[] enums) {
			return enums == null ? update
				: MethodHandles.insertArguments(update, 1, (Object)enums);
		}

		static boolean readI1(INBT nbt) {return nbt instanceof NumberNBT && ((NumberNBT)nbt).getAsByte() != 0;}
		static byte readI8(INBT nbt) {return nbt instanceof NumberNBT ? ((NumberNBT)nbt).getAsByte() : 0;}
		static short readI16(INBT nbt) {return nbt instanceof NumberNBT ? ((NumberNBT)nbt).getAsShort() : 0;}
		static int readI32(INBT nbt) {return nbt instanceof NumberNBT ? ((NumberNBT)nbt).getAsInt() : 0;}
		static long readI64(INBT nbt) {return nbt instanceof NumberNBT ? ((NumberNBT)nbt).getAsLong() : 0L;}
		static float readF32(INBT nbt) {return nbt instanceof NumberNBT ? ((NumberNBT)nbt).getAsFloat() : 0F;}
		static double readF64(INBT nbt) {return nbt instanceof NumberNBT ? ((NumberNBT)nbt).getAsDouble() : 0D;}
		static <E extends Enum<E>> E readEnum(INBT nbt, E[] values) {
			int i = readI8(nbt) & 0xff;
			return i < values.length ? values[i] : null;
		}
		static <E extends Enum<E>> E readEnum(ByteBuf buf, E[] values) {
			int i = buf.readUnsignedByte();
			return i < values.length ? values[i] : null;
		}
		static <E extends Enum<E>> E readEnum0(INBT nbt, E[] values) {
			return values[Math.min(readI8(nbt) & 0xff, values.length - 1)];
		}
		static <E extends Enum<E>> E readEnum0(ByteBuf buf, E[] values) {
			return values[Math.min(buf.readUnsignedByte(), values.length - 1)];
		}
		static INBT writeI1(boolean val) {return ByteNBT.valueOf(val);}
		static INBT writeI8(byte val) {return ByteNBT.valueOf(val);}
		static INBT writeI16(short val) {return ShortNBT.valueOf(val);}
		static INBT writeI32(int val) {return IntNBT.valueOf(val);}
		static INBT writeI64(long val) {return LongNBT.valueOf(val);}
		static INBT writeF32(float val) {return FloatNBT.valueOf(val);}
		static INBT writeF64(double val) {return DoubleNBT.valueOf(val);}
		static INBT writeEnum(Enum<?> val) {return writeI8((byte)val.ordinal());}
		static INBT writeEnum0(Enum<?> val) {return writeI8((byte)(val == null ? -1 : val.ordinal()));}
		static boolean checkI1(boolean val, ByteBuffer state) {
			if (state.get() == 0 ^ val) return false;
			state.put(state.position() - 1, (byte)(val ? 1 : 0));
			return true;
		}
		static boolean checkI8(byte val, ByteBuffer state) {
			if (val == state.get()) return false;
			state.put(state.position() - 1, val);
			return true;
		}
		static boolean checkI16(short val, ByteBuffer state) {
			if (val == state.getShort()) return false;
			state.putShort(state.position() - 2, val);
			return true;
		}
		static boolean checkI32(int val, ByteBuffer state) {
			if (val == state.getInt()) return false;
			state.putInt(state.position() - 4, val);
			return true;
		}
		static boolean checkI64(long val, ByteBuffer state) {
			if (val == state.getLong()) return false;
			state.putLong(state.position() - 8, val);
			return true;
		}
		static boolean checkF32(float val, ByteBuffer state) {return checkI32(Float.floatToIntBits(val), state);}
		static boolean checkF64(double val, ByteBuffer state) {return checkI64(Double.doubleToLongBits(val), state);}
		static boolean checkEnum(Enum<?> val, ByteBuffer state) {return checkI8((byte)val.ordinal(), state);}
		static boolean checkEnum0(Enum<?> val, ByteBuffer state) {return checkI8((byte)(val == null ? -1 : val.ordinal()), state);}
	}

}
