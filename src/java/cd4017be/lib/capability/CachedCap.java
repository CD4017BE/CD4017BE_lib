package cd4017be.lib.capability;

import static cd4017be.lib.util.Utils.getTileAt;

import java.util.function.Supplier;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullConsumer;

public class CachedCap<T> implements NonNullConsumer<LazyOptional<T>>, Supplier<T> {

	protected final World world;
	protected final BlockPos pos;
	protected final Direction side;
	protected final Capability<T> cap;
	protected final T fallback;
	protected T value;

	public CachedCap(World world, BlockPos pos, Direction side, Capability<T> cap, T fallback) {
		this.fallback = fallback;
		this.cap = cap;
		this.world = world;
		this.pos = pos;
		this.side = side;
		this.value = fallback;
	}

	public void update() {
		if (value == fallback)
			accept(null); //par not used so no problem with null
	}

	@Override
	public void accept(LazyOptional<T> t) {
		TileEntity te = getTileAt(world, pos);
		if (te != null) {
			LazyOptional<T> lo = te.getCapability(cap, side);
			value = lo.orElse(fallback);
			if (value != fallback)
				lo.addListener(this);
		} else value = fallback;
	}

	@Override
	public T get() {
		return value;
	}

}
