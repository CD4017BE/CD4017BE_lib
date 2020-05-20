package cd4017be.lib.Gui.comp;

import java.io.File;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.ObjIntConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import cd4017be.lib.Lib;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

/** A sub Frame that lets the user choose a file.
 * Triggers an event when the "select File" button is clicked.
 * The event should then call {@link #close()} and {@link #getFile()} to retrieve the selected file.
 * @author cd4017be */
public class FileBrowser extends GuiFrame
implements ObjIntConsumer<GuiList>, Supplier<String>, Consumer<String> {

	private static final String PRE_DIR = "\u00a7e", PRE_FILE = "\u00a7f", BACK = "\u00a7e<--";
	static final ResourceLocation TEX
	= new ResourceLocation(Lib.ID, "textures/gui/file.png");

	final Consumer<FileBrowser> action;
	final Predicate<String> filter;
	File dir = new File("");
	String file = "";

	/**@param parent the gui-component container this will register to
	 * @param action a function to execute when a file was selected
	 * @param filter an optional filename filter */
	public FileBrowser(GuiFrame parent, Consumer<FileBrowser> action, Predicate<String> filter) {
		super(parent, 248, 148, 5);
		background(TEX, 0, 0);
		new GuiList(this, w - 24, 9, 8, 24, 12, this).scrollbar(7, 12, 248, 0);
		new TextField(this, w - 25, 7, 8, 133, 128, this, this);
		new Button(this, 9, 9, w - 16, 132, 0, null, (i) -> action.accept(this)).tooltip("gui.cd4017be.sel_file");
		new FrameGrip(this, 8, 8, 0, 0);
		new Button(this, 8, 8, w - 8, 0, 0, null, (i) -> close()).tooltip("gui.cd4017be.close");
		this.action = action;
		this.filter = filter;
		parent.setFocus(this);
	}

	@Override
	public void drawBackground(int mx, int my, float t) {
		GlStateManager.disableDepth();
		super.drawBackground(mx, my, t);
		String path = dir.getPath() + "/";
		if(fontRenderer.getStringWidth(path) > w - 16)
			path = "..." + fontRenderer.trimStringToWidth(
				path, w - 16 - fontRenderer.getStringWidth("..."), true
			);
		fontRenderer.drawString(path, x + 8, y + 16, 0xff202020);
		GlStateManager.enableDepth();
	}

	/** close this sub Frame */
	public void close() {
		parent.remove(this);
	}

	public FileBrowser setFile(File file) {
		this.dir = file.getParentFile();
		this.file = file.getName();
		updateDir((GuiList)get(0));
		return this;
	}

	public File getFile() {
		return new File(dir, file);
	}

	private void updateDir(GuiList t) {
		File[] files = dir.listFiles();
		if (files == null) files = new File[0];
		String[] names = new String[files.length + 1];
		names[0] = BACK;
		int n = 1, j = 1;
		for(File file : files)
			if(file.isDirectory()) {
				names[n++] = names[j];
				names[j++] = PRE_DIR + file.getName() + "/";
			} else {
				String s = file.getName();
				if (filter == null || filter.test(s))
					names[n++] = PRE_FILE + s;
			}
		if (n < names.length) names = Arrays.copyOf(names, n);
		Arrays.sort(names, 1, n);
		t.setElements(names);
		setFocus(t);
	}

	@Override
	public void accept(GuiList t, int value) {
		if(value <= 0) {
			File parent = dir.getParentFile();
			if(parent != null) dir = parent;
		} else {
			String s = t.elements[value];
			if(s.startsWith(PRE_DIR) && s.endsWith("/"))
				dir = new File(dir, s.substring(PRE_DIR.length(), s.length() - 1));
			else {
				file = s.substring(PRE_FILE.length());
				setFocus(get(2));
				return;
			}
		}
		updateDir(t);
	}

	@Override
	public String get() {
		return file;
	}

	@Override
	public void accept(String t) {
		if (filter == null || filter.test(t))
			file = t;
	}

}
