package ic2.api.energy.tile;

import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

public abstract interface IEnergyEmitter extends IEnergyTile
{
  public abstract boolean emitsEnergyTo(TileEntity paramTileEntity, ForgeDirection paramForgeDirection);
}