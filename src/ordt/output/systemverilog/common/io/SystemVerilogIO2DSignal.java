package ordt.output.systemverilog.common.io;

import ordt.output.common.MsgUtils;
import ordt.output.systemverilog.common.SystemVerilogParameterizedRange;
import ordt.output.systemverilog.common.SystemVerilogRange;
import ordt.output.systemverilog.common.SystemVerilogResolvedRange;

public class SystemVerilogIO2DSignal extends SystemVerilogIOSignal {
	protected SystemVerilogRange unpackedRange;  // 2d sigmal adds an unpacked range
	
	/** add a new simple 2d vector array with freeform slice strings to the child list */ 
	public SystemVerilogIO2DSignal(Integer from, Integer to, String tagPrefix, String name, SystemVerilogRange range, SystemVerilogRange unpackedRange) {
		super(from, to, tagPrefix, name, range, false);
		this.unpackedRange = unpackedRange;
	}

	/** add a new simple 2d vector array with freeform slice strings to the child list */ 
	public SystemVerilogIO2DSignal(Integer from, Integer to, String tagPrefix, String name, String packedSlice, String unpackedSlice) {
		super(from, to, tagPrefix, name, packedSlice);
		// save unpacked slice as resolved if an integer range else store as parameterized
		SystemVerilogRange temprange = new SystemVerilogResolvedRange(unpackedSlice);
		if (temprange.isValid()) this.unpackedRange = temprange;
		else this.unpackedRange = new SystemVerilogParameterizedRange(unpackedSlice);
	}
	
	/** return the array string used for definitions */
	public String getUnpackedDefArray() {
		return unpackedRange.getDefArray();
	}

    // ------------ methods overriding super

	/** return sv string instancing this element - assumes element name is full instance name */
	@Override
	public String getInstanceString(boolean addTagPrefix) {
        //System.out.println("SystemVerilogIOSignal getInstanceString: addTagPrefix=" + addTagPrefix + ", fullName=" + getFullName(null, addTagPrefix));
		return getType() + " " + getDefArray() + getFullName(null, addTagPrefix) + getUnpackedDefArray() + ";";
	}
	
	/** return sv string used in definition of this element in input/output lists - assumes element name is full instance name 
	 *   includes type if non-virtual sigset, name, and array *
	 *   @param addTagPrefix - if true signal tag prefix will be added to name
	 *   @param sigIOType - this string will be used as IO define type for IOSignals */
	@Override
	public String getIODefString(boolean addTagPrefix, String sigIOType) {
		return sigIOType + " " + getDefArray() + getFullName(null, addTagPrefix) + getUnpackedDefArray();
	}

	/** return a simple IOElement with full generated name */
	@Override
	public SystemVerilogIOElement getFullNameIOElement(String pathPrefix, boolean addTagPrefix) {
		String newTagPrefix = addTagPrefix? tagPrefix : "";
        //System.out.println("SystemVerilogIOSignal getFullNameIOElement: addTagPrefix=" + addTagPrefix + ", newTagPrefix=" + newTagPrefix + ", pathPrefix=" + pathPrefix);
		return new SystemVerilogIO2DSignal(from, to, newTagPrefix, pathPrefix + name, range, unpackedRange);
	}

    @Override
	public void display(int indentLvl) {
		System.out.println(MsgUtils.repeat(' ', indentLvl*4) + "SystemVerilogIOSignal: name=" + name + ", tagPrefix=" + tagPrefix+ ", reps=" + reps + ", lowIndex=" + range.getLowIndex() + ", size=" + range.getSize() + ", from=" + from + ", to=" + to);
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
