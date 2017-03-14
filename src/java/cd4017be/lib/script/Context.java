package cd4017be.lib.script;

import java.util.HashMap;
import java.util.function.Function;
import javax.script.ScriptException;

public class Context implements Module {

	private static final Function<Object[], Object>
		PRINT = (p) -> {
			System.out.println(p[0]);
			return null;
		}, TIME = (p) -> (double)System.currentTimeMillis();
	
	public HashMap<String, Module> modules = new HashMap<String, Module>();
	public HashMap<String, Function<Object[], Object>> defFunc = new HashMap<String, Function<Object[], Object>>();
	public int recursion = 0;
	
	public Context() {
		defFunc.put("print", PRINT);
		defFunc.put("time", TIME);
	}
	
	private String[] split(String name) {
		int p = name.lastIndexOf('.');
		if (p >= 0) return new String[]{name.substring(0, p), name.substring(p + 1)};
		else return new String[]{"", name};
	}
	
	@Override
	public Object invoke(String name, Object... args) throws NoSuchMethodException, ScriptException {
		String[] s = split(name);
		if (s[0] == null) {
			Function<Object[], Object> f = defFunc.get(s[1]);
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
