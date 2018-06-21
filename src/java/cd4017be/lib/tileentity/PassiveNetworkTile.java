package cd4017be.lib.tileentity;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import cd4017be.lib.TickRegistry.IUpdatable;
import cd4017be.lib.block.AdvancedBlock.INeighborAwareTile;
import cd4017be.lib.templates.NetworkNode;
import cd4017be.lib.templates.SharedNetwork;

/**
 * Implementation for a TileEntity that takes part in a SharedNetwork structure by providing a NetworkNode.<br>
 * All required registration and unregistration of nodes and connection with neighbouring blocks is already performed by this class using only block events and single tick delayed updates so being {@link net.minecraft.util.ITickable} is not required.
 * @param C the type of {@link NetworkNode} this operates with
 * @param N the type of {@link SharedNetwork} this operates with
 * @param T should be the class extending this, so that '(T)this' won't throw a ClassCastException.
 * @author CD4017BE
 */
public class PassiveNetworkTile<C extends NetworkNode<C, N, T>, N extends SharedNetwork<C, N>, T extends PassiveNetworkTile<C, N, T>> extends BaseTileEntity implements INeighborAwareTile, IUpdatable {

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
	public <X> X getCapability(Capability<X> cap, EnumFacing s) {
		if (comp instanceof ICapabilityProvider) return ((ICapabilityProvider)comp).getCapability(cap, s);
		if (cap == comp.getCap()) return (X)comp;
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
