package ic2.api.energy.tile;

public abstract interface IEnergySource extends IEnergyEmitter
{
  public abstract double getOfferedEnergy();

  public abstract void drawEnergy(double paramDouble);

  public abstract int getSourceTier();
}