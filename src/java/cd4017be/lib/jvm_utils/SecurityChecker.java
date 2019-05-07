package cd4017be.lib.jvm_utils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Used to check class data from non trustworthy sources (like item NBT-data) to prevent people from using exploits to run malicious code on the server's JVM.
 * @author CD4017BE
 */
public class SecurityChecker {

	private static final Set<String> ANY = Collections.singleton("*");
	private final HashMap<String, Set<String>> whitelist = new HashMap<>();

	/**
	 * Create a new SecurityChecker which rejects all but the given classes (+ own & super class).
	 * @param whitelist list of allowed classes
	 */
	public SecurityChecker() {
	}

	public SecurityChecker put(Class<?> c, String... members) {
		String name = c.getName().replace('.', '/');
		Set<String> set = whitelist.get(name);
		if (set == null) whitelist.put(name, set = new HashSet<>());
		for (String s : members)
			set.add(s.replace('.', '/'));
		return this;
	}

	public SecurityChecker putAll(Class<?> c) {
		String name = c.getName().replace('.', '/');
		whitelist.put(name, ANY);
		return this;
	}

	/**
	 * verifies the given class file
	 * @param c class file data
	 * @throws SecurityException if it accesses non white-listed classes and members
	 */
	public void verify(byte[] c) throws SecurityException {
		ArrayList<String> violations = new ArrayList<>();
		try {
			ByteBuffer b = ByteBuffer.wrap(c);
			b.position(8);
			int n = b.getShort() - 1;
			String[] utf8s = new String[n];
			int[] ref = new int[n];
			byte[] tags = new byte[n];
			for (int i = 0; i < n; i++) {
				byte tag = b.get();
				if (tag == 1) {
					byte[] arr = new byte[b.getShort()];
					b.get(arr);
					utf8s[i] = new String(arr, ClassUtils.UTF8);
				} else {
					int l = getLength(tag);
					if (l == 2) {
						ref[i] = b.getShort();
					} else if (l == 4) {
						ref[i] = b.getInt();
					} else b.position(b.position() + l);
					tags[i] = tag;
				}
			}
			b.position(b.position() + 2);
			short tc = b.getShort();
			for (int i = 0; i < n; i++) {
				byte tag = tags[i];
				int e = ref[i];
				String s;
				switch(tag) {
				case 7:
					if (i+1 == tc) continue; //access to own class is always allowed
					s = utf8s[e - 1];
					if (!whitelist.containsKey(s))
						violations.add(s);
					break;
				case 9:
				case 10:
				case 11:
					int e1 = e >>> 16;
					if (e1 == tc) continue; //access to own class is always allowed
					s = utf8s[ref[e1 - 1] - 1];
					Set<String> set = whitelist.get(s);
					if (set != null && set != ANY) {
						e = ref[(e & 0xffff) - 1];
						String s1 = utf8s[(e >>> 16) - 1];
						if (tag != 9) s1 += utf8s[(e & 0xffff) - 1];
						if (set.contains(s1)) continue;
						violations.add(s + "." + s1);
					}
				}
			}
		} catch (Exception e) {
			throw new SecurityException(e);
		}
		if (!violations.isEmpty()) {
			StringBuilder sb = new StringBuilder("Runtime assembled code may potentially access the following illegal objects:\n");
			for (String s : violations)
				sb.append(s).append(", ");
			sb.delete(sb.length() - 2, sb.length());
			throw new SecurityException(sb.toString());
		}
	}

	private int getLength(byte tag) {
		switch(tag) {
		case 1: //Utf8
		case 7: //Class
		case 8: //String
			return 2;
		case 3: //Int
		case 4: //Float
		case 9: //Field
		case 10: //Method
		case 11: //Interface
		case 12: //Name&Type
			return 4;
		case 5: //Long
		case 6: //Double
			return 8;
		/*
		case 15: //Method Handle
			return 3;
		case 16: //Method Type
			return 2;
		case 18: //Invoke Dynamic
			return 4;
		*/
		default:
			throw new IllegalArgumentException();
		}
	}

}
