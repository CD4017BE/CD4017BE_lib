package cd4017be.api.automation;

/**
 *
 * @author CD4017BE
 */
public enum ProtectLvl {

	Free(0), Protected(1.5F), NoAcces(2F), NoInventory(3);

	public float energyCost;

	private ProtectLvl(float ec) {
		this.energyCost = ec;
	}

	public static ProtectLvl getLvl(int lvl) {
		if (lvl <= 0 || lvl > values().length) return Free;
		else return values()[lvl];
	}
}
