/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cd4017be.lib;

import cpw.mods.fml.common.FMLCommonHandler;

import java.lang.reflect.Method;
import java.util.Iterator;

import cd4017be.api.automation.IOperatingArea;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.S07PacketRespawn;
import net.minecraft.network.play.server.S1DPacketEntityEffect;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

/**
 *
 * @author CD4017BE
 */
public class MovedBlock 
{
    public final NBTTagCompound nbt;
    public final Block blockId;
    public final byte metadata;
    
    public MovedBlock(Block id, int m, NBTTagCompound tile) 
    {
        this.blockId = id;
        this.metadata = (byte)m;
        this.nbt = tile;
    }
    
    public boolean set(World world, int x, int y, int z)
    {
        TileEntity tile = null;
        boolean multipart = false;
        if (nbt != null) {
            nbt.setInteger("x", x);
            nbt.setInteger("y", y);
            nbt.setInteger("z", z);
            multipart = nbt.getString("id").equals("savedMultipart");
            if (multipart) {
                try {
                    Class multipartHelper = Class.forName("codechicken.multipart.MultipartHelper");
                    Method m = multipartHelper.getMethod("createTileFromNBT", new Class[] { World.class, NBTTagCompound.class });
                    tile = (TileEntity)m.invoke(null, new Object[] { world, nbt });
                } catch (Exception e) {e.printStackTrace();}
            } else {
                tile = TileEntity.createAndLoadEntity(nbt);
                if (tile instanceof IOperatingArea) {
                    int [] area = ((IOperatingArea)tile).getOperatingArea();
                    area[0] += x; area[3] += x;
                    area[1] += y; area[4] += y;
                    area[2] += z; area[5] += z;
                }
            }
        }
        boolean set = setBlock(world, x, y, z, blockId, metadata, tile);
        if (multipart && set) {
            try {
                Class multipartHelper = Class.forName("codechicken.multipart.MultipartHelper");
                multipartHelper.getMethod("sendDescPacket", new Class[] { World.class, TileEntity.class }).invoke(null, new Object[] { world, tile });
                Class tileMultipart = Class.forName("codechicken.multipart.TileMultipart");
                tileMultipart.getMethod("onMoved", new Class[0]).invoke(tile, new Object[0]);
            } catch (Exception e) {e.printStackTrace();}
        }
        return set;
    }
    
    public static MovedBlock get(World world, int x, int y, int z)
    {
        Block id = world.getBlock(x, y, z);
        int m = world.getBlockMetadata(x, y, z);
        NBTTagCompound nbt = null;
        TileEntity te = world.getTileEntity(x, y, z);
        if (te != null) 
        {
            if (te instanceof IOperatingArea) {
                int[] area = ((IOperatingArea)te).getOperatingArea();
                area[0] -= x; area[3] -= x;
                area[1] -= y; area[4] -= y;
                area[2] -= z; area[5] -= z;
            }
            nbt = new NBTTagCompound();
            te.writeToNBT(nbt);
        }
        return new MovedBlock(id, m, nbt);
    }
    
    /**
     * Place a Block without notify anything
     * @param world the World
     * @param x block x position
     * @param y block y position
     * @param z block z position
     * @param id block id
     * @param m block metadata
     * @param tile block TileEntity
     * @return true if placed successfully
     */
    public static boolean setBlock(World world, int x, int y, int z, Block id, int m, TileEntity tile)
    {
        if (x < -30000000 || z < -30000000 || x >= 30000000 || z >= 30000000 || y < 0 || y >= 256) return false;
        Chunk chunk = world.getChunkFromBlockCoords(x, z);
        world.removeTileEntity(x, y, z);
        int cx = x & 0xf;
        int cy = y & 15;
        int cz = z & 0xf;
        int j1 = cz << 4 | cx;
        if (y >= chunk.precipitationHeightMap[j1] - 1)
        {
            chunk.precipitationHeightMap[j1] = -999;
        }
        int k1 = chunk.heightMap[j1];
        ExtendedBlockStorage[] storageArrays = chunk.getBlockStorageArray();
        ExtendedBlockStorage extendedblockstorage = storageArrays[y >> 4];
        boolean flag = false;
        if (extendedblockstorage == null)
        {
            if (id == null) return true;
            extendedblockstorage = storageArrays[y >> 4] = new ExtendedBlockStorage(y >> 4 << 4, !world.provider.hasNoSky);
            flag = y >= k1;
        }
        extendedblockstorage.func_150818_a(cx, cy, cz, id);
        extendedblockstorage.setExtBlockMetadata(cx, cy, cz, m);
        /*
        if (flag)
        {
            chunk.generateSkylightMap();
        }
        else
        {
            if (chunk.getBlockLightOpacity(cx, y, cz) > 0)
            {
                if (y >= k1)
                {
                    chunk.relightBlock(cx, y + 1, cz);
                }
            }
            else if (y == k1 - 1)
            {
                chunk.relightBlock(cx, y, cz);
            }
            chunk.propagateSkylightOcclusion(cx, cz);
        }
        */
        chunk.isModified = true;
        world.setTileEntity(x, y, z, tile);
        //world.updateAllLightTypes(x, y, z);
        world.markBlockForUpdate(x, y, z);
        return true;
    }
    
    /**
     * Move an entity to a new Position
     * @param entity the Entity to move
     * @param dim new world dimension
     * @param x new x position
     * @param y new y position
     * @param z new z position
     */
    public static void moveEntity(Entity entity, int dim, double x, double y, double z)
    {
        int dimO = entity.worldObj.provider.dimensionId;
        if (entity instanceof EntityPlayerMP) {
            if (dim != dimO) tpPlayerToDim((EntityPlayerMP)entity, dim, x, y, z);
            else ((EntityPlayerMP)entity).setPositionAndUpdate(x, y, z);
        } else if (dim != dimO) {
            entity.worldObj.theProfiler.startSection("changeDimension");
            MinecraftServer server = MinecraftServer.getServer();
            WorldServer worldO = server.worldServerForDimension(dimO);
            WorldServer worldN = server.worldServerForDimension(dim);
            entity.dimension = dim;
            entity.worldObj.removeEntity(entity);
            entity.isDead = false;
            entity.worldObj.theProfiler.startSection("reposition");
            tpEntity(entity, worldO, worldN, x, y, z);
            entity.worldObj.theProfiler.endStartSection("reloading");
            Entity var6 = EntityList.createEntityByName(EntityList.getEntityString(entity), worldN);
            if (var6 != null)
            {
                var6.copyDataFrom(entity, true);
                worldN.spawnEntityInWorld(var6);
            }
            entity.isDead = true;
            entity.worldObj.theProfiler.endSection();
            worldO.resetUpdateEntityTick();
            worldN.resetUpdateEntityTick();
            entity.worldObj.theProfiler.endSection();
        } else {
            entity.setPosition(x, y, z);
        }
    }
    
    private static void tpPlayerToDim(EntityPlayerMP player, int dim, double x, double y, double z)
    {
        MinecraftServer server = MinecraftServer.getServer();
        ServerConfigurationManager manager = server.getConfigurationManager();
        
        int j = player.dimension;
        WorldServer worldO = server.worldServerForDimension(player.dimension);
        player.dimension = dim;
        WorldServer worldN = server.worldServerForDimension(player.dimension);
        player.playerNetServerHandler.sendPacket(new S07PacketRespawn(player.dimension, worldN.difficultySetting, worldN.getWorldInfo().getTerrainType(), player.theItemInWorldManager.getGameType()));
        worldO.removePlayerEntityDangerously(player);
        player.isDead = false;
        tpEntity(player, worldO, worldN, x, y, z);
        worldO.getPlayerManager().removePlayer(player);
        worldN.getPlayerManager().addPlayer(player);
        worldN.theChunkProviderServer.loadChunk((int)player.posX >> 4, (int)player.posZ >> 4);
        player.playerNetServerHandler.setPlayerLocation(player.posX, player.posY, player.posZ, player.rotationYaw, player.rotationPitch);
        player.theItemInWorldManager.setWorld(worldN);
        manager.updateTimeAndWeatherForPlayer(player, worldN);
        manager.syncPlayerInventory(player);
        Iterator iterator = player.getActivePotionEffects().iterator();
        while (iterator.hasNext()) {
            PotionEffect potioneffect = (PotionEffect)iterator.next();
            player.playerNetServerHandler.sendPacket(new S1DPacketEntityEffect(player.getEntityId(), potioneffect));
        }
        FMLCommonHandler.instance().firePlayerChangedDimensionEvent(player, j, dim);
    }
    
    private static void tpEntity(Entity entity, WorldServer os, WorldServer ns, double x, double y, double z)
    {
        os.theProfiler.startSection("placing");
        if (entity.isEntityAlive())
        {
            ns.spawnEntityInWorld(entity);
            entity.setLocationAndAngles(x, y, z, entity.rotationYaw, entity.rotationPitch);
            ns.updateEntityWithOptionalForce(entity, false);
        }
        os.theProfiler.endSection();
        entity.setWorld(ns);
    }
    
}
