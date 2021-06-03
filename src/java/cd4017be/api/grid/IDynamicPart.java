package cd4017be.api.grid;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**Implemented by {@link GridPart}s that need dynamic rendering.
 * @author CD4017BE */
public interface IDynamicPart {

	/**Render the part dynamically.
	 * @param ms transformed to lowest XYZ-corner of grid block
	 * @param rtb render buffers
	 * @param light for grid block
	 * @param overlay
	 * @param t partial tick
	 * @param opaque voxels for visibility tests */
	@OnlyIn(Dist.CLIENT)
	void render(MatrixStack ms, IRenderTypeBuffer rtb, int light, int overlay, float t, long opaque);

	/**
	 * @param pkt */
	void readSync(PacketBuffer pkt);

	/**
	 * @param pkt 
	 * @param init */
	void writeSync(PacketBuffer pkt, boolean init);

}
