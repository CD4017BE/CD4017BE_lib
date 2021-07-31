package cd4017be.lib.network;

import java.util.BitSet;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;

/** 
 * {@link BitSet}
 * @author CD4017BE */
public class SyncBitset {
	/**total number of objects */
	public final int count;
	/**number of bytes used to encode the modification set */
	private final int modSetBytes;
	/**number of bits used to encode each modified index */
	private final int idxBits;
	/**number of bits used to encode how many objects were modified */
	private final int idxCountBits;
	/**threshold above which to encode the full set rather than individual indices */
	private final int maxIdxCount;

	public BitSet set;

	public SyncBitset(int count) {
		this.set = new BitSet(count + 1);
		this.count = count;
		if (count < 8) {
			this.modSetBytes = 1;
			this.maxIdxCount = 0;
			this.idxBits = 0;
			this.idxCountBits = 0;
		} else {
			int m = (this.modSetBytes = count / 8 + 1) * 8 - 9;
			int i = this.idxBits = 32 - Integer.numberOfLeadingZeros(count - 1);
			int j = 32 - Integer.numberOfLeadingZeros(m / i);
			j = (m - j) / i;
			this.idxCountBits = 32 - Integer.numberOfLeadingZeros(j);
			this.maxIdxCount = j;
		}
	}

	public BitSet clear() {
		set.clear();
		return set;
	}

	public boolean isEmpty() {
		return set.isEmpty();
	}

	/**
	 * encode a data packet containing the resolved changes.
	 * @return the synchronization packet to be send to {@link StateSyncClient#decodePacket(FriendlyByteBuf)} or null if there are no changes to send
	 */
	public boolean write(ByteBuf buf) {
		BitSet chng = set;
		int cc = chng.cardinality();
		if (cc == 0) return false;
		if (cc > maxIdxCount) {
			int p = buf.writerIndex() + modSetBytes;
			buf.writeBytes(chng.toByteArray());
			while(buf.writerIndex() < p) buf.writeByte(0);
		} else {
			int b = idxBits;
			int j = 1 + idxCountBits;
			int v = 1 | cc << 1;
			for (int i = chng.nextSetBit(1); i >= 0; i = chng.nextSetBit(i + 1)) {
				v |= (i - 1) << j;
				if ((j += b) >= 16) {
					buf.writeShortLE(v);
					v >>>= 16; j -= 16;
				}
			}
			if (j > 8) buf.writeShortLE(v);
			else if (j > 0) buf.writeByte(v);
		}
		return true;
	}

	/**
	 * decode a synchronization packet to start reading values.<br>
	 * The result is stored in {@link #buffer}, with all fixed sized elements first, followed by the variable sized elements.
	 * @param buf packet data
	 * @return this
	 */
	public BitSet read(ByteBuf buf) {
		int p = buf.readerIndex();
		if ((buf.getByte(p) & 1) == 0) {
			byte[] arr = new byte[modSetBytes];
			buf.readBytes(arr);
			return set = BitSet.valueOf(arr);
		} else {
			int j = idxCountBits + 1, b = idxBits, mask = 0xffff >> (16 - b);
			int v = j <= 8 ? buf.readUnsignedByte() : buf.readUnsignedShortLE();
			int cc = (v & 0xffff >> (16 - j)) >> 1;
			BitSet chng = set;
			chng.clear();
			v >>= j; j = -j & 7;
			for (int i = 0; i < cc; i++) {
				if (j < b)
					if(j < b - 8) {
						v |= buf.readUnsignedShortLE() << j;
						j += 16;
					} else {
						v |= buf.readUnsignedByte() << j;
						j += 8;
					}
				chng.set((v & mask) + 1);
				v >>= b;
				j -= b;
			}
			return chng;
		}
	}

}
