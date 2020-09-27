package cd4017be.lib.tileentity.test;

import java.util.ArrayList;
import java.util.function.Supplier;
import cd4017be.lib.Lib;
import cd4017be.lib.Gui.AdvancedContainer;
import cd4017be.lib.Gui.SlotHolo;
import cd4017be.lib.Gui.AdvancedContainer.IStateInteractionHandler;
import cd4017be.lib.Gui.comp.Button;
import cd4017be.lib.Gui.comp.FormatText;
import cd4017be.lib.Gui.comp.GuiFrame;
import cd4017be.lib.Gui.comp.Slider;
import cd4017be.lib.Gui.ModularGui;
import cd4017be.lib.capability.BasicInventory;
import cd4017be.lib.capability.LinkedInventory;
import cd4017be.lib.network.IGuiHandlerTile;
import cd4017be.lib.network.StateSyncClient;
import cd4017be.lib.network.StateSyncServer;
import cd4017be.lib.network.StateSynchronizer;
import cd4017be.lib.tileentity.BaseTileEntity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import static net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY;

/** @author CD4017BE */
public class ItemSupply extends BaseTileEntity
implements IItemHandler, IGuiHandlerTile, IStateInteractionHandler {

	public static int MAX_SLOTS = 64;

	ArrayList<Slot> slots = new ArrayList<>();
	int scroll;

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
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		return capability == ITEM_HANDLER_CAPABILITY;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		return capability == ITEM_HANDLER_CAPABILITY ? (T)this : null;
	}

	@Override
	protected void storeState(NBTTagCompound nbt, int mode) {
		super.storeState(nbt, mode);
		NBTTagList list = new NBTTagList();
		for(Slot s : slots) {
			NBTTagCompound tag = s.stack.writeToNBT(new NBTTagCompound());
			tag.setInteger("in", s.countIn);
			tag.setInteger("out", s.countOut);
			list.appendTag(tag);
		}
		nbt.setTag("slots", list);
	}

	@Override
	protected void loadState(NBTTagCompound nbt, int mode) {
		super.loadState(nbt, mode);
		slots.clear();
		for(NBTBase tb : nbt.getTagList("slots", NBT.TAG_COMPOUND)) {
			NBTTagCompound tag = (NBTTagCompound)tb;
			Slot s = new Slot(new ItemStack(tag));
			s.countIn = tag.getInteger("in");
			s.countOut = tag.getInteger("out");
			slots.add(s);
		}
	}

	private ItemStack getSlot(int slot) {
		return slot + scroll < slots.size() ? slots.get(slot + scroll).stack
			: ItemStack.EMPTY;
	}

	private void setSlot(ItemStack stack, int slot) {
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

	@Override
	public AdvancedContainer getContainer(EntityPlayer player, int id) {
		AdvancedContainer cont = new AdvancedContainer(
			this, ssb.build(world.isRemote), player
		);
		IItemHandler inv = world.isRemote ? new BasicInventory(12)
			: new LinkedInventory(12, 127, this::getSlot, this::setSlot);
		for(int j = 0; j < 4; j++)
			for(int i = 0; i < 3; i++)
				cont.addItemSlot(
					new SlotHolo(
						inv, i + j * 3, 8 + i * 54, 16 + j * 18, false, true
					), true
				);
		cont.addPlayerInventory(8, 104);
		if(world.isRemote)
			while(slots.size() < 12)
			slots.add(new Slot(ItemStack.EMPTY));
		return cont;
	}

	private static final StateSynchronizer.Builder ssb = StateSynchronizer
	.builder().addFix(1).addMulFix(4, 24).addVar(12);

	@Override
	public void writeState(StateSyncServer state, AdvancedContainer cont) {
		state.buffer.writeByte(scroll);
		int l = scroll + 12;
		for(int i = scroll; i < l; i++)
			if (i < slots.size()) {
				Slot s = slots.get(i);
				state.buffer.writeInt(s.countIn).writeInt(s.countOut);
			} else state.buffer.writeLong(0);
		state.endFixed();
	}

	@Override
	public void readState(StateSyncClient state, AdvancedContainer cont) {
		scroll = state.get(scroll);
		for(int i = 0; i < 12; i++) {
			Slot s = slots.get(i);
			s.countIn = state.get(s.countIn);
			s.countOut = state.get(s.countOut);
		}
	}

	@Override
	public boolean canInteract(EntityPlayer player, AdvancedContainer cont) {
		return canPlayerAccessUI(player);
	}

	@Override
	public void handleAction(PacketBuffer pkt, EntityPlayerMP sender)
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

	private static final ResourceLocation TEX = new ResourceLocation(
		Lib.ID, "textures/gui/supply.png"
	);

	@Override
	@SideOnly(Side.CLIENT)
	public ModularGui getGuiScreen(EntityPlayer player, int id) {
		ModularGui gui = new ModularGui(getContainer(player, id));
		GuiFrame frame = new GuiFrame(gui, 186, 186, 25)
		.title("gui.cd4017be.item_supp.name", 0.5F).background(TEX, 0, 70);
		new Slider(
			frame, 8, 12, 70, 170, 16, 176, 0, false, () -> scroll, (x) -> {
				int s = (int)x;
				if(s == scroll) return;
				gui.sendPkt((byte)-1, (byte)(scroll = s));
			}, null, 0, MAX_SLOTS - 12
		).scroll(-3).tooltip("gui.cd4017be.scroll");
		for(int j = 0; j < 4; j++)
			for(int i = 0; i < 3; i++) {
				byte k = (byte)(j * 3 + i);
				new FormatText(
					frame, 36, 16, 24 + i * 54, 16 + j * 18,
					"\\\u00a72%d\n\u00a74%d", slots.get(k)
				);
				new Button(
					frame, 36, 16, 24 + i * 54, 16 + j * 18,
					0, null, (a) -> gui.sendPkt(a == 0 ? k : (byte)(k | 16))
				).tooltip("gui.cd4017be.reset_count");
			}
		gui.compGroup = frame;
		return gui;
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
