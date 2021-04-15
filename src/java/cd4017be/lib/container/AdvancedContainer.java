package cd4017be.lib.container;

import static cd4017be.lib.network.GuiNetworkHandler.GNH_INSTANCE;
import static cd4017be.lib.network.GuiNetworkHandler.preparePacket;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import cd4017be.lib.Lib;
import cd4017be.lib.container.slot.*;
import cd4017be.lib.network.*;
import cd4017be.lib.util.ItemFluidUtil;
import cd4017be.lib.util.Utils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.container.*;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.ItemHandlerHelper;

/**A Container that offers improved data synchronization features
 * and support for {@link ISpecialSlot}s and {@link IFluidSlot}s.
 * @author CD4017BE */
public class AdvancedContainer extends Container
implements IServerPacketReceiver, IPlayerPacketReceiver {

	protected final StateSyncAdv sync;
	public final PlayerInventory inv;
	protected final int idxCount;
	private int playerInvS;
	private int playerInvE;
	private BitSet syncSlots = new BitSet();
	boolean hardInvUpdate = false;
	public final ArrayList<IQuickTransferHandler> transferHandlers = new ArrayList<>();

	/**@param type
	 * @param id unique GUI session id
	 * @param inv player's inventory
	 * @param sync server -> client data synchronization handler
	 * @param idxCount object indices available for slot synchronization */
	public AdvancedContainer(
		ContainerType<?> type, int id, PlayerInventory inv,
		StateSyncAdv sync, int idxCount
	) {
		super(type, id);
		this.sync = sync;
		this.inv = inv;
		this.idxCount = idxCount;
	}

	@Override
	public void addListener(IContainerListener listener) {
		if (listener instanceof ServerPlayerEntity)
			sync.set.set(0);
		super.addListener(listener);
	}

	/**Add 36 slots for the player main inventory
	 * @param x left most slot pos
	 * @param y upper most slot pos */
	public void addPlayerInventory(int x, int y) {
		this.addPlayerInventory(x, y, false);
	}

	/**Add slots for the player inventory
	 * @param x left most slot pos
	 * @param y upper most slot pos
	 * @param armor whether also add armor and shield slots */
	public void addPlayerInventory(int x, int y, boolean armor) {
		playerInvS = this.inventorySlots.size();
		playerInvE = playerInvS + (armor ? 41 : 36);
		for (int i = 0; i < 3; i++) 
			for (int j = 0; j < 9; j++)
				this.addSlot(new HidableSlot(inv, i * 9 + j + 9, x + j * 18, y + i * 18));
		for (int i = 0; i < 9; i++)
			this.addSlot(new HidableSlot(inv, i, x + i * 18, y + 58));
		if (armor) {
			for (int i = 0; i < 4; i++)
				this.addSlot(new SlotArmor(inv, i + 36, x - 18, y - i * 18 + 36, EquipmentSlotType.values()[i + 2]));
			this.addSlot(new SlotArmor(inv, 40, x - 18, y + 58, EquipmentSlotType.OFFHAND));
		}
	}

	/**Adds a slot to this container
	 * @param slot with support for {@link ISpecialSlot} and {@link IFluidSlot}
	 * @param sync whether to synchronize items with 32-bit stack-size
	 *  or the fluid content of {@link IFluidSlot}s.
	 *  Requires object indices in {@link #sync} (idxCount argument in constructor). */
	public void addSlot(Slot slot, boolean sync) {
		this.addSlot(slot);
		if (sync) {
			int n = syncSlots.cardinality();
			if (n < idxCount) {
				syncSlots.set(slot.slotNumber);
				this.sync.set(n, slot instanceof IFluidSlot ? FluidStack.EMPTY : ItemStack.EMPTY);
			} else Lib.LOG.error(
				"Can't add another synced ItemSlot in {}, only {} channels reserved!",
				getClass(), idxCount
			);
		}
	}

	public void hardInvUpdate() {
		this.hardInvUpdate = true;
	}

	public boolean hasPlayerInv() {
		return playerInvS != playerInvE;
	}

	public int playerInvStart() {
		return playerInvS;
	}

	@Override
	public boolean mergeItemStack(ItemStack item, int ss, int se, boolean d) {
		ItemStack item1 = item.copy();
		if (item1.isStackable())
			for (int i = se - ss; i > 0 && item1.getCount() > 0; i--) {
				Slot slot = inventorySlots.get(d ? i + ss - 1 : se - i);
				ItemStack stack = slot.getStack();
				if (stack.isEmpty()) continue;
				if (slot instanceof ISpecialSlot) {
					ISpecialSlot s = (ISpecialSlot)slot;
					if ((item1 = s.insertItem(item1, false)).isEmpty()) {
						item.setCount(0);
						return true;
					} else if (s.insertHereOnly(item1)) {
						item.setCount(item1.getCount());
						return true;
					}
				} else if (ItemHandlerHelper.canItemStacksStack(stack, item1)) {
					int j = stack.getCount() + item1.getCount();
					int mxs = Math.min(item1.getMaxStackSize(), slot.getSlotStackLimit());
					if (j <= mxs) {
						item.setCount(0);
						stack.setCount(j);
						slot.onSlotChanged();
						return true;
					} else if (stack.getCount() < mxs) {
						item1.shrink(mxs - stack.getCount());
						stack.setCount(mxs);
						slot.onSlotChanged();
					}
				}
			}
		if (item1.getCount() > 0)
			for (int i = se - ss; i > 0; i--) {
				Slot slot = inventorySlots.get(d ? i + ss - 1 : se - i);
				if (slot.getStack().getCount() != 0) continue;
				if (slot instanceof ISpecialSlot) {
					ISpecialSlot s = (ISpecialSlot)slot;
					if ((item1 = s.insertItem(item1, false)).getCount() == 0) {
						item.setCount(0);
						return true;
					} else if (s.insertHereOnly(item1)) {
						item.setCount(item1.getCount());
						return true;
					}
				} else if (slot.isItemValid(item1)) {
					int mxs = slot.getItemStackLimit(item1);
					if (item1.getCount() <= mxs) {
						slot.putStack(item1.copy());
						slot.onSlotChanged();
						item.setCount(0);
						return true;
					} else {
						slot.putStack(item1.split(mxs));
						slot.onSlotChanged();
					}
				}
			}
		if (item1.getCount() != item.getCount()) {
			item.setCount(item1.getCount());
			return true;
		} else return false;
	}

	@Override
	public ItemStack transferStackInSlot(PlayerEntity player, int id) {
		Slot slot = inventorySlots.get(id);
		if (slot == null || !slot.getHasStack()) return ItemStack.EMPTY;
		ItemStack stack = slot.getStack();
		ItemStack item = stack.copy();
		if (id >= playerInvS && id < playerInvE) {
			for (IQuickTransferHandler h : transferHandlers)
				if (h.transfer(stack, this)) break;
		} else mergeItemStack(stack, playerInvS, playerInvE, false);
		if (stack.getCount() == item.getCount()) return ItemStack.EMPTY;
		slot.onSlotChange(stack, item);
		slot.onSlotChanged();
		slot.onTake(player, stack);
		return item;
	}

	@Override
	public ItemStack slotClick(int s, int b, ClickType m, PlayerEntity player) {
		Slot slot = s >= 0 && s < inventorySlots.size() ? inventorySlots.get(s) : null;
		if (slot instanceof ISpecialSlot) {
			ISpecialSlot ss = (ISpecialSlot)slot;
			return ss.onClick(b, m, player, this);
		} else return super.slotClick(s, b, m, player);
	}

	@Override
	public void detectAndSendChanges() {
		if (hardInvUpdate && inv.player instanceof ServerPlayerEntity) {
			((ServerPlayerEntity)inv.player).isChangingQuantityOnly = false;
			hardInvUpdate = false;
		}
		super.detectAndSendChanges();
		detectChanges(sync.clear());
		sync.detectChanges();
		if(!sync.isEmpty()) {
			PacketBuffer pkt = preparePacket(this);
			sync.write(pkt);
			writeChanges(sync.set, pkt);
			sync.writeChanges(pkt);
			GNH_INSTANCE.sendToPlayer(pkt, (ServerPlayerEntity)inv.player);
		}
	}

	protected void detectChanges(BitSet chng) {
		BitSet slots = syncSlots;
		for (int i = slots.nextSetBit(0), j = 0; i >= 0; i = slots.nextSetBit(i + 1), j++) {
			Slot slot = inventorySlots.get(i);
			if (slot instanceof IFluidSlot) {
				FluidStack fluidN = ((IFluidSlot)slot).getFluid();
				FluidStack fluidO = sync.get(j);
				if (!fluidO.isFluidStackIdentical(fluidN))
					sync.set(j, fluidN.copy());
			} else {
				ItemStack itemN = slot.getStack();
				ItemStack itemO = sync.get(j);
				if (!itemO.equals(itemN, true))
					sync.set(j, itemN.copy());
			}
		}
	}

	protected void writeChanges(BitSet chng, PacketBuffer pkt) {
		int i0 = sync.objIdxOfs();
		for (int i = chng.nextSetBit(i0); i > 0 && i - i0 < idxCount; i = chng.nextSetBit(i + 1)) {
			Object o = sync.get(i - i0);
			if (o instanceof ItemStack)
				ItemFluidUtil.writeItemHighRes(pkt, (ItemStack)o);
			else if (o instanceof FluidStack)
				ItemFluidUtil.writeFluidStack(pkt, (FluidStack)o);
		}
	}

	protected void readChanges(BitSet chng, PacketBuffer pkt) throws Exception {
		BitSet slots = syncSlots;
		for (int i = slots.nextSetBit(0), j = sync.objIdxOfs(); i >= 0; i = slots.nextSetBit(i + 1), j++)
			if (chng.get(j)) {
				Slot slot = inventorySlots.get(i);
				if (slot instanceof IFluidSlot)
					((IFluidSlot)slot).putFluid(ItemFluidUtil.readFluidStack(pkt));
				else slot.putStack(ItemFluidUtil.readItemHighRes(pkt));
			}
	}

	@Override
	public void putStackInSlot(int slotID, ItemStack stack) {
		if (syncSlots.get(slotID)) return;
		getSlot(slotID).putStack(stack);
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void setAll(List<ItemStack> items) {
		int m = Math.min(items.size(), inventorySlots.size());
		for (int i = 0; i < m; ++i)
			putStackInSlot(i, items.get(i));
	}

	@Override
	public void handleServerPacket(PacketBuffer pkt) throws Exception {
		readChanges(sync.read(pkt), pkt);
		sync.readChanges(pkt);
	}

	@Override
	public void handlePlayerPacket(PacketBuffer pkt, ServerPlayerEntity sender)
	throws Exception {
		for(Object e : sync.holders)
			if(e instanceof IPlayerPacketReceiver) {
				((IPlayerPacketReceiver)e).handlePlayerPacket(pkt, sender);
				return;
			}
	}

	@Override
	public boolean canInteractWith(PlayerEntity playerIn) {
		for(Object e : sync.holders)
			if(e instanceof TileEntity) {
				TileEntity te = (TileEntity)e;
				if(te.isRemoved() || te.getWorld() != playerIn.world) return false;
				if(!te.getPos().withinDistance(playerIn.getPositionVec(), 8)) return false;
			}
		return true;
	}

	public BlockPos getPos() {
		for(Object e : sync.holders)
			if(e instanceof TileEntity)
				return ((TileEntity)e).getPos();
		return Utils.NOWHERE;
	}

}
