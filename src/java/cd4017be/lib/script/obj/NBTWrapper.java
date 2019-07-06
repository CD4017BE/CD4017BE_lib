package cd4017be.lib.script.obj;

import java.util.Iterator;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTPrimitive;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.nbt.NBTTagShort;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.common.util.Constants.NBT;

/**
 * @author CD4017BE
 *
 */
public class NBTWrapper implements IOperand {

	private boolean copied;
	public final NBTTagCompound nbt;

	public NBTWrapper() {
		this.nbt = new NBTTagCompound();
	}

	public NBTWrapper(NBTTagCompound nbt) {
		this.nbt = nbt;
	}

	@Override
	public NBTWrapper onCopy() {
		copied = true;
		return this;
	}

	@Override
	public boolean asBool() throws Error {
		return !nbt.hasNoTags();
	}

	@Override
	public Object value() {
		return nbt;
	}

	@Override
	public int asIndex() {
		return nbt.getSize();
	}

	@Override
	public IOperand len() {
		return new Number(nbt.getSize());
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
		NBTBase tag = nbt.getTag(idx.toString());
		return tag == null ? Nil.NIL : expand(tag);
	}

	private IOperand expand(NBTBase tag) {
		switch(tag.getId()) {
		case NBT.TAG_COMPOUND: return new NBTWrapper((NBTTagCompound)tag);
		case NBT.TAG_LIST: {
			NBTTagList list = (NBTTagList)tag;
			Array a = new Array(list.tagCount());
			for (int i = 0; i < a.array.length; i++)
				a.array[i] = expand(list.get(i));
			return a;
		}
		case NBT.TAG_STRING: return new Text(((NBTTagString)tag).getString());
		case NBT.TAG_BYTE_ARRAY: {
			byte[] arr = ((NBTTagByteArray)tag).getByteArray();
			Vector v = new Vector(arr.length);
			for (int i = 0; i < arr.length; i++)
				v.value[i] = arr[i] & 0xff;
			return v;
		}
		case NBT.TAG_INT_ARRAY: {
			int[] arr = ((NBTTagIntArray)tag).getIntArray();
			Vector v = new Vector(arr.length);
			for (int i = 0; i < arr.length; i++)
				v.value[i] = arr[i];
			return v;
		}
		default:
			if (tag instanceof NBTPrimitive)
				return new Number(((NBTPrimitive)tag).getDouble());
			else return Nil.NIL;
		}
	}

	@Override
	public void put(IOperand idx, IOperand val) {
		String key = idx.toString();
		if (key.isEmpty()) return;
		char c = key.charAt(0);
		key = key.substring(1);
		NBTBase tag = val == Nil.NIL ? null : parse(c, val);
		if (tag == null) nbt.removeTag(key);
		else nbt.setTag(key, tag);
	}

	private NBTBase parse(char type, IOperand val) {
		if (val instanceof NBTWrapper)
			return ((NBTWrapper)val).nbt;
		else if (val instanceof Array) {
			NBTTagList list = new NBTTagList();
			NBTBase tag;
			for (IOperand op : ((Array)val).array)
				if ((tag = parse(type, op)) != null)
					list.appendTag(tag);
			return list;
		} else if (val instanceof Text)
			return new NBTTagString(val.toString());
		else if (val instanceof Vector) {
			double[] v = ((Vector)val).value;
			switch(type) {
			case 'B': {
				byte[] arr = new byte[v.length];
				for (int i = 0; i < arr.length; i++)
					arr[i] = (byte)v[i];
				return new NBTTagByteArray(arr);
			}
			case 'I': {
				int[] arr = new int[v.length];
				for (int i = 0; i < arr.length; i++)
					arr[i] = (int)v[i];
				return new NBTTagIntArray(arr);
			}
			default: return null;
			}
		} else {
			double v = val.asDouble();
			switch(type) {
			case 'B': return new NBTTagByte((byte)v);
			case 'S': return new NBTTagShort((short)v);
			case 'I': return new NBTTagInt((int)v);
			case 'L': return new NBTTagLong((long)v);
			case 'F': return new NBTTagFloat((float)v);
			case 'D': return new NBTTagDouble(v);
			default: return null;
			}
		}
	}

	@Override
	public String toString() {
		return nbt.toString();
	}

	@Override
	public OperandIterator iterator() throws Error {
		return new KeyIterator(nbt.getKeySet().iterator());
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
