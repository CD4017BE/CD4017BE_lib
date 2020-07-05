package cd4017be.lib.capability;

import net.minecraftforge.energy.IEnergyStorage;


/** 
 * @author CD4017BE */
public class NullEnergyStorage implements IEnergyStorage {

	public static final NullEnergyStorage INSTANCE = new NullEnergyStorage();

	@Override
	public int receiveEnergy(int maxReceive, boolean simulate) {return 0;}

	@Override
	public int extractEnergy(int maxExtract, boolean simulate) {return 0;}

	@Override
	public int getEnergyStored() {return 0;}

	@Override
	public int getMaxEnergyStored() {return 0;}

	@Override
	public boolean canExtract() {return false;}

	@Override
	public boolean canReceive() {return false;}

}
