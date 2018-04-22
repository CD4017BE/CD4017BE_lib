package cd4017be.lib.tileentity;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import cd4017be.lib.TickRegistry.IUpdatable;
import cd4017be.lib.block.AdvancedBlock.INeighborAwareTile;
import cd4017be.lib.templates.MultiblockComp;
import cd4017be.lib.templates.SharedNetwork;

/**
 * 
 * @author CD4017BE
 */
public class PassiveMultiblockTile<C extends MultiblockComp<C, N>, N extends SharedNetwork<C, N>> extends BaseTileEntity implements INeighborAwareTile, IUpdatable {

	protected C comp;

	@Override
	public void process() {
		if (!unloaded && comp.updateCon) comp.updateCons();
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
	public void neighborBlockChange(Block b, BlockPos src) {
		comp.markDirty();
	}

	@Override
	public void neighborTileChange(TileEntity te, EnumFacing side) {
		comp.markDirty();
	}

	@Override
	protected void setupData() {
		comp.setUID(SharedNetwork.ExtPosUID(pos, world.provider.getDimension()));
	}

	@Override
	protected void clearData() {
		if (comp.network != null) comp.network.remove(comp);
		comp.updateCon = false;
	}

}
