package cd4017be.lib.container.slot;

import java.util.function.Predicate;

import cd4017be.lib.capability.IFluidHandlerModifiable;
import cd4017be.lib.capability.IMultiFluidHandler;
import cd4017be.lib.container.AdvancedContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.NonNullList;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.SlotItemHandler;

/**The fluid equivalent of {@link SlotItemHandler} for use in {@link AdvancedContainer}.
 * Slot interaction allows filling and emptying of fluid containers with the referenced Fluid Tank.
 * @author CD4017BE */
public class SlotFluidHandler extends Slot implements IFluidSlot, ISpecialSlot {

	private static final Container emptyInventory = new SimpleContainer(0);
	private final IFluidHandler fluidHandler;

	public SlotFluidHandler(IFluidHandler fluidHandler, int index, int xPosition, int yPosition) {
		super(emptyInventory, index, xPosition, yPosition);
		this.fluidHandler = fluidHandler;
	}

	@Override
	public boolean mayPlace(ItemStack stack) {
		return false;
	}

	@Override
	public ItemStack getItem() {
		return ItemStack.EMPTY;
	}

	@Override
	public void set(ItemStack stack) {
		throw new UnsupportedOperationException("Fluid slots don't have an item stack to set!");
	}

	@Override
	public int getMaxStackSize() {
		return 0;
	}

	@Override
	public ItemStack remove(int amount) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean mayPickup(Player playerIn) {
		return false;
	}

	//fluid slot

	@Override
	public FluidStack getFluid() {
		return fluidHandler.getFluidInTank(getSlotIndex());
	}

	@Override
	public void putFluid(FluidStack stack) {
		((IFluidHandlerModifiable)fluidHandler).setFluidInTank(getSlotIndex(), stack);
	}

	@Override
	public int getCapacity() {
		return fluidHandler.getTankCapacity(getSlotIndex());
	}

	@Override
	public boolean insertHereOnly(ItemStack stack) {
		return false;
	}

	@Override
	public ItemStack onClick(int b, ClickType ct, Player player, AdvancedContainer cont) {
		ItemStack curItem = cont.getCarried();
		if (ct == ClickType.CLONE) {
			if (!curItem.isEmpty()) return ItemStack.EMPTY;
			FluidStack stack = getFluid();
			Predicate<FluidStack> filter = f ->
				stack.isEmpty() ? fluidHandler.isFluidValid(getSlotIndex(), f)
					: f.isEmpty() || f.isFluidEqual(stack);
			NonNullList<ItemStack> inv =  player.getInventory().items;
			for (int i = 0; i < inv.size(); i++) {
				ItemStack item = inv.get(i);
				if (FluidUtil.getFluidContained(item).filter(filter).isPresent()) {
					cont.setCarried(item);
					inv.set(i, curItem);
					break;
				}
			}
			if (player.isCreative() && !stack.isEmpty())
				cont.setCarried(FluidUtil.getFilledBucket(stack));
		} else if (ct == ClickType.PICKUP || ct == ClickType.PICKUP_ALL || ct == ClickType.QUICK_MOVE) {
			IFluidHandler inv = fluidHandler;
			if (inv instanceof IMultiFluidHandler)
				inv = ((IMultiFluidHandler)inv).accessTank(getSlotIndex());
			int limit = ct == ClickType.QUICK_MOVE ? Integer.MAX_VALUE : 1000;
			FluidActionResult r;
			if (b == 0)
				r = FluidUtil.tryEmptyContainerAndStow(curItem, inv, null, limit, player, true);
			else r = FluidUtil.tryFillContainerAndStow(curItem, inv, null, limit, player, true);
			if (r.success) cont.setCarried(r.result);
		}
		return ItemStack.EMPTY;
	}

	@Override
	public ItemStack insertItem(ItemStack stack, boolean sim) {
		return stack;
	}

	@Override
	public ItemStack extractItem(int am, boolean sim) {
		return ItemStack.EMPTY;
	}

}
