package cd4017be.lib.jvm_utils;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import static cd4017be.lib.jvm_utils.ConstantPool.*;

/**
 * ClassLoader for loading runtime generated classes.<br>
 * Also provides utilities for assembling such classes.
 * @see #register
 * @author CD4017BE
 */
public class ClassAssembler extends ClassLoader {

	public static final ClassAssembler INSTANCE = new ClassAssembler();
	/** encoded method data for the default constructor that simply calls {@code super();} */
	public static final byte[] DEFAULT_CONSTR;

	/** JVM op codes */
	public static final byte
		_nop = 0, _aconst_null = 1, //constants
		_iconst_m1 = 2, _iconst_0 = 3, _iconst_1 = 4, _iconst_2 = 5, _iconst_3 = 6, _iconst_4 = 7, _iconst_5 = 8,
		_lconst_0 = 9, _lconst_1 = 10, _fconst_0 = 11, _fconst_1 = 12, _fconst_2 = 13, _dconst_0 = 14, _dconst_1 = 15,
		_bipush = 16, _sipush = 17, _ldc = 18, _ldc_w = 19, _ldc2_w = 20,
		_iload = 21, _lload = 22, _fload = 23, _dload = 24, _aload = 25, //loads
		_iload_0 = 26, _iload_1 = 27, _iload_2 = 28, _iload_3 = 29,
		_lload_0 = 30, _lload_1 = 31, _lload_2 = 32, _lload_3 = 33,
		_fload_0 = 34, _fload_1 = 35, _fload_2 = 36, _fload_3 = 37,
		_dload_0 = 38, _dload_1 = 39, _dload_2 = 40, _dload_3 = 41,
		_aload_0 = 42, _aload_1 = 43, _aload_2 = 44, _aload_3 = 45,
		_iaload = 46, _laload = 47, _faload = 48, _daload = 49, _aaload = 50, _baload = 51, _caload = 52, _saload = 53,
		_istore = 54, _lstore = 55, _fstore = 56, _dstore = 57, _astore = 58, //stores
		_istore_0 = 59, _istore_1 = 60, _istore_2 = 61, _istore_3 = 62,
		_lstore_0 = 63, _lstore_1 = 64, _lstore_2 = 65, _lstore_3 = 66,
		_fstore_0 = 67, _fstore_1 = 68, _fstore_2 = 69, _fstore_3 = 70,
		_dstore_0 = 71, _dstore_1 = 72, _dstore_2 = 73, _dstore_3 = 74,
		_astore_0 = 75, _astore_1 = 76, _astore_2 = 77, _astore_3 = 78,
		_iastore = 79, _lastore = 80, _fastore = 81, _dastore = 82, _aastore = 83, _bastore = 84, _castore = 85, _sastore = 86,
		_pop = 87, pop2 = 88, _dup = 89, _dup_x1 = 90, _dup_x2 = 91, _dup2 = 92, _dup2_x1 = 93, dup2_x2 = 94, _swap = 95, //stack
		_iadd = 96, _isub = 100, _imul = 104, _idiv = 108, _irem = 112, _ineg = 116, //artihmetic
		_ladd = 97, _lsub = 101, _lmul = 105, _ldiv = 109, _lrem = 113, _lneg = 117,
		_fadd = 98, _fsub = 102, _fmul = 106, _fdiv = 110, _frem = 114, _fneg = 118,
		_dadd = 99, _dsub = 103, _dmul = 107, _ddiv = 111, _drem = 115, _dneg = 119,
		_ishl = 120, _ishr = 122, _iushr = 124, _iand = 126, _ior = (byte)128, _ixor = (byte)130, _iinc = (byte)132,
		_lshl = 121, _lshr = 123, _lushr = 125, _land = 127, _lor = (byte)129, _lxor = (byte)131,
		_i2l = (byte)133, _i2f = (byte)134, _i2d = (byte)135, _i2b = (byte)145, _i2c = (byte)146, _i2s = (byte)147, //conversion
		_l2i = (byte)136, _l2f = (byte)137, _l2d = (byte)138, _f2i = (byte)139, _f2l = (byte)140, _f2d = (byte)141,
		_d2i = (byte)142, _d2l = (byte)143, _d2f = (byte)144,
		_lcmp = (byte)148, _fcmpl = (byte)149, _fcmpg = (byte)150, _dcmpl = (byte)151, _dcmpg = (byte)152, //compare
		_ifeq = (byte)153, _ifne = (byte)154, _iflt = (byte)155, _ifle = (byte)156, _ifgt = (byte)157, _ifge = (byte)158,
		_if_icmpeq = (byte)159, _if_icmpne = (byte)160, _if_icmplt = (byte)161, _if_icmpge = (byte)162, _if_icmpgt = (byte)163, _if_icmple = (byte)164,
		_if_acmpeq = (byte)165, _if_acmpne = (byte)166,
		_goto = (byte)167, _jsr = (byte)168, _ret = (byte)169, _tableswitch = (byte)170, _lookupswitch = (byte)171, //control
		_ireturn = (byte)172, _lreturn = (byte)173, _freturn = (byte)174, _dreturn = (byte)175, _areturn = (byte)176, _return = (byte)177,
		_getstatic = (byte)178, _putstatic = (byte)179, _getfield = (byte)180, _putfield = (byte)181, //reference
		_invokevirtual = (byte)182, _invokespecial = (byte)183, _invokestatic = (byte)184, _invokeinterface = (byte)185, _invokedynamic = (byte)186,
		_new = (byte)187, _newarray = (byte)188, _anewarray = (byte)189, _arraylength = (byte)190,
		_athrow = (byte)191, _checkcast = (byte)192, _instanceof = (byte)193, _monitorenter = (byte)194, _monitorexit = (byte)195,
		_multianewarray = (byte)197, _ifnull = (byte)198, _ifnonnull = (byte)199;

	public static byte[] _iconst_(short val) {
		if (val >= -1 && val <= 5)
			return new byte[] {(byte)(_iconst_0 + val)};
		else if (val >= -256 && val < 256)
			return new byte[] {_bipush, (byte)val};
		else
			return new byte[] {_sipush, (byte)(val >> 8), (byte)val};
	}

	static {
		ConstantPool cpt = new ConstantPool(ClassUtils.NAME_BASE, Object.class);
		DEFAULT_CONSTR = genMethod(0x01, cpt.putUtf8("<init>"), cpt.putUtf8("()V"), genCode(1, 1, new byte[] {
			_aload_0,
			_invokespecial, (byte)(SUPER_CONSTR>>8), (byte)SUPER_CONSTR,
			_return,
		}, null, null, cpt));
	}

	private HashMap<String, Function<String, byte[]>> scheduled = new HashMap<>();

	private ClassAssembler() {
		super(ClassAssembler.class.getClassLoader());
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		Function<String, byte[]> gen = scheduled.remove(name);
		if (gen == null) throw new ClassNotFoundException(name);
		byte[] data = gen.apply(name);
		return defineClass(name, data, 0, data.length);
	}

	/**
	 * Registers a runtime generated class with a specific name
	 * @param name name of the class to create
	 * @param generator function that provides the class file data for a given name in case the class doesn't exist yet
	 * @return whether a class with that name was already registered
	 */
	public boolean register(String name, Function<String, byte[]> generator) {
		if (findLoadedClass(name) != null) return true;
		return scheduled.put(name, generator) != null;
	}

	/**
	 * Create a new class
	 * @param parent super class
	 * @param interfaces implemented interfaces (optional)
	 * @param cpt constant pool table
	 * @param fields list of encoded fields
	 * @param methods list of encoded methods
	 * @param attributes attribute table (optional)
	 * @return byte data encoded in the class file format
	 * @see #newConstPool
	 * @see #genField
	 * @see #genMethod
	 */
	public static byte[] genClass(List<Class<?>> interfaces, ConstantPool cpt, List<byte[]> fields, List<byte[]> methods, Map<Short, byte[]> attributes) {
		//interfaces
		short[] iidx;
		if (interfaces != null) {
			iidx = new short[interfaces.size()];
			int i = 0;
			for (Class<?> c : interfaces)
				iidx[i++] = cpt.putClass(c.getName());
		} else iidx = new short[0];
		if (fields == null) fields = Collections.emptyList();
		//count required bytes
		int n = 2 * iidx.length + 22 + cpt.getSize();
		for (byte[] arr : fields) n += arr.length;
		for (byte[] arr : methods) n += arr.length;
		//write data
		ByteBuffer b = writeAttributes(n, attributes);
		b.putInt(0xCAFEBABE); //magic
		b.putShort((short)0).putShort((short)52); //version 52.0 (JDK 1.8)
		b.putShort((short)cpt.getCount()); //const pool count
			cpt.write(b); //const pool entries
		b.putShort((short)0x1011).putShort(THIS_CLASS).putShort(SUPER_CLASS); //public final class ... extends ...
		b.putShort((short)iidx.length); //interfaces count
		for (short idx : iidx) b.putShort(idx); //interfaces list
		b.putShort((short)fields.size()); //field count
		for (byte[] arr : fields) b.put(arr); //field list
		b.putShort((short)methods.size()); //method count
		for (byte[] arr : methods) b.put(arr); //method list
		return b.array();
	}

	/**
	 * Create a field
	 * @param acc flags &0x01:public, &0x02:private, &0x04:protected, &0x08:static, 0x10:final, 0x40:volatile, 0x80:transient
	 * @param descr field descriptor like: {@code Ljava.lang.Object; fieldname}
	 * @param attributes attribute table
	 * @param cpt constant pool table
	 * @return encoded byte data
	 */
	public static byte[] genField(int acc, String descr, Map<Short, byte[]> attributes, ConstantPool cpt) {
		int i = descr.indexOf(' ');
		if (i < 0) throw new IllegalArgumentException();
		ByteBuffer b = writeAttributes(6, attributes);
		b.putShort((short)(acc & 0x00df | 0x1000));
		b.putShort(cpt.putUtf8(descr.substring(i + 1)));
		b.putShort(cpt.putUtf8(descr.substring(0, i).replace('.', '/')));
		return b.array();
	}

	/**
	 * Create a method
	 * @param acc flags &0x01:public, &0x02:private, &0x04:protected, &0x08:static, 0x10:final, 0x20:synchronized, 0x80:varargs
	 * @param name method name index
	 * @param descr method descriptor index
	 * @param attributes attribute table (containing the code)
	 * @return encoded byte data
	 * @see #genCode
	 */
	public static byte[] genMethod(int acc, short name, short descr, Map<Short, byte[]> attributes) {
		ByteBuffer b = writeAttributes(6, attributes);
		b.putShort((short)(acc & 0x00bf | 0x1000));
		b.putShort(name).putShort(descr);
		return b.array();
	}

	/**
	 * Create the code attribute for a method
	 * @param locals number of local variables
	 * @param stack maximum stack depth
	 * @param code op code array
	 * @param exceptions exception handlers (optional)
	 * @param stackMap stack map table entries (optional)
	 * @param cpt constant pool table
	 * @return method attribute table containing the code
	 * @see #catchEx
	 */
	public static Map<Short, byte[]> genCode(int locals, int stack, byte[] code, long[] exceptions, List<byte[]> stackMap, ConstantPool cpt) {
		int nEx = exceptions != null ? exceptions.length : 0, lSM;
		if (stackMap != null && !stackMap.isEmpty()) {
			lSM = 2;
			for (byte[] e : stackMap)
				lSM += e.length;
		} else lSM = 0;
		int l = code.length;
		ByteBuffer b = ByteBuffer.allocate(l + 12 + nEx * 8 + (lSM > 0 ? lSM + 6 : 0));
		b.putShort((short)stack).putShort((short)locals);
		b.putInt(l); //code length
		b.put(code); //code
		if (exceptions != null) {
			b.putShort((short)exceptions.length); //exceptions count
			for (long eh : exceptions)
				b.putLong(eh); //exception handlers
		} else b.putShort((short)0);
		if (lSM > 0) {
			b.putShort((short)1);
			b.putShort(cpt.putUtf8("StackMapTable"));
			b.putInt(lSM);
			b.putShort((short)stackMap.size());
			for (byte[] e : stackMap)
				b.put(e);
		} else b.putShort((short)0); //attributes count
		return Collections.singletonMap(cpt.putUtf8("Code"), b.array());
	}

	/**
	 * Encode an exception handler
	 * @param start start_pc [S]
	 * @param end end_pc [E]
	 * @param handler handler_pc [H]
	 * @param type catch_type [T]
	 * @param cpt constant pool table
	 * @return 0xSSSSEEEEHHHHTTTT
	 */
	public static long catchEx(int start, int end, int handler, Class<?extends Throwable> type, ConstantPool cpt) {
		return (long)(start & 0xffff) << 48
				| (long)(end & 0xffff) << 32
				| (long)(handler & 0xffff) << 16
				| (long)(cpt.putClass(type.getName()) & 0xffff);
	}

	/**
	 * @param ofs number of bytes reserved at the beginning
	 * @param attributes attribute table
	 * @return new ByteBuffer (positioned to 0) and with attributes already written
	 */
	private static ByteBuffer writeAttributes(int ofs, Map<Short, byte[]> attributes) {
		if (attributes == null) return ByteBuffer.allocate(ofs + 2);
		int l = attributes.size(), j = 0, n = 0;
		short[] keys = new short[l];
		byte[][] values = new byte[l][];
		for (Entry<Short, byte[]> e : attributes.entrySet()) {
			keys[j] = e.getKey();
			n += (values[j++] = e.getValue()).length;
		}
		ByteBuffer b = ByteBuffer.allocate(6 * l + n + ofs + 2);
		b.position(ofs);
		b.putShort((short)l);
		for (j = 0; j < l; j++) {
			b.putShort(keys[j]);
			byte[] arr = values[j];
			b.putInt(arr.length).put(arr);
		}
		b.position(0);
		return b;
	}

}
