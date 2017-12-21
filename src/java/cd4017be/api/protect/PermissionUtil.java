package cd4017be.api.protect;

import java.util.UUID;

import com.mojang.authlib.GameProfile;

import net.minecraft.nbt.NBTTagCompound;

/**
 * 
 * @author cd4017be
 */
public class PermissionUtil {

	/**fallback GameProfile to use when the responsible player is unknown */
	public static final GameProfile DEFAULT_PLAYER = new GameProfile(new UUID(0, 0), "#machine");

	/**the currently active handler to be used by devices */
	public static IProtectionHandler handler = IProtectionHandler.DEFAULT;

	public static void writeOwner(NBTTagCompound nbt, GameProfile owner) {
		nbt.setString("ownerName", owner.getName());
		nbt.setLong("ownerID0", owner.getId().getMostSignificantBits());
		nbt.setLong("ownerID1", owner.getId().getLeastSignificantBits());
	}

	public static GameProfile readOwner(NBTTagCompound nbt) {
		try {
			return new GameProfile(new UUID(nbt.getLong("ownerID0"), nbt.getLong("ownerID1")), nbt.getString("ownerName"));
		} catch (Exception e) {
			return DEFAULT_PLAYER;
		}
	}

}
