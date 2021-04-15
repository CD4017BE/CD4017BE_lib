package cd4017be.lib.util;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import org.apache.commons.io.IOUtils;
import cd4017be.lib.Lib;

/**
 * 
 * @author CD4017BE
 */
public class FileUtil {

	public static String readTextFile(InputStream is) throws IOException {
		InputStreamReader isr = new InputStreamReader(is);
		String s = "";
		int n;
		char[] buff = new char[256];
		while((n = isr.read(buff)) > 0) s += String.valueOf(buff, 0, n);
		return s;
	}

	public static void copyData(String resourcePath, File target) throws IOException {
		Lib.LOG.info("File copy: {} -> {}", resourcePath, target);
		InputStream in = FileUtil.class.getResourceAsStream(resourcePath);
		target.getParentFile().mkdirs();
		if (!target.createNewFile()) {
			File old = new File(target.getPath() + ".old");
			if (!old.exists()) target.renameTo(old);
		}
		OutputStream out = new DataOutputStream(new FileOutputStream(target));
		IOUtils.copy(in, out);
	}

}
