package cd4017be.lib.render;

import net.minecraft.client.Minecraft;

import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;


/**
 * 
 * @author CD4017BE
 */
@OnlyIn(Dist.CLIENT)
public class HybridFastTESR {

	/**
	 * @param te the TileEntity
	 * @return whether the player currently aims at the given TileEntity
	 */
	public static boolean isAimedAt(TileEntity te) {
		@SuppressWarnings("resource")
		RayTraceResult rts = Minecraft.getInstance().hitResult;
		return rts instanceof BlockRayTraceResult
		&& te.getBlockPos().equals(((BlockRayTraceResult)rts).getBlockPos());
	}

	/**
	 * @param te the TileEntity
	 * @param range maximum distance in blocks
	 * @return whether given TileEntity is within given distance to the camera
	 */
	public static boolean isWithinRange(TileEntity te, double range) {
		EntityRendererManager rm = Minecraft.getInstance().getEntityRenderDispatcher();
		BlockPos pos = te.getBlockPos();
		return rm.distanceToSqr(
			pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5
		) < range * range;
	}

}
