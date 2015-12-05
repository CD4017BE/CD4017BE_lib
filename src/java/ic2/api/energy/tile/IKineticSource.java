package ic2.api.energy.tile;

import net.minecraftforge.common.util.ForgeDirection;

public abstract interface IKineticSource
{
  public abstract int maxrequestkineticenergyTick(ForgeDirection paramForgeDirection);

  public abstract int requestkineticenergy(ForgeDirection paramForgeDirection, int paramInt);
}