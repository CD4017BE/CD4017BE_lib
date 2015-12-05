package ic2.api.energy.tile;

import net.minecraftforge.common.util.ForgeDirection;

public abstract interface IHeatSource
{
  public abstract int maxrequestHeatTick(ForgeDirection paramForgeDirection);

  public abstract int requestHeat(ForgeDirection paramForgeDirection, int paramInt);
}