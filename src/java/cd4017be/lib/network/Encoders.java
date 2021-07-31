package cd4017be.lib.network;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.apache.commons.lang3.tuple.Pair;
import cd4017be.lib.util.ItemFluidUtil;
import cd4017be.lib.util.Utils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.fluids.FluidStack;

public class Encoders<T> {

	private static final HashMap<Class<?>, Encoders<?>> ENCODERS = new HashMap<>();

	/**register encoding and decoding over NBT for the given object type.
	 * @param <T> type to register
	 * @param c class of type
	 * @param enc encoder function: {@code val -> val.serializeNBT()}
	 * @param dec default decoder function: {@code nbt -> new T(nbt)}
	 * @param idec in-place decoder function, used for final fields or if <b>dec</b> == null:
	 * {@code (state, nbt)-> state.deserializeNBT(nbt)} */
	public static <T> void register(Class<T> c,
		Function<T, Tag> enc, Function<Tag, T> dec,
		BiConsumer<T, Tag> idec, BinObjEnc<T> bin
	) {
		ENCODERS.put(c, new Encoders<T>(enc, dec, idec, bin));
	}

	@SuppressWarnings("unchecked")
	public static <T> Encoders<T> of(Class<T> c) {
		Encoders<T> enc = (Encoders<T>)ENCODERS.get(c);
		if (enc == null && INBTSerializable.class.isAssignableFrom(c))
			enc = (Encoders<T>)ENCODERS.get(INBTSerializable.class);
		return enc;
	}

	public final Function<T, Tag> encode;
	public final Function<Tag, T> decode;
	public final BiConsumer<T, Tag> idec;
	public final BinObjEnc<T> binary;

	private Encoders(
		Function<T, Tag> encode,
		Function<Tag, T> decode,
		BiConsumer<T, Tag> idec,
		BinObjEnc<T> binary
	) {
		this.encode = encode;
		this.decode = decode;
		this.idec = idec;
		this.binary = binary;
	}

	<C> Pair<Function<C, Tag>, BiConsumer<C, Tag>> compose(
		Function<C, T> get, BiConsumer<C, T> set
	) {
		BiConsumer<C, Tag> write;
		if (set != null && decode != null) {
			final Function<Tag, T> decode = this.decode;
			write = (o, nbt) -> set.accept(o, decode.apply(nbt));
		} else if (idec != null) {
			BiConsumer<T, Tag> idec = this.idec;
			write = (o, nbt) -> idec.accept(get.apply(o), nbt);
		} else write = null;
		return Pair.of(encode == null ? null : encode.compose(get), write);
	}


	public interface BinObjEnc<T> {
		default boolean hasChanged(T val, Object[] states, int i) {
			if (Objects.equals(val, states[i])) return false;
			states[i] = val;
			return true;
		}
		void encode(T val, FriendlyByteBuf pkt);
		
		T decode(T old, FriendlyByteBuf pkt) throws IOException;
		default void update(T old, FriendlyByteBuf pkt) throws IOException {
			decode(old, pkt);
		}
	}

	static {
		register(CompoundTag.class,
			val -> val,
			nbt -> nbt instanceof CompoundTag ? (CompoundTag)nbt : new CompoundTag(),
			null, new BinObjEnc<CompoundTag>() {
				@Override public void encode(CompoundTag val, FriendlyByteBuf pkt)
				{pkt.writeNbt(val);}
				@Override public CompoundTag decode(CompoundTag old, FriendlyByteBuf pkt) throws IOException
				{return pkt.readNbt();}
			}
		);
		register(ListTag.class,
			val -> val,
			nbt -> nbt instanceof ListTag ? (ListTag)nbt : new ListTag(),
			null, null
		);
		register(String.class,
			StringTag::valueOf,
			nbt -> nbt instanceof StringTag ? ((StringTag)nbt).getAsString() : "",
			null, new BinObjEnc<String>() {
				@Override public void encode(String val, FriendlyByteBuf pkt)
				{pkt.writeUtf(val);}
				@Override public String decode(String old, FriendlyByteBuf pkt)
				{return pkt.readUtf(1024);}
			}
		);
		register(ItemStack.class,
			val -> val.isEmpty() ? null : val.save(new CompoundTag()),
			nbt -> nbt instanceof CompoundTag ? ItemStack.of((CompoundTag)nbt) : ItemStack.EMPTY,
			null, new BinObjEnc<ItemStack>() {
				@Override public void encode(ItemStack val, FriendlyByteBuf pkt)
				{pkt.writeItem(val);}
				@Override public ItemStack decode(ItemStack old, FriendlyByteBuf pkt) throws IOException
				{return pkt.readItem();}
			}
		);
		register(FluidStack.class,
			val -> val == null ? null : val.writeToNBT(new CompoundTag()),
			nbt -> nbt instanceof CompoundTag ? FluidStack.loadFluidStackFromNBT((CompoundTag)nbt) : null,
			null, new BinObjEnc<FluidStack>() {
				@Override public void encode(FluidStack val, FriendlyByteBuf pkt)
				{ItemFluidUtil.writeFluidStack(pkt, val);}
				@Override public FluidStack decode(FluidStack old, FriendlyByteBuf pkt) throws IOException
				{return ItemFluidUtil.readFluidStack(pkt);}
			}
		);
		register(BlockPos.class,
			val -> val == null ? null : LongTag.valueOf(val.asLong()),
			nbt -> nbt instanceof LongTag ? BlockPos.of(((LongTag)nbt).getAsLong()) : null,
			null, new BinObjEnc<BlockPos>() {
				@Override public void encode(BlockPos val, FriendlyByteBuf pkt)
				{pkt.writeBlockPos(val != null ? val : Utils.NOWHERE);}
				@Override public BlockPos decode(BlockPos old, FriendlyByteBuf pkt) throws IOException
				{return pkt.readBlockPos();}
			}
		);
		register(byte[].class,
			ByteArrayTag::new,
			nbt -> nbt instanceof ByteArrayTag ? ((ByteArrayTag)nbt).getAsByteArray() : new byte[0],
			(val, nbt)-> relaxedArrayCopy(val, nbt instanceof ByteArrayTag ? ((ByteArrayTag)nbt).getAsByteArray() : null),
			new BinObjEnc<byte[]>() {
				@Override public void encode(byte[] val, FriendlyByteBuf pkt)
				{pkt.writeByteArray(val);}
				@Override public byte[] decode(byte[] old, FriendlyByteBuf pkt) throws IOException 
				{return pkt.readByteArray();}
			}
		);
		register(int[].class,
			IntArrayTag::new,
			nbt -> nbt instanceof IntArrayTag ? ((IntArrayTag)nbt).getAsIntArray() : new int[0],
			(val, nbt)-> relaxedArrayCopy(val, nbt instanceof IntArrayTag ? ((IntArrayTag)nbt).getAsIntArray() : null),
			new BinObjEnc<int[]>() {
				@Override public void encode(int[] val, FriendlyByteBuf pkt)
				{pkt.writeVarIntArray(val);}
				@Override public int[] decode(int[] old, FriendlyByteBuf pkt) throws IOException 
				{return pkt.readVarIntArray();}
			}
		);
	}

	public static void relaxedArrayCopy(Object to, Object from) {
		int l1 = Array.getLength(to),
			l0 = from == null ? 0 : Math.min(l1, Array.getLength(from));
		if (l0 > 0) System.arraycopy(from, 0, to, 0, l0);
	}

}