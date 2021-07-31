package cd4017be.api.protect;

import java.util.UUID;

import com.mojang.authlib.GameProfile;

import net.minecraft.nbt.CompoundTag;

/**
 * 
 * @author cd4017be
 */
public class PermissionUtil {

	/**fallback GameProfile to use when the responsible player is unknown */
	public static final GameProfile DEFAULT_PLAYER = new GameProfile(new UUID(0, 0), "#machine");

	/**the currently active handler to be used by devices */
	public static IProtectionHandler handler = IProtectionHandler.DEFAULT;

	public static void writeOwner(CompoundTag nbt, GameProfile owner) {
		nbt.putString("ownerName", owner.getName());
		nbt.putLong("ownerID0", owner.getId().getMostSignificantBits());
		nbt.putLong("ownerID1", owner.getId().getLeastSignificantBits());
	}

	public static GameProfile readOwner(CompoundTag nbt) {
		try {
			return new GameProfile(new UUID(nbt.getLong("ownerID0"), nbt.getLong("ownerID1")), nbt.getString("ownerName"));
		} catch (Exception e) {
			return DEFAULT_PLAYER;
		}
	}

}
