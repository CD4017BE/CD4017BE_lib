package cd4017be.lib.render;

/**
 * implemented by 'final' (resource-pack independent) models that should not be automatically discarded and reloaded when resource-pack changes.
 * @author CD4017BE
 */
public interface IHardCodedModel {

	/**
	 * Called when resource-manager starts to reload models
	 */
	public void onReload();

}
