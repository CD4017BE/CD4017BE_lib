package cd4017be.lib.tileentity.test;

import static net.minecraftforge.fluids.capability.CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY;
import java.io.IOException;
import java.util.ArrayList;
import java.util.function.Supplier;
import cd4017be.lib.Lib;
import cd4017be.lib.Gui.comp.*;
import cd4017be.lib.network.*;
import cd4017be.lib.Gui.*;
import cd4017be.lib.Gui.AdvancedContainer.IStateInteractionHandler;
import cd4017be.lib.tileentity.BaseTileEntity;
import cd4017be.lib.util.IntBiConsumer;
import cd4017be.lib.util.ItemFluidUtil;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.FluidTankProperties;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** @author CD4017BE */
public class FluidSupply extends BaseTileEntity
implements IFluidHandler, IGuiHandlerTile, IStateInteractionHandler, ITankContainer {

	public static int MAX_SLOTS = 12;

	ArrayList<Slot> slots = new ArrayList<>();
	int scroll;

	@Override
	public IFluidTankProperties[] getTankProperties() {
		int l = slots.size();
		IFluidTankProperties[] props = slots.toArray(new IFluidTankProperties[Math.min(l + 1, MAX_SLOTS)]);
		if (props.length > l) props[l] = new FluidTankProperties(null, Integer.MAX_VALUE);
		return props;
	}

	@Override
	public int fill(FluidStack resource, boolean doFill) {
		int n = resource.amount;
		for (Slot s : slots) {
			if (!resource.isFluidEqual(s.stack)) continue;
			if (doFill) s.countIn += n;
			return n;
		}
		if (slots.size() >= MAX_SLOTS) return 0;
		if (doFill) {
			Slot s = new Slot(new FluidStack(resource, 1000));
			s.countIn = n;
			slots.add(s);
		}
		return n;
	}

	@Override
	public FluidStack drain(FluidStack resource, boolean doDrain) {
		int n = resource.amount;
		for (Slot s : slots) {
			if (!resource.isFluidEqual(s.stack)) continue;
			n = Math.min(n, s.stack.amount);
			if (doDrain) s.countOut += n;
			return new FluidStack(s.stack, n);
		}
		return null;
	}

	@Override
	public FluidStack drain(int maxDrain, boolean doDrain) {
		if (slots.isEmpty()) return null;
		Slot s = slots.get(0);
		int n = Math.min(maxDrain, s.stack.amount);
		if (doDrain) s.countOut += n;
		return new FluidStack(s.stack, n);
	}

	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		return capability == FLUID_HANDLER_CAPABILITY;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		return capability == FLUID_HANDLER_CAPABILITY ? (T)this : null;
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
			Slot s = new Slot(FluidStack.loadFluidStackFromNBT(tag));
			s.countIn = tag.getInteger("in");
			s.countOut = tag.getInteger("out");
			slots.add(s);
		}
	}

	@Override
	public AdvancedContainer getContainer(EntityPlayer player, int id) {
		AdvancedContainer cont = new AdvancedContainer(
			this, ssb.build(world.isRemote), player
		);
		cont.addPlayerInventory(8, 104);
		if(world.isRemote)
			while(slots.size() < 12)
			slots.add(new Slot(null));
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
		for (int i = 0; i < 12; i++)
			state.set(25 + i, i < slots.size() ? slots.get(i).stack : null);
	}

	@Override
	public void readState(StateSyncClient state, AdvancedContainer cont) throws IOException {
		scroll = state.get(scroll);
		for(int i = 0; i < 12; i++) {
			Slot s = slots.get(i);
			s.countIn = state.get(s.countIn);
			s.countOut = state.get(s.countOut);
		}
		for(int i = 0; i < 12; i++) {
			Slot s = slots.get(i);
			s.stack = state.get(s.stack);
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
		if (cmd >= 32) {
			cmd = (cmd & 15) + scroll;
			if (cmd < slots.size()) slots.get(cmd).stack = ItemFluidUtil.readFluidStack(pkt);
			else if (cmd >= MAX_SLOTS) return;
			else slots.add(new Slot(ItemFluidUtil.readFluidStack(pkt)));
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

	@SideOnly(Side.CLIENT)
	private static final ResourceLocation TEX = new ResourceLocation(
		Lib.ID, "textures/gui/supply.png"
	);

	@Override
	@SideOnly(Side.CLIENT)
	public ModularGui getGuiScreen(EntityPlayer player, int id) {
		ModularGui gui = new ModularGui(getContainer(player, id));
		GuiFrame frame = new GuiFrame(gui, 186, 186, 25)
		.title("gui.cd4017be.fluid_supp.name", 0.5F).background(TEX, 0, 70);
		new Slider(
			frame, 8, 12, 70, 170, 16, 176, 0, false, () -> scroll, (x) -> {
				int s = (int)x;
				if(s == scroll) return;
				gui.sendPkt((byte)-1, (byte)(scroll = s));
			}, null, 0, MAX_SLOTS - 12
		).scroll(-3).tooltip("gui.cd4017be.scroll");
		IntBiConsumer setFluid = (i, a) -> {
			FluidStack fluid = getTank(i);
			if (fluid != null && a < 0) {
				fluid.amount += (a + 2) * (GuiScreen.isShiftKeyDown() ? 10 : 1) * (GuiScreen.isCtrlKeyDown() ? 100 : 1) * (GuiScreen.isAltKeyDown() ? 1 : 1000);
				if (fluid.amount < 0) fluid.amount = 0;
			} else fluid = FluidUtil.getFluidContained(gui.mc.player.inventory.getItemStack());
			PacketBuffer buff = GuiNetworkHandler.preparePacket(gui.container);
			buff.writeByte(32 + i);
			ItemFluidUtil.writeFluidStack(buff, fluid);
			GuiNetworkHandler.GNH_INSTANCE.sendToServer(buff);
		};
		for(int j = 0; j < 4; j++)
			for(int i = 0; i < 3; i++) {
				byte k = (byte)(j * 3 + i);
				new TankInterface(frame, 16, 16, 8 + i * 54, 16 + j * 18, this, k, setFluid);
				new FormatText(
					frame, 36, 16, 24 + i * 54, 16 + j * 18,
					"\\§2%d\n§4%d", slots.get(k)
				);
				new Button(
					frame, 36, 16, 24 + i * 54, 16 + j * 18,
					0, null, (a) -> gui.sendPkt(a == 0 ? k : (byte)(k | 16))
				).tooltip("gui.cd4017be.reset_count");
			}
		gui.compGroup = frame;
		return gui;
	}

	@Override
	public int getTanks() {
		return MAX_SLOTS;
	}

	@Override
	public FluidStack getTank(int i) {
		return i < slots.size() ? slots.get(i).stack : null;
	}

	@Override
	public int getCapacity(int i) {
		return 0;
	}

	@Override
	public void setTank(int i, FluidStack fluid) {}


	public static class Slot implements IFluidTankProperties, Supplier<Object[]> {

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

		@Override
		public FluidStack getContents() {
			return stack;
		}

		@Override
		public int getCapacity() {
			return Integer.MAX_VALUE;
		}

		@Override
		public boolean canFill() {
			return true;
		}

		@Override
		public boolean canDrain() {
			return stack.amount > 0;
		}

		@Override
		public boolean canFillFluidType(FluidStack fluidStack) {
			return fluidStack.isFluidEqual(stack);
		}

		@Override
		public boolean canDrainFluidType(FluidStack fluidStack) {
			return fluidStack.isFluidEqual(stack);
		}

	}

}
