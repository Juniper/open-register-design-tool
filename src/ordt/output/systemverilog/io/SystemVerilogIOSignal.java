package ordt.output.systemverilog.io;

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
		this.reps = 1;  // signal rep count is always 1
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

    /** return type of this signal - always "logic" */
	@Override
	public String getType() {
		return "logic";
	}

	/** return sv string instancing this element - assumes element name is full instance name */
	@Override
	public String getInstanceString() {
		return getType() + " " + getFullName() + getDefArray();
	}

	/** return a simple IOElement with full generated name */
	@Override
	public SystemVerilogIOElement getFullNameIOElement(String pathPrefix, boolean addTagPrefix) {
		String newTagPrefix = addTagPrefix? tagPrefix : "";
		return new SystemVerilogIOSignal(from, to, newTagPrefix, pathPrefix + name, lowIndex, size);
	}

}
