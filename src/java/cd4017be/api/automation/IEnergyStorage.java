/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cd4017be.api.automation;

/**
 *
 * @author CD4017BE
 */
public interface IEnergyStorage 
{
    public double getEnergy();
    public double getCapacity();
    public double addEnergy(double e);
}
