package cd4017be.lib.Gui;

import net.minecraftforge.fluids.FluidStack;

/**
 * 
 * @author CD4017BE
 */
public interface ITankContainer {
	public int getTanks();
	public FluidStack getTank(int i);
	public int getCapacity(int i);
	public void setTank(int i, FluidStack fluid);
}
