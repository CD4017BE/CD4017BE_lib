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
public interface IRedstone1bit 
{
    /**
     * Get the redstone state at given side
     * @param s side
     * @return true if state is active
     */
    public boolean getBitValue(int s);
    
    /**
     * Get the signal direction at given side
     * @param s side
     * @return -1 for input, 1 for output, 0 for inactive 
     */
    public byte getBitDirection(int s);
    
    /**
     * Update the redstone state at given side
     * @param s side
     * @param v new state
     * @param recursion amount of Blocks the signal has travelled this tick
     */
    public void setBitValue(int s, boolean v, int recursion);
    
}
