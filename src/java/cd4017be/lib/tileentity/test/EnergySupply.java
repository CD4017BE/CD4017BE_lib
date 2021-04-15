package cd4017be.lib.tileentity.test;

import cd4017be.lib.block.BlockTE.ITENeighborChange;
import cd4017be.lib.capability.CachedCap;
import cd4017be.lib.container.IUnnamedContainerProvider;
import cd4017be.lib.container.test.ContainerEnergySupply;
import cd4017be.lib.network.*;
import cd4017be.lib.tileentity.BaseTileEntity;
import cd4017be.lib.tileentity.BaseTileEntity.ITickableServerOnly;
import net.minecraft.entity.player.*;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import static net.minecraftforge.energy.CapabilityEnergy.ENERGY;
import static cd4017be.lib.capability.NullEnergyStorage.INSTANCE;
import static cd4017be.lib.network.Sync.*;

/** @author CD4017BE */
public class EnergySupply extends BaseTileEntity
implements IEnergyStorage, ITickableServerOnly, ITENeighborChange, IUnnamedContainerProvider, IPlayerPacketReceiver {

	final LazyOptional<IEnergyStorage> handler = LazyOptional.of(()->this);
	@SuppressWarnings("unchecked")
	final CachedCap<IEnergyStorage>[] receivers = new CachedCap[6];

	public int flowI, flowO;
	@Sync(to=GUI) public int lastI, lastO;
	@Sync(to=SAVE|GUI) public int limI, limO;
	@Sync(to=SAVE|GUI) public long sumI, sumO;
	@Sync(to=SAVE) public long t0;
	@Sync(to=GUI) public long t() {return world.getGameTime() - t0 - 1;}
	boolean updateCaps;

	public EnergySupply(TileEntityType<EnergySupply> type) {
		super(type);
	}

	@Override
	public void tick() {
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
	public void onNeighborTEChange(BlockPos from) {
		updateCaps = true;
	}

	@Override
	public void onLoad() {
		super.onLoad();
		for (Direction d : Direction.values())
			receivers[d.ordinal()] = new CachedCap<>(
				world, pos.offset(d, -1), d, ENERGY, INSTANCE
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
	public ContainerEnergySupply createMenu(int windowId, PlayerInventory playerInv, PlayerEntity player) {
		return new ContainerEnergySupply(windowId, playerInv, this);
	}

	@Override
	public void handlePlayerPacket(PacketBuffer pkt, ServerPlayerEntity sender) throws Exception {
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
			t0 = world.getGameTime();
			break;
		default:
			return;
		}
		saveDirty();
	}

}
