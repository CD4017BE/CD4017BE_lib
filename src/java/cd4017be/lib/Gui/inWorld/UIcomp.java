package cd4017be.lib.Gui.inWorld;

import cd4017be.lib.render.InWorldUIRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 
 * @author CD4017BE
 */
public class UIcomp {

	public final AxisAlignedBB bounds;

	public UIcomp(AxisAlignedBB bounds) {
		this.bounds = bounds;
	}

	public boolean interact(RayTraceResult hit, EntityPlayer player, ClickType type) {return false;}

	@SideOnly(Side.CLIENT)
	public void draw(InWorldUIRenderer tesr) {}

	@SideOnly(Side.CLIENT)
	public void drawOverlay(InWorldUIRenderer tesr, RayTraceResult hit, EntityPlayer player) {}

}
