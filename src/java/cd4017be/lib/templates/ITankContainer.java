package cd4017be.lib.templates;

import net.minecraftforge.fluids.FluidStack;

public interface ITankContainer {
	public int getTanks();
	public FluidStack getTank(int i);
	public int getCapacity(int i);
	public void setTank(int i, FluidStack fluid);
}
