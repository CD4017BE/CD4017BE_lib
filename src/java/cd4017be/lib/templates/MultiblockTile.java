package cd4017be.lib.templates;

import cd4017be.lib.ModTileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

public class MultiblockTile<C extends MultiblockComp<C, N>, N extends SharedNetwork<C, N>> extends ModTileEntity implements ITickable {

	protected C comp;

	@Override
	public void update() {
		if (comp.network != null) comp.network.updateTick(comp);
		if (comp instanceof ITickable) ((ITickable)comp).update();
	}

	@Override
	public boolean hasCapability(Capability<?> cap, EnumFacing s) {
		if (comp instanceof ICapabilityProvider) return ((ICapabilityProvider)comp).hasCapability(cap, s);
		if (cap == comp.getCap()) return true;
		return super.hasCapability(cap, s);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getCapability(Capability<T> cap, EnumFacing s) {
		if (comp instanceof ICapabilityProvider) return ((ICapabilityProvider)comp).getCapability(cap, s);
		if (cap == comp.getCap()) return (T)comp;
		return super.getCapability(cap, s);
	}

	@Override
	public void onNeighborTileChange(BlockPos pos) {
		comp.updateCon = true;
	}

	@Override
	public void validate() {
		super.validate();
		comp.setUID(SharedNetwork.ExtPosUID(pos, dimensionId));
	}

	@Override
	public void invalidate() {
		super.invalidate();
		if (comp.network != null) comp.network.remove(comp);
	}

	@Override
	public void onChunkUnload() {
		super.onChunkUnload();
		if (comp.network != null) comp.network.remove(comp);
	}

}
