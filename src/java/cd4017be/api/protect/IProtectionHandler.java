package cd4017be.api.protect;

import com.mojang.authlib.GameProfile;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A handler that defines restrictions for editing blocks in the world.<br>
 * Used to make machines follow the rules of claimed / protected areas.
 * @author cd4017be
 */
public interface IProtectionHandler {

	/**
	 * requests permission for a player to break or place a block.
	 * @param world world in which the request is made
	 * @param pos position of the block
	 * @param player name & UUID of the player
	 * @return whether the operation is allowed
	 */
	boolean canEdit(World world, BlockPos pos, GameProfile player);

	/**
	 * requests permission for a player to break or place blocks within an area.
	 * @param world world in which the request is made
	 * @param p0 first corner of the area (included)
	 * @param p1 opposite corner of the area (included)
	 * @param player name & UUID of the player
	 * @return whether the operation is allowed for all blocks inside the area
	 */
	boolean canEdit(World world, BlockPos p0, BlockPos p1, GameProfile player);

	/**
	 * Fallback implementation that provides no restrictions at all
	 */
	public static final IProtectionHandler DEFAULT = new IProtectionHandler() {
		@Override
		public boolean canEdit(World world, BlockPos p0, BlockPos p1, GameProfile player) { return true; }
		@Override
		public boolean canEdit(World world, BlockPos pos, GameProfile player) { return true; }
	};

}
