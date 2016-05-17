/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cd4017be.lib.templates;

import cd4017be.lib.ModTileEntity;
import cd4017be.lib.util.Obj2;
import cd4017be.lib.util.Utils;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;


/**
 *
 * @author CD4017BE
 */
public class TankContainer implements IFluidHandler
{
    public int netIdxLong = 0;
    public int netIdxFluid = 0;
    public final Tank[] tanks;
    private final ModTileEntity tile;
    
    public static class Tank
    {
        public final int cap;
        public final byte dir;
        public Fluid[] types;
        public int inSlot = -1;
        public int outSlot = -1;
        
        public Tank(int cap, int dir, Fluid... types)
        {
            this.cap = cap;
            this.types = types;
            this.dir = (byte)dir;
        }
        
        public Tank setIn(int slot)
        {
            this.inSlot = slot;
            return this;
        }
        public Tank setOut(int slot)
        {
            this.outSlot = slot;
            return this;
        }
    }
    
    public TankContainer(ModTileEntity tile, Tank... config)
    {
        this.tile = tile;
        this.tanks = config == null ? new Tank[0] : config;
        for (int i = 0; i < tanks.length; i++) {
            if (tanks[i].types != null && tanks[i].types.length == 1) tile.netData.fluids[netIdxFluid + i] = new FluidStack(tanks[i].types[0], 0);
        }
    }
    
    public TankContainer setNetLong(int idx)
    {
        netIdxLong = idx;
        return this;
    }
    
    public void update()
    {
        for (int id = 0; id < tanks.length; id++)
            if (tanks[id].dir != 0)
                for (int s = 0; s < 6; s++) {
                    byte cfg = this.getConfig(s, id);
                    if (cfg == 0) continue;
                    if (tanks[id].dir > 0 && cfg == 3 && this.getAmount(id) > 0)
                    {
                        TileEntity te = Utils.getTileOnSide(tile, (byte)s);
                        if (!(te instanceof IFluidHandler)) continue;
                        IFluidHandler handler = (IFluidHandler)te;
                        this.drain(id, handler.fill(EnumFacing.VALUES[s ^ 1], this.getFluid(id), true), true);
                    } else if (tanks[id].dir < 0 && cfg == 2 && this.getSpace(id) > 0)
                    {
                        TileEntity te = Utils.getTileOnSide(tile, (byte)s);
                        if (!(te instanceof IFluidHandler)) continue;
                        IFluidHandler handler = (IFluidHandler)te;
                        int am = this.getSpace(id);
                        FluidStack stack = null;
                        if (this.getFluid(id) != null) stack = new FluidStack(this.getFluid(id), am);
                        else if (tanks[id].types.length > 0) {
                            for (Fluid type : tanks[id].types)
                                if (handler.canDrain(EnumFacing.VALUES[s ^ 1], type)) {
                                    stack = new FluidStack(type, am);
                                    break;
                                }
                            if (stack == null) continue;
                        }
                        this.fill(id, stack == null ? handler.drain(EnumFacing.VALUES[s ^ 1], am, true) : handler.drain(EnumFacing.VALUES[s ^ 1], stack, true), true);
                    }
                }
        if (tile instanceof IInventory)
        {
            IInventory inv = (IInventory)tile;
            for (int id = 0; id < tanks.length; id++) {
                if (tanks[id].inSlot >= 0 && tanks[id].inSlot < inv.getSizeInventory() && this.getSpace(id) > 0)
                {
                    ItemStack item = inv.getStackInSlot(tanks[id].inSlot);
                    if (this.getFluid(id) == null || this.getFluid(id).isFluidEqual(item)) {
                    	Obj2<ItemStack, FluidStack> output = Utils.drainFluid(item, this.getSpace(id));
                    	this.fill(id, output.objB, true);
                    	inv.setInventorySlotContents(tanks[id].inSlot, output.objA);
                    }
                }
                if (tanks[id].outSlot >= 0 && tanks[id].outSlot < inv.getSizeInventory() && this.getAmount(id) > 0)
                {
                    ItemStack item = inv.getStackInSlot(tanks[id].outSlot);
                    Obj2<ItemStack, Integer> output = Utils.fillFluid(item, this.getFluid(id));
                    this.drain(id, output.objB, true);
                    inv.setInventorySlotContents(tanks[id].outSlot, output.objA);
                }
            }
        }
    }
    
    public FluidStack getFluid(int id)
    {
        return tile.netData.fluids[netIdxFluid + id];
    }
    
    public void setFluid(int id, FluidStack fluid)
    {
    	if (fluid != null && fluid.amount == 0) fluid = null;
    	if (fluid == null && tile.netData.fluids[netIdxFluid + id] != null && this.isLocked(id)) {
    		tile.netData.fluids[netIdxFluid + id].amount = 0;
    	} else tile.netData.fluids[netIdxFluid + id] = fluid;
    }
    
    public int getAmount(int id)
    {
        FluidStack stack = tile.netData.fluids[netIdxFluid + id];
        return stack == null ? 0 : stack.amount;
    }
    
    public int getSpace(int id)
    {
        FluidStack stack = tile.netData.fluids[netIdxFluid + id];
        return stack == null ? tanks[id].cap : tanks[id].cap - stack.amount;
    }
    
    public int fill(int id, FluidStack resource, boolean doFill)
    {
        if (resource == null) return 0;
        FluidStack stack = this.getFluid(id);
        if (stack != null && !stack.isFluidEqual(resource)) return 0;
        int m = tanks[id].cap - (stack == null ? 0 : stack.amount);
        if (m > resource.amount) m = resource.amount;
        if (doFill) {
            if (stack == null){
                stack = resource.copy();
                stack.amount = m;
            } else {
                stack.amount += m;
            }
            this.setFluid(id, stack);
        }
        return m;
    }
    
    public FluidStack drain(int id, int amount, boolean doDrain)
    {
        FluidStack stack = this.getFluid(id);
        if (stack == null || amount <= 0) return null;
        int m = stack.amount;
        if (m > amount) m = amount;
        FluidStack ret = stack.copy();
        if (doDrain) {
            stack.amount -= m;
            if (stack.amount <= 0) stack = null;
            this.setFluid(id, stack);
        }
        ret.amount = m;
        return ret;
    }
    
    public byte getConfig(int s, int id)
    {
        return (byte)(tile.netData.longs[netIdxLong] >> (2 * s + 16 * id) & 3);
    }
    
    public boolean isLocked(int id)
    {
    	return tanks[id].types.length == 1 || (tile.netData.longs[netIdxLong] >> (12 + 16 * id) & 1) != 0;
    }
    
    private boolean canFillTank(int side, Fluid type, int id)
    {
        byte cfg = this.getConfig(side, id);
        if (cfg == 0 || cfg == 3) return false;
        FluidStack stack = this.getFluid(id);
        if (stack != null) return stack.getFluid().equals(type) && stack.amount < tanks[id].cap;
        if (tanks[id].types.length > 0) {
            for (Fluid fluid : tanks[id].types)
                if (fluid.equals(type)) return true;
            return false;
        } else return true;
    }
    
    private boolean canDrainTank(int side, Fluid type, int id)
    {
        byte cfg = this.getConfig(side, id);
        if (cfg == 0 || cfg == 2) return false;
        FluidStack stack = this.getFluid(id);
        if (stack == null || stack.amount == 0) return false;
        return type == null || stack.getFluid().equals(type);
    }
    
    @Override
    public int fill(EnumFacing from, FluidStack resource, boolean doFill) 
    {
        int ref = -1;
        for (int id = 0; id < tanks.length; id++)
            if (this.canFillTank(from.ordinal(), resource.getFluid(), id))
                if (ref == -1 || tanks[id].dir < tanks[ref].dir || (tanks[id].dir == tanks[ref].dir && this.getSpace(id) > this.getSpace(ref))) ref = id;
        if (ref >= 0) return this.fill(ref, resource, doFill);
        else return 0;
    }

    @Override
    public FluidStack drain(EnumFacing from, FluidStack resource, boolean doDrain) 
    {
        int ref = -1;
        for (int id = 0; id < tanks.length; id++)
            if (this.canDrainTank(from.ordinal(), resource.getFluid(), id))
                if (ref == -1 || tanks[id].dir > tanks[ref].dir || (tanks[id].dir == tanks[ref].dir && this.getAmount(id) > this.getAmount(ref))) ref = id;
        if (ref >= 0) return this.drain(ref, resource.amount, doDrain);
        else return null;
    }

    @Override
    public FluidStack drain(EnumFacing from, int maxDrain, boolean doDrain) 
    {
        int ref = -1;
        for (int id = 0; id < tanks.length; id++)
            if (this.canDrainTank(from.ordinal(), null, id))
                if (ref == -1 || tanks[id].dir > tanks[ref].dir || (tanks[id].dir == tanks[ref].dir && this.getAmount(id) > this.getAmount(ref))) ref = id;
        if (ref >= 0) return this.drain(ref, maxDrain, doDrain);
        else return null;
    }

    @Override
    public boolean canFill(EnumFacing from, Fluid fluid) 
    {
        for (int id = 0; id < tanks.length; id++)
            if (this.canFillTank(from.ordinal(), fluid, id)) return true;
        return false;
    }

    @Override
    public boolean canDrain(EnumFacing from, Fluid fluid) 
    {
        for (int id = 0; id < tanks.length; id++)
            if (this.canDrainTank(from.ordinal(), fluid, id)) return true;
        return false;
    }

    @Override
    public FluidTankInfo[] getTankInfo(EnumFacing from) 
    {
        FluidTankInfo[] array = new FluidTankInfo[tanks.length];
        int n = 0;
        for (int id = 0; id < tanks.length; id++)
            if (this.getConfig(from.ordinal(), id) != 0)
                array[n++] = new FluidTankInfo(this.getFluid(id), tanks[id].cap);
        FluidTankInfo[] info = new FluidTankInfo[n];
        System.arraycopy(array, 0, info, 0, n);
        return info;
    }
    
    public void readFromNBT(NBTTagCompound nbt, String name)
    {
        for (int id = 0; id < tanks.length; id++)
        {
            String tagName = name + Integer.toString(id);
            if (nbt.hasKey(tagName)) {
                this.setFluid(id, FluidStack.loadFluidStackFromNBT(nbt.getCompoundTag(tagName)));
            } else this.setFluid(id, null);
        }
    }
    
    public void writeToNBT(NBTTagCompound nbt, String name)
    {
        for (int id = 0; id < tanks.length; id++)
        {
            FluidStack stack = this.getFluid(id);
            if (stack != null) {
                NBTTagCompound tag = new NBTTagCompound();
                stack.writeToNBT(tag);
                nbt.setTag(name + Integer.toString(id), tag);
            }
        }
    }
    
}
