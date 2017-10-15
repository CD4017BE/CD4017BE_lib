package cd4017be.lib.script;

import javax.script.ScriptException;

/**
 * 
 * @author CD4017BE
 */
public interface Module {
	public Object invoke(String name, Parameters args) throws NoSuchMethodException, ScriptException;
	public void assign(String name, Object val);
	public Object read(String name);
	public String addToContext(Context cont);
}
