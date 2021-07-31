package cd4017be.lib.block;

import cd4017be.lib.tileentity.Grid;
import cd4017be.lib.util.VoxelShape4x4x4;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.BlockGetter;

/**
 * @author CD4017BE */
public class BlockGrid extends BlockTE<Grid> {

	public BlockGrid(Properties properties) {
		super(properties, flags(Grid.class));
	}

	@Override
	public VoxelShape getOcclusionShape(BlockState state, BlockGetter world, BlockPos pos) {
		if (state.canOcclude()) return Shapes.block();
		BlockEntity te = world.getBlockEntity(pos);
		if (te instanceof Grid)
			return new VoxelShape4x4x4(((Grid)te).opaque).shape();
		return Shapes.empty();
	}

}
