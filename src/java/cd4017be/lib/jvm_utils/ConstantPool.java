package cd4017be.lib.jvm_utils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Both a List and a Set for byte arrays with some utility methods for creating the various constant pool table entries.
 * @author CD4017BE
 */
public class ConstantPool {

	/** default preinitialized constant pool table indices */
	public static final short THIS_NAME = 1, THIS_CLASS = 2, SUPER_CLASS = 4, CODE = 5, SUPER_CONSTR = 9;
	/** number of preinitialized constant pool table entries */
	public static final int PREINIT_LEN = 10;
	public static final byte[] EMPTY = new byte[0];

	private final ArrayList<byte[]> entries = new ArrayList<>();
	private int size = 0;

	public ConstantPool() {
	}

	/**
	 * Create a preinitialized constant pool table
	 * @param name class to create for
	 * @param parent super class
	 */
	public ConstantPool(String name, Class<?> parent) {
		add(EMPTY); //0
		putClass(name); //2 (name=1)
		putClass(parent.getName());//4 (name=3)
		add(constUtf8("Code"));//5
		putFieldMethod(parent, "<init>()V");//9 (descr=8 (name=6, type=7))
	}

	/**
	 * append the given entry to the end of this table
	 * @param entry data to add
	 * @return index where the given entry was stored (= getCount())
	 */
	public short add(byte[] entry) {
		ArrayList<byte[]> entries = this.entries;
		int k = entries.size();
		entries.add(entry);
		size += entry.length;
		return (short)k;
	}

	/**
	 * add the given entry to this table if it didn't already exist
	 * @param entry data to add
	 * @return index where the given entry is stored
	 */
	public short put(byte[] entry) {
		int k = 0;
		for (byte[] arr : entries) {
			if (Arrays.equals(arr, entry))
				return (short)k;
			k++;
		}
		return add(entry);
	}

	/**
	 * replace the entry at given index with new data
	 * @param idx index
	 * @param entry new data
	 */
	public void set(int idx, byte[] entry) {
		size += entry.length - entries.set(idx, entry).length;
	}

	/**
	 * @param idx index
	 * @return entry at given index
	 */
	public byte[] get(int idx) {
		return entries.get(idx);
	}

	/**
	 * @return amount of entries in this table
	 */
	public int getCount() {
		return entries.size();
	}

	/**
	 * @return size of the total table in bytes
	 */
	public int getSize() {
		return size;
	}

	/**
	 * write this table to the given buffer
	 * @param buf ByteBuffer to store in
	 */
	public void write(ByteBuffer buf) {
		for (byte[] arr : entries)
			buf.put(arr);
	}

	/**
	 * write this table to the given array
	 * @param array byte array to store in
	 * @param ofs offset in array to start at
	 */
	public void write(byte[] array, int ofs) {
		for (byte[] arr : entries) {
			int l = arr.length;
			System.arraycopy(arr, 0, array, ofs, l);
			ofs += l;
		}
	}

	/**
	 * @return a single byte array containing all entries in order
	 */
	public byte[] toArray() {
		byte[] data = new byte[size];
		write(data, 0);
		return data;
	}

	private static byte[] constUtf8(String s) {
		byte[] data = s.getBytes(ClassUtils.UTF8);
		int l = data.length;
		byte[] res = new byte[l + 3];
		res[0] = 1;
		res[1] = (byte)(l >> 8);
		res[2] = (byte)l;
		System.arraycopy(data, 0, res, 3, l);
		return res;
	}

	/**
	 * changes the value of a string entry
	 * @param idx entry index
	 * @param s new String value
	 */
	public void setUtf8(int idx, String s) {
		set(idx, constUtf8(s));
	}

	/**
	 * reference a String
	 * @param s String
	 * @return constant pool index
	 */
	public short putUtf8(String s) {
		return put(constUtf8(s));
	}

	/**
	 * create an int constant
	 * @param val int value
	 * @return constant pool index
	 */
	public short putInt(int val) {
		return put(new byte[] {3, (byte)(val >> 24), (byte)(val >> 16), (byte)(val >> 8), (byte)val});
	}

	/**
	 * create a float constant
	 * @param val float value
	 * @return constant pool index
	 */
	public short putFloat(float val) {
		int i = Float.floatToIntBits(val);
		return put(new byte[] {4, (byte)(i >> 24), (byte)(i >> 16), (byte)(i >> 8), (byte)i});
	}

	/**
	 * create an long constant
	 * @param val long value
	 * @return constant pool index
	 */
	public short putLong(long val) {
		return put(new byte[] {5, (byte)(val >> 56), (byte)(val >> 48), (byte)(val >> 40), (byte)(val >> 32), (byte)(val >> 24), (byte)(val >> 16), (byte)(val >> 8), (byte)val});
	}

	/**
	 * create an double constant
	 * @param val double value
	 * @return constant pool index
	 */
	public short putDouble(double val) {
		long i = Double.doubleToLongBits(val);
		return put(new byte[] {6, (byte)(i >> 56), (byte)(i >> 48), (byte)(i >> 40), (byte)(i >> 32), (byte)(i >> 24), (byte)(i >> 16), (byte)(i >> 8), (byte)i});
	}

	/**
	 * reference a class or interface
	 * @param clsName class or interface name like: {@code java.lang.Object}
	 * @return constant pool index
	 */
	public short putClass(String clsName) {
		short i = putUtf8(clsName.replace('.', '/'));
		return put(new byte[] {7, (byte)(i >> 8), (byte)i});
	}

	/**
	 * create a String constant
	 * @param s String value
	 * @return constant pool index
	 */
	public short putString(String s) {
		short i = putUtf8(s);
		return put(new byte[] {8, (byte)(i >> 8), (byte)i});
	}

	/**
	 * reference a field or method
	 * @param c owner class or null for this
	 * @param desc field descriptor like: {@code Ljava.lang.Object; fieldname} <br>or method descriptor like: {@code methodName(Ljava.lang.String;I)I}
	 * @return constant pool index
	 */
	public short putFieldMethod(Class<?> c, String desc) {
		byte tag;
		String name, type;
		int i = desc.indexOf('(');
		if (i < 0) {
			tag = FIELD;
			if ((i = desc.indexOf(' ')) < 0)
				throw new IllegalArgumentException();
			name = desc.substring(i + 1);
			type = desc.substring(0, i);
		} else {
			tag = c != null && c.isInterface() ? INTERFACE : METHOD;
			name = desc.substring(0, i);
			type = desc.substring(i);
		}
		return putFieldMethod(c == null ? THIS_CLASS : putClass(c.getName()), putNameType(name, type), tag);
	}

	public static final byte FIELD = 9, METHOD = 10, INTERFACE = 11;

	/**
	 * reference a field or method
	 * @param ci owner class index
	 * @param desc field or method name and type index
	 * @param tag one of {@link #FIELD}, {@link #METHOD} or {@link #INTERFACE}
	 * @return constant pool index
	 */
	public short putFieldMethod(short c, short desc, byte tag) {
		return put(new byte[] {tag, (byte)(c >> 8), (byte)c, (byte)(desc >> 8), (byte)desc});
	}

	/**
	 * reference a name and type
	 * @param name field name or method name
	 * @param desc field type like: {@code Ljava.lang.Object;} <br>or method descriptor like: {@code (Ljava.lang.String;I)I}
	 * @return constant pool index
	 */
	public short putNameType(String name, String desc) {
		return putNameType(putUtf8(name), putUtf8(desc.replace('.', '/')));
	}

	/**
	 * reference a name and type
	 * @param name index of field name or method name
	 * @param desc field type index
	 * @return constant pool index
	 */
	public short putNameType(short name, short desc) {
		return put(new byte[] {12, (byte)(name >> 8), (byte)name, (byte)(desc >> 8), (byte)desc});
	}

}
