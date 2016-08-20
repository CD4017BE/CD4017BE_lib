package cd4017be.api.circuits;

import net.minecraft.util.EnumFacing;

/**
 *
 * @author CD4017BE
 */
public interface IDirectionalRedstone {
	/**
	 * Get the signal direction at given side
	 * @param s side
	 * @return 0 none, 1 input, 2 output, 3 both 
	 */
	public byte getRSDirection(EnumFacing s);
}
