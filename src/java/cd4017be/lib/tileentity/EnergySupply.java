package cd4017be.lib.tileentity;

import net.minecraft.world.level.block.state.BlockState;
import cd4017be.lib.block.BlockTE.ITEBlockUpdate;
import cd4017be.lib.capability.CachedCap;
import cd4017be.lib.container.ContainerEnergySupply;
import cd4017be.lib.container.IUnnamedContainerProvider;
import cd4017be.lib.network.*;
import cd4017be.lib.tileentity.BaseTileEntity.TickableServer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import static net.minecraftforge.energy.CapabilityEnergy.ENERGY;
import static cd4017be.lib.capability.NullEnergyStorage.INSTANCE;
import static cd4017be.lib.network.Sync.*;

/** @author CD4017BE */
public class EnergySupply extends BaseTileEntity
implements IEnergyStorage, TickableServer, ITEBlockUpdate, IUnnamedContainerProvider, IPlayerPacketReceiver {

	final LazyOptional<IEnergyStorage> handler = LazyOptional.of(()->this);
	@SuppressWarnings("unchecked")
	final CachedCap<IEnergyStorage>[] receivers = new CachedCap[6];

	public int flowI, flowO;
	@Sync(to=GUI) public int lastI, lastO;
	@Sync(to=SAVE|GUI) public int limI, limO;
	@Sync(to=SAVE|GUI) public long sumI, sumO;
	@Sync(to=SAVE) public long t0;
	@Sync(to=GUI) public long t() {return level.getGameTime() - t0 - 1;}
	boolean updateCaps;

	public EnergySupply(BlockEntityType<EnergySupply> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void tickServer(Level world, BlockPos pos, BlockState state) {
		lastI = flowI;
		lastO = flowO;
		flowI = 0;
		flowO = 0;
		if(limO <= 0) return;
		if (updateCaps) {
			for (CachedCap<IEnergyStorage> cc : receivers)
				cc.update();
			updateCaps = false;
		}
		for(CachedCap<IEnergyStorage> cc : receivers)
			this.extractEnergy(cc.get().receiveEnergy(limO, false), false);
	}

	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
		if (cap == ENERGY) return handler.cast();
		return super.getCapability(cap, side);
	}

	@Override
	protected void invalidateCaps() {
		super.invalidateCaps();
		handler.invalidate();
	}

	@Override
	public void onNeighborBlockChange(BlockPos from, Block block, boolean moving) {
		updateCaps = true;
	}

	@Override
	public void onLoad() {
		super.onLoad();
		for (Direction d : Direction.values())
			receivers[d.ordinal()] = new CachedCap<>(
				level, worldPosition.relative(d, -1), d, ENERGY, INSTANCE
			);
		updateCaps = true;
	}

	@Override
	public int receiveEnergy(int maxReceive, boolean simulate) {
		if(maxReceive > limI) maxReceive = limI;
		if(!simulate) {
			flowI += maxReceive;
			sumI += maxReceive;
		}
		return maxReceive;
	}

	@Override
	public int extractEnergy(int maxExtract, boolean simulate) {
		if(maxExtract > limO) maxExtract = limO;
		if(!simulate) {
			flowO += maxExtract;
			sumO += maxExtract;
		}
		return maxExtract;
	}

	@Override
	public int getEnergyStored() {
		return limO;
	}

	@Override
	public int getMaxEnergyStored() {
		int n = limI + limO;
		return n < 0 ? Integer.MAX_VALUE : n;
	}

	@Override
	public boolean canExtract() {
		return limO > 0;
	}

	@Override
	public boolean canReceive() {
		return limI > 0;
	}

	@Override
	public ContainerEnergySupply createMenu(int windowId, Inventory playerInv, Player player) {
		return new ContainerEnergySupply(windowId, playerInv, this);
	}

	@Override
	public void handlePlayerPacket(FriendlyByteBuf pkt, ServerPlayer sender) throws Exception {
		switch(pkt.readByte()) {
		case 0:
			if((limI = pkt.readInt()) < 0) limI = 0;
			break;
		case 1:
			if((limO = pkt.readInt()) < 0) limO = 0;
			break;
		case 2:
			sumI = 0;
			sumO = 0;
			t0 = level.getGameTime();
			break;
		default:
			return;
		}
		saveDirty();
	}

}
