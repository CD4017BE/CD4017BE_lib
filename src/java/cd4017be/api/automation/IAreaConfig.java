package cd4017be.api.automation;

import net.minecraftforge.common.ForgeChunkManager.Ticket;

/**
 * 
 * @author CD4017BE
 */
public interface IAreaConfig {

	public int[] getPosition();
	public byte getProtectLvlFor(String name, int cx, int cz);
	public boolean isChunkProtected(int cx, int cz);
	public int[][] getProtectedChunks(String name);
	public void setTicket(Ticket t);
	public Ticket getTicket();
	public boolean isChunkLoaded(int cx, int cz);

}
