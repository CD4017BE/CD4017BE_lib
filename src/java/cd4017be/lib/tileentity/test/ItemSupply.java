package cd4017be.lib.tileentity.test;

import java.util.ArrayList;
import java.util.function.Supplier;
import cd4017be.lib.Lib;
import cd4017be.lib.container.IUnnamedContainerProvider;
import cd4017be.lib.container.test.ContainerItemSupply;
import cd4017be.lib.network.IPlayerPacketReceiver;
import cd4017be.lib.network.Sync;
import cd4017be.lib.tileentity.BaseTileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import static cd4017be.lib.network.Sync.GUI;
import static net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY;

/** @author CD4017BE */
public class ItemSupply extends BaseTileEntity
implements IItemHandler, IUnnamedContainerProvider, IPlayerPacketReceiver {

	public static int MAX_SLOTS = 64;

	final LazyOptional<IItemHandler> handler = LazyOptional.of(()->this);
	public final ArrayList<Slot> slots = new ArrayList<>();
	@Sync(to=GUI) public int scroll;

	public ItemSupply() {
		super(Lib.T_ITEM_SUPP);
	}

	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
		if (cap == ITEM_HANDLER_CAPABILITY) return handler.cast();
		return super.getCapability(cap, side);
	}

	@Override
	public boolean isItemValid(int slot, ItemStack stack) {
		return true;
	}

	@Override
	public int getSlots() {
		return Math.min(slots.size() + 1, MAX_SLOTS);
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		return slot < slots.size() ? slots.get(slot).stack : ItemStack.EMPTY;
	}

	@Override
	public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
		if(slot < slots.size()) {
			Slot s = slots.get(slot);
			if(
				!ItemHandlerHelper.canItemStacksStack(stack, s.stack)
			) return stack;
			if(!simulate) s.countIn += stack.getCount();
		} else if(!simulate) {
			Slot s = new Slot(ItemHandlerHelper.copyStackWithSize(stack, 1));
			s.countIn = stack.getCount();
			slots.add(s);
		}
		return ItemStack.EMPTY;
	}

	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		if(slot >= slots.size()) return ItemStack.EMPTY;
		Slot s = slots.get(slot);
		if(!simulate) s.countOut += amount;
		return ItemHandlerHelper.copyStackWithSize(s.stack, amount);
	}

	@Override
	public int getSlotLimit(int slot) {
		return Integer.MAX_VALUE;
	}

	@Override
	public void storeState(CompoundNBT nbt, int mode) {
		super.storeState(nbt, mode);
		ListNBT list = new ListNBT();
		for(Slot s : slots) {
			CompoundNBT tag = s.stack.write(new CompoundNBT());
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
			Slot s = new Slot(ItemStack.read(tag));
			s.countIn = tag.getInt("in");
			s.countOut = tag.getInt("out");
			slots.add(s);
		}
	}

	@Override
	public ContainerItemSupply createMenu(int id, PlayerInventory inv, PlayerEntity player) {
		return new ContainerItemSupply(id, inv, this);
	}

	@Override
	public void handlePlayerPacket(PacketBuffer pkt, ServerPlayerEntity sender)
	throws Exception {
		int cmd = pkt.readByte();
		if (cmd < 0) {
			scroll = pkt.readUnsignedByte();
			return;
		}
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

	public ItemStack getSlot(int slot) {
		return slot + scroll < slots.size() ? slots.get(slot + scroll).stack
			: ItemStack.EMPTY;
	}

	public void setSlot(ItemStack stack, int slot) {
		slot += scroll;
		if(slot < slots.size()) {
			if(stack.isEmpty()) {
				slots.remove(slot);
				return;
			}
			Slot s = slots.get(slot);
			if(ItemHandlerHelper.canItemStacksStack(stack, s.stack))
				s.stack.setCount(stack.getCount());
			else slots.set(slot, new Slot(stack));
		} else if(!stack.isEmpty())
			slots.add(new Slot(stack));
	}

	public static class Slot implements Supplier<Object[]> {

		public final ItemStack stack;
		public int countOut, countIn;

		public Slot(ItemStack stack) {
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
