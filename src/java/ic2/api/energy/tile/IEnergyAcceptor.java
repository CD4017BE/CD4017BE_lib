package ic2.api.energy.tile;

import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

public abstract interface IEnergyAcceptor extends IEnergyTile
{
  public abstract boolean acceptsEnergyFrom(TileEntity paramTileEntity, ForgeDirection paramForgeDirection);
}