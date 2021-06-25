package cd4017be.lib.util;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;


/**Used to compute a hash code of supplied data.
 * @see #hashCode()
 * @author CD4017BE */
public class HashOutputStream extends OutputStream {

	private int hash = 1;

	@Override
	public void write(int b) throws IOException {
		hash = 31 * hash + (b & 0xff);
	}

	/**The hash code is computed the same way {@link Arrays#hashCode(byte[])} does.
	 * @return the hash code of all written bytes. */
	@Override
	public int hashCode() {
		return hash;
	}

}
