package cd4017be.lib.script;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;

import javax.script.ScriptException;

import cd4017be.lib.script.obj.IOperand;
import cd4017be.lib.script.obj.Nil;

/**
 * @author CD4017BE */
public abstract class ScriptLoader implements IOperand {

	protected final HashMap<String, Script> scripts = new HashMap<>();
	protected final Parser parser = new Parser();

	@Override
	public boolean asBool() {
		return true;
	}

	@Override
	public IOperand opR(int code, IOperand x) {
		if (code == index) return get(x.toString());
		return x.opL(code, this);
	}

	@Override
	public IOperand get(String member) {
		return Nil.of(scripts.computeIfAbsent(member, this::load));
	}

	protected abstract Script load(String name);

	public Script compile(String name, Reader src) throws ScriptException, IOException {
		Script s = new Compiler(name, parser.parse(src, name)).compile();
		s.set("LOAD", this);
		return s;
	}

}
