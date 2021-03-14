package cd4017be.lib.script;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

import javax.script.ScriptException;

import cd4017be.lib.script.obj.Array;
import cd4017be.lib.script.obj.IOperand;
import cd4017be.lib.script.obj.Nil;

/**
 * 
 * @author CD4017BE
 */
public class Script implements IOperand {

	public final Function[] functions;
	public final String[] dictionary;
	public final IOperand[] globals;
	public final String fileName;
	public long editDate;
	public int version;
	public boolean run = true;

	public Script(String name, Function[] functions, String[] names, int globals) {
		this.fileName = name;
		this.functions = functions;
		this.dictionary = names;
		this.globals = new IOperand[globals];
		Arrays.fill(this.globals, Nil.NIL);
		for (Function f : functions)
			f.script = this;
	}

	public Script(DataInputStream dis) throws IOException {
		this.fileName = dis.readUTF();
		this.version = dis.readInt();
		this.editDate = dis.readLong();
		this.globals = new IOperand[dis.readUnsignedShort()];
		this.dictionary = new String[dis.readUnsignedShort()];
		this.functions = new Function[dis.readUnsignedByte() + 1];
	}

	public Script readData(DataInputStream dis) throws IOException {
		for(int i = 0; i < dictionary.length; i++)
			dictionary[i] = dis.readUTF();
		for (int i = 0; i < functions.length; i++)
			(functions[i] = new Function(dis)).script = this;
		return this;
	}

	public void writeHeader(DataOutputStream dos) throws IOException {
		dos.writeUTF(fileName);
		dos.writeLong(editDate);
		dos.writeInt(version);
		dos.writeShort(globals.length);
		dos.writeShort(dictionary.length);
		dos.writeByte(functions.length - 1);
	}

	public void writeData(DataOutputStream dos) throws IOException {
		for (String s : dictionary) dos.writeUTF(s);
		for (Function f : functions) f.writeData(dos);
	}

	public int varIndex(String name) {
		return Arrays.binarySearch(dictionary, 0, globals.length, name);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < functions.length; i++)
			sb.append("function").append(i).append(functions[i]).append('\n');
		return sb.toString();
	}

	@Override
	public boolean asBool() {
		return true;
	}

	@Override
	public Object value() {
		return this;
	}

	@Override
	public void call(IOperand[] stack, int bot, int top) throws ScriptException {
		int i = varIndex("ARG");
		if (i >= 0) globals[i] =
			top == bot ? Nil.NIL :
			top - bot == 1 ? stack[bot] :
			new Array(stack, bot, top);
		if (i >= 0 || run) {
			functions[0].call(stack, bot, bot);
			run = false;
		}
		stack[bot-1] = this;
	}

	@Override
	public IOperand get(String member) {
		int i = varIndex(member);
		return i < 0 ? Nil.NIL : globals[i];
	}

	@Override
	public void set(String member, IOperand val) {
		int i = varIndex(member);
		if (i >= 0) globals[i] = val;
	}

	public static IOperand call(IOperand func, int stacksize, IOperand... param) throws ScriptException {
		IOperand[] stack = new IOperand[stacksize];
		System.arraycopy(param, 0, stack, 1, param.length);
		func.call(stack, 1, 1 + param.length);
		return stack[0];
	}

}
