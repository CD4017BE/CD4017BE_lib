package cd4017be.api.circuits;

import java.util.ArrayList;
import java.util.HashMap;

import cd4017be.lib.templates.SharedNetwork;

/**
 * 
 * @author CD4017BE
 */
public class SharedInteger extends SharedNetwork<IntegerComp, SharedInteger> {

	private static final short AllIn = 0x5555, AllOut = (short)0xaaaa;
	public ArrayList<IntegerComp> inputs = new ArrayList<IntegerComp>();
	public ArrayList<IntegerComp> outputs = new ArrayList<IntegerComp>();
	public int outputState = 0;
	public boolean updateState;

	protected SharedInteger(HashMap<Long, IntegerComp> comps) {
		super(comps);
	}

	public SharedInteger(IntegerComp comp) {
		super(comp);
		if ((comp.rsIO & AllIn) != 0) inputs.add(comp);
		if ((comp.rsIO & AllOut) != 0) outputs.add(comp);
		outputState = comp.inputState;
	}

	@Override
	public void onMerged(SharedInteger network) {
		super.onMerged(network);
		updateState = true;
		inputs.addAll(network.inputs);
		outputs.addAll(network.outputs);
	}

	@Override
	public void remove(IntegerComp comp) {
		super.remove(comp);
		if ((comp.rsIO & AllIn) != 0) {
			inputs.remove(comp);
			updateState = true;
		}
		if ((comp.rsIO & AllOut) != 0) 
			outputs.remove(comp);
	}

	@Override
	public SharedInteger onSplit(HashMap<Long, IntegerComp> comps) {
		SharedInteger si = new SharedInteger(comps);
		for (IntegerComp c : comps.values()) {
			if ((c.rsIO & AllIn) != 0) {
				this.inputs.remove(c);
				si.inputs.add(c);
				si.updateState = updateState = true;
			}
			if ((c.rsIO & AllOut) != 0) {
				this.outputs.remove(c);
				si.outputs.add(c);
			}
		}
		return si;
	}

	public void setIO(IntegerComp c, short con) {
		int add = con & ~c.rsIO, rem = c.rsIO & ~con;
		if ((add & AllIn) != 0) inputs.add(c);
		else if ((rem & AllIn) != 0) inputs.remove(c);
		if ((add & AllOut) != 0) outputs.add(c);
		else if ((rem & AllOut) != 0) outputs.remove(c);
		c.rsIO = con;
	}

	@Override
	protected void updatePhysics() {
		if (updateState) { 
			int newState = 0;
			for (IntegerComp c : inputs) newState |= c.inputState;
			if (newState != outputState) {
				outputState = newState;
				for (IntegerComp c : outputs) c.onStateChange();
			}
			updateState = false;
		}
	}

}
