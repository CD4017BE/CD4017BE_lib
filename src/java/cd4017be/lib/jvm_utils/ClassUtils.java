package cd4017be.lib.jvm_utils;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.UUID;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

/**
 * Utilities for runtime creation of anonymous on demand classes.
 * @see #registerUnnamed
 * @author CD4017BE
 */
public class ClassUtils {

	private static final HashFunction hashfunc = Hashing.murmur3_128();

	/**
	 * @param tag optional tag string (gets included in the hash)
	 * @param data the byte data to hash
	 * @return 128-bit hash of the supplied data and type
	 */
	public static UUID hash(String tag, byte[] data) {
		HashCode hash;
		if (tag != null) {
			byte[] typeb = tag.getBytes(UTF_8);
			hash = hashfunc.newHasher(data.length + typeb.length)
					.putBytes(typeb).putBytes(data).hash();
		} else hash = hashfunc.hashBytes(data);
		ByteBuffer buf = ByteBuffer.wrap(hash.asBytes());
		return new UUID(buf.getLong(), buf.getLong());
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
