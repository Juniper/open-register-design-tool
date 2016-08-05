package ordt.output.systemverilog.io;

import java.util.List;

import ordt.output.systemverilog.SystemVerilogBuilder;

public class SystemVerilogIOSignal extends SystemVerilogIOElement {
	protected int lowIndex;
	protected int size;

	public SystemVerilogIOSignal(Integer from, Integer to, String namePrefix, String name, int lowIndex, int size) {   
		this.name = name;
		this.namePrefix = namePrefix;
		this.lowIndex = lowIndex;
		this.size = size;
		this.from = from;
		this.to = to;
		this.repCount = 1;  // signal repcount is always 1
	}
	
	// ----- SystemVerilogElement abstract methods
	/*
	/** return the name used for definitions (includes) prefixed array string *
	@Override
	public String getDefName(String prefix) {
		return getDefArray() + prefix + name;
	}

	@Override
	public List<SystemVerilogIOSignal> getIOSignals(Integer fromLoc, Integer toLoc) {
		// TODO Auto-generated method stub
		return null;
	}
	*/
	
	// ----- other methods
	/** get lowIndex for this io signal */
	public int getLowIndex() {
		return lowIndex;
	}

	/** get size in bits of this io signal */
	public int getSize() {
		return size;
	}
	
	/** return the array string used for definitions (includes) prefixed array string */
	public String getDefArray() {
		return SystemVerilogBuilder.genDefArrayString(lowIndex, size);
	}

    // ------------ methods overriding super

	@Override
	public String getInstanceString() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<SystemVerilogIOSignal> getIOSignals(Integer fromLoc, Integer toLoc) {
		// TODO Auto-generated method stub
		return null;
	}

}
