package cd4017be.api.grid.port;

import java.util.function.*;

import cd4017be.api.grid.Link;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

/**Grid port handler for item inventory interaction.
 * It's essentially {@link IItemHandler} broken down to just inspecting inventory
 * contents and moving items from one inventory to another without a concept of slots.
 * @author CD4017BE */
public interface IInventoryAccess extends ToIntFunction<ItemStack> {

	/**@param inspector function called for each item
	 * alongside its stack limit (don't modify given stack) */
	default void getContent(ObjIntConsumer<ItemStack> inspector) {
		getContent(inspector, Link.REC_ITEM);
	}

	/**@param inspector function called for each item
	 * alongside its stack limit (don't modify given stack)
	 * @param rec */
	void getContent(ObjIntConsumer<ItemStack> inspector, int rec);

	/**Attempt to transfer items to another inventory
	 * @param amount maximum amount to transfer
	 * @param filter to restrict, what items to transfer
	 * @param target destination inventory, see {@link #apply(ItemStack)}
	 * @return amount actually transfered */
	default int transfer(int amount, Predicate<ItemStack> filter, ToIntFunction<ItemStack> target) {
		return transfer(amount, filter, target, Link.REC_ITEM);
	}

	/**Attempt to transfer items to another inventory
	 * @param amount maximum amount to transfer
	 * @param filter to restrict, what items to transfer
	 * @param target destination inventory, see {@link #applyAsInt(ItemStack)}
	 * @param rec recursion count-down
	 * @return amount actually transfered */
	int transfer(int amount, Predicate<ItemStack> filter, ToIntFunction<ItemStack> target, int rec);

	/**Attempt to insert the given stack.
	 * @param stack item to insert, not to be modified (neither immediately nor in the future)
	 * @return amount inserted */
	@Override
	default int applyAsInt(ItemStack stack) {
		return insert(stack, Link.REC_ITEM);
	}

	/**Attempt to insert the given stack.
	 * @param stack item to insert, not to be modified (neither immediately nor in the future)
	 * @param rec recursion count-down
	 * @return remainder that could not be inserted */
	int insert(ItemStack stack, int rec);

	/** does nothing */
	IInventoryAccess NOP = new IInventoryAccess() {
		@Override
		public void getContent(ObjIntConsumer<ItemStack> inspector, int rec) {}
		@Override
		public int transfer(int amount, Predicate<ItemStack> filter, ToIntFunction<ItemStack> target, int rec) {return 0;}
		@Override
		public int insert(ItemStack stack, int rec) {return 0;}
	};

	/** port type id */
	int TYPE_ID = 2;

	static IInventoryAccess of(Object handler) {
		return handler instanceof IInventoryAccess ? (IInventoryAccess)handler : NOP;
	}

	static Predicate<ItemStack> filter(ItemStack stack) {
		return s -> ItemHandlerHelper.canItemStacksStack(s, stack);
	}

}
