package cd4017be.lib.render.model;

import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;


public class JsonArrayLineReader extends Reader {
	private final Iterator<JsonElement> it;
	private String cur;
	private int pos = 0, length = -1;

	public JsonArrayLineReader(JsonArray arr) {
		this.it = arr.iterator();
	}

	private boolean next() {
		if (!it.hasNext()) return false;
		cur = it.next().getAsString();
		pos = 0;
		length = cur.length();
		return true;
	}
	
	@Override
	public int read(char[] cbuf, int off, int len) throws IOException {
		if (pos > length && !next()) return -1;
		if (len <= 0) return 0;
		if (pos == length) {
			cbuf[off++] = '\n';
			len--;
			pos++;
			if (!next()) return 1;
		}
		int len1 = Math.min(len, length - pos);
		cur.getChars(pos, pos += len1, cbuf, off);
		if (len1 < len) {
			cbuf[off + len] = '\n';
			pos++;
			len++;
		}
		return len;
	}

	@Override
	public int read() throws IOException {
		if (pos > length && !next()) return -1;
		if (pos < length) return cur.charAt(pos++);
		pos++;
		return '\n';
	}

	@Override
	public void close() throws IOException {}
}