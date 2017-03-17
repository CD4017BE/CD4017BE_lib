package cd4017be.lib.script;

import java.util.HashMap;
import java.util.function.Function;
import javax.script.ScriptException;

public class Context implements Module {

	private static final Function<Parameters, Object>
		PRINT = (p) -> {
			System.out.println(p.get(0));
			return null;
		}, TIME = (p) -> (double)System.currentTimeMillis(),
		REPL = (p) -> p.getString(0).replaceFirst(p.getString(1), p.getString(2));
	
	public HashMap<String, Module> modules = new HashMap<String, Module>();
	public HashMap<String, Function<Parameters, Object>> defFunc = new HashMap<String, Function<Parameters, Object>>();
	public int recursion = 0;
	
	public Context() {
		defFunc.put("print", PRINT);
		defFunc.put("time", TIME);
		defFunc.put("repl", REPL);
	}
	
	private String[] split(String name) {
		int p = name.lastIndexOf('.');
		if (p >= 0) return new String[]{name.substring(0, p), name.substring(p + 1)};
		else return new String[]{"", name};
	}
	
	@Override
	public Object invoke(String name, Parameters args) throws NoSuchMethodException, ScriptException {
		String[] s = split(name);
		if (s[0] == null) {
			Function<Parameters, Object> f = defFunc.get(s[1]);
			if (f != null) return f.apply(args);
		} else {
			Module m = modules.get(s[0]);
			if (m != null) return m.invoke(s[1], args);
		}
		throw new NoSuchMethodException();
	}

	@Override
	public void assign(String name, Object val) {
		String[] s = split(name);
		Module m = modules.get(s[0]);
		if (m != null) m.assign(s[1], val);
	}

	@Override
	public Object read(String name) {
		String[] s = split(name);
		Module m = modules.get(s[0]);
		if (m != null) return m.read(s[1]);
		else return null;
	}
	
	public void add(Module m) {
		modules.put(m.addToContext(this), m);
	}

	@Override
	public String addToContext(Context cont) {return null;}

	public void reset() {
		recursion = 0;
	}

}
