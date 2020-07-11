package cd4017be.lib.tileentity.test;

import cd4017be.lib.Lib;
import cd4017be.lib.Gui.AdvancedContainer;
import cd4017be.lib.Gui.AdvancedContainer.IStateInteractionHandler;
import cd4017be.lib.Gui.ModularGui;
import cd4017be.lib.Gui.comp.Button;
import cd4017be.lib.Gui.comp.FormatText;
import cd4017be.lib.Gui.comp.GuiFrame;
import cd4017be.lib.Gui.comp.Progressbar;
import cd4017be.lib.Gui.comp.TextField;
import cd4017be.lib.network.IGuiHandlerTile;
import cd4017be.lib.network.StateSyncClient;
import cd4017be.lib.network.StateSyncServer;
import cd4017be.lib.network.StateSynchronizer;
import cd4017be.lib.tileentity.BaseTileEntity;
import cd4017be.lib.tileentity.BaseTileEntity.ITickableServerOnly;
import cd4017be.lib.util.Utils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import static net.minecraftforge.energy.CapabilityEnergy.ENERGY;

/** @author CD4017BE */
public class EnergySupply extends BaseTileEntity
implements IEnergyStorage, IGuiHandlerTile, IStateInteractionHandler,
ITickableServerOnly {

	public int lastIn, lastOut, limitIn, flowIn, limitOut, flowOut;
	public long t0, sumIn, sumOut;

	@Override
	public void update() {
		lastIn = flowIn;
		lastOut = flowOut;
		flowIn = 0;
		flowOut = 0;
		if(limitOut > 0)
			for(EnumFacing side : EnumFacing.VALUES) {
				IEnergyStorage acc = Utils.neighborCapability(
					this, side, ENERGY
				);
				if(acc == null) continue;
				this.extractEnergy(acc.receiveEnergy(limitOut, false), false);
			}
	}

	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		return capability == ENERGY;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		return capability == ENERGY ? (T)this : null;
	}

	@Override
	public int receiveEnergy(int maxReceive, boolean simulate) {
		if(maxReceive > limitIn) maxReceive = limitIn;
		if(!simulate) {
			flowIn += maxReceive;
			sumIn += maxReceive;
		}
		return maxReceive;
	}

	@Override
	public int extractEnergy(int maxExtract, boolean simulate) {
		if(maxExtract > limitOut) maxExtract = limitOut;
		if(!simulate) {
			flowOut += maxExtract;
			sumOut += maxExtract;
		}
		return maxExtract;
	}

	@Override
	public int getEnergyStored() {
		return limitOut;
	}

	@Override
	public int getMaxEnergyStored() {
		int n = limitIn + limitOut;
		return n < 0 ? Integer.MAX_VALUE : n;
	}

	@Override
	public boolean canExtract() {
		return limitOut > 0;
	}

	@Override
	public boolean canReceive() {
		return limitIn > 0;
	}

	@Override
	protected void storeState(NBTTagCompound nbt, int mode) {
		super.storeState(nbt, mode);
		nbt.setInteger("li", limitIn);
		nbt.setInteger("lo", limitOut);
		nbt.setLong("si", sumIn);
		nbt.setLong("so", sumOut);
		nbt.setLong("t0", t0);
	}

	@Override
	protected void loadState(NBTTagCompound nbt, int mode) {
		super.loadState(nbt, mode);
		limitIn = nbt.getInteger("li");
		limitOut = nbt.getInteger("lo");
		sumIn = nbt.getLong("si");
		sumOut = nbt.getLong("so");
		t0 = nbt.getLong("t0");
	}

	private static final StateSynchronizer.Builder ssb = StateSynchronizer
	.builder().addFix(4, 4, 4, 4, 8, 8, 8);

	@Override
	public AdvancedContainer getContainer(EntityPlayer player, int id) {
		return new AdvancedContainer(this, ssb.build(world.isRemote), player);
	}

	@Override
	public void writeState(StateSyncServer state, AdvancedContainer cont) {
		state.putAll(
			lastIn, lastOut, limitIn, limitOut, sumIn, sumOut, world
			.getTotalWorldTime() - t0 - 1
		);
	}

	@Override
	public void readState(StateSyncClient state, AdvancedContainer cont) {
		flowIn = state.get(flowIn);
		flowOut = state.get(flowOut);
		limitIn = state.get(limitIn);
		limitOut = state.get(limitOut);
		sumIn = state.get(sumIn);
		sumOut = state.get(sumOut);
		t0 = state.get(t0);
	}

	@Override
	public boolean canInteract(EntityPlayer player, AdvancedContainer cont) {
		return canPlayerAccessUI(player);
	}

	@Override
	public void handleAction(PacketBuffer pkt, EntityPlayerMP sender)
	throws Exception {
		switch(pkt.readByte()) {
		case 0:
			if((limitIn = pkt.readInt()) < 0) limitIn = 0;
			break;
		case 1:
			if((limitOut = pkt.readInt()) < 0) limitOut = 0;
			break;
		case 2:
			sumIn = 0;
			sumOut = 0;
			t0 = world.getTotalWorldTime();
			break;
		default:
			return;
		}
		markDirty(SAVE);
	}

	@SideOnly(Side.CLIENT)
	private static final ResourceLocation TEX = new ResourceLocation(
		Lib.ID, "textures/gui/supply.png"
	);

	@Override
	@SideOnly(Side.CLIENT)
	public ModularGui getGuiScreen(EntityPlayer player, int id) {
		ModularGui gui = new ModularGui(getContainer(player, id));
		GuiFrame frame = new GuiFrame(gui, 169, 67, 10)
		.background(TEX, 0, 0).title("gui.cd4017be.energy_supp.name", 0.5F);
		new TextField(
			frame, 70, 7, 19, 16, 12, () -> Integer.toString(limitOut), (t) -> {
				try {
					gui.sendPkt((byte)1, Integer.parseInt(t));
				} catch(NumberFormatException e) {}
			}
		).color(0xff202020, 0xff800000).tooltip("gui.cd4017be.limit_o");
		new TextField(
			frame, 70, 7, 91, 16, 12, () -> Integer.toString(limitIn), (t) -> {
				try {
					gui.sendPkt((byte)0, Integer.parseInt(t));
				} catch(NumberFormatException e) {}
			}
		).color(0xff202020, 0xff800000).tooltip("gui.cd4017be.limit_i");
		new Progressbar(
			frame, 72, 9, 18, 24, 184, 0, Progressbar.H_FILL,
			() -> -(double)flowOut / limitOut, 0.0, -1.0
		);
		new Progressbar(
			frame, 72, 9, 90, 24, 184, 9, Progressbar.H_FILL,
			() -> (double)flowIn / (double)limitIn
		);
		new Progressbar(
			frame, 72, 9, 18, 42, 184, 0, Progressbar.H_FILL,
			() -> -(double)sumOut / t0 / limitOut, 0.0, -1.0
		);
		new Progressbar(
			frame, 72, 9, 90, 42, 184, 9, Progressbar.H_FILL,
			() -> (double)sumIn / (double)t0 / limitIn
		);
		new FormatText(
			frame, 70, 7, 19, 25, "\\%d\n%1$.6u RF\n%2$.6u RF/t",
			() -> new Object[] {
				flowOut, (double)sumOut, (double)sumOut / (double)t0
			}
		).align(0F).color(0xff202020);
		new FormatText(
			frame, 70, 7, 91, 25, "\\%d\n%1$.6u RF\n%2$.6u RF/t",
			() -> new Object[] {
				flowIn, (double)sumIn, (double)sumIn / (double)t0
			}
		).align(0F).color(0xff202020);
		new FormatText(
			frame, 126, 9, 18, 51, "\\%d:%02d:%05.2f", () -> new Object[] {
				t0 / 72000L, t0 / 1200L % 60L, t0 % 1200L / 20.0
			}
		).align(0.5F).color(0xff202020);
		new Button(frame, 18, 9, 144, 51, 0, null, (a) -> gui.sendPkt((byte)2))
		.tooltip("gui.cd4017be.reset_count");
		gui.compGroup = frame;
		return gui;
	}

}
