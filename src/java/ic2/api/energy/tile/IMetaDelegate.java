package ic2.api.energy.tile;

import java.util.List;
import net.minecraft.tileentity.TileEntity;

public abstract interface IMetaDelegate extends IEnergyTile
{
  public abstract List<TileEntity> getSubTiles();
}