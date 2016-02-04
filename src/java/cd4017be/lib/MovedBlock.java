/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cd4017be.lib;

import net.minecraftforge.fml.common.FMLCommonHandler;

import java.lang.reflect.Method;
import java.util.Iterator;

import cd4017be.api.automation.IOperatingArea;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.S07PacketRespawn;
import net.minecraft.network.play.server.S1DPacketEntityEffect;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

/**
 *
 * @author CD4017BE
 */
public class MovedBlock 
{
    public final NBTTagCompound nbt;
    public final IBlockState block;
    
    public MovedBlock(IBlockState block, NBTTagCompound tile) 
    {
        this.block = block;
        this.nbt = tile;
    }
    
    public boolean set(World world, BlockPos pos)
    {
        TileEntity tile = null;
        boolean multipart = false;
        if (nbt != null) {
            nbt.setInteger("x", pos.getX());
            nbt.setInteger("y", pos.getY());
            nbt.setInteger("z", pos.getZ());
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
                    area[0] += pos.getX(); area[3] += pos.getX();
                    area[1] += pos.getY(); area[4] += pos.getY();
                    area[2] += pos.getZ(); area[5] += pos.getZ();
                }
            }
        }
        boolean set = setBlock(world, pos, block, tile);
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
    
    public static MovedBlock get(World world, BlockPos pos)
    {
        IBlockState id = world.getBlockState(pos);
        NBTTagCompound nbt = null;
        TileEntity te = world.getTileEntity(pos);
        if (te != null) {
            if (te instanceof IOperatingArea) {
                int[] area = ((IOperatingArea)te).getOperatingArea();
                area[0] -= pos.getX(); area[3] -= pos.getX();
                area[1] -= pos.getY(); area[4] -= pos.getY();
                area[2] -= pos.getZ(); area[5] -= pos.getZ();
            }
            nbt = new NBTTagCompound();
            te.writeToNBT(nbt);
        }
        return new MovedBlock(id, nbt);
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
    public static boolean setBlock(World world, BlockPos pos, IBlockState state, TileEntity tile)
    {
    	if (!world.isBlockLoaded(pos)) return false;
        Chunk chunk = world.getChunkFromBlockCoords(pos);
        IBlockState state0 = chunk.getBlockState(pos);
        Block block = state.getBlock();
        Block block0 = state0.getBlock();
        int oldLight = block0.getLightValue(world, pos);

        world.removeTileEntity(pos);
        if (state0 == state) {
        	world.setTileEntity(pos, tile);
        	world.markBlockForUpdate(pos);
        	return true;
        }

        int bx = pos.getX() & 15;
        int y = pos.getY();
        int bz = pos.getZ() & 15;
        int p = bz << 4 | bx;

        if (y >= chunk.precipitationHeightMap[p] - 1) chunk.precipitationHeightMap[p] = -999;
        int h = chunk.getHeightMap()[p];
        
        ExtendedBlockStorage[] storageArrays = chunk.getBlockStorageArray();
        ExtendedBlockStorage extendedblockstorage = storageArrays[y >> 4];
        boolean flag = false;
        if (extendedblockstorage == null) {
        	if (block == Blocks.air) return false;
        	extendedblockstorage = storageArrays[y >> 4] = new ExtendedBlockStorage(y >> 4 << 4, !world.provider.getHasNoSky());
            flag = y >= h;
        }

        extendedblockstorage.set(bx, y & 15, bz, state);
        if (extendedblockstorage.getBlockByExtId(bx, y & 15, bz) != block) return false;
        
        if (flag) chunk.generateSkylightMap();
        else {
        	int l = block.getLightOpacity(world, pos);
            int l0 = block0.getLightOpacity(world, pos);

            if (l > 0) {
            	if (y >= h) chunk.relightBlock(bx, y + 1, bz);
            } else if (y == h - 1) chunk.relightBlock(bx, y, bz);

            if (l != l0 && (l < l0 || chunk.getLightFor(EnumSkyBlock.SKY, pos) > 0 || chunk.getLightFor(EnumSkyBlock.BLOCK, pos) > 0)) {
            	chunk.propagateSkylightOcclusion(bx, bz);
            }
        }

        world.setTileEntity(pos, tile);
        chunk.setModified(true);

        if (block.getLightOpacity() != block0.getLightOpacity() || block.getLightValue(world, pos) != oldLight) {
          	world.theProfiler.startSection("checkLight");
           	world.checkLight(pos);
           	world.theProfiler.endSection();
        }
        world.markBlockForUpdate(pos);
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
        int dimO = entity.worldObj.provider.getDimensionId();
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
                var6.copyDataFromOld(entity);
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
        player.playerNetServerHandler.sendPacket(new S07PacketRespawn(player.dimension, worldN.getDifficulty(), worldN.getWorldInfo().getTerrainType(), player.theItemInWorldManager.getGameType()));
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
