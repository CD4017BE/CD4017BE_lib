/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cd4017be.api.circuits;

/**
 *
 * @author CD4017BE
 */
public interface IRedstone8bit 
{
    /**
     * Get the redstone state at given side
     * @param s side
     * @return 8-bit state
     */
    public byte getValue(int s);
    
    /**
     * Get the signal direction at given side
     * @param s side
     * @return -1 for input, 1 for output, 0 for inactive 
     */
    public byte getDirection(int s);
    
    /**
     * Update the redstone state at given side
     * @param s side
     * @param v new 8-bit state
     * @param recursion amount of Blocks the signal has travelled this tick
     */
    public void setValue(int s, byte v, int recursion);
}
