package ic2.api.energy.tile;

import net.minecraftforge.common.util.ForgeDirection;

public abstract interface IEnergySink extends IEnergyAcceptor
{
  public abstract double getDemandedEnergy();

  public abstract int getSinkTier();

  public abstract double injectEnergy(ForgeDirection paramForgeDirection, double paramDouble1, double paramDouble2);
}