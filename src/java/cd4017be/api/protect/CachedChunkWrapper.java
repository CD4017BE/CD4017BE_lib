package cd4017be.api.protect;

import java.util.HashMap;

import com.mojang.authlib.GameProfile;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Caches the results of another IProtectionHandler (that potentially involves complex computation) and arranges them on a chunk grid
 * @author cd4017be
 */
public class CachedChunkWrapper implements IProtectionHandler {

	private HashMap<Key, Boolean> cache = new HashMap<Key, Boolean>();
	private final IProtectionHandler parent;

	/**
	 * @param parent the protection handler that actually defines the permissions
	 */
	public CachedChunkWrapper(IProtectionHandler parent) {
		this.parent = parent;
	}

	/**
	 * clears the internal cache so it will use fresh data from the wrapped handler.
	 */
	public void refresh() {
		cache.clear();
	}

	@Override
	public boolean canEdit(World world, BlockPos pos, GameProfile player) {
		return canEdit(new Key(pos.getX() >> 4, pos.getZ() >> 4, world.provider.getDimension(), player), world);
	}

	@Override
	public boolean canEdit(World world, BlockPos p0, BlockPos p1, GameProfile player) {
		int dim = world.provider.getDimension();
		int x0 = p0.getX() >> 4, x1 = p1.getX() >> 4;
		if (x0 > x1) {int x = x0; x0 = x1; x1 = x;}
		int z0 = p0.getZ() >> 4, z1 = p1.getZ() >> 4;
		if (z0 > z1) {int z = z0; z0 = z1; z1 = z;}
		for (int x = x0; x <= x1; x++)
			for (int z = z0; z <= z1; z++)
				if (!canEdit(new Key(x, z, dim, player), world))
					return false;
		return true;
	}

	boolean canEdit(Key key, World world) {
		Boolean perm = cache.get(key);
		if (perm == null) {
			perm = parent.canEdit(world, new BlockPos(key.cx << 4, 0, key.cz << 4), new BlockPos(key.cx << 4 | 15, 255, key.cz << 4 | 15), key.player);
			cache.put(key, perm);
		}
		return perm;
	}

	static class Key {

		final int cx, cz, dim;
		final GameProfile player;

		Key(int cx, int cz, int dim, GameProfile player) {
			this.cx = cx;
			this.cz = cz;
			this.dim = dim;
			this.player = player;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = prime + cx;
			result = prime * result + cz;
			result = prime * result + dim;
			result = prime * result + player.hashCode();
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj instanceof Key) {
				Key other = (Key) obj;
				return cx == other.cx && cz == other.cz && dim == other.dim && player.equals(other.player);
			}
			return false;
		}

	}

}
