package cd4017be.lib.script;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

public class ScriptFiles {

	public static Script[] createCompiledPackage(File out) {
		File[] files = out.getParentFile().listFiles();
		ArrayList<File> in = new ArrayList<File>();
		for (File f : files)
			if (f.getName().endsWith(".scr"))
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
			System.out.printf("done in %.3f s", (float)t / 1000F);
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
					Object v = e.getValue();
					if (v instanceof Boolean) {
						dos.writeByte((Boolean)v ? 2 : 1);
					} else if (v instanceof Double) {
						dos.writeByte(3);
						dos.writeDouble((Double)v);
					} else if (v instanceof String) {
						dos.writeByte(4);
						dos.writeUTF((String)v);
					} else {
						dos.writeByte(0);
					}
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
	
	public static Script[] loadPackage(File in, HashMap<String, Version> versions) throws IOException {
		DataInputStream dis = new DataInputStream(new FileInputStream(in));
		try {
			File dir = in.getParentFile();
			Script[] scripts = new Script[dis.readByte() & 0xff];
			boolean outdated = false;
			for (int i = 0; i < scripts.length; i++) {
				String name = dis.readUTF();
				Script s = new Script(name, new HashMap<String, Function>(), new HashMap<String, Object>());
				s.editDate = dis.readLong();
				File f = new File(dir, name + ".scr");
				outdated |= s.editDate < f.lastModified();
				s.version = dis.readInt();
				Version v = versions.get(name);
				if (v != null && s.version >= v.version) versions.remove(name);
			}
			if (outdated || !versions.isEmpty()) return null;//TODO copy fallback
			for (Script s : scripts) {
				int n = dis.readShort();
				for (int i = 0; i < n; i++) {
					String name = dis.readUTF();
					Object obj;
					switch(dis.readByte()) {
					case 0: obj = null; break;
					case 1: obj = false; break;
					case 2: obj = true; break;
					case 3: obj = dis.readDouble();
					case 4: obj = dis.readUTF();
					default: return null;
					}
					s.variables.put(name, obj);
				}
				n = dis.readShort();
				for (int i = 0; i < n; i++) {
					String name = dis.readUTF();
					s.methods.put(name, new Function(s.fileName + "." + name, dis));
				}
			}
			return scripts;
		} finally {
			dis.close();
		}
	}

	public static class Version {
		public final int version;
		public final String fallback, name;
		public Version(String name, int version, String fallback) {this.name = name; this.version = version; this.fallback = fallback;}
		public Version(String name) {this(name, 0, null);}
	}

}
