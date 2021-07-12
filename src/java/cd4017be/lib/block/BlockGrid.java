package cd4017be.lib.block;

import cd4017be.lib.tileentity.Grid;
import cd4017be.lib.util.VoxelShape4x4x4;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapeCube;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;

/**
 * @author CD4017BE */
public class BlockGrid extends BlockTE<Grid> {

	public BlockGrid(Properties properties) {
		super(properties, flags(Grid.class));
	}

	@Override
	public boolean is(Block block) {
		return block instanceof BlockGrid;
	}

	@Override
	public VoxelShape getOcclusionShape(BlockState state, IBlockReader world, BlockPos pos) {
		if (state.canOcclude()) return VoxelShapes.block();
		TileEntity te = world.getBlockEntity(pos);
		if (te instanceof Grid)
			return new VoxelShapeCube(new VoxelShape4x4x4(((Grid)te).opaque));
		return VoxelShapes.empty();
	}

}
