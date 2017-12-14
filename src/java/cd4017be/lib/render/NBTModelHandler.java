package cd4017be.lib.render;

import cd4017be.lib.render.model.RawModelData;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.client.model.IModel;

public class NBTModelHandler {

	final NBTTagCompound data;
	RawModelData main;

	public IModel get(String path, String... args) {
		if (path.isEmpty()) return main;
		return new RawModelData(main, data, path);
	}

}
