/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cd4017be.api.circuits;

import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

/**
 *
 * @author CD4017BE
 */
public class RedstoneHandler 
{
    /**
     * Get the 1-bit input power state of this TileEntity
     * @param te the TileEntity position
     * @param useRst if true standart redstone will be also checked
     * @return power state
     */
    public static boolean get1bitState(TileEntity te, boolean useRst)
    {
        if (te == null || !(te instanceof IRedstone1bit)) return false;
        IRedstone1bit rs = (IRedstone1bit)te;
        ForgeDirection d;
        for (int i = 0; i < 6; i++) {
            if (rs.getBitDirection(i) >= 0) continue;
            d = ForgeDirection.getOrientation(i);
            TileEntity tile = te.getWorldObj().getTileEntity(te.xCoord + d.offsetX, te.yCoord + d.offsetY, te.zCoord + d.offsetZ);
            if (tile != null && tile instanceof IRedstone1bit && ((IRedstone1bit)tile).getBitDirection(i^1) > 0 && ((IRedstone1bit)tile).getBitValue(i^1)) return true;
            else if (useRst && te.getWorldObj().getIndirectPowerOutput(te.xCoord + d.offsetX, te.yCoord + d.offsetY, te.zCoord + d.offsetZ, i)) return true;
        }
        return false;
    }
    
    /**
     * Get the 8-bit input power state of this TileEntity
     * @param te the TileEntity position
     * @return power state
     */
    public static byte get8bitState(TileEntity te)
    {
        if (te == null || !(te instanceof IRedstone8bit)) return 0;
        IRedstone8bit rs = (IRedstone8bit)te;
        ForgeDirection d;
        byte state = 0;
        for (int i = 0; i < 6 && state != -1; i++) {
            if (rs.getDirection(i) >= 0) continue;
            d = ForgeDirection.getOrientation(i);
            TileEntity tile = te.getWorldObj().getTileEntity(te.xCoord + d.offsetX, te.yCoord + d.offsetY, te.zCoord + d.offsetZ);
            if (tile != null && tile instanceof IRedstone8bit && ((IRedstone8bit)tile).getDirection(i^1) > 0) state |= ((IRedstone8bit)tile).getValue(i^1);
        }
        return state;
    }
    
    /**
     * Notify neighboring Blocks that this TileEntity's 1-bit state changed
     * @param te the TileEntity that changed
     * @param s the new state
     * @param rec the amount of blocks this signal has travelled this tick
     * @param useRst if true standart Blocks will also be notified
     */
    public static void notify1bitNeighbors(TileEntity te, boolean s, int rec, boolean useRst)
    {
        if (te == null || !(te instanceof IRedstone1bit)) return;
        IRedstone1bit rs = (IRedstone1bit)te;
        ForgeDirection d;
        for (int i = 0; i < 6; i++) {
            if (rs.getBitDirection(i) <= 0) continue;
            d = ForgeDirection.getOrientation(i);
            TileEntity tile = te.getWorldObj().getTileEntity(te.xCoord + d.offsetX, te.yCoord + d.offsetY, te.zCoord + d.offsetZ);
            if (tile != null && tile instanceof IRedstone1bit && ((IRedstone1bit)tile).getBitDirection(i^1) < 0) ((IRedstone1bit)tile).setBitValue(i^1, s, rec);
            else if (useRst) {
                te.getWorldObj().notifyBlockOfNeighborChange(te.xCoord + d.offsetX, te.yCoord + d.offsetY, te.zCoord + d.offsetZ, te.getBlockType());
                te.getWorldObj().notifyBlocksOfNeighborChange(te.xCoord + d.offsetX, te.yCoord + d.offsetY, te.zCoord + d.offsetZ, te.getBlockType());
            }
        }
    }
    
    /**
     * Notify neighboring Blocks that this TileEntity's 8-bit state changed
     * @param te the TileEntity that changed
     * @param s the new state
     * @param rec the amount of blocks this signal has travelled this tick
     */
    public static void notify8bitNeighbors(TileEntity te, byte s, int rec)
    {
        if (te == null || !(te instanceof IRedstone8bit)) return;
        IRedstone8bit rs = (IRedstone8bit)te;
        ForgeDirection d;
        for (int i = 0; i < 6; i++) {
            if (rs.getDirection(i) <= 0) continue;
            d = ForgeDirection.getOrientation(i);
            TileEntity tile = te.getWorldObj().getTileEntity(te.xCoord + d.offsetX, te.yCoord + d.offsetY, te.zCoord + d.offsetZ);
            if (tile != null && tile instanceof IRedstone8bit && ((IRedstone8bit)tile).getDirection(i^1) < 0) ((IRedstone8bit)tile).setValue(i^1, s, rec);
        }
    }
    
}
