package cd4017be.api.automation;

public interface ITeslaTransmitter {
	
	public short getFrequency();
	public boolean checkAlive();
	public double getSqDistance(ITeslaTransmitter t);
	public double getVoltage();
	public double getPower(double R, double U);
	public double addEnergy(double E);
	public int[] getLocation();
}
