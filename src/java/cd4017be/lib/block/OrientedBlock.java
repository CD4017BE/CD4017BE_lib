package cd4017be.lib.block;

import cd4017be.lib.property.PropertyOrientation;
import cd4017be.lib.util.Orientation;
import net.minecraft.state.StateContainer.Builder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.*;

/**
 * 
 * @author CD4017BE
 */
public class OrientedBlock<T extends TileEntity> extends BlockTE<T> {

	public final PropertyOrientation orientProp;
	VoxelShape[] shapes;

	/**
	 * @param prop orientation type
	 */
	public OrientedBlock(Properties p, int flags, PropertyOrientation prop) {
		super(p, flags);
		this.orientProp = prop;
	}

	@Override
	protected void fillStateContainer(Builder<Block, BlockState> builder) {
		super.fillStateContainer(builder);
		//FIXME field not initialized in time
		builder.add(orientProp);
	}

	@Override
	public BlockState getStateForPlacement(BlockItemUseContext context) {
		return getDefaultState().with(orientProp, orientProp.getPlacementState(context, handlerFlags >> 24 & 3));
	}

	@Override
	public VoxelShape
	getShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext context) {
		return shapes != null ? shapes[state.get(orientProp).ordinal()]
			: super.getShape(state, world, pos, context);
	}

	public OrientedBlock<T> setShape(VoxelShape main) {
		shapes = new VoxelShape[16];
		for (Orientation o : orientProp.getAllowedValues())
			shapes[o.ordinal()] = o.apply(main);
		return this;
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rot) {
		if (rot == Rotation.NONE) return state;
		int i = state.get(orientProp).ordinal();
		Orientation o = Orientation.values()[i & 12 | rot.rotate(i & 3, 4)];
		return orientProp.getAllowedValues().contains(o) ? state.with(orientProp, o) : state;
	}

	@Override
	public BlockState mirror(BlockState state, Mirror mirror) {
		if (mirror == Mirror.NONE) return state;
		int i = state.get(orientProp).ordinal();
		Orientation o = Orientation.values()[i & 12 | mirror.mirrorRotation(i & 3, 4)];
		return orientProp.getAllowedValues().contains(o) ? state.with(orientProp, o) : state;
	}

}
