/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cd4017be.lib.templates;

import cd4017be.api.automation.AreaProtect;
import cd4017be.api.automation.IOperatingArea;
import cd4017be.api.automation.PipeEnergy;
import cd4017be.lib.ModTileEntity;
import cd4017be.lib.templates.TankContainer;

import java.io.DataInputStream;
import java.io.IOException;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;

/**
 *
 * @author CD4017BE
 */
public class AutomatedTile extends ModTileEntity implements ISidedInventory
{
    public Inventory inventory;
    public TankContainer tanks;
    public PipeEnergy energy;
    
    @Override
    public void updateEntity() 
    {
        if (worldObj.isRemote) return;
        if (inventory != null) inventory.update();
        if (tanks != null) tanks.update();
        if (energy != null) energy.update(this);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) 
    {
        super.readFromNBT(nbt);
        if (inventory != null){
            inventory.readFromNBT(nbt, "Items");
            netData.longs[inventory.netIdxLong] = nbt.getLong("icfg");
        }
        if (tanks != null) {
            tanks.readFromNBT(nbt, "tank");
            netData.longs[tanks.netIdxLong] = nbt.getLong("tcfg");
        }
        if (energy != null) {
        	energy.readFromNBT(nbt, "wire");
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        if (inventory != null){
            inventory.writeToNBT(nbt, "Items");
            nbt.setLong("icfg", netData.longs[inventory.netIdxLong]);
        }
        if (tanks != null){
            tanks.writeToNBT(nbt, "tank");
            nbt.setLong("tcfg", netData.longs[tanks.netIdxLong]);
        }
        if (energy != null) energy.writeToNBT(nbt, "wire");
    }
    
    public static int CmdOffset = 16;
    
    @Override
    public void onPlayerCommand(DataInputStream dis, EntityPlayerMP player) throws IOException 
    {
        if (!AreaProtect.instance.isInteractingAllowed(player.getCommandSenderName(), worldObj, xCoord >> 4, zCoord >> 4)) return;
        byte cmd = dis.readByte();
        if (cmd == 0 && inventory != null) netData.longs[inventory.netIdxLong] = dis.readLong();
        else if (cmd == 1 && tanks != null) netData.longs[tanks.netIdxLong] = dis.readLong();
        else if (cmd == 2 && tanks != null){
            int id = dis.readByte();
            tanks.setFluid(id, null);
        } else if (cmd == 3 && energy != null) {
        	energy.con = dis.readByte();
        	worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
        else if (cmd >= CmdOffset)
            this.customPlayerCommand((byte)(cmd - CmdOffset), dis, player);
    }
    protected void customPlayerCommand(byte cmd, DataInputStream dis, EntityPlayerMP player) throws IOException {}

    public int[] getAccessibleSlotsFromSide(int var1) 
    {
    	if (inventory == null) return new int[0];
        return inventory.getAccessibleSlotsFromSide(var1);
    }

    public boolean canInsertItem(int i, ItemStack itemstack, int j) 
    {
    	if (inventory == null) return false;
        return inventory.canInsertItem(i, itemstack, j);
    }

    public boolean canExtractItem(int i, ItemStack itemstack, int j) 
    {
    	if (inventory == null) return false;
        return inventory.canExtractItem(i, itemstack, j);
    }

    public int getSizeInventory() 
    {
    	if (inventory == null) return 0;
        return inventory.getSizeInventory();
    }

    public ItemStack getStackInSlot(int i) 
    {
    	if (inventory == null) return null;
        return inventory.getStackInSlot(i);
    }

    public ItemStack decrStackSize(int i, int j) 
    {
    	if (inventory == null) return null;
        return inventory.decrStackSize(i, j);
    }

    public ItemStack getStackInSlotOnClosing(int i) 
    {
    	if (inventory == null) return null;
        return inventory.getStackInSlotOnClosing(i);
    }

    public void setInventorySlotContents(int i, ItemStack itemstack) 
    {
    	if (inventory == null) return;
        inventory.setInventorySlotContents(i, itemstack);
    }

    public int getInventoryStackLimit() 
    {
    	if (inventory == null) return 0;
        return inventory.getInventoryStackLimit();
    }

    public boolean isItemValidForSlot(int i, ItemStack itemstack) 
    {
    	if (inventory == null) return false;
        return inventory.isItemValidForSlot(i, itemstack);
    }

    public int fill(ForgeDirection from, FluidStack resource, boolean doFill) {
        return tanks.fill(from, resource, doFill);
    }

    public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain) {
        return tanks.drain(from, resource, doDrain);
    }

    public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain) {
        return tanks.drain(from, maxDrain, doDrain);
    }

    public boolean canFill(ForgeDirection from, Fluid fluid) {
        return tanks.canFill(from, fluid);
    }

    public boolean canDrain(ForgeDirection from, Fluid fluid) {
        return tanks.canDrain(from, fluid);
    }

    public FluidTankInfo[] getTankInfo(ForgeDirection from) {
        return tanks.getTankInfo(from);
    }

    public PipeEnergy getEnergy(byte side) {
        return energy;
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() 
    {
		if (this instanceof IOperatingArea && IOperatingArea.Handler.renderArea((IOperatingArea)this)) {
	            int[] area = ((IOperatingArea)this).getOperatingArea();
	            return AxisAlignedBB.getBoundingBox(area[0], area[1], area[2], area[3], area[4], area[5]);
		} else return super.getRenderBoundingBox();
    }

	@Override
	public String getInventoryName() {
		if (inventory == null) return "";
        return inventory.getInventoryName();
	}

	@Override
	public boolean hasCustomInventoryName() {
		if (inventory == null) return true;
        return inventory.hasCustomInventoryName();
	}

	@Override
	public void openInventory() {}

	@Override
	public void closeInventory() {}
    
	@Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) 
    {
		NBTTagCompound nbt = pkt.func_148857_g();
		boolean update = false;
		if (this instanceof IOperatingArea) {
			int[] a = nbt.getIntArray("area");
	        int[] a1 = ((IOperatingArea)this).getOperatingArea();
			for (int i = 0; i < a.length && i < a1.length; i++) a1[i] = a[i];
			update = true;
		}
		if (energy != null) {
			energy.con = nbt.getByte("con");
			update = true;
		}
		if (update) worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    @Override
    public Packet getDescriptionPacket() 
    {
        NBTTagCompound nbt = new NBTTagCompound();
        boolean send = false;
        if (this instanceof IOperatingArea) {
        	nbt.setIntArray("area", ((IOperatingArea)this).getOperatingArea());
        	send = true;
        }
        if (energy != null) {
        	nbt.setByte("con", energy.con);
        	send = true;
        }
        if (send) return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, -1, nbt);
        else return null;
    }
    
}
