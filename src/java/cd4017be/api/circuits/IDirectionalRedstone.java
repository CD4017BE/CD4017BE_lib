/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

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
	 * @return -1 for input, 1 for output, 0 for inactive 
	 */
	public byte getRSDirection(EnumFacing s);
}
