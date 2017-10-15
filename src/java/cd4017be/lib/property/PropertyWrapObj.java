package cd4017be.lib.property;

import net.minecraftforge.common.property.IUnlistedProperty;

/**
 * 
 * @author CD4017BE
 * @param <T>
 */
public class PropertyWrapObj<T> implements IUnlistedProperty<T> {

	private final String name;
	private final Class<T> type;
	
	public PropertyWrapObj(String name, Class<T> type) {
		this.name = name;
		this.type = type;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean isValid(T value) {
		return true;
	}

	@Override
	public Class<T> getType() {
		return type;
	}

	@Override
	public String valueToString(T value) {
		return value == null ? "null" : value.toString();
	}

}
