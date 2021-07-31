package cd4017be.lib.text;

import java.io.*;
import java.util.HashMap;
import java.util.Map.Entry;

import com.google.gson.*;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import cd4017be.lib.Lib;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.LanguageInfo;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.GuiScreenEvent.*;

import static cd4017be.lib.Lib.CFG_CLIENT;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.lwjgl.glfw.GLFW.*;

/**
 * @author CD4017BE
 *
 */
public class TooltipEditor {

	private static final int CURSOR_BLINK_INTERVAL = 500;
	private HashMap<String, String> edited = new HashMap<String, String>();
	private String editingKey, editingValue = "";
	String[] lastKeys = new String[8];
	int pos, ofs;
	private int cursor, cursor2;
	private LanguageInfo lastLanguage;
	private boolean specialCombo;

	@OnlyIn(Dist.CLIENT)
	public static void init() {
		if (TooltipUtil.editor == null && CFG_CLIENT.tooltipEditEnable.get()) {
			TooltipUtil.editor = new TooltipEditor();
			Lib.LOG.info("ingame tooltip editor is enabled");
		}
	}

	public TooltipEditor() {
		MinecraftForge.EVENT_BUS.register(this);
		this.lastLanguage = Minecraft.getInstance().getLanguageManager().getSelected();
	}

	/**
	 * @param key
	 * @return
	 */
	public boolean hasEdited(String key) {
		lastKeys[pos = pos+1 & 7] = key;
		return key.equals(editingKey) || edited.containsKey(key);
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
		return TooltipUtil.getUnhideIllegalFormat(key);
	}

	private String textField() {
		if ((System.currentTimeMillis() % CURSOR_BLINK_INTERVAL) * 2 >= CURSOR_BLINK_INTERVAL) return editingValue;
		if (cursor == cursor2) return put(cursor, "|", cursor);
		int a, b;
		if (cursor < cursor2) {a = cursor; b = cursor2;}
		else {a = cursor2; b = cursor;}
		return editingValue.substring(0, a) + "[" + editingValue.substring(a, b) + "]" + editingValue.substring(b);
	}

	@OnlyIn(Dist.CLIENT)
	@SubscribeEvent(priority = EventPriority.HIGH)
	public void keyPressed(KeyboardKeyPressedEvent.Pre event) {
		int k = event.getKeyCode();
		if (editingKey != null) textFieldInput(k);
		else if (k == GLFW_KEY_S && glfwGetKey(Minecraft.getInstance().getWindow().getWindow(), GLFW_KEY_F4) == GLFW_PRESS) {
			specialCombo = true;
			save();
		} else if (k == GLFW_KEY_F4) {
			LanguageInfo l = Minecraft.getInstance().getLanguageManager().getSelected();
			if (!l.equals(lastLanguage)) {
				save();
				lastLanguage = l;
				edited.clear();
			}
		} else return;
		event.setCanceled(true);
	}

	@OnlyIn(Dist.CLIENT)
	@SubscribeEvent(priority = EventPriority.HIGH)
	public void keyReleased(KeyboardKeyReleasedEvent.Pre event) {
		if (editingKey != null) event.setCanceled(true);
		else if (event.getKeyCode() == GLFW_KEY_F4) {
			if (specialCombo) specialCombo = false;
			else {
				String key = lastKeys[pos];
				if (key != null) {
					editingValue = getTranslation(key);
					cursor = cursor2 = editingValue.length();
					editingKey = key;
					ofs = 0;
					TooltipUtil.altOverride = Screen.hasControlDown();
					TooltipUtil.shiftOverride = Screen.hasShiftDown();
					TooltipUtil.overrideModifiers = true;
				}
			}
		}
	}

	@OnlyIn(Dist.CLIENT)
	@SubscribeEvent(priority = EventPriority.HIGH)
	public void keyTyped(KeyboardCharTypedEvent.Pre event) {
		if (editingKey == null) return;
		char c = event.getCodePoint();
		if (!Character.isISOControl(c)) {
			editingValue = put(cursor, "" + c, cursor2);
			cursor = cursor2 = (cursor < cursor2 ? cursor : cursor2) + 1;
		}
		event.setCanceled(true);
	}

	@OnlyIn(Dist.CLIENT)
	@SubscribeEvent
	public void render(GuiScreenEvent.BackgroundDrawnEvent e) {
		if (editingKey == null) return;
		Screen gui = e.getGui();
		@SuppressWarnings("resource")
		Font fr = gui.getMinecraft().font;
		for (int i = 0; i < 8; i++) {
			String s = i == ofs ? editingKey : lastKeys[pos-i & 7];
			if (s == null) continue;
			fr.drawShadow(
				e.getMatrixStack(), s, 5, gui.height - fr.lineHeight * (i + 1) - 5,
				i == ofs ? 0xffff80 : edited.containsKey(s) ? 0x8080ff : 0xc0c0c0
			);
		}
	}

	@OnlyIn(Dist.CLIENT)
	private void textFieldInput(int k) {
		boolean shift = Screen.hasShiftDown();
		switch(k) {
		case GLFW_KEY_RIGHT:
			if (shift) {
				if (cursor2 < editingValue.length()) cursor2++;
			} else if (cursor < editingValue.length()) cursor2 = ++cursor;
			break;
		case GLFW_KEY_LEFT:
			if (shift) {
				if (cursor2 > 0) cursor2--;
			} else if (cursor > 0) cursor2 = --cursor;
			break;
		case GLFW_KEY_END:
			cursor2 = editingValue.length();
			if (!shift) cursor = cursor2;
			break;
		case GLFW_KEY_HOME:
			cursor2 = 0;
			if (!shift) cursor = cursor2;
			break;
		case GLFW_KEY_DELETE:
			if (cursor != cursor2) {
				editingValue = put(cursor, "", cursor2);
				cursor = cursor2 = cursor < cursor2 ? cursor : cursor2;
			} else if (cursor < editingValue.length())
				editingValue = put(cursor, "", cursor + 1);
			break;
		case GLFW_KEY_BACKSPACE:
			if (cursor != cursor2) {
				editingValue = put(cursor, "", cursor2);
				cursor = cursor2 = cursor < cursor2 ? cursor : cursor2;
			} else if (cursor > 0) {
				editingValue = put(cursor - 1, "", cursor);
				cursor2 = --cursor;
			}
			break;
		case GLFW_KEY_ENTER:
			editingValue = put(cursor, "\n", cursor2);
			cursor = cursor2 = (cursor < cursor2 ? cursor : cursor2) + 1;
			break;
		case GLFW_KEY_UP: {
			int p = editingValue.lastIndexOf('\n', cursor - 1);
			cursor2 = p < 0 ? 0 : p;
			if (!shift) cursor = cursor2;
		}	break;
		case GLFW_KEY_DOWN: {
			int p = editingValue.indexOf('\n', cursor + 1);
			cursor2 = p < 0 ? editingValue.length() : p;
			if (!shift) cursor = cursor2;
		}	break;
		case GLFW_KEY_ESCAPE: {
			String key = editingKey;
			editingKey = null;
			if (!editingValue.equals(getTranslation(key))) edited.put(key, editingValue);
			TooltipUtil.overrideModifiers = false;
		}	break;
		case GLFW_KEY_PAGE_UP: {
			if (ofs < 7) ofs++;
			String nkey = lastKeys[pos-ofs & 7];
			String key = editingKey;
			editingKey = null;
			if (!editingValue.equals(getTranslation(key))) edited.put(key, editingValue);
			if (nkey != null) {
				editingValue = getTranslation(nkey);
				editingKey = nkey;
				cursor = cursor2 = editingValue.length();
			} else TooltipUtil.overrideModifiers = false;
		}	break;
		case GLFW_KEY_PAGE_DOWN: {
			if (ofs > 0) ofs--;
			String nkey = lastKeys[pos-ofs & 7];
			String key = editingKey;
			editingKey = null;
			if (!editingValue.equals(getTranslation(key))) edited.put(key, editingValue);
			if (nkey != null) {
				editingValue = getTranslation(nkey);
				editingKey = nkey;
				cursor = cursor2 = editingValue.length();
			} else TooltipUtil.overrideModifiers = false;
		}	break;
		default:
			if (Screen.hasControlDown() && !shift && !Screen.hasAltDown()) {
				switch(k) {
				case GLFW_KEY_C:
					if (cursor != cursor2)
						TooltipUtil.setClipboardString(get(cursor, cursor2));
					break;
				case GLFW_KEY_X:
					if (cursor != cursor2) {
						TooltipUtil.setClipboardString(get(cursor, cursor2));
						editingValue = put(cursor, "", cursor2);
						cursor = cursor2 = cursor < cursor2 ? cursor : cursor2;
					}
					break;
				case GLFW_KEY_V: {
					String s = TooltipUtil.getClipboardString();
					editingValue = put(cursor, s, cursor2);
					cursor = cursor2 = (cursor < cursor2 ? cursor : cursor2) + s.length();
				}	break;
				case GLFW_KEY_A:
					cursor = 0;
					cursor2 = editingValue.length();
					break;
				case GLFW_KEY_S:
					edited.put(editingKey, editingValue);
					break;
				case GLFW_KEY_Z: {
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
		File file = FMLPaths.GAMEDIR.get().resolve(CFG_CLIENT.tooltipEditPath.get()).toFile();
		file.mkdirs();
		file = new File(file, lastLanguage.getCode() + ".json");
		JsonObject jo;
		try (JsonReader jr = new JsonReader(new InputStreamReader(new FileInputStream(file), UTF_8))) {
			jo = Streams.parse(jr).getAsJsonObject();
		} catch (FileNotFoundException e) {
			jo = new JsonObject();
		} catch (IOException | RuntimeException e) {
			Lib.LOG.error("TooltipEditor failed reading existing lang file: " + file, e);
			return;
		}
		
		for (Entry<String, String> e : edited.entrySet())
			jo.addProperty(e.getKey(), TooltipUtil.hideIllegalFormat(e.getValue()));
		
		try (JsonWriter jw = new JsonWriter(new OutputStreamWriter(new FileOutputStream(file), UTF_8))) {
			jw.setIndent("  ");
			Streams.write(jo, jw);
			Lib.LOG.info("TooltipEditor modified {} lang entries", edited.size());
		} catch (IOException e) {
			Lib.LOG.error("TooltipEditor failed saving changes to lang file: " + file, e);
		}
	}

}
