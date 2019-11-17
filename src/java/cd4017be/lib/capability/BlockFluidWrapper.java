package cd4017be.lib.capability;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

/** @author CD4017BE */
public class BlockFluidWrapper implements IFluidHandler {

	private World world;
	private BlockPos pos;
	private IBlockState state;

	public BlockFluidWrapper set(World world, BlockPos pos) {
		this.world = world;
		this.pos = pos;
		this.state = null;
		return this;
	}

	private IBlockState block() {
		return state != null ? state
			: (state = world.getBlockState(pos));
	}

	@Override
	public IFluidTankProperties[] getTankProperties() {
		return null;
	}

	@Override
	public int fill(FluidStack resource, boolean doFill) {
		IBlockState state = block();
		Block block = state.getBlock();
		if(block instanceof IFluidBlock)
			return ((IFluidBlock)block).place(world, pos, resource, doFill);
		if(resource.amount < Fluid.BUCKET_VOLUME) return 0;
		Material m = state.getMaterial();
		if(
			!m.isReplaceable() || m.isLiquid()
			&& state == block.getDefaultState()
		) return 0;
		if(doFill) {
			world.destroyBlock(pos, true);
			world.setBlockState(
				pos, resource.getFluid().getBlock().getDefaultState(), 3
			);
		}
		return Fluid.BUCKET_VOLUME;
	}

	@Override
	public FluidStack drain(FluidStack resource, boolean doDrain) {
		FluidStack stack = drain(resource.amount, false);
		if(!resource.containsFluid(stack)) return null;
		if(doDrain) return drain(resource.amount, true);
		return stack;
	}

	@Override
	public FluidStack drain(int maxDrain, boolean doDrain) {
		IBlockState state = block();
		Block block = state.getBlock();
		if(block instanceof IFluidBlock)
			return ((IFluidBlock)block).drain(world, pos, doDrain);
		if(maxDrain < Fluid.BUCKET_VOLUME || state != block.getDefaultState())
			return null;
		Fluid fluid;
		if(block == Blocks.WATER || block == Blocks.FLOWING_WATER)
			fluid = FluidRegistry.WATER;
		else if(block == Blocks.LAVA || block == Blocks.FLOWING_LAVA)
			fluid = FluidRegistry.LAVA;
		else return null;
		if(doDrain) world.setBlockToAir(pos);
		return new FluidStack(fluid, Fluid.BUCKET_VOLUME);
	}

}
