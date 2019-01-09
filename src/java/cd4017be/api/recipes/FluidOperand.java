package cd4017be.api.recipes;

import cd4017be.lib.script.obj.Error;
import cd4017be.lib.script.obj.IOperand;
import cd4017be.lib.script.obj.Number;
import net.minecraftforge.fluids.FluidStack;

/**
 * 
 * @author cd4017be
 */
public class FluidOperand implements IOperand {

	boolean copied;
	final FluidStack stack;

	public FluidOperand(FluidStack stack) {
		this.stack = stack;
	}

	@Override
	public IOperand onCopy() {
		copied = true;
		return this;
	}

	@Override
	public boolean asBool() throws Error {
		return stack.amount > 0;
	}

	@Override
	public Object value() {
		return stack;
	}

	@Override
	public IOperand mulR(IOperand x) {
		if (copied)
			return new FluidOperand(new FluidStack(stack, x.asIndex()));
		stack.amount = x.asIndex();
		return this;
	}

	@Override
	public IOperand mulL(IOperand x) {
		return mulR(x);
	}

	@Override
	public IOperand len() {
		return new Number(stack.amount);
	}

	@Override
	public boolean equals(IOperand obj) {
		if (obj instanceof FluidOperand)
			return stack.isFluidEqual(((FluidOperand)obj).stack);
		else if (obj instanceof ItemOperand)
			return stack.isFluidEqual(((ItemOperand)obj).stack);
		return false;
	}

	@Override
	public String toString() {
		return stack.amount + "*" + stack.getUnlocalizedName();
	}

}
