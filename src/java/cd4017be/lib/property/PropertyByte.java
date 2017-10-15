package cd4017be.lib.property;

import net.minecraftforge.common.property.IUnlistedProperty;

public class PropertyByte implements IUnlistedProperty<Byte> {

	private final String name;
	
	public PropertyByte(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean isValid(Byte value) {
		return true;
	}

	@Override
	public Class<Byte> getType() {
		return Byte.class;
	}

	@Override
	public String valueToString(Byte value) {
		return value.toString();
	}

	/**
	 * convenience method to cast an int to the required property element type
	 * @param i value
	 * @return Byte value casted as T
	 */
	@SuppressWarnings("unchecked")
	public static <T> T cast(int i) {
		return (T)Byte.valueOf((byte)i);
	}

}
