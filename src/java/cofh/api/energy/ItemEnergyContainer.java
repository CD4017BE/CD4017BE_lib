package cofh.api.energy;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class ItemEnergyContainer extends Item
  implements IEnergyContainerItem
{
  protected int capacity;
  protected int maxReceive;
  protected int maxExtract;

  public ItemEnergyContainer()
  {
  }

  public ItemEnergyContainer(int paramInt)
  {
    this(paramInt, paramInt, paramInt);
  }

  public ItemEnergyContainer(int paramInt1, int paramInt2)
  {
    this(paramInt1, paramInt2, paramInt2);
  }

  public ItemEnergyContainer(int paramInt1, int paramInt2, int paramInt3)
  {
    this.capacity = paramInt1;
    this.maxReceive = paramInt2;
    this.maxExtract = paramInt3;
  }

  public ItemEnergyContainer setCapacity(int paramInt)
  {
    this.capacity = paramInt;
    return this;
  }

  public void setMaxTransfer(int paramInt)
  {
    setMaxReceive(paramInt);
    setMaxExtract(paramInt);
  }

  public void setMaxReceive(int paramInt)
  {
    this.maxReceive = paramInt;
  }

  public void setMaxExtract(int paramInt)
  {
    this.maxExtract = paramInt;
  }

  public int receiveEnergy(ItemStack paramItemStack, int paramInt, boolean paramBoolean)
  {
    if (paramItemStack.stackTagCompound == null) {
      paramItemStack.stackTagCompound = new NBTTagCompound();
    }
    int i = paramItemStack.stackTagCompound.getInteger("Energy");
    int j = Math.min(this.capacity - i, Math.min(this.maxReceive, paramInt));

    if (!paramBoolean) {
      i += j;
      paramItemStack.stackTagCompound.setInteger("Energy", i);
    }
    return j;
  }

  public int extractEnergy(ItemStack paramItemStack, int paramInt, boolean paramBoolean)
  {
    if ((paramItemStack.stackTagCompound == null) || (!paramItemStack.stackTagCompound.hasKey("Energy"))) {
      return 0;
    }
    int i = paramItemStack.stackTagCompound.getInteger("Energy");
    int j = Math.min(i, Math.min(this.maxExtract, paramInt));

    if (!paramBoolean) {
      i -= j;
      paramItemStack.stackTagCompound.setInteger("Energy", i);
    }
    return j;
  }

  public int getEnergyStored(ItemStack paramItemStack)
  {
    if ((paramItemStack.stackTagCompound == null) || (!paramItemStack.stackTagCompound.hasKey("Energy"))) {
      return 0;
    }
    return paramItemStack.stackTagCompound.getInteger("Energy");
  }

  public int getMaxEnergyStored(ItemStack paramItemStack)
  {
    return this.capacity;
  }
}