package cd4017be.lib.util;

/**
 * 
 * @author CD4017BE
 */
public interface IFilter<Obj, Inv> {

	public int insertAmount(Obj obj, Inv inv);

	public Obj getExtract(Obj obj, Inv inv);

	public boolean transfer(Obj obj);

}
