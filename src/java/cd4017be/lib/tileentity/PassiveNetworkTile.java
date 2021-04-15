package cd4017be.lib.tileentity;

import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import cd4017be.lib.TickRegistry.IUpdatable;
import cd4017be.lib.block.BlockTE.ITENeighborChange;
import cd4017be.lib.templates.NetworkNode;
import cd4017be.lib.templates.SharedNetwork;

/**
 * Implementation for a TileEntity that takes part in a SharedNetwork structure by providing a NetworkNode.<br>
 * All required registration and unregistration of nodes and connection with neighbouring blocks is already performed by this class using only block events and single tick delayed updates so being {@link net.minecraft.tileentity.ITickableTileEntity} is not required.
 * @param C the type of {@link NetworkNode} this operates with
 * @param N the type of {@link SharedNetwork} this operates with
 * @param T should be the class extending this, so that '(T)this' won't throw a ClassCastException.
 * @author CD4017BE
 * @deprecated not fully implemented
 */
public class PassiveNetworkTile<C extends NetworkNode<C, N, T>, N extends SharedNetwork<C, N>, T extends PassiveNetworkTile<C, N, T>> extends BaseTileEntity implements ITENeighborChange, IUpdatable {

	public PassiveNetworkTile(TileEntityType<?> type) {
		super(type);
		// TODO Auto-generated constructor stub
	}

	protected C comp;

	@Override
	public void process() {
		if (!unloaded && comp.updateCon) comp.updateCons();
	}

	@Override
	public <X> LazyOptional<X> getCapability(Capability<X> cap, Direction side) {
		if (comp instanceof ICapabilityProvider) return ((ICapabilityProvider)comp).getCapability(cap, side);
		if (cap == comp.getCap())
			return LazyOptional.of(() -> comp).cast();
		return super.getCapability(cap, side);
	}

	@Override
	public void onNeighborTEChange(BlockPos from) {
		comp.markDirty();
	}

	@Override
	public void onLoad() {
		super.onLoad();
		comp.setUID(SharedNetwork.ExtPosUID(pos, world.getDimensionKey().hashCode()));
	}

	@Override
	protected void onUnload() {
		super.onUnload();
		if (comp.network != null) comp.network.remove(comp);
		comp.updateCon = false;
	}

}
