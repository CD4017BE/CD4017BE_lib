package cd4017be.lib.script;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

/**
 * 
 * @author CD4017BE
 */
public class ScriptFiles {

	public static Script[] createCompiledPackage(File out) {
		File[] files = out.getParentFile().listFiles((f, n) -> n.endsWith(".rcp"));
		if (files == null || files.length == 0) {
			System.out.println("no valid files found!");
			return null;
		}
		Parser parser = new Parser();
		try {
			long t = System.currentTimeMillis();
			System.out.printf("Compiling %d scripts:\n", files.length);
			Script[] scripts = new Script[files.length];
			for (int i = 0; i < scripts.length; i++) {
				File file = files[i];
				String name = file.getName();
				name = name.substring(0, name.length() - 4);
				System.out.print(name + " ");
				parser.parse(new FileReader(file), name);
				System.out.print("> parsed ");
				Script script = new Compiler(name, parser).compile();
				System.out.println("> compiled");
				script.editDate = file.lastModified();
				scripts[i] = script;
			}
			saveCompiledPackage(out, scripts);
			t = System.currentTimeMillis() - t;
			System.out.printf("done in %.3f s\n", (float)t / 1000F);
			return scripts;
		} catch (Exception e) {
			System.out.println("> failed: " + e.getClass().getName() + "\n" + e.getMessage());
			return null;
		}
	}

	public static void saveCompiledPackage(File out, Script[] scripts) throws IOException {
		DataOutputStream dos = new DataOutputStream(new FileOutputStream(out));
		try {
			System.out.printf("Saving %d scripts:\n", scripts.length);
			dos.writeByte(scripts.length);
			for (Script s : scripts)
				s.writeHeader(dos);
			for (Script s : scripts) {
				System.out.printf(
					"> %s: v. %d, names %d, var %d, func %d\n",
					s.fileName, s.version, s.dictionary.length,
					s.globals.length, s.functions.length
				);
				s.writeData(dos);
				//System.out.printf("- %s: par %d, stack %d, ret %b, size %d bytes\n", e.getKey(), f.Nparam, f.Nstack, f.hasReturn, f.size());
			}
		} finally {
			dos.close();
		}
	}

	public static Script[] loadPackage(File in, HashMap<String, Version> versions, boolean check) throws IOException {
		DataInputStream dis = new DataInputStream(new FileInputStream(in));
		try {
			File dir = in.getParentFile();
			Script[] scripts = new Script[dis.readByte() & 0xff];
			boolean outdated = false;
			for (int i = 0; i < scripts.length; i++) {
				Script s = new Script(dis);
				File f = new File(dir, s.fileName + ".rcp");
				outdated |= f.exists() && s.editDate < f.lastModified();
				Version v = versions.get(s.fileName);
				if (v != null && s.version >= v.version) versions.remove(s.fileName);
				scripts[i] = s;
			}
			if (check && (outdated || !versions.isEmpty())) return null;
			for (Script s : scripts) s.readData(dis);
			return scripts;
		} finally {
			dis.close();
		}
	}

	public static class Version {
		public int version;
		public final String fallback, name;
		public Version(String name, int version, String fallback) {this.name = name; this.version = version; this.fallback = fallback;}
		public Version(String name, String fallback) {this(name, -1, fallback);}
		public Version(String name) {this(name, -1, null);}

		public void checkVersion() {
			if (version >= 0 || fallback == null) return;
			InputStream in = Version.class.getResourceAsStream(fallback);
			if (in == null) return;
			version = getFileVersion(in);
		}

		public int getFileVersion(InputStream in) {
			try (BufferedReader r = new BufferedReader(new InputStreamReader(in))) {
				String l = r.readLine();
				if (l == null) return -1;
				int p = l.indexOf('='), q = l.indexOf(';');
				if (p > 0 && l.substring(0, p).trim().equals("VERSION")) {
					if (q < 0) q = l.length();
					try {return (int)Double.parseDouble(l.substring(p + 1, q).trim());} catch (NumberFormatException e) {}
				}
			} catch (IOException e) {e.printStackTrace();}
			return -1;
		}
	}

}
