package ic2.api.energy.tile;

public abstract interface IEnergyConductor extends IEnergyAcceptor, IEnergyEmitter
{
  public abstract double getConductionLoss();

  public abstract double getInsulationEnergyAbsorption();

  public abstract double getInsulationBreakdownEnergy();

  public abstract double getConductorBreakdownEnergy();

  public abstract void removeInsulation();

  public abstract void removeConductor();
}