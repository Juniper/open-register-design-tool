package ordt.output.systemverilog.io;

import ordt.output.systemverilog.common.SystemVerilogSignal;
import ordt.parameters.Utils;

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

    @Override
	public void display(int indentLvl) {
		System.out.println(Utils.repeat(' ', indentLvl*4) + "SystemVerilogIOSignal: name=" + name + ", tagPrefix=" + tagPrefix+ ", reps=" + reps + ", lowIndex=" + lowIndex + ", size=" + size + ", from=" + from + ", to=" + to);
	}

	// hashCode/Equals overrides - super.name and super.reps are added to match
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + lowIndex;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + reps;
		result = prime * result + size;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		SystemVerilogIOSignal other = (SystemVerilogIOSignal) obj;
		if (lowIndex != other.lowIndex)
			return false;
		if (reps != other.reps)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (size != other.size)
			return false;
		return true;
	}

}
