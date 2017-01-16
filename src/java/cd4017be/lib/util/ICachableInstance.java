package cd4017be.lib.util;

/**
 * implement this interface to allow other devices to keep a quick access reference of your instance
 * @author CD4017BE
 */
public interface ICachableInstance {

	/**@return whether this instance is invalid, meaning it should not be used anymore*/
	public boolean invalid();

}
