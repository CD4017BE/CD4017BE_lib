package cd4017be.api.rs_ctr.com;

import java.lang.ref.WeakReference;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.capabilities.Capability;

/**
 * Communication object that allows devices to remotely interact with dynamically selected blocks in the world.
 * @author CD4017BE
 */
public class BlockReference {

	private static final WeakReference<World> NULL = new WeakReference<>(null);

	private WeakReference<World> world;
	public final int dim;
	public final BlockPos pos;
	public final EnumFacing face;

	public BlockReference(NBTTagCompound nbt) {
		this.world = NULL;
		this.pos = new BlockPos(nbt.getInteger("x"), nbt.getInteger("y"), nbt.getInteger("z"));
		this.face = EnumFacing.VALUES[(nbt.getByte("f") & 0xff) % 6];
		this.dim = nbt.getInteger("d");
	}

	public BlockReference(World world, BlockPos pos, EnumFacing face) {
		this.world = new WeakReference<>(world);
		this.pos = pos;
		this.face = face;
		this.dim = world.provider.getDimension();
	}

	/**
	 * Note: this also refreshes the world reference used for the other methods in this class
	 * so they can be safely used afterwards within the same call stack on the main server thread
	 * (dimensions have no chance to unload in the meantime).
	 * @return whether currently any actions can be performed on this block.
	 */
	public boolean isLoaded() {
		World world = this.world.get();
		if (world == null)
			if ((world = DimensionManager.getWorld(dim)) != null)
				this.world = new WeakReference<World>(world);
			else return false;
		return world.isBlockLoaded(pos);
	}

	public World world() {
		return world.get();
	}

	/**
	 * @return this block's state
	 */
	public IBlockState getState() {
		return world().getBlockState(pos);
	}

	/**
	 * @return the TileEntity of this block
	 */
	public TileEntity getTileEntity() {
		return world().getTileEntity(pos);
	}

	/**
	 * @param <C>
	 * @param cap the capability to obtain
	 * @return an instance of the given capability or null if not available
	 */
	public @Nullable <C> C getCapability(Capability<C> cap) {
		TileEntity te = getTileEntity();
		if (te == null) return null;
		return te.getCapability(cap, face);
	}

	public NBTTagCompound serializeNBT() {
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setByte("f", (byte)face.ordinal());
		nbt.setInteger("x", pos.getX());
		nbt.setInteger("y", pos.getY());
		nbt.setInteger("z", pos.getZ());
		nbt.setInteger("d", dim);
		return nbt;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof BlockReference && equal(this, (BlockReference)obj);
	}

	public static boolean equal(BlockReference a, BlockReference b) {
		if (a == b) return true;
		if (a == null || b == null) return false;
		return a.pos.equals(b.pos) && a.face == b.face && a.dim == b.dim;
	}

	/**
	 * The callback interface for transmitting BlockReferences.
	 */
	@FunctionalInterface
	public interface BlockHandler {

		/**
		 * called when the BlockReference changes
		 * @param ref the new BlockReference
		 */
		void updateBlock(BlockReference ref);

	}

}
