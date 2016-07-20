package cd4017be.lib.util;

public class Obj2<A, B>
{
	public A objA;
	public B objB;
	
	public Obj2(A objA, B objB)
	{
		this.objA = objA;
		this.objB = objB;
	}
	
	public Obj2()
	{
		objA = null;
		objB = null;
	}
	
	public Obj2<A, B> copy()
	{
		return new Obj2<A, B>(objA, objB);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public boolean equals(Object arg0) {
		if (arg0 == null || !(arg0 instanceof Obj2)) return false;
		Obj2 other = (Obj2)arg0;
		return ((objA == null && other.objA == null) || objA.equals(other.objA)) && 
			((objB == null && other.objB == null)) || objB.equals(other.objB);
	}
	
}
