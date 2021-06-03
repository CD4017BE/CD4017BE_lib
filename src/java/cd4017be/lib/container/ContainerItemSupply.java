package cd4017be.lib.container;

import static cd4017be.lib.Content.iTEM_SUPP;
import static cd4017be.lib.container.ContainerEnergySupply.TEX;
import static cd4017be.lib.tileentity.ItemSupply.MAX_SLOTS;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.BitSet;
import java.util.function.DoubleSupplier;
import cd4017be.lib.capability.BasicInventory;
import cd4017be.lib.capability.LinkedInventory;
import cd4017be.lib.container.slot.SlotHolo;
import cd4017be.lib.gui.ModularGui;
import cd4017be.lib.gui.comp.*;
import cd4017be.lib.network.StateSyncAdv;
import cd4017be.lib.tileentity.ItemSupply;
import cd4017be.lib.tileentity.ItemSupply.Slot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.items.IItemHandler;

/**@author CD4017BE */
public class ContainerItemSupply extends AdvancedContainer {

	private static final int[] indices = StateSyncAdv.array(8, 12);

	public ContainerItemSupply(int id, PlayerInventory inv, PacketBuffer pkt) {
		this(id, inv, new BasicInventory(12), true, ItemSupply.class);
	}

	public ContainerItemSupply(int id, PlayerInventory inv, ItemSupply tile) {
		this(id, inv, new LinkedInventory(12, 127, tile::getSlot, tile::setSlot), false, tile);
	}

	private ContainerItemSupply(int id, PlayerInventory pinv, IItemHandler inv, boolean client, Object... ref) {
		super(iTEM_SUPP, id, pinv, StateSyncAdv.of(client, indices, 0, ref), 0);
		for(int j = 0; j < 4; j++)
			for(int i = 0; i < 3; i++)
				addSlot(new SlotHolo(
					inv, i + j * 3, 8 + i * 54, 16 + j * 18, false, true
				), false);
		addPlayerInventory(8, 104);
	}

	@Override
	protected void detectChanges(BitSet chng) {
		super.detectChanges(chng);
		ItemSupply tile = (ItemSupply)sync.holders[0];
		for(int i = 0; i < 12; i++) {
			int j = i + tile.scroll;
			if (j < tile.slots.size()) {
				Slot s = tile.slots.get(j);
				sync.setLong(i, s.countIn & 0xffffffffL | (long)s.countOut << 32);
			} else sync.setLong(i, 0);
		}
	}

	@OnlyIn(Dist.CLIENT)
	public ModularGui<ContainerItemSupply> setupGui(PlayerInventory inv, ITextComponent name) {
		DoubleSupplier scroll = sync.floatGetter("scroll", true);
		IntBuffer data = ((ByteBuffer)sync.buffer().clear()).asIntBuffer();
		
		ModularGui<ContainerItemSupply> gui = new ModularGui<>(this, inv, name);
		GuiFrame frame = new GuiFrame(gui, 186, 186, 25)
		.title("gui.cd4017be.item_supp", 0.5F).background(TEX, 0, 70);
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
