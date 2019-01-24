package cd4017be.lib.util;

/**
 * Filter for resource transfer
 * @author CD4017BE
 */
public interface IFilter<Obj, Inv> {

	/**
	 * @param obj inserted resource 
	 * @param inv target inventory
	 * @return maximum amount of the given resource to insert into given inventory
	 */
	public int insertAmount(Obj obj, Inv inv);

	/**
	 * @param obj resource wanted to extract or null for any
	 * @param inv inventory to extract from
	 * @return the actual resource that should be extracted
	 */
	public Obj getExtract(Obj obj, Inv inv);

	/**
	 * @param obj resource to pass on
	 * @return whether the given resource is allowed to pass through default route
	 */
	public boolean transfer(Obj obj);

}
