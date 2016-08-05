package ordt.output.systemverilog.io;

import java.util.List;

import ordt.output.systemverilog.SystemVerilogBuilder;

public class SystemVerilogIOSignal extends SystemVerilogIOElement {
	protected int lowIndex;
	protected int size;

	public SystemVerilogIOSignal(Integer from, Integer to, String tagPrefix, String name, int lowIndex, int size) {   
		this.name = name;
		this.tagPrefix = tagPrefix;
		this.lowIndex = lowIndex;
		this.size = size;
		this.from = from;
		this.to = to;
		this.repCount = 1;  // signal repcount is always 1
	}
	
	// ----- 
	
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

	/** return a simple IOSignal with full generated name for this element */
	@Override
	public SystemVerilogIOSignal getIOSignal(String pathPrefix, boolean addTagPrefix) {
		String newTagPrefix = addTagPrefix? tagPrefix : "";
		return new SystemVerilogIOSignal(from, to, newTagPrefix, pathPrefix + name, lowIndex, size);
	}

}
