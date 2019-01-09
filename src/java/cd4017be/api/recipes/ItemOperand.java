package cd4017be.api.recipes;

import cd4017be.lib.script.obj.Error;
import cd4017be.lib.script.obj.IOperand;
import cd4017be.lib.script.obj.Number;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * 
 * @author cd4017be
 */
public class ItemOperand implements IOperand {

	boolean copied;
	public final ItemStack stack;

	public ItemOperand(ItemStack stack) {
		this.stack = stack;
	}

	@Override
	public IOperand onCopy() {
		copied = true;
		return this;
	}

	@Override
	public boolean asBool() throws Error {
		return !stack.isEmpty();
	}

	@Override
	public Object value() {
		return stack;
	}

	@Override
	public IOperand mulR(IOperand x) {
		if (copied) {
			ItemStack stack = this.stack.copy();
			stack.setCount(x.asIndex());
			return new ItemOperand(stack);
		}
		stack.setCount(x.asIndex());
		return this;
	}

	@Override
	public IOperand mulL(IOperand x) {
		return mulR(x);
	}

	@Override
	public IOperand subR(IOperand x) {
		if (copied) {
			ItemStack stack = this.stack.copy();
			stack.setItemDamage(x.asIndex());
			return new ItemOperand(stack);
		}
		stack.setItemDamage(x.asIndex());
		return this;
	}

	@Override
	public IOperand len() {
		return new Number(stack.getCount());
	}

	@Override
	public boolean equals(IOperand obj) {
		if (obj instanceof ItemOperand)
			return ItemHandlerHelper.canItemStacksStack(stack, ((ItemOperand)obj).stack);
		return false;
	}

	@Override
	public String toString() {
		return stack.toString();
	}

}
