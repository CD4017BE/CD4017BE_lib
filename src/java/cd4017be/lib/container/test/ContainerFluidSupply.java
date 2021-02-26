package cd4017be.lib.container.test;

import static cd4017be.lib.Lib.C_FLUID_SUPP;
import static cd4017be.lib.tileentity.test.FluidSupply.MAX_SLOTS;

import java.nio.IntBuffer;
import java.util.BitSet;
import java.util.function.DoubleSupplier;
import cd4017be.lib.Lib;
import cd4017be.lib.capability.BasicTanks;
import cd4017be.lib.container.AdvancedContainer;
import cd4017be.lib.container.slot.SlotFluidHandler;
import cd4017be.lib.gui.ModularGui;
import cd4017be.lib.gui.comp.*;
import cd4017be.lib.network.StateSyncAdv;
import cd4017be.lib.tileentity.test.FluidSupply;
import cd4017be.lib.tileentity.test.FluidSupply.Slot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fluids.capability.IFluidHandler;

/**
 * @author CD4017BE */
public class ContainerFluidSupply extends AdvancedContainer {

	private static final int[] indices = StateSyncAdv.array(8, 12);

	public ContainerFluidSupply(int id, PlayerInventory inv, PacketBuffer pkt) {
		this(id, inv, new BasicTanks(12, 0), true, FluidSupply.class);
	}

	public ContainerFluidSupply(int id, PlayerInventory inv, FluidSupply tile) {
		this(id, inv, tile, false, tile);
	}

	private ContainerFluidSupply(int id, PlayerInventory pinv, IFluidHandler inv, boolean client, Object... ref) {
		super(C_FLUID_SUPP, id, pinv, StateSyncAdv.of(client, indices, 12, ref), 12);
		for(int j = 0; j < 4; j++)
			for(int i = 0; i < 3; i++)
				addSlot(new SlotFluidHandler(
					inv, i + j * 3, 8 + i * 54, 16 + j * 18
				), true);
		addPlayerInventory(8, 104);
	}

	@Override
	protected void detectChanges(BitSet chng) {
		super.detectChanges(chng);
		FluidSupply tile = (FluidSupply)sync.holders[0];
		for(int i = 0; i < 12; i++) {
			int j = i + tile.scroll;
			if (j < tile.slots.size()) {
				Slot s = tile.slots.get(j);
				sync.setLong(i, s.countIn & 0xffffffffL | (long)s.countOut << 32);
			} else sync.setLong(i, 0);
		}
	}

	@OnlyIn(Dist.CLIENT)
	private static final ResourceLocation TEX = Lib.rl("textures/gui/supply.png");

	@OnlyIn(Dist.CLIENT)
	public ModularGui<ContainerFluidSupply> setupGui(PlayerInventory inv, ITextComponent name) {
		DoubleSupplier scroll = sync.floatGetter("scroll", true);
		IntBuffer data = sync.buffer().clear().asIntBuffer();
		
		ModularGui<ContainerFluidSupply> gui = new ModularGui<>(this, inv, name);
		GuiFrame frame = new GuiFrame(gui, 186, 186, 25)
		.title("gui.cd4017be.fluid_supp", 0.5F).background(TEX, 0, 70);
		new Slider(
			frame, 8, 12, 70, 170, 16, 176, 0, false, scroll, (x) -> {
				if((int)x == (int)scroll.getAsDouble()) return;
				gui.sendPkt((byte)-1, (byte)x);
			}, null, 0, MAX_SLOTS - 12
		).scroll(-3).tooltip("gui.cd4017be.scroll");
		for(int j = 0; j < 4; j++)
			for(int i = 0; i < 3; i++) {
				byte k = (byte)(j * 3 + i);
				new FormatText(
					frame, 36, 16, 24 + i * 54, 16 + j * 18,
					"\\§2%d\n§4%d", ()-> new Object[] {
						data.get(k << 1),
						data.get(k << 1 | 1)
					}
				);
				new Button(
					frame, 36, 16, 24 + i * 54, 16 + j * 18,
					0, null, (a) -> gui.sendPkt(a == 0 ? k : (byte)(k | 16))
				).tooltip("gui.cd4017be.reset_count");
			}
		return gui.setComps(frame, false);
	}
}
