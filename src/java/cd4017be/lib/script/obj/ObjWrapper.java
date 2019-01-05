package cd4017be.lib.script.obj;

/**
 * 
 * @author cd4017be
 */
public class ObjWrapper implements IOperand {

	Object obj;

	public ObjWrapper(Object obj) {
		this.obj = obj;
	}

	@Override
	public boolean asBool() {
		return obj != null;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (!(o instanceof ObjWrapper)) return false;
		Object obj = ((ObjWrapper)o).obj;
		return obj == null ? this.obj == null : this.obj.equals(obj);
	}

	@Override
	public String toString() {
		return obj == null ? "null" : obj.toString();
	}

	@Override
	public Object value() {
		return obj;
	}

}
