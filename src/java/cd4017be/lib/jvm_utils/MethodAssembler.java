package cd4017be.lib.jvm_utils;

import static cd4017be.lib.jvm_utils.ClassAssembler.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Map;

import cd4017be.lib.jvm_utils.ConstantPool;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.IntArrayList;

/**
 * @author CD4017BE
 *
 */
public class MethodAssembler {

	private final IntArrayList locals, stack;
	/** instructions */
	public final ByteBuf code;
	/** constant pool table */
	public final ConstantPool cpt;
	private int maxStack;
	private ArrayList<byte[]> frames;
	private int lastFrame = -1;

	/**
	 * Start assembling a new method
	 * @param cpt constant pool table
	 * @param param types of the method parameters
	 */
	public MethodAssembler(ConstantPool cpt, int... param) {
		this.locals = new IntArrayList(param);
		this.stack = new IntArrayList();
		this.code = Unpooled.buffer();
		this.cpt = cpt;
		this.frames = new ArrayList<>();
	}

	/**
	 * register a new local variable
	 * @param type variable type
	 * @return index
	 */
	public int newLocal(int type) {
		int i = locals.indexOf(0);
		if (i >= 0)
			locals.set(i, type);
		else {
			i = locals.size();
			locals.add(type);
		}
		return i;
	}

	/**
	 * unregister a local variable
	 * @param i index
	 */
	public void clearLocal(int i) {
		locals.set(i, 0);
	}

	/**
	 * mark that some stack entries changed type
	 * @param types new types of the top most stack entries
	 */
	public void change(int... types) {
		for (int l = stack.size(), i = l - types.length, j = 0; i < l; i++, j++)
			stack.set(i, types[j]);
	}

	/**
	 * mark that values were pushed on the stack
	 * @param types types of the added values
	 */
	public void push(int... types) {
		stack.addElements(stack.size(), types);
		int l = stack.size();
		if (l > maxStack) maxStack = l;
	}

	/**
	 * mark that values were pushed on the stack
	 * @param n number of times added
	 * @param type value type
	 */
	public void pushN(int n, int type) {
		for (int i = 0; i < n; i++)
			stack.add(type);
		int l = stack.size();
		if (l > maxStack) maxStack = l;
	}

	/**
	 * mark that values were popped from the stack
	 * @param n number of elements removed
	 */
	public void pop(int n) {
		int l = stack.size();
		stack.removeElements(l - n, l);
	}

	/**
	 * encode a constant integer pushed onto the operand stack
	 * @param v constant value
	 * @return number of bytes written
	 */
	public int pushConst(int v) {
		push(T_INT);
		if (v >= Short.MIN_VALUE && v <= Short.MAX_VALUE) {
			byte[] arr = _iconst_((short)v);
			code.writeBytes(arr);
			return arr.length;
		} else {
			short i = cpt.putInt(v);
			if (i < 256) {
				code.writeByte(_ldc).writeByte(i);
				return 2;
			} else {
				code.writeByte(_ldc_w).writeShort(i);
				return 3;
			}
		}
	}

	/**
	 * encode a store instruction
	 * @param i local variable index
	 * @return number of bytes written
	 */
	public int store(int i) {
		int t = locals.getInt(i);
		switch(t) {
		case T_INT: t = 0; pop(1); break;
		case T_LONG: t = 1; pop(2); break;
		case T_FLOAT: t = 2; pop(1); break;
		case T_DOUBLE: t = 3; pop(2); break;
		default: t = 4; pop(1);
		}
		if (i < 4) {
			code.writeByte(_istore_0 + i + t * 4);
			return 1;
		} else {
			code.writeByte(_istore + t).writeByte(i);
			return 2;
		}
	}

	/**
	 * encode a load instruction
	 * @param i local variable index
	 * @return number of bytes written
	 */
	public int load(int i) {
		int t = locals.getInt(i);
		switch(t) {
		case T_INT: push(t); t = 0; break;
		case T_LONG: push(t, T_TOP); t = 1; break;
		case T_FLOAT: push(t); t = 2; break;
		case T_DOUBLE: push(t, T_TOP); t = 3; break;
		default: push(t); t = 4;
		}
		if (i < 4) {
			code.writeByte(_iload_0 + i + t * 4);
			return 1;
		} else {
			code.writeByte(_iload + t).writeByte(i);
			return 2;
		}
	}

	/**
	 * add a stack map frame at current position
	 */
	public void frame() {
		int l = 7;
		for (int e : locals) l += e >= T_OBJECT ? 3 : 1;
		for (int e : stack) l += e >= T_OBJECT ? 3 : 1;
		int p = code.writerIndex(), d = p - lastFrame - 1;
		ByteBuffer b = ByteBuffer.allocate(l);
		b.put((byte)255).putShort((short)d);
		b.putShort((short)locals.size());
		for (int e : locals) {
			b.put((byte)(e >> 16));
			if (e >= T_OBJECT)
				b.putShort((short)e);
		}
		b.putShort((short)stack.size());
		for (int e : stack) {
			b.put((byte)(e >> 16));
			if (e >= T_OBJECT)
				b.putShort((short)e);
		}
		lastFrame = p;
		frames.add(b.array());
	}

	/**
	 * @return the assembled Code attribute
	 */
	public Map<Short, byte[]> generate() {
		ByteBuf b = code;
		byte[] code = new byte[b.writerIndex()];
		b.readBytes(code);
		return genCode(locals.size(), maxStack, code, null, frames, cpt);
	}

	/**
	 * @param c class of type
	 * @return the type numeral for the given class
	 */
	public int type(Class<?> c) {
		return T_OBJECT + cpt.putClass(c.getName());
	}

	/** type numerals */
	public static final int
		T_TOP = 0x0_0000,
		T_INT = 0x1_0000,
		T_FLOAT = 0x2_0000,
		T_DOUBLE = 0x3_0000,
		T_LONG = 0x4_0000,
		T_NULL = 0x5_0000,
		T_UI_THIS = 0x6_0000,
		T_OBJECT = 0x7_0000,
		T_UI_OBJ = 0x8_0000,
		T_THIS = T_OBJECT + ConstantPool.THIS_CLASS;

}
