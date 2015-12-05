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
}
