package cd4017be.lib.script;

import java.util.HashMap;

import javax.script.ScriptException;

/**
 * 
 * @author CD4017BE
 */
public class Script implements Module {

	public final HashMap<String, Function> methods;
	public final HashMap<String, Object> variables;
	public final String fileName;
	public Context context;
	public long editDate;
	public int version;

	public Script(String name, HashMap<String, Function> methods, HashMap<String, Object> vars) {
		this.fileName = name;
		this.methods = methods;
		this.variables = vars;
		for (Function f : methods.values()) f.script = this;
	}

	@Override
	public Object invoke(String name, Parameters args) throws NoSuchMethodException, ScriptException {
		Function f = methods.get(name);
		if (f != null) return f.apply(args);
		java.util.function.Function<Parameters, Object> f1 = context.defFunc.get(name);
		if (f1 != null) return f1.apply(args);
		throw new NoSuchMethodException();
	}

	@Override
	public void assign(String name, Object val) {
		variables.put(name, val);
	}

	@Override
	public Object read(String name) {
		return variables.get(name);
	}

	@Override
	public String addToContext(Context cont) {
		this.context = cont;
		return fileName;
	}

}
