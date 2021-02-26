package cd4017be.lib.container.test;

import static cd4017be.lib.Lib.C_ENERGY_SUPP;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;

import cd4017be.lib.Lib;
import cd4017be.lib.container.AdvancedContainer;
import cd4017be.lib.gui.ModularGui;
import cd4017be.lib.gui.comp.*;
import cd4017be.lib.network.StateSyncAdv;
import cd4017be.lib.tileentity.test.*;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** @author CD4017BE */
public class ContainerEnergySupply extends AdvancedContainer {

	public ContainerEnergySupply(int id, PlayerInventory inv, PacketBuffer pkt) {
		super(C_ENERGY_SUPP, id, inv, StateSyncAdv.of(true, EnergySupply.class), 0);
	}

	public ContainerEnergySupply(int id, PlayerInventory inv, EnergySupply tile) {
		super(C_ENERGY_SUPP, id, inv, StateSyncAdv.of(false, tile), 0);
	}


	@OnlyIn(Dist.CLIENT)
	private static final ResourceLocation TEX = Lib.rl("textures/gui/supply.png");

	@OnlyIn(Dist.CLIENT)
	public ModularGui<ContainerEnergySupply> setupGui(PlayerInventory inv, ITextComponent name) {
		IntSupplier limO = sync.intGetter("limO", false);
		IntSupplier limI = sync.intGetter("limI", false);
		IntSupplier flowO = sync.intGetter("lastO", false);
		IntSupplier flowI = sync.intGetter("lastI", false);
		DoubleSupplier sumI = sync.floatGetter("sumI", true);
		DoubleSupplier sumO = sync.floatGetter("sumO", true);
		LongSupplier t0 = sync.longGetter("t", false);
		DoubleSupplier avgI = ()-> sumI.getAsDouble() / t0.getAsLong();
		DoubleSupplier avgO = ()-> sumO.getAsDouble() / t0.getAsLong();
		
		ModularGui<ContainerEnergySupply> gui = new ModularGui<>(this, inv, name);
		GuiFrame frame = new GuiFrame(gui, 169, 67, 10)
		.background(TEX, 0, 0).title("gui.cd4017be_lib.energy_supp", 0.5F);
		new TextField(
			frame, 70, 7, 19, 16, 12, () -> Integer.toString(limO.getAsInt()), (t) -> {
				try {
					gui.sendPkt((byte)1, Integer.parseInt(t));
				} catch(NumberFormatException e) {}
			}
		).color(0xff202020, 0xff800000).tooltip("gui.cd4017be.limit_o");
		new TextField(
			frame, 70, 7, 91, 16, 12, () -> Integer.toString(limI.getAsInt()), (t) -> {
				try {
					gui.sendPkt((byte)0, Integer.parseInt(t));
				} catch(NumberFormatException e) {}
			}
		).color(0xff202020, 0xff800000).tooltip("gui.cd4017be.limit_i");
		new Progressbar(
			frame, 72, 9, 18, 24, 184, 0, Progressbar.H_FILL,
			() -> -(double)flowO.getAsInt() / limO.getAsInt(), 0.0, -1.0
		);
		new Progressbar(
			frame, 72, 9, 90, 24, 184, 9, Progressbar.H_FILL,
			() -> (double)flowI.getAsInt() / limI.getAsInt()
		);
		new Progressbar(
			frame, 72, 9, 18, 42, 184, 0, Progressbar.H_FILL,
			() -> -avgO.getAsDouble() / limO.getAsInt(), 0.0, -1.0
		);
		new Progressbar(
			frame, 72, 9, 90, 42, 184, 9, Progressbar.H_FILL,
			() -> avgI.getAsDouble() / limI.getAsInt()
		);
		new FormatText(
			frame, 70, 7, 19, 25, "\\%d\n%1$.6u RF\n%2$.6u RF/t", () -> new Object[] {
				flowO.getAsInt(), sumO.getAsDouble(), avgO.getAsDouble()
			}
		).align(0F).color(0xff202020);
		new FormatText(
			frame, 70, 7, 91, 25, "\\%d\n%1$.6u RF\n%2$.6u RF/t", () -> new Object[] {
				flowI.getAsInt(), sumI.getAsDouble(), avgI.getAsDouble()
			}
		).align(0F).color(0xff202020);
		new FormatText(
			frame, 126, 9, 18, 51, "\\%d:%02d:%05.2f", () -> {
				long t = t0.getAsLong();
				return new Object[] { t / 72000L, t / 1200L % 60L, t % 1200L / 20.0 };
			}
		).align(0.5F).color(0xff202020);
		new Button(frame, 18, 9, 144, 51, 0, null, (a) -> gui.sendPkt((byte)2))
		.tooltip("gui.cd4017be.reset_count");
		return gui.setComps(frame, false);
	}

}
