package cd4017be.lib.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.logging.log4j.Level;

import cd4017be.api.recipes.RecipeScriptContext;
import cd4017be.lib.Lib;
import cd4017be.lib.script.Module;
import static org.lwjgl.input.Keyboard.*;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.client.event.GuiScreenEvent.KeyboardInputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author CD4017BE
 *
 */
@SuppressWarnings("deprecation")
public class TooltipEditor {

	private static final int CURSOR_BLINK_INTERVAL = 500;
	private HashMap<String, String> edited = new HashMap<String, String>();
	private String editingKey, editingValue = "";
	String[] lastKeys = new String[8];
	int pos, ofs;
	private int cursor, cursor2;
	private final File langFile;
	private String lastLanguage;
	private boolean specialCombo;

	@SideOnly(Side.CLIENT)
	public static void init() {
		if (TooltipUtil.editor != null) return;
		Module m = RecipeScriptContext.instance.modules.get(Lib.ConfigName);
		if (m != null) {
			Object path = m.read("tooltip_editor_file");
			if (path instanceof String) {
				File file = new File(Minecraft.getMinecraft().mcDataDir, (String)path);
				if (file.exists()) {
					TooltipUtil.editor = new TooltipEditor(file);
					FMLLog.log("tooltipEditor", Level.INFO, "ingame tooltip editor is enabled");
				} else FMLLog.log("tooltipEditor", Level.WARN, "tooltip editor could not be enabled because assigned output file %s doesn't exist!", file);
			}
		}
	}

	public TooltipEditor(File langFile) {
		this.langFile = langFile;
		MinecraftForge.EVENT_BUS.register(this);
		this.lastLanguage = FMLClientHandler.instance().getCurrentLanguage();
	}

	/**
	 * @param s
	 * @return
	 */
	public boolean hasEdited(String s) {
		return s.equals(editingKey) || edited.containsKey(s);
	}

	/**
	 * 
	 * @param key
	 * @return
	 */
	public String getTranslation(String key) {
		lastKeys[pos = pos+1 & 7] = key;
		if (key.equals(editingKey)) return textField();
		String s = edited.get(key);
		if (s != null) return s;
		return I18n.translateToLocal(key);
	}

	private String textField() {
		if ((System.currentTimeMillis() % CURSOR_BLINK_INTERVAL) * 2 >= CURSOR_BLINK_INTERVAL) return editingValue;
		if (cursor == cursor2) return put(cursor, "|", cursor);
		int a, b;
		if (cursor < cursor2) {a = cursor; b = cursor2;}
		else {a = cursor2; b = cursor;}
		return editingValue.substring(0, a) + "[" + editingValue.substring(a, b) + "]" + editingValue.substring(b);
	}

	@SideOnly(Side.CLIENT)
	@SubscribeEvent(priority = EventPriority.HIGH)
	public void keyTyped(KeyboardInputEvent.Pre event) {
		int k = getEventKey();
		if (editingKey != null) {
			if (getEventKeyState()) textFieldInput();
		} else if (k == KEY_S && isKeyDown(KEY_F4)) {
			if (getEventKeyState()) {
				specialCombo = true;
				save();
			}
		} else if (k == KEY_F4) {
			if (getEventKeyState()) {
				String l = FMLClientHandler.instance().getCurrentLanguage();
				if (!l.equals(lastLanguage)) {
					save();
					lastLanguage = l;
					edited.clear();
				}
			} else if (specialCombo) specialCombo = false;
			else {
				String key = lastKeys[pos];
				if (key != null) {
					editingValue = getTranslation(key);
					cursor = cursor2 = editingValue.length();
					editingKey = key;
					ofs = 0;
				}
			}
		} else return;
		event.setCanceled(true);
	}

	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void render(net.minecraftforge.client.event.GuiScreenEvent.BackgroundDrawnEvent e) {
		if (editingKey == null) return;
		GuiScreen gui = e.getGui();
		for (int i = 0; i < 8; i++) {
			String s = i == ofs ? editingKey : lastKeys[pos-i & 7];
			if (s == null) continue;
			gui.drawString(gui.mc.fontRendererObj, s, 5, gui.height - gui.mc.fontRendererObj.FONT_HEIGHT * (i + 1) - 5, i == ofs ? 0xffff80 : edited.containsKey(s) ? 0x8080ff : 0xc0c0c0);
		}
	}

	@SideOnly(Side.CLIENT)
	private void textFieldInput() {
		int k = getEventKey();
		boolean shift = GuiScreen.isShiftKeyDown();
		switch(k) {
		case KEY_RIGHT:
			if (shift) {
				if (cursor2 < editingValue.length()) cursor2++;
			} else if (cursor < editingValue.length()) cursor2 = ++cursor;
			break;
		case KEY_LEFT:
			if (shift) {
				if (cursor2 > 0) cursor2--;
			} else if (cursor > 0) cursor2 = --cursor;
			break;
		case KEY_END:
			cursor2 = editingValue.length();
			if (!shift) cursor = cursor2;
			break;
		case KEY_HOME:
			cursor2 = 0;
			if (!shift) cursor = cursor2;
			break;
		case KEY_DELETE:
			if (cursor != cursor2) {
				editingValue = put(cursor, "", cursor2);
				cursor = cursor2 = cursor < cursor2 ? cursor : cursor2;
			} else if (cursor < editingValue.length())
				editingValue = put(cursor, "", cursor + 1);
			break;
		case KEY_BACK:
			if (cursor != cursor2) {
				editingValue = put(cursor, "", cursor2);
				cursor = cursor2 = cursor < cursor2 ? cursor : cursor2;
			} else if (cursor > 0) {
				editingValue = put(cursor - 1, "", cursor);
				cursor2 = --cursor;
			}
			break;
		case KEY_RETURN:
			editingValue = put(cursor, "\n", cursor2);
			cursor = cursor2 = (cursor < cursor2 ? cursor : cursor2) + 1;
			break;
		case KEY_UP: {
			int p = editingValue.lastIndexOf('\n', cursor - 1);
			cursor2 = p < 0 ? 0 : p;
			if (!shift) cursor = cursor2;
		}	break;
		case KEY_DOWN: {
			int p = editingValue.indexOf('\n', cursor + 1);
			cursor2 = p < 0 ? editingValue.length() : p;
			if (!shift) cursor = cursor2;
		}	break;
		case KEY_ESCAPE: {
			String key = editingKey;
			editingKey = null;
			if (!editingValue.equals(getTranslation(key))) edited.put(key, editingValue);
		}	break;
		case KEY_PRIOR: {
			if (ofs < 7) ofs++;
			String nkey = lastKeys[pos-ofs & 7];
			String key = editingKey;
			editingKey = null;
			if (!editingValue.equals(getTranslation(key))) edited.put(key, editingValue);
			if (nkey != null) {
				editingValue = getTranslation(nkey);
				editingKey = nkey;
				cursor = cursor2 = editingValue.length();
			}
		}	break;
		case KEY_NEXT: {
			if (ofs > 0) ofs--;
			String nkey = lastKeys[pos-ofs & 7];
			String key = editingKey;
			editingKey = null;
			if (!editingValue.equals(getTranslation(key))) edited.put(key, editingValue);
			if (nkey != null) {
				editingValue = getTranslation(nkey);
				editingKey = nkey;
				cursor = cursor2 = editingValue.length();
			}
		}	break;
		default:
			if (GuiScreen.isCtrlKeyDown() && !shift && !GuiScreen.isAltKeyDown()) {
				switch(k) {
				case KEY_C:
					if (cursor != cursor2)
						GuiScreen.setClipboardString(get(cursor, cursor2));
					break;
				case KEY_X:
					if (cursor != cursor2) {
						GuiScreen.setClipboardString(get(cursor, cursor2));
						editingValue = put(cursor, "", cursor2);
						cursor = cursor2 = cursor < cursor2 ? cursor : cursor2;
					}
					break;
				case KEY_V: {
					String s = GuiScreen.getClipboardString();
					editingValue = put(cursor, s, cursor2);
					cursor = cursor2 = (cursor < cursor2 ? cursor : cursor2) + s.length();
				}	break;
				case KEY_A:
					cursor = 0;
					cursor2 = editingValue.length();
					break;
				case KEY_S:
					edited.put(editingKey, editingValue);
					break;
				case KEY_Z: {
					String s = edited.get(editingKey);
					if (s != null) {
						editingValue = s;
						if (cursor > editingValue.length()) cursor = editingValue.length();
						cursor2 = cursor;
					}
				}	break;
				}
				break;
			}
			char c = getEventCharacter();
			if (!Character.isISOControl(c)) {
				editingValue = put(cursor, "" + c, cursor2);
				cursor = cursor2 = (cursor < cursor2 ? cursor : cursor2) + 1;
			}
		}
	}

	private String put(int start, String repl, int end) {
		if (end < start) {int x = start; start = end; end = x;}
		return editingValue.substring(0, start) + repl + editingValue.substring(end);
	}

	private String get(int start, int end) {
		if (end < start) {int x = start; start = end; end = x;}
		return editingValue.substring(start, end);
	}

	public void save() {
		if (edited.isEmpty()) return;
		Path file = new File(langFile, lastLanguage + ".lang").toPath();
		@SuppressWarnings("unchecked")
		HashMap<String, String> changes = (HashMap<String, String>)edited.clone();
		int n = changes.size();
		ArrayList<String> entries = new ArrayList<String>();
		try {
			for (String l : Files.readAllLines(file)) {
				if (!l.isEmpty() && l.charAt(0) != '#') {
					int p = findUnescaped(l, '='), q = findUnescaped(l, ':');
					if (p < 0 || q >= 0 && q < p) p = q;
					if (p >= 0) {
						String key = removeEscapes(l.substring(0, p));
						String value = changes.remove(key);
						if (value != null) l = l.substring(0, p + 1) + escape(value);
					}
				}
				entries.add(l);
			}
			if ((n -= changes.size()) > 0) FMLLog.log("tooltipEditor", Level.INFO, "%d lang entries were changed", n);
			if (!changes.isEmpty()) {
				entries.add("#added by ingame editor:");
				for (Entry<String, String> e : changes.entrySet())
					entries.add(escape(e.getKey()) + "=" + escape(e.getValue()));
				FMLLog.log("tooltipEditor", Level.INFO, "%d lang entries were added", changes.size());
			}
			Files.write(file, entries);
		} catch (IOException e) {
			FMLLog.log("tooltipEditor", Level.ERROR, e, "failed saving changes to lang file: %s", langFile);
			return;
		}
	}

	private int findUnescaped(String s, char c) {
		int p = -1;
		do {
			p = s.indexOf(c, p + 1);
		} while (isEscaped(s, p));
		return p;
	}

	private boolean isEscaped(String s, int p) {
		return p > 0 && s.charAt(p - 1) == '\\' && !isEscaped(s, p - 1);
	}

	private String removeEscapes(String s) {
		int p = 0;
		while (p < s.length() && (p = s.indexOf('\\', p)) >= 0) {
			s = s.substring(0, p) + s.substring(++p);
		}
		return s;
	}

	private String escape(String s) {
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '\\' || c == '=' || c == ':') {
				s = s.substring(0, i) + '\\' + s.substring(i);
				i++;
			} else if (c == '\n') {
				s = s.substring(0, i) + "\\n" + s.substring(i+1);
				i++;
			}
		}
		return s;
	}

}
