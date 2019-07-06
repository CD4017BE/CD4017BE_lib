package cd4017be.api.rs_ctr.port;


/**
 * @author CD4017BE
 *
 */
public interface ITagableConnector extends IConnector {

	public void setTag(MountedPort port, String tag);

	public String getTag();

	@Override
	default String displayInfo(MountedPort port, int linkID) {
		String tag = getTag();
		return tag != null ? "\n\u00a7e" + tag : IConnector.super.displayInfo(port, linkID);
	}

}
