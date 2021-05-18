package cd4017be.lib.tileentity;

import static cd4017be.lib.network.Sync.GUI;
import static net.minecraftforge.fluids.capability.CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY;
import java.util.ArrayList;
import java.util.function.Supplier;
import cd4017be.lib.capability.IMultiFluidHandler;
import cd4017be.lib.container.ContainerFluidSupply;
import cd4017be.lib.container.IUnnamedContainerProvider;
import cd4017be.lib.network.IPlayerPacketReceiver;
import cd4017be.lib.network.Sync;
import net.minecraft.nbt.INBT;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

/** @author CD4017BE */
public class FluidSupply extends BaseTileEntity
implements IMultiFluidHandler, IUnnamedContainerProvider, IPlayerPacketReceiver {

	public static int MAX_SLOTS = 12;

	final LazyOptional<IFluidHandler> handler = LazyOptional.of(()->this);
	public final ArrayList<Slot> slots = new ArrayList<>();
	@Sync(to=GUI) public int scroll;

	public FluidSupply(TileEntityType<FluidSupply> type) {
		super(type);
	}

	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
		if (cap == FLUID_HANDLER_CAPABILITY) return handler.cast();
		return super.getCapability(cap, side);
	}

	@Override
	public int getTanks() {
		return MAX_SLOTS;
	}

	@Override
	public FluidStack getFluidInTank(int tank) {
		return tank < slots.size() ? slots.get(tank).stack : FluidStack.EMPTY;
	}

	@Override
	public void setFluidInTank(int tank, FluidStack stack) {
		if (tank < slots.size())
			if (stack.isEmpty()) slots.remove(tank);
			else slots.get(tank).stack = stack;
		else if (!stack.isEmpty()) slots.add(new Slot(stack));
	}

	@Override
	public int getTankCapacity(int tank) {
		return Integer.MAX_VALUE;
	}

	@Override
	public boolean isFluidValid(int tank, FluidStack stack) {
		return true;
	}

	@Override
	public boolean shouldFill(int tank) {
		return true;
	}

	@Override
	public boolean shouldDrain(int tank) {
		return true;
	}

	@Override
	public int fill(FluidStack resource, FluidAction action) {
		int n = resource.getAmount();
		for (Slot s : slots) {
			if (!resource.isFluidEqual(s.stack)) continue;
			if (action.execute()) s.countIn += n;
			return n;
		}
		if (slots.size() >= MAX_SLOTS) return 0;
		if (action.execute()) {
			Slot s = new Slot(new FluidStack(resource, 1000));
			s.countIn = n;
			slots.add(s);
		}
		return n;
	}

	@Override
	public FluidStack drain(FluidStack resource, FluidAction action) {
		int n = resource.getAmount();
		for (Slot s : slots) {
			if (!resource.isFluidEqual(s.stack)) continue;
			n = Math.min(n, s.stack.getAmount());
			if (action.execute()) s.countOut += n;
			return new FluidStack(s.stack, n);
		}
		return FluidStack.EMPTY;
	}

	@Override
	public FluidStack drain(int maxDrain, FluidAction action) {
		if (slots.isEmpty()) return FluidStack.EMPTY;
		Slot s = slots.get(0);
		int n = Math.min(maxDrain, s.stack.getAmount());
		if (action.execute()) s.countOut += n;
		return new FluidStack(s.stack, n);
	}

	@Override
	public void storeState(CompoundNBT nbt, int mode) {
		super.storeState(nbt, mode);
		ListNBT list = new ListNBT();
		for(Slot s : slots) {
			CompoundNBT tag = s.stack.writeToNBT(new CompoundNBT());
			tag.putInt("in", s.countIn);
			tag.putInt("out", s.countOut);
			list.add(tag);
		}
		nbt.put("slots", list);
	}

	@Override
	public void loadState(CompoundNBT nbt, int mode) {
		super.loadState(nbt, mode);
		slots.clear();
		for(INBT tb : nbt.getList("slots", NBT.TAG_COMPOUND)) {
			CompoundNBT tag = (CompoundNBT)tb;
			Slot s = new Slot(FluidStack.loadFluidStackFromNBT(tag));
			s.countIn = tag.getInt("in");
			s.countOut = tag.getInt("out");
			slots.add(s);
		}
	}

	@Override
	public ContainerFluidSupply createMenu(int id, PlayerInventory inv, PlayerEntity player) {
		return new ContainerFluidSupply(id, inv, this);
	}

	@Override
	public void handlePlayerPacket(PacketBuffer pkt, ServerPlayerEntity sender)
	throws Exception {
		int cmd = pkt.readByte();
		if (cmd < 0) {
			scroll = pkt.readUnsignedByte();
			return;
		}
		/*if (cmd >= 32) {
			cmd = (cmd & 15) + scroll;
			if (cmd < slots.size()) slots.get(cmd).stack = ItemFluidUtil.readFluidStack(pkt);
			else if (cmd >= MAX_SLOTS) return;
			else slots.add(new Slot(ItemFluidUtil.readFluidStack(pkt)));
			return;
		}*/
		boolean zero = cmd < 16;
		cmd = (cmd & 15) + scroll;
		if (cmd >= slots.size()) return;
		Slot s = slots.get(cmd);
		if (zero) {
			s.countIn = 0;
			s.countOut = 0;
		} else if (s.countIn < s.countOut) {
			s.countOut -= s.countIn;
			s.countIn = 0;
		} else {
			s.countIn -= s.countOut;
			s.countOut = 0;
		}
	}


	public static class Slot implements Supplier<Object[]> {

		public FluidStack stack;
		public int countOut, countIn;

		public Slot(FluidStack stack) {
			this.stack = stack;
		}

		@Override
		public Object[] get() {
			return new Object[] {
				countIn, countOut
			};
		}

	}

}
