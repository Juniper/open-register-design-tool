package ordt.output.systemverilog.io;

import ordt.output.systemverilog.SystemVerilogSignal;

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
		return SystemVerilogSignal.genDefArrayString(lowIndex, size);
	}

    // ------------ methods overriding super

    /** return type of this signal - always "logic" */
	@Override
	public String getType() {
		return "logic";
	}

	/** return sv string instancing this element - assumes element name is full instance name */
	@Override
	public String getInstanceString(boolean addTagPrefix) {
        //System.out.println("SystemVerilogIOSignal getInstanceString: addTagPrefix=" + addTagPrefix + ", fullName=" + getFullName(null, addTagPrefix));
		return getType() + " " + getDefArray() + getFullName(null, addTagPrefix) + ";";
	}
	
	/** return sv string used in definition of this element in input/output lists - assumes element name is full instance name 
	 *   includes type if non-virtual sigset, name, and array *
	 *   @param addTagPrefix - if true signal tag prefix will be added to name
	 *   @param sigIOType - this string will be used as IO define type for IOSignals */
	@Override
	public String getIODefString(boolean addTagPrefix, String sigIOType) {
		return sigIOType + " " + getDefArray() + getFullName(null, addTagPrefix);
	}

	/** return a simple IOElement with full generated name */
	@Override
	public SystemVerilogIOElement getFullNameIOElement(String pathPrefix, boolean addTagPrefix) {
		String newTagPrefix = addTagPrefix? tagPrefix : "";
        //System.out.println("SystemVerilogIOSignal getFullNameIOElement: addTagPrefix=" + addTagPrefix + ", newTagPrefix=" + newTagPrefix + ", pathPrefix=" + pathPrefix);
		return new SystemVerilogIOSignal(from, to, newTagPrefix, pathPrefix + name, lowIndex, size);
	}

}
