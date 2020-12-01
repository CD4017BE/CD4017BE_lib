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
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
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
		Function<T, NBTBase> enc, Function<NBTBase, T> dec,
		BiConsumer<T, NBTBase> idec, BinObjEnc<T> bin
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

	public final Function<T, NBTBase> encode;
	public final Function<NBTBase, T> decode;
	public final BiConsumer<T, NBTBase> idec;
	public final BinObjEnc<T> binary;

	private Encoders(
		Function<T, NBTBase> encode,
		Function<NBTBase, T> decode,
		BiConsumer<T, NBTBase> idec,
		BinObjEnc<T> binary
	) {
		this.encode = encode;
		this.decode = decode;
		this.idec = idec;
		this.binary = binary;
	}

	<C> Pair<Function<C, NBTBase>, BiConsumer<C, NBTBase>> compose(
		Function<C, T> get, BiConsumer<C, T> set
	) {
		BiConsumer<C, NBTBase> write;
		if (set != null && decode != null) {
			final Function<NBTBase, T> decode = this.decode;
			write = (o, nbt) -> set.accept(o, decode.apply(nbt));
		} else if (idec != null) {
			BiConsumer<T, NBTBase> idec = this.idec;
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
		void encode(T val, PacketBuffer pkt);
		
		T decode(T old, PacketBuffer pkt) throws IOException;
		default void update(T old, PacketBuffer pkt) throws IOException {
			decode(old, pkt);
		}
	}

	static {
		register(NBTTagCompound.class,
			val -> val,
			nbt -> nbt instanceof NBTTagCompound ? (NBTTagCompound)nbt : new NBTTagCompound(),
			null, new BinObjEnc<NBTTagCompound>() {
				@Override public void encode(NBTTagCompound val, PacketBuffer pkt)
				{pkt.writeCompoundTag(val);}
				@Override public NBTTagCompound decode(NBTTagCompound old, PacketBuffer pkt) throws IOException
				{return pkt.readCompoundTag();}
			}
		);
		register(NBTTagList.class,
			val -> val,
			nbt -> nbt instanceof NBTTagList ? (NBTTagList)nbt : new NBTTagList(),
			null, null
		);
		register(String.class,
			NBTTagString::new,
			nbt -> nbt instanceof NBTTagString ? ((NBTTagString)nbt).getString() : "",
			null, new BinObjEnc<String>() {
				@Override public void encode(String val, PacketBuffer pkt)
				{pkt.writeString(val);}
				@Override public String decode(String old, PacketBuffer pkt)
				{return pkt.readString(1024);}
			}
		);
		register(ItemStack.class,
			val -> val.isEmpty() ? null : val.writeToNBT(new NBTTagCompound()),
			nbt -> nbt instanceof NBTTagCompound ? new ItemStack((NBTTagCompound)nbt) : ItemStack.EMPTY,
			null, new BinObjEnc<ItemStack>() {
				@Override public void encode(ItemStack val, PacketBuffer pkt)
				{pkt.writeItemStack(val);}
				@Override public ItemStack decode(ItemStack old, PacketBuffer pkt) throws IOException
				{return pkt.readItemStack();}
			}
		);
		register(FluidStack.class,
			val -> val == null ? null : val.writeToNBT(new NBTTagCompound()),
			nbt -> nbt instanceof NBTTagCompound ? FluidStack.loadFluidStackFromNBT((NBTTagCompound)nbt) : null,
			null, new BinObjEnc<FluidStack>() {
				@Override public void encode(FluidStack val, PacketBuffer pkt)
				{ItemFluidUtil.writeFluidStack(pkt, val);}
				@Override public FluidStack decode(FluidStack old, PacketBuffer pkt) throws IOException
				{return ItemFluidUtil.readFluidStack(pkt);}
			}
		);
		register(BlockPos.class,
			val -> val == null ? null : new NBTTagLong(val.toLong()),
			nbt -> nbt instanceof NBTTagLong ? BlockPos.fromLong(((NBTTagLong)nbt).getLong()) : null,
			null, new BinObjEnc<BlockPos>() {
				@Override public void encode(BlockPos val, PacketBuffer pkt)
				{pkt.writeBlockPos(val != null ? val : Utils.NOWHERE);}
				@Override public BlockPos decode(BlockPos old, PacketBuffer pkt) throws IOException
				{return pkt.readBlockPos();}
			}
		);
		register(byte[].class,
			NBTTagByteArray::new,
			nbt -> nbt instanceof NBTTagByteArray ? ((NBTTagByteArray)nbt).getByteArray() : new byte[0],
			(val, nbt)-> relaxedArrayCopy(val, nbt instanceof NBTTagByteArray ? ((NBTTagByteArray)nbt).getByteArray() : null),
			new BinObjEnc<byte[]>() {
				@Override public void encode(byte[] val, PacketBuffer pkt)
				{pkt.writeByteArray(val);}
				@Override public byte[] decode(byte[] old, PacketBuffer pkt) throws IOException 
				{return pkt.readByteArray();}
			}
		);
		register(int[].class,
			NBTTagIntArray::new,
			nbt -> nbt instanceof NBTTagIntArray ? ((NBTTagIntArray)nbt).getIntArray() : new int[0],
			(val, nbt)-> relaxedArrayCopy(val, nbt instanceof NBTTagIntArray ? ((NBTTagIntArray)nbt).getIntArray() : null),
			new BinObjEnc<int[]>() {
				@Override public void encode(int[] val, PacketBuffer pkt)
				{pkt.writeVarIntArray(val);}
				@Override public int[] decode(int[] old, PacketBuffer pkt) throws IOException 
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