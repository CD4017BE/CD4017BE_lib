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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

/**
 * 
 * @author CD4017BE
 */
public class ScriptFiles {

	public static Script[] createCompiledPackage(File out) {
		File[] files = out.getParentFile().listFiles();
		ArrayList<File> in = new ArrayList<File>();
		if (files != null)
			for (File f : files)
				if (f.getName().endsWith(".rcp"))
					in.add(f);
		if (in.isEmpty()) {
			System.out.println("no valid files found!");
			return null;
		}
		try {
			long t = System.currentTimeMillis();
			System.out.printf("Compiling %d scripts:\n", in.size());
			Script[] scripts = new Script[in.size()];
			for (int i = 0; i < scripts.length; i++) {
				File file = in.get(i);
				String name = file.getName();
				name = name.substring(0, name.length() - 4);
				System.out.print(name + " ");
				Compiler c = new Compiler(name, Compiler.parse(name, new FileReader(file)));
				System.out.print("> parsed ");
				Script script = c.compile();
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
			for (Script s : scripts) {
				dos.writeUTF(s.fileName);
				dos.writeLong(s.editDate);
				dos.writeInt(s.version);
			}
			for (Script s : scripts) {
				System.out.printf("> %s: v. %d, const %d, func %d\n", s.fileName, s.version, s.variables.size(), s.methods.size());
				dos.writeShort(s.variables.size());
				for (Entry<String, Object> e : s.variables.entrySet()) {
					dos.writeUTF(e.getKey());
					serialize(e.getValue(), dos);
				}
				dos.writeShort(s.methods.size());
				for (Entry<String, Function> e : s.methods.entrySet()) {
					dos.writeUTF(e.getKey());
					Function f = e.getValue();
					System.out.printf("- %s: par %d, stack %d, ret %b, size %d bytes\n", e.getKey(), f.Nparam, f.Nstack, f.hasReturn, f.size());
					f.writeData(dos);
				}
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
				String name = dis.readUTF();
				Script s = new Script(name, new HashMap<String, Function>(), new HashMap<String, Object>());
				s.editDate = dis.readLong();
				File f = new File(dir, name + ".rcp");
				outdated |= f.exists() && s.editDate < f.lastModified();
				s.version = dis.readInt();
				Version v = versions.get(name);
				if (v != null && s.version >= v.version) versions.remove(name);
				scripts[i] = s;
			}
			if (check && (outdated || !versions.isEmpty())) return null;
			for (Script s : scripts) {
				int n = dis.readShort();
				for (int i = 0; i < n; i++) {
					String name = dis.readUTF();
					s.variables.put(name, deserialize(dis));
				}
				n = dis.readShort();
				for (int i = 0; i < n; i++) {
					String name = dis.readUTF();
					Function f = new Function(s.fileName + "." + name, dis);
					s.methods.put(name, f);
					f.script = s;
				}
			}
			return scripts;
		} finally {
			dis.close();
		}
	}

	private static void serialize(Object v, DataOutputStream dos) throws IOException {
		if (v instanceof Boolean) {
			dos.writeByte((Boolean)v ? 2 : 1);
		} else if (v instanceof Double) {
			dos.writeByte(3);
			dos.writeDouble((Double)v);
		} else if (v instanceof String) {
			dos.writeByte(4);
			dos.writeUTF((String)v);
		} else if (v instanceof double[]) {
			dos.writeByte(5);
			double[] vec = (double[])v;
			dos.writeByte(vec.length);
			for (double d : vec) dos.writeDouble(d);
		} else if (v instanceof Object[]) {
			dos.writeByte(6);
			Object[] arr = (Object[])v;
			dos.writeByte(arr.length);
			for (Object o : arr) serialize(o, dos);
		} else {
			dos.writeByte(0);
		}
	}

	private static Object deserialize(DataInputStream dis) throws IOException {
		switch(dis.readByte()) {
		case 1: return false;
		case 2: return true;
		case 3: return dis.readDouble();
		case 4: return dis.readUTF();
		case 5: 
			double[] vec = new double[dis.readByte() & 0xff];
			for (int i = 0; i < vec.length; i++) vec[i] = dis.readDouble();
			return vec;
		case 6:
			Object[] arr = new Object[dis.readByte() & 0xff];
			for (int i = 0; i < arr.length; i++) arr[i] = deserialize(dis);
			return arr;
		default: return null;
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
			try (BufferedReader r = new BufferedReader(new InputStreamReader(in))) {
				String l = r.readLine();
				int p = l.indexOf('='), q = l.indexOf(';');
				if (p > 0 && l.substring(0, p).trim().equals("VERSION")) {
					if (q < 0) q = l.length();
					try {version = (int)Double.parseDouble(l.substring(p + 1, q).trim());} catch (NumberFormatException e) {}
				}
			} catch (IOException e) {e.printStackTrace();}
		}
	}

}
