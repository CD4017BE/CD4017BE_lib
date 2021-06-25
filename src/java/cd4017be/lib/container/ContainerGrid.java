package cd4017be.lib.container;

import static cd4017be.lib.Content.*;

import java.util.function.*;

import cd4017be.lib.Lib;
import cd4017be.lib.container.slot.HidableSlot;
import cd4017be.lib.gui.ModularGui;
import cd4017be.lib.gui.comp.*;
import cd4017be.lib.network.StateSyncAdv;
import cd4017be.lib.tileentity.Grid;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * @author CD4017BE */
public class ContainerGrid extends ItemContainer {

	private final Inventory sideInv = new Inventory(6);
	private Grid cache;

	public ContainerGrid(int id, PlayerInventory inv, PacketBuffer pkt) {
		this(id, inv, pkt.readUnsignedByte());
	}

	public ContainerGrid(int id, PlayerInventory inv, int slot) {
		super(
			gRID, id, inv, slot, grid,
			StateSyncAdv.of(inv.player.level.isClientSide), 0
		);
		((HidableSlot)addSlot(new HidableSlot(inv, slot, 199, 45))).lock();
		addSlot(new Slot(sideInv, 0, 199, 63));
		addSlot(new Slot(sideInv, 1, 199, 27));
		addSlot(new Slot(sideInv, 2, 217, 54));
		addSlot(new Slot(sideInv, 3, 181, 36));
		addSlot(new Slot(sideInv, 4, 217, 36));
		addSlot(new Slot(sideInv, 5, 181, 54));
		addPlayerInventory(8, 16, false);
	}

	@Override
	public void removed(PlayerEntity player) {
		clearContainer(player, player.level, sideInv);
		super.removed(player);
	}

	private Grid grid() {
		if (cache == null) cache = grid.tileEntity(getStack());
		return cache;
	}

	private Grid grid(ItemStack stack) {
		if (stack.getItem() == grid)
			return grid.tileEntity(stack);
		return null;
	}

	private ItemStack set(int slot, Grid grid, int n) {
		ItemStack stack = grid.getItem();
		if (!stack.isEmpty()) stack.setCount(n);
		slots.get(slot).set(stack);
		return stack;
	}

	@Override
	public ItemStack clicked(int s, int b, ClickType m, PlayerEntity player) {
		if (s != 0) return super.clicked(s, b, m, player);
		if (player.level.isClientSide) return ItemStack.EMPTY;
		ItemStack stack = getStack(), stack1 = player.inventory.getCarried();
		int n = stack.getCount();
		if (stack1.getCount() < n || stack1.getItem() != stack.getItem())
			return ItemStack.EMPTY;
		Grid grid = grid();
		if (!grid.merge(grid(stack1))) return ItemStack.EMPTY;
		stack1.shrink(n);
		return set(s, grid, n);
	}

	@Override
	public void handlePlayerPacket(PacketBuffer pkt, ServerPlayerEntity sender)
	throws Exception {
		byte cmd = pkt.readByte();
		int i = cmd & 0x7f, n = cmd < 0 ? 2 : 1;
		if (i < 6) {
			Direction d = Direction.from3DDataValue(i);
			//get stacks and check counts
			ItemStack stack1 = getStack();
			int m = stack1.getCount();
			ItemStack stack2 = sideInv.getItem(i);
			if (!stack2.isEmpty() && stack2.getCount() != m) return;
			ItemStack stack0 = sideInv.getItem(i^1);
			if (!stack0.isEmpty() && stack0.getCount() != m) return;
			//shift grid 2 -> null
			Grid gridX = grid(stack2);
			if (gridX == null) {
				if (!stack2.isEmpty()) return;
				gridX = GRID.tileType.create();
			} else if (!gridX.shift(d, n, null)) return;
			//shift grid 1 -> grid 2
			Grid grid = grid();
			boolean done = grid.shift(d, n, gridX);
			set(i + 1, gridX, m);
			if (!done) return;
			//shift grid 0 -> grid 1
			gridX = grid(stack0);
			if (gridX != null && gridX.shift(d, n, grid))
				set((i^1) + 1, gridX, m);
			grid.updateBounds();
			set(0, grid, m);
		} else if (i < 8) {
			if (i == 7) n = 4 - n;
			// rotate grid
			Grid grid = grid();
			if (!grid.rotate(n)) return;
			set(0, grid, getStack().getCount());
		}
	}

	public static final ResourceLocation TEX = Lib.rl("textures/gui/grid_edit.png");

	@OnlyIn(Dist.CLIENT)
	public ModularGui<ContainerGrid> setupGui(PlayerInventory inv, ITextComponent name) {
		ModularGui<ContainerGrid> gui = new ModularGui<>(this, inv, name);
		GuiFrame frame = new GuiFrame(gui, 252, 98, 8)
		.background(TEX, 0, 0).title("gui.cd4017be.grid_edit", 0.9F);
		
		new Button(frame, 10, 10, 202, 80, 0, null, send(gui, 0)).tooltip("gui.cd4017be.shift0");
		new Button(frame, 10, 10, 202, 16, 0, null, send(gui, 1)).tooltip("gui.cd4017be.shift1");
		new Button(frame, 10, 10, 234, 67, 0, null, send(gui, 2)).tooltip("gui.cd4017be.shift2");
		new Button(frame, 10, 10, 170, 29, 0, null, send(gui, 3)).tooltip("gui.cd4017be.shift3");
		new Button(frame, 10, 10, 234, 29, 0, null, send(gui, 4)).tooltip("gui.cd4017be.shift4");
		new Button(frame, 10, 10, 170, 67, 0, null, send(gui, 5)).tooltip("gui.cd4017be.shift5");
		new Button(frame, 8, 18, 235, 44, 0, null, send(gui, 6)).tooltip("gui.cd4017be.rot_right");
		new Button(frame, 8, 18, 170, 44, 0, null, send(gui, 7)).tooltip("gui.cd4017be.rot_left");
		gui.slotTooltips.put(0, "gui.cd4017be.merge");
		return gui.setComps(frame, false);
	}

	@OnlyIn(Dist.CLIENT)
	private static IntConsumer send(ModularGui<?> gui, int i) {
		return b -> gui.sendPkt((byte)(b == IGuiComp.B_RIGHT ? i | Byte.MIN_VALUE : i));
	}

}
