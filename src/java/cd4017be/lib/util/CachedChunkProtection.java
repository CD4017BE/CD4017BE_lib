package cd4017be.lib.util;

import com.mojang.authlib.GameProfile;

import cd4017be.api.automation.AreaProtect;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CachedChunkProtection {

	public final int cx, cz;
	public final boolean allow;
	
	public CachedChunkProtection(int cx, int cz, boolean allow) {
		this.cx = cx;
		this.cz = cz;
		this.allow = allow;
	}
	
	public boolean equalPos(BlockPos pos) {
		return cx == pos.getX() >> 4 && cz == pos.getZ() >> 4;
	}
	
	public static CachedChunkProtection get(GameProfile player, World world, BlockPos pos) {
		int cx = pos.getX() >> 4, cz = pos.getZ() >> 4;
		return new CachedChunkProtection(cx, cz, AreaProtect.operationAllowed(player, world, cx, cz));
	}

}
