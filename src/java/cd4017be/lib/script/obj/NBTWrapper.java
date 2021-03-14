package cd4017be.lib.script.obj;

import java.util.Iterator;
import net.minecraft.nbt.CompoundNBT;

/**
 * @author CD4017BE
 *
 */
public class NBTWrapper implements IOperand {

	private boolean copied;
	public final CompoundNBT nbt;

	public NBTWrapper() {
		this.nbt = new CompoundNBT();
	}

	public NBTWrapper(CompoundNBT nbt) {
		this.nbt = nbt;
	}

	@Override
	public NBTWrapper onCopy() {
		copied = true;
		return this;
	}

	@Override
	public boolean asBool() {
		return !nbt.isEmpty();
	}

	@Override
	public Object value() {
		return nbt;
	}

	@Override
	public int asIndex() {
		return nbt.size();
	}
/*
	@Override
	public IOperand len() {
		return new Number(nbt.size());
	}

	@Override
	public IOperand addR(IOperand x) {
		if (x instanceof NBTWrapper) {
			NBTWrapper a = copied ? new NBTWrapper(nbt.copy()) : this, b = (NBTWrapper)x;
			a.nbt.merge(b.nbt);
			return a;
		} else return IOperand.super.addR(x);
	}

	@Override
	public IOperand get(IOperand idx) {
		INBT tag = nbt.get(idx.toString());
		return tag == null ? Nil.NIL : expand(tag);
	}

	private IOperand expand(INBT tag) {
		switch(tag.getId()) {
		case NBT.TAG_COMPOUND: return new NBTWrapper((CompoundNBT)tag);
		case NBT.TAG_LIST: {
			ListNBT list = (ListNBT)tag;
			Array a = new Array(list.size());
			for (int i = 0; i < a.array.length; i++)
				a.array[i] = expand(list.get(i));
			return a;
		}
		case NBT.TAG_STRING: return new Text(((StringNBT)tag).getString());
		case NBT.TAG_BYTE_ARRAY: {
			byte[] arr = ((ByteArrayNBT)tag).getByteArray();
			Vector v = new Vector(arr.length);
			for (int i = 0; i < arr.length; i++)
				v.value[i] = arr[i] & 0xff;
			return v;
		}
		case NBT.TAG_INT_ARRAY: {
			int[] arr = ((IntArrayNBT)tag).getIntArray();
			Vector v = new Vector(arr.length);
			for (int i = 0; i < arr.length; i++)
				v.value[i] = arr[i];
			return v;
		}
		default:
			if (tag instanceof NumberNBT)
				return new Number(((NumberNBT)tag).getDouble());
			else return Nil.NIL;
		}
	}

	@Override
	public void put(IOperand idx, IOperand val) {
		String key = idx.toString();
		if (key.isEmpty()) return;
		char c = key.charAt(0);
		key = key.substring(1);
		INBT tag = val == Nil.NIL ? null : parse(c, val);
		if (tag == null) nbt.remove(key);
		else nbt.put(key, tag);
	}

	private INBT parse(char type, IOperand val) {
		if (val instanceof NBTWrapper)
			return ((NBTWrapper)val).nbt;
		else if (val instanceof Array) {
			ListNBT list = new ListNBT();
			INBT tag;
			for (IOperand op : ((Array)val).array)
				if ((tag = parse(type, op)) != null)
					list.add(tag);
			return list;
		} else if (val instanceof Text)
			return StringNBT.valueOf(val.toString());
		else if (val instanceof Vector) {
			double[] v = ((Vector)val).value;
			switch(type) {
			case 'B': {
				byte[] arr = new byte[v.length];
				for (int i = 0; i < arr.length; i++)
					arr[i] = (byte)v[i];
				return new ByteArrayNBT(arr);
			}
			case 'I': {
				int[] arr = new int[v.length];
				for (int i = 0; i < arr.length; i++)
					arr[i] = (int)v[i];
				return new IntArrayNBT(arr);
			}
			default: return null;
			}
		} else {
			double v = val.asDouble();
			switch(type) {
			case 'B': return ByteNBT.valueOf((byte)v);
			case 'S': return ShortNBT.valueOf((short)v);
			case 'I': return IntNBT.valueOf((int)v);
			case 'L': return LongNBT.valueOf((long)v);
			case 'F': return FloatNBT.valueOf((float)v);
			case 'D': return DoubleNBT.valueOf(v);
			default: return null;
			}
		}
	}
*/
	@Override
	public String toString() {
		return nbt.toString();
	}

	@Override
	public OperandIterator iterator() throws Error {
		return new KeyIterator(nbt.keySet().iterator());
	}

	static class KeyIterator implements OperandIterator {

		final Iterator<String> it;

		KeyIterator(Iterator<String> it) {
			this.it = it;
		}

		@Override
		public boolean hasNext() {
			return it.hasNext();
		}

		@Override
		public IOperand next() {
			return new Text(it.next());
		}

		@Override
		public Object value() {
			return this;
		}

		@Override
		public void set(IOperand obj) {
		}

	}
}
