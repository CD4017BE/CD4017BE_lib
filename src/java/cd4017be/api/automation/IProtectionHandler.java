package cd4017be.api.automation;

import com.mojang.authlib.GameProfile;

import net.minecraft.world.World;

public interface IProtectionHandler {
	/**
     * @param player
     * @param world
     * @param chunkX
     * @param chunkZ
     * @return the restriction level for given username at given position.
     */
    public ProtectLvl getPlayerAccess(GameProfile player, World world, int chunkX, int chunkZ);
    /**
     * @param player
     * @param world
     * @param cx
     * @param cz
     * @return true if block editing should be allowed within given chunk for given username
     */
    public boolean isOperationAllowed(GameProfile player, World world, int cx, int cz);
    /**
     * @param player
     * @param world
     * @param x0
     * @param x1
     * @param z0
     * @param z1
     * @return true if block editing should be allowed within given range for given username
     */
    public boolean isOperationAllowed(GameProfile player, World world, int x0, int x1, int z0, int z1);
    /**
     * @param player
     * @param world
     * @param cx
     * @param cz
     * @return true if right-click operation should be allowed within given chunk for given username
     */
    public boolean isInteractingAllowed(GameProfile player, World world, int cx, int cz);
}