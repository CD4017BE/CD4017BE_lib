package cd4017be.lib.jvm_utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import static cd4017be.lib.jvm_utils.ClassAssembler.*;

/**
 * Utilities for runtime creation of anonymous on demand classes.
 * @see #registerUnnamed
 * @author CD4017BE
 */
public class ClassUtils {

	public static final String NAME_BASE = "cd4017be.generated.C_";
	static final Charset UTF8 = Charset.forName("UTF-8");
	static final HashFunction hashfunc = Hashing.murmur3_128();
	static final HashMap<UUID, String> registered = new HashMap<>();
	static int lastIdx = 0;

	/**
	 * Registers a (unnamed) runtime generated class by uuid
	 * @param uuid unique identifier of the class to generate (used to avoid duplicates)
	 * @param generator function that provides the class file data for a given name in case the class doesn't exist yet
	 * @return enumerated name of the registered class
	 * @see #hash
	 * @see #generator
	 * @see ClassAssembler#genClass
	 * @see NBT2Class
	 */
	public static String registerUnnamed(UUID uuid, Function<String, byte[]> generator) {
		String name = registered.get(uuid);
		if (name != null) return name;
		name = NAME_BASE + Integer.toHexString(lastIdx++);
		registered.put(uuid, name);
		INSTANCE.register(name, generator);
		return name;
	}

	/**
	 * @param tag optional tag string (gets included in the hash)
	 * @param data the byte data to hash
	 * @return 128-bit hash of the supplied data and type
	 */
	public static UUID hash(String tag, byte[] data) {
		HashCode hash;
		if (tag != null) {
			byte[] typeb = tag.getBytes(UTF8);
			hash = hashfunc.newHasher(data.length + typeb.length)
					.putBytes(typeb).putBytes(data).hash();
		} else hash = hashfunc.hashBytes(data);
		ByteBuffer buf = ByteBuffer.wrap(hash.asBytes());
		return new UUID(buf.getLong(), buf.getLong());
	}

	/**
	 * Generator for use with {@link #registerUnnamed} that automatically replaces the class name
	 * @param interfaces implemented interfaces (optional)
	 * @param cpt constant pool table
	 * @param fields list of encoded fields
	 * @param methods list of encoded methods
	 * @param attributes attribute table (optional)
	 * @return class file generator function
	 */
	public static Function<String, byte[]> generator(List<Class<?>> interfaces, ConstantPool cpt, List<byte[]> fields, List<byte[]> methods, Map<Short, byte[]> attributes) {
		return (name)-> {
			cpt.setUtf8(ConstantPool.THIS_NAME, name.replace('.', '/'));
			return genClass(interfaces, cpt, fields, methods, attributes);
		};
	}

	/**
	 * @param name the name of a registered class
	 * @param type cast type for return value (typically the template superclass)
	 * @return a instance of the registered class
	 */
	public static <T> T makeInstance(String name, Class<T> type) {
		try {
			return type.cast(Class.forName(name, true, INSTANCE).newInstance());
		} catch (ClassNotFoundException e) {
			return null;
		} catch (ClassFormatError | VerifyError e) {
			e.printStackTrace();
			return null;
		} catch (InstantiationException e) {
			e.printStackTrace();
			return null;
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			return null;
		} catch (ClassCastException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static Class<?> getClassOrNull(String name) {
		try {
			return Class.forName(name);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

	public static Method getMethodOrNull(Class<?> owner, String name, Class<?>... args) {
		try {
			return owner == null ? null : owner.getMethod(name, args);
		} catch (NoSuchMethodException e) {
			return null;
		}
	}

	public static Field getFieldOrNull(Class<?> owner, String name) {
		if (owner == null) return null;
		try {
			Field f = owner.getDeclaredField(name);
			f.setAccessible(true);
			return f; 
		} catch (NoSuchFieldException e) {
			return null;
		}
	}

}
