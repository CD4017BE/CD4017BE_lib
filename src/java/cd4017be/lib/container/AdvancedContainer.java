package cd4017be.lib.container;

import static cd4017be.lib.network.GuiNetworkHandler.GNH_INSTANCE;
import static cd4017be.lib.network.GuiNetworkHandler.preparePacket;

import java.util.ArrayList;
import java.util.BitSet;
import cd4017be.lib.Lib;
import cd4017be.lib.container.slot.*;
import cd4017be.lib.network.*;
import cd4017be.lib.util.ItemFluidUtil;
import cd4017be.lib.util.Utils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.ItemHandlerHelper;

/**A AbstractContainerMenu that offers improved data synchronization features
 * and support for {@link ISpecialSlot}s and {@link IFluidSlot}s.
 * @author CD4017BE */
public class AdvancedContainer extends AbstractContainerMenu
implements IServerPacketReceiver, IPlayerPacketReceiver {

	protected final StateSyncAdv sync;
	public final Inventory inv;
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
		MenuType<?> type, int id, Inventory inv,
		StateSyncAdv sync, int idxCount
	) {
		super(type, id);
		this.sync = sync;
		this.inv = inv;
		this.idxCount = idxCount;
	}

	@Override
	public void addSlotListener(ContainerListener listener) {
		if (listener instanceof ServerPlayer)
			sync.set.set(0);
		super.addSlotListener(listener);
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
		playerInvS = this.slots.size();
		playerInvE = playerInvS + (armor ? 41 : 36);
		for (int i = 0; i < 3; i++)
			for (int j = 0; j < 9; j++)
				this.addSlot(new HidableSlot(inv, i * 9 + j + 9, x + j * 18, y + i * 18));
		for (int i = 0; i < 9; i++)
			this.addSlot(new HidableSlot(inv, i, x + i * 18, y + 58));
		if (armor) {
			for (int i = 0; i < 4; i++)
				this.addSlot(new SlotArmor(inv, i + 36, x - 18, y - i * 18 + 36, EquipmentSlot.values()[i + 2]));
			this.addSlot(new SlotArmor(inv, 40, x - 18, y + 58, EquipmentSlot.OFFHAND));
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
				syncSlots.set(slot.index);
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
	public boolean moveItemStackTo(ItemStack item, int ss, int se, boolean d) {
		ItemStack item1 = item.copy();
		if (item1.isStackable())
			for (int i = se - ss; i > 0 && item1.getCount() > 0; i--) {
				Slot slot = slots.get(d ? i + ss - 1 : se - i);
				ItemStack stack = slot.getItem();
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
					int mxs = Math.min(item1.getMaxStackSize(), slot.getMaxStackSize());
					if (j <= mxs) {
						item.setCount(0);
						stack.setCount(j);
						slot.setChanged();
						return true;
					} else if (stack.getCount() < mxs) {
						item1.shrink(mxs - stack.getCount());
						stack.setCount(mxs);
						slot.setChanged();
					}
				}
			}
		if (item1.getCount() > 0)
			for (int i = se - ss; i > 0; i--) {
				Slot slot = slots.get(d ? i + ss - 1 : se - i);
				if (slot.getItem().getCount() != 0) continue;
				if (slot instanceof ISpecialSlot) {
					ISpecialSlot s = (ISpecialSlot)slot;
					if ((item1 = s.insertItem(item1, false)).getCount() == 0) {
						item.setCount(0);
						return true;
					} else if (s.insertHereOnly(item1)) {
						item.setCount(item1.getCount());
						return true;
					}
				} else if (slot.mayPlace(item1)) {
					int mxs = slot.getMaxStackSize(item1);
					if (item1.getCount() <= mxs) {
						slot.set(item1.copy());
						slot.setChanged();
						item.setCount(0);
						return true;
					} else {
						slot.set(item1.split(mxs));
						slot.setChanged();
					}
				}
			}
		if (item1.getCount() != item.getCount()) {
			item.setCount(item1.getCount());
			return true;
		} else return false;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int id) {
		Slot slot = slots.get(id);
		if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
		ItemStack stack = slot.getItem();
		ItemStack item = stack.copy();
		if (id >= playerInvS && id < playerInvE) {
			for (IQuickTransferHandler h : transferHandlers)
				if (h.transfer(stack, this)) break;
		} else moveItemStackTo(stack, playerInvS, playerInvE, false);
		if (stack.getCount() == item.getCount()) return ItemStack.EMPTY;
		slot.onQuickCraft(stack, item);
		slot.setChanged();
		slot.onTake(player, stack);
		return item;
	}

	@Override
	public void clicked(int s, int b, ClickType m, Player player) {
		Slot slot = s >= 0 && s < slots.size() ? slots.get(s) : null;
		if (slot instanceof ISpecialSlot) {
			ISpecialSlot ss = (ISpecialSlot)slot;
			ss.onClick(b, m, player, this);
		} else super.clicked(s, b, m, player);
	}

	@Override
	public void broadcastChanges() {
		if (hardInvUpdate && inv.player instanceof ServerPlayer) {
			//((ServerPlayer)inv.player).ignoreSlotUpdateHack = false;
			hardInvUpdate = false;
		}
		super.broadcastChanges();
		if (inv.player.level.isClientSide) return;
		detectChanges(sync.clear());
		sync.detectChanges();
		if(!sync.isEmpty()) {
			FriendlyByteBuf pkt = preparePacket(this);
			sync.write(pkt);
			writeChanges(sync.set, pkt);
			sync.writeChanges(pkt);
			GNH_INSTANCE.sendToPlayer(pkt, (ServerPlayer)inv.player);
		}
	}

	protected void detectChanges(BitSet chng) {
		BitSet slots = syncSlots;
		for (int i = slots.nextSetBit(0), j = 0; i >= 0; i = slots.nextSetBit(i + 1), j++) {
			Slot slot = this.slots.get(i);
			if (slot instanceof IFluidSlot) {
				FluidStack fluidN = ((IFluidSlot)slot).getFluid();
				FluidStack fluidO = sync.get(j);
				if (!fluidO.isFluidStackIdentical(fluidN))
					sync.set(j, fluidN.copy());
			} else {
				ItemStack itemN = slot.getItem();
				ItemStack itemO = sync.get(j);
				if (!itemO.equals(itemN, true))
					sync.set(j, itemN.copy());
			}
		}
	}

	protected void writeChanges(BitSet chng, FriendlyByteBuf pkt) {
		int i0 = sync.objIdxOfs();
		for (int i = chng.nextSetBit(i0); i > 0 && i - i0 < idxCount; i = chng.nextSetBit(i + 1)) {
			Object o = sync.get(i - i0);
			if (o instanceof ItemStack)
				ItemFluidUtil.writeItemHighRes(pkt, (ItemStack)o);
			else if (o instanceof FluidStack)
				ItemFluidUtil.writeFluidStack(pkt, (FluidStack)o);
		}
	}

	protected void readChanges(BitSet chng, FriendlyByteBuf pkt) throws Exception {
		BitSet slots = syncSlots;
		for (int i = slots.nextSetBit(0), j = sync.objIdxOfs(); i >= 0; i = slots.nextSetBit(i + 1), j++)
			if (chng.get(j)) {
				Slot slot = this.slots.get(i);
				if (slot instanceof IFluidSlot)
					((IFluidSlot)slot).putFluid(ItemFluidUtil.readFluidStack(pkt));
				else slot.set(ItemFluidUtil.readItemHighRes(pkt));
			}
	}

	@Override
	public void setRemoteSlotNoCopy(int slotID, ItemStack stack) {
		if (syncSlots.get(slotID)) return;
		super.setRemoteSlot(slotID, stack);
	}

	@Override
	public void handleServerPacket(FriendlyByteBuf pkt) throws Exception {
		readChanges(sync.read(pkt), pkt);
		sync.readChanges(pkt);
	}

	@Override
	public void handlePlayerPacket(FriendlyByteBuf pkt, ServerPlayer sender)
	throws Exception {
		for(Object e : sync.holders)
			if(e instanceof IPlayerPacketReceiver) {
				((IPlayerPacketReceiver)e).handlePlayerPacket(pkt, sender);
				return;
			}
	}

	@Override
	public boolean stillValid(Player playerIn) {
		for(Object e : sync.holders)
			if(e instanceof BlockEntity) {
				BlockEntity te = (BlockEntity)e;
				if(te.isRemoved() || te.getLevel() != playerIn.level) return false;
				if(!te.getBlockPos().closerThan(playerIn.position(), 8)) return false;
			}
		return true;
	}

	public BlockPos getPos() {
		for(Object e : sync.holders)
			if(e instanceof BlockEntity)
				return ((BlockEntity)e).getBlockPos();
		return Utils.NOWHERE;
	}

}
