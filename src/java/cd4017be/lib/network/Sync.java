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
		F64(double.class, 8);

		final Class<?> inType;
		final MethodHandle read, write, comp, update;
		final int size;

		private Type(Class<?> inType, int size) {
			this.inType = inType;
			this.size = size;
			if (inType.isPrimitive()) try {
				Lookup l = MethodHandles.lookup();
				this.read = l.unreflect(Type.class.getDeclaredMethod("read" + name(), NBTBase.class));
				this.write = l.unreflect(Type.class.getDeclaredMethod("write" + name(), inType));
				this.comp = l.unreflect(Type.class.getDeclaredMethod("check" + name(), inType, ByteBuffer.class));
				String name = inType.getName();
				this.update = l.unreflect(ByteBuf.class.getDeclaredMethod(
					"read" + Character.toUpperCase(name.charAt(0)) + name.substring(1)
				));
			} catch(IllegalAccessException | NoSuchMethodException | SecurityException e) {
				throw new RuntimeException(e);
			} else {
				this.read = null;
				this.write = null;
				this.comp = null;
				this.update = null;
			}
		}

		public Type actual(Class<?> type) {
			//if (type.isArray()) type = type.getComponentType();
			if (!type.isPrimitive()) return Obj;
			if (inType.isPrimitive()) return this;
			for (Type t : values())
				if (t.inType == type)
					return t;
			return this;
		}

		static boolean readI1(NBTBase nbt) {return nbt instanceof NBTPrimitive && ((NBTPrimitive)nbt).getByte() != 0;}
		static byte readI8(NBTBase nbt) {return nbt instanceof NBTPrimitive ? ((NBTPrimitive)nbt).getByte() : 0;}
		static short readI16(NBTBase nbt) {return nbt instanceof NBTPrimitive ? ((NBTPrimitive)nbt).getShort() : 0;}
		static int readI32(NBTBase nbt) {return nbt instanceof NBTPrimitive ? ((NBTPrimitive)nbt).getInt() : 0;}
		static long readI64(NBTBase nbt) {return nbt instanceof NBTPrimitive ? ((NBTPrimitive)nbt).getLong() : 0L;}
		static float readF32(NBTBase nbt) {return nbt instanceof NBTPrimitive ? ((NBTPrimitive)nbt).getFloat() : 0F;}
		static double readF64(NBTBase nbt) {return nbt instanceof NBTPrimitive ? ((NBTPrimitive)nbt).getDouble() : 0D;}
		static NBTBase writeI1(boolean val) {return new NBTTagByte((byte)(val ? 1:0));}
		static NBTBase writeI8(byte val) {return new NBTTagByte(val);}
		static NBTBase writeI16(short val) {return new NBTTagShort(val);}
		static NBTBase writeI32(int val) {return new NBTTagInt(val);}
		static NBTBase writeI64(long val) {return new NBTTagLong(val);}
		static NBTBase writeF32(float val) {return new NBTTagFloat(val);}
		static NBTBase writeF64(double val) {return new NBTTagDouble(val);}
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
	}

}
