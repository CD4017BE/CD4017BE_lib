package cd4017be.lib.script;

import javax.script.ScriptException;

import cd4017be.lib.script.obj.IOperand;

/**
 * 
 * @author CD4017BE
 */
public interface Module {
	public IOperand invoke(String name, Parameters args) throws NoSuchMethodException, ScriptException;
	public void assign(String name, IOperand val);
	public IOperand read(String name);
	public String addToContext(Context cont);
}
