package cd4017be.lib.Gui.inWorld;

import java.util.List;

import javax.annotation.Nullable;

import cd4017be.lib.block.AdvancedBlock.IInteractiveTile;
import cd4017be.lib.util.Obj2;
import cd4017be.lib.util.Orientation;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.RayTraceResult;

public interface IInWorldUITile extends IInteractiveTile {

	public List<UIcomp> UIComponents();
	public Orientation getOrientation();
	public @Nullable Obj2<UIcomp, RayTraceResult> getSelectedComp(Entity entity, float t);

}
