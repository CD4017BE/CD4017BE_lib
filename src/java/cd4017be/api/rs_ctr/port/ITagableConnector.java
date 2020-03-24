package cd4017be.api.rs_ctr.port;

/** implement on a Connector to support tagging with the Wire Tag.
 * @author CD4017BE */
public interface ITagableConnector {

	public void setTag(String tag);

	public String getTag();

}
