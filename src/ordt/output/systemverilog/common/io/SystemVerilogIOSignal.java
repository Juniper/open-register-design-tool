package ordt.output.systemverilog.common.io;

import ordt.output.common.MsgUtils;
import ordt.output.systemverilog.common.SystemVerilogParameterizedRange;
import ordt.output.systemverilog.common.SystemVerilogRange;
import ordt.output.systemverilog.common.SystemVerilogResolvedRange;

public class SystemVerilogIOSignal extends SystemVerilogIOElement {
	protected SystemVerilogRange range;
	protected boolean signed = false;
	
	/** create io signal using a range */
	public SystemVerilogIOSignal(Integer from, Integer to, String tagPrefix, String name, SystemVerilogRange range, boolean signed) {
		this.name = name;
		this.tagPrefix = tagPrefix;
		this.from = from;
		this.to = to;
		this.reps = 1;  // signal rep count is always 1
		this.range = range;
		this.signed = signed;
	}

	/** create io signal using low index and size (assumes msb left) */
	public SystemVerilogIOSignal(Integer from, Integer to, String tagPrefix, String name, int lowIndex, int size, boolean signed) {
		this(from, to, tagPrefix, name, null, signed);
		this.range = new SystemVerilogResolvedRange(lowIndex, size);
	}
	
	/** create an unsigned io signal using low index and size (assumes msb left) */
	public SystemVerilogIOSignal(Integer from, Integer to, String tagPrefix, String name, int lowIndex, int size) {   
		this(from, to, tagPrefix, name, lowIndex, size, false);
	}
	
	/** create io signal using a slice string */
	public SystemVerilogIOSignal(Integer from, Integer to, String tagPrefix, String name, String slice) {
		this.name = name;
		this.tagPrefix = tagPrefix;
		this.from = from;
		this.to = to;
		this.reps = 1;  // signal rep count is always 1
		// save slice as resolved if an integer range else store as parameterized
		SystemVerilogRange temprange = new SystemVerilogResolvedRange(slice);
		if (temprange.isValid()) this.range = temprange;
		else this.range = new SystemVerilogParameterizedRange(slice);
	}

	// ----- 

	/** get lowIndex for this io signal */
	public int getLowIndex() {
		return range.getLowIndex();
	}

	/** get size in bits of this io signal */
	public int getSize() {
		return range.getSize();
	}
	
	/** return the array string used for definitions */
	public String getDefArray() {
		return range.getDefArray();
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
		String pfix = this.signed? "signed " : " ";
        //System.out.println("SystemVerilogIOSignal getIODefString: " + sigIOType + pfix + getDefArray() + getFullName(null, addTagPrefix));
		return sigIOType + pfix + getDefArray() + getFullName(null, addTagPrefix);
	}

	/** return a simple IOElement with full generated name */
	@Override
	public SystemVerilogIOElement getFullNameIOElement(String pathPrefix, boolean addTagPrefix) {
		String newTagPrefix = addTagPrefix? tagPrefix : "";
        //System.out.println("SystemVerilogIOSignal getFullNameIOElement: addTagPrefix=" + addTagPrefix + ", newTagPrefix=" + newTagPrefix + ", pathPrefix=" + pathPrefix);
		return new SystemVerilogIOSignal(from, to, newTagPrefix, pathPrefix + name, range, signed);
	}

    @Override
	public void display(int indentLvl) {
		System.out.println(MsgUtils.repeat(' ', indentLvl*4) + "SystemVerilogIOSignal: name=" + name + ", tagPrefix=" + tagPrefix+ ", reps=" + reps + ", range=" + (range.isVector()? range.getDefArray() : "scalar") + ", from=" + from + ", to=" + to);
	}

	// hashCode/Equals overrides - super.name and super.reps are added to match
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + range.hashCode();
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + reps;
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
		if (range == null) {
			if (other.range != null)
				return false;
		} else if (!range.equals(other.range))
			return false;
		if (reps != other.reps)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
	
    public static void main(String[] args) throws Exception {
    	SystemVerilogIOSignal newsig = new SystemVerilogIOSignal(0, 0, null, null, 10, 2);
    	System.out.println("newsig" + newsig.getDefArray());
    	newsig = new SystemVerilogIOSignal(0, 0, null, null, "0: 22");
    	System.out.println("newsig" + newsig.getDefArray());
    	newsig = new SystemVerilogIOSignal(0, 0, null, null, "N*7 : WIDTH-2");
    	System.out.println("newsig" + newsig.getDefArray());
    }

}
