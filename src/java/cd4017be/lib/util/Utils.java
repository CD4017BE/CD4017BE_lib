package cd4017be.lib.util;

import java.text.DecimalFormatSymbols;
import java.util.HashSet;
import java.util.Set;
import java.util.function.ObjIntConsumer;

import javax.annotation.Nullable;

import cd4017be.api.circuits.IQuickRedstoneHandler;
import cd4017be.lib.ModTileEntity;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.oredict.OreDictionary;

/**
 *
 * @author CD4017BE
 */
public class Utils {

	public static final BlockPos NOWHERE = new BlockPos(0, -1, 0);
	public static final byte IN = -1, OUT = 1, ACC = 0;

	/**@deprecated use {@link ItemHandlerHelper.canItemStacksStack}*/
	@Deprecated
	public static boolean itemsEqual(ItemStack item0, ItemStack item1)
	{
		return (item0 == null && item1 == null) || (item0 != null && item1 != null && item0.isItemEqual(item1) && ItemStack.areItemStackTagsEqual(item0, item1));
	}

	/**@deprecated better compare to OreDictionary entry directly */
	@Deprecated
	public static boolean oresEqual(ItemStack item0, ItemStack item1)
	{
		if (itemsEqual(item0, item1)) return true;
		else
		{
			int[] ids = OreDictionary.getOreIDs(item0);
			for (int id1 : OreDictionary.getOreIDs(item1))
				for (int id0 : ids) if (id0 == id1) return true;
			return false;
		}
	}

	/**
	 * Get all slots of the inventory that can be accessed from given side
	 * @param inv the inventory to access
	 * @param side the side to access from
	 * @return the accessible slots
	 * @deprecated use capabilities
	 */
	@Deprecated
	public static int[] accessibleSlots(IInventory inv, int side) {
		if (inv instanceof ISidedInventory) {
			return ((ISidedInventory) inv).getSlotsForFace(EnumFacing.VALUES[side]);
		} else {
			int[] s = new int[inv.getSizeInventory()];
			for (int i = 0; i < s.length; i++) {
				s[i] = i;
			}
			return s;
		}
	}

	/**
	 * performs the given operation for all blocks in a square ring.
	 * @param center center of the ring
	 * @param axis the axis around which the ring forms
	 * @param radius radius of the ring in blocks
	 * @param operation performed operation supplied with the block's position and its square distance to the center
	 */
	public static void forRing(BlockPos center, Axis axis, int radius, ObjIntConsumer<BlockPos> operation) {
		int x = center.getX(), y = center.getY(), z = center.getZ();
		switch(axis) {
		case X:
			for (int i = -radius; i <= radius; i++) {
				int d = radius * radius + i * i;
				operation.accept(new BlockPos(x, y - radius, z + i), d);
				operation.accept(new BlockPos(x, y + radius, z + i), d);
				operation.accept(new BlockPos(x, y + i, z - radius), d);
				operation.accept(new BlockPos(x, y + i, z + radius), d);
			}
		case Y:
			for (int i = -radius; i <= radius; i++) {
				int d = radius * radius + i * i;
				operation.accept(new BlockPos(x - radius, y, z + i), d);
				operation.accept(new BlockPos(x + radius, y, z + i), d);
				operation.accept(new BlockPos(x + i, y, z - radius), d);
				operation.accept(new BlockPos(x + i, y, z + radius), d);
			}
		case Z:
			for (int i = -radius; i <= radius; i++) {
				int d = radius * radius + i * i;
				operation.accept(new BlockPos(x - radius, y + i, z), d);
				operation.accept(new BlockPos(x + radius, y + i, z), d);
				operation.accept(new BlockPos(x + i, y - radius, z), d);
				operation.accept(new BlockPos(x + i, y + radius, z), d);
			}
		}
	}

	/**
	 * Chunk-flushing save way to get a neighboring TileEntity
	 * @param tile source tile
	 * @param side side of source tile to get neighbor for
	 * @return neighbor TileEntity or null if not existing or chunk not loaded
	 */
	public static @Nullable TileEntity neighborTile(TileEntity tile, EnumFacing side) {
		World world = tile.getWorld();
		BlockPos pos = tile.getPos().offset(side);
		return world.isBlockLoaded(pos) ? world.getTileEntity(pos) : null;
	}

	/**
	 * Chunk-flushing save way to get a TileEntity for given position
	 * @param world World
	 * @param pos tile position
	 * @return the TileEntity or null if not existing or chunk not loaded
	 */
	public static @Nullable TileEntity getTileAt(World world, BlockPos pos) {
		return world.isBlockLoaded(pos) ? world.getTileEntity(pos) : null;
	}

	/**
	 * Chunk-flushing save way to get a capability from a neighboring block
	 * @param tile source tile
	 * @param side side of source tile to get neighboring capability for
	 * @param cap the capability type
	 * @return capability instance or null if chunk not loaded, no TileEntity or capability not available
	 */
	public static @Nullable <T> T neighborCapability(TileEntity tile, EnumFacing side, Capability<T> cap) {
		World world = tile.getWorld();
		BlockPos pos = tile.getPos().offset(side);
		if (!world.isBlockLoaded(pos)) return null;
		TileEntity te = world.getTileEntity(pos);
		return te != null ? te.getCapability(cap, side.getOpposite()) : null;
	}

	/**
	 * Chunk-flushing save way to get a capability for given position and side
	 * @param world World
	 * @param pos tile position
	 * @param side side to access
	 * @param cap the capability type
	 * @return capability instance or null if chunk not loaded, no TileEntity or capability not available
	 */
	public static @Nullable <T> T getCapabilityAt(World world, BlockPos pos, @Nullable EnumFacing side, Capability<T> cap) {
		if (!world.isBlockLoaded(pos)) return null;
		TileEntity te = world.getTileEntity(pos);
		return te != null ? te.getCapability(cap, side) : null; 
	}

	/**
	 * checks to which block side the given position belongs
	 * @param X block relative x
	 * @param Y block relative y
	 * @param Z block relative z
	 * @return side
	 */
	public static EnumFacing hitSide(float X, float Y, float Z) {
		float dx = Math.abs(X -= 0.5F);
		float dy = Math.abs(Y -= 0.5F);
		float dz = Math.abs(Z -= 0.5F);
		return dy > dz && dy > dx ?
				Y < 0 ? EnumFacing.DOWN : EnumFacing.UP
			: dz > dx ?
				Z < 0 ? EnumFacing.NORTH : EnumFacing.SOUTH
			: X < 0 ? EnumFacing.WEST : EnumFacing.EAST;
	}

	public static RayTraceResult getHit(EntityPlayer player, IBlockState block, BlockPos pos) {
		Vec3d p = player.getPositionEyes(1);
		return block.collisionRayTrace(player.world, pos, p, p.add(player.getLook(1).scale(16)));
	}

	/**@deprecated use {@link neighborTile} */
	@Deprecated
	public static TileEntity getTileOnSide(ModTileEntity tileEntity, byte s) {
		if (tileEntity == null) return null;
		return tileEntity.getLoadedTile(tileEntity.getPos().offset(EnumFacing.VALUES[s]));
	}
	
	public static class ItemType {
		public final ItemStack[] types;
		public final boolean meta;
		public final boolean nbt;
		public final int[] ores;
		/**
		 * An ItemType that matches all items
		 */
		public ItemType() 
		{
			this.types = null;
			this.ores = null;
			this.meta = false;
			this.nbt = false;
		}
		/**
		 * An ItemType that matches only the exact given items
		 * @param types the items to match
		 */
		public ItemType(ItemStack... types)
		{
			this.types = types;
			this.ores = null;
			this.meta = true;
			this.nbt = true;
		}
		/**
		 * This ItemType matches the given items with special flags
		 * @param meta Metadata flag (false = ignore different metadata)
		 * @param nbt NBT-data flag (false = ignore different NBT-data)
		 * @param ore OreDictionary flag (true = also matches if equal ore types)
		 * @param types the items to match
		 */
		public ItemType(boolean meta, boolean nbt, boolean ore, ItemStack... types)
		{
			this.types = types;
			this.meta = meta;
			this.nbt = nbt;
			if (ore) {
				Set<Integer> list = new HashSet<Integer>();
				for (int i = 0; i < types.length; i++)
					for (int j : OreDictionary.getOreIDs(types[i])) 
						list.add(j);
				ores = new int[list.size()];
				int n = 0;
				for (int i : list) ores[n++] = i;
			} else ores = null;
		}
		
		public boolean matches(ItemStack item) 
		{
			if (item == null) return false;
			else if (types == null) return true;
			for (ItemStack type : types) {
				if (type == null) continue;
				if (item.getItem() == type.getItem() && 
					(!meta || item.getItemDamage() == type.getItemDamage()) &&
					(!nbt || ItemStack.areItemStackTagsEqual(item, type)))
					return true;
			}
			if (ores == null) return false;
			for (int o : OreDictionary.getOreIDs(item))
				for (int i : ores)
					if (i == o) return true;
			return false;
		}
		
		public int getMatch(ItemStack item)
		{
			if (item == null) return -1;
			else if (types == null) return -1;
			for (int i = 0; i < types.length; i++) {
				ItemStack type = types[i];
				if (type == null) continue;
				if (item.getItem() == type.getItem() && 
					(!meta || item.getItemDamage() == type.getItemDamage()) &&
					(!nbt || ItemStack.areItemStackTagsEqual(item, type)))
					return i;
			}
			if (ores == null) return -1;
			for (int o : OreDictionary.getOreIDs(item))
				for (int i = 0; i < ores.length; i++)
					if (ores[i] == o) return i;
			return -1;
		}
		
	}
	
	private static final String[] DecScale  = {"a", "f", "p", "n", "u", "m", "", "k", "M", "G", "T", "P", "E"};
	private static final int ofsDecScale = 6;
	
	/**
	 * @param x number
	 * @param w significant digits
	 * @param c clip below exponent of 10
	 * @return formatted number
	 */
	public static String formatNumber(double x, int w, int c)
	{
		double s = Math.signum(x);
		if (x == 0 || Double.isNaN(x) || Double.isInfinite(x)) return "" + x;
		int o = (int)Math.floor(Math.log10(x * s)) + 3 * ofsDecScale;
		int p = (o + c) / 3;
		int n = w - o + p * 3 - 1;
		if (p < 0) return "0";
		else if (p > DecScale.length) return "" + (s == -1 ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY);
		x *= Math.pow(0.001, p - ofsDecScale);
		String tex = String.format("%." + n + "f", x);
		String ds = "" + DecimalFormatSymbols.getInstance().getDecimalSeparator();
		if (tex.contains(ds)) {
			while(tex.endsWith("0")) tex = tex.substring(0, tex.length() - 1);
			if (tex.endsWith(ds)) tex = tex.substring(0, tex.length() - 1);
		}
		return tex + DecScale[p];
	}
	
	/**
	 * @param x number
	 * @param w max fractal digits
	 * @return formatted number
	 */
	public static String formatNumber(double x, int w) {
		String tex = String.format("%." + w + "f", x);
		String ds = "" + DecimalFormatSymbols.getInstance().getDecimalSeparator();
		if (tex.contains(ds)) {
			while(tex.endsWith("0")) tex = tex.substring(0, tex.length() - 1);
			if (tex.endsWith(ds)) tex = tex.substring(0, tex.length() - 1);
		}
		return tex;
	}
	
	public static FluidStack getFluid(World world, BlockPos pos, boolean sourceOnly)
	{
		IBlockState state = world.getBlockState(pos);
		Block block = state.getBlock();
		if (block == Blocks.AIR) return null;
		else if (block instanceof IFluidBlock) {
			FluidStack fluid = ((IFluidBlock)block).drain(world, pos, false);
			if (!sourceOnly && fluid == null) return new FluidStack(((IFluidBlock)block).getFluid(), 0);
			else return fluid;
		}
		boolean source = state == block.getDefaultState();
		if (block == Blocks.WATER || block == Blocks.FLOWING_WATER) return source || !sourceOnly ? new FluidStack(FluidRegistry.WATER, source ? 1000 : 0) : null;
		else if (block == Blocks.LAVA|| block == Blocks.FLOWING_LAVA) return source || !sourceOnly ? new FluidStack(FluidRegistry.LAVA, source ? 1000 : 0) : null;
		else return null;
	}

	/**@deprecated use capabilities */
	@Deprecated
	public static int findStack(ItemStack item, IInventory inv, int[] s, int start)
	{
		if (item == null) return -1;
		for (int i = start; i < s.length; i++) {
			if (itemsEqual(item, inv.getStackInSlot(s[i]))) return i;
		}
		return -1;
	}
	
	public static int mod(int a, int b)
	{
		return a < 0 ? b - (-a - 1) % b - 1 : a % b;
	}
	
	public static int div(int a, int b)
	{
		return a < 0 ? -1 - (-a - 1) / b: a / b;
	}
	
	@Deprecated
	public static byte getLookDir(Entity entity)
	{
		if (entity.rotationPitch < -45.0F) return 0;
		if (entity.rotationPitch > 45.0F) return 1;
		int d = MathHelper.floor((double)(entity.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
		if (d == 0) return 2;
		if (d == 1) return 5;
		if (d == 2) return 3;
		return 4;
	}

	public static EnumFacing getLookDirStrict(Entity entity) {
		if (entity.rotationPitch < -45.0F) return EnumFacing.DOWN;
		if (entity.rotationPitch > 45.0F) return EnumFacing.UP;
		return entity.getHorizontalFacing();
	}

	public static EnumFacing getLookDirPlacement(Entity entity) {
		if (entity.rotationPitch < -35.0F) return EnumFacing.DOWN;
		if (entity.rotationPitch > 40.0F) return EnumFacing.UP;
		return entity.getHorizontalFacing();
	}

	/**
	 * @param pos position
	 * @param ref reference point
	 * @return the side of the reference point that faces towards the given position or null if both are equal or not direct neighbors of each other.
	 */
	public static EnumFacing getSide(BlockPos pos, BlockPos ref) {
		int dx = pos.getX() - ref.getX();
		int dy = pos.getY() - ref.getY();
		int dz = pos.getZ() - ref.getZ();
		if (dx != 0)
			if (dy != 0 || dz != 0) return null;
			else return dx == -1 ? EnumFacing.WEST : dx == 1 ? EnumFacing.EAST : null;
		else if (dy != 0)
			if (dz != 0) return null;
			else return dy == -1 ? EnumFacing.DOWN : dy == 1 ? EnumFacing.UP : null;
		else return dz == -1 ? EnumFacing.NORTH : dz == 1 ? EnumFacing.SOUTH : null;
	}

	/**
	 * @param pos
	 * @param axis
	 * @return the element of pos specified by axis
	 */
	public static int coord(BlockPos pos, Axis axis) {
		switch(axis) {
		case X: return pos.getX();
		case Y: return pos.getY();
		case Z: return pos.getZ();
		default: return 0;
		}
	}

	public static double coord(double x, double y, double z, EnumFacing d) {
		switch(d) {
		case DOWN: return -y;
		case UP: return y;
		case NORTH: return -z;
		case SOUTH: return z;
		case WEST: return -x;
		case EAST: return x;
		default: return 0;
		}
	}

	public static void updateRedstoneOnSide(TileEntity te, int value, EnumFacing side) {
		ICapabilityProvider cp = neighborTile(te, side);
		if (cp != null && cp instanceof IQuickRedstoneHandler) ((IQuickRedstoneHandler)cp).onRedstoneStateChange(side.getOpposite(), value, te);
		else te.getWorld().neighborChanged(te.getPos().offset(side), te.getBlockType(), te.getPos());
	}

	/**
	 * forward or backward cycle a number stored in some sub-bits of an integer
	 * @param cfg storage integer
	 * @param i start bit index
	 * @param m bit mask
	 * @param r number range
	 * @param incr true to increase, false to decrease
	 * @return edited storage integer
	 */
	public static int cycleState(int cfg, int i, int m, int r, boolean incr) {
		return cfg & ~(m << i) | ((cfg >> i & m) + (incr ? 1 : r - 1)) % r << i;
	}

	/**
	 * set a number stored in some sub-bits of an integer
	 * @param cfg storage integer
	 * @param i start bit index
	 * @param m bit mask
	 * @param x number to store
	 * @return edited storage integer
	 */
	public static int setState(int cfg, int i, int m, int x) {
		return cfg & ~(m << i) | (x & m) << i;
	}

	/**
	 * forward or backward cycle a number stored in some sub-bits of an long
	 * @param cfg storage long
	 * @param i start bit index
	 * @param m bit mask
	 * @param r number range
	 * @param incr true to increase, false to decrease
	 * @return edited storage long
	 */
	public static long cycleState(long cfg, int i, long m, int r, boolean incr) {
		return cfg & ~(m << i) | (long)(((int)(cfg >> i & m) + (incr ? 1 : r - 1)) % r) << i;
	}

	/**
	 * set a number stored in some sub-bits of an long
	 * @param cfg storage long
	 * @param i start bit index
	 * @param m bit mask
	 * @param x number to store
	 * @return edited storage long
	 */
	public static long setState(long cfg, int i, long m, int x) {
		return cfg & ~(m << i) | ((long)x & m) << i;
	}

}
