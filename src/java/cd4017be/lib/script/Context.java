package cd4017be.lib.script;

import java.util.HashMap;
import java.util.function.Function;
import javax.script.ScriptException;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import cd4017be.lib.script.obj.Array;
import cd4017be.lib.script.obj.IOperand;
import cd4017be.lib.script.obj.Number;
import cd4017be.lib.script.obj.Text;
import cd4017be.lib.script.obj.Vector;

/**
 * 
 * @author CD4017BE
 */
public class Context implements Module {

	public static final Marker
		SCRIPT = MarkerManager.getMarker("SCRIPT"),
		PRINT = MarkerManager.getMarker("PRINT").setParents(SCRIPT),
		ERROR = MarkerManager.getMarker("ERROR").setParents(SCRIPT);

	private static final Function<Parameters, IOperand>
		TIME = (p) -> new Number((double)System.currentTimeMillis() / 1000D),
		REPL = (p) -> new Text(p.getString(0).replaceFirst(p.getString(1), p.getString(2))),
		NARR = (p) -> new Array((int)p.getNumber(0)),
		NVEC = (p) -> new Vector((int)p.getNumber(0));

	public HashMap<String, Module> modules = new HashMap<>();
	public HashMap<String, Function<Parameters, IOperand>> defFunc = new HashMap<>();
	public int recursion = 0;
	public final Logger LOG;

	public Context(Logger log) {
		this.LOG = log;
		defFunc.put("print", (p)-> {
			LOG.info(PRINT, p.get(0));
			return null;
		});
		defFunc.put("time", TIME);
		defFunc.put("repl", REPL);
		defFunc.put("narr", NARR);
		defFunc.put("nvec", NVEC);
	}

	private String[] split(String name) {
		int p = name.lastIndexOf('.');
		if (p >= 0) return new String[]{name.substring(0, p), name.substring(p + 1)};
		else return new String[]{"", name};
	}

	@Override
	public IOperand invoke(String name, Parameters args) throws NoSuchMethodException, ScriptException {
		String[] s = split(name);
		if (s[0] == null) {
			Function<Parameters, IOperand> f = defFunc.get(s[1]);
			if (f != null) return f.apply(args);
		} else {
			Module m = modules.get(s[0]);
			if (m != null) return m.invoke(s[1], args);
		}
		throw new NoSuchMethodException();
	}

	@Override
	public void assign(String name, IOperand val) {
		String[] s = split(name);
		Module m = modules.get(s[0]);
		if (m != null) m.assign(s[1], val);
	}

	@Override
	public IOperand read(String name) {
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
