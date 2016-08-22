/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved.
 */
package ordt.output.systemverilog.oldio;

import java.util.ArrayList;
import java.util.List;

import ordt.output.systemverilog.SystemVerilogSignal;

/** extended signal class which also contains source/dest info and may represent a SV interface */  
public class SystemVerilogIOSignal extends SystemVerilogSignal {
	
	// direction extensions
	Integer from, to = 0;  // direction of this signal
	// interface extensions
	SystemVerilogInterface intf = null;  // signals contained in this interface
	private String indexedName;  // name containing indexed rep counts for intf name generation
	protected int repCount = 1;  // number of times this interface is replicated  
	protected int repNum = 0;  // rep number of this intf 
	private String extIntfName = null;  // externally defined name for this interface 
	
	/** create a simple IO with from/to info */
	public SystemVerilogIOSignal(Integer from, Integer to, String name, int lowIndex, int size) {
		super(name, lowIndex, size);
		this.from = from;
		this.to = to;
	}
	
	/** create an interface IO */
    public SystemVerilogIOSignal(Integer from, Integer to, String indexedName, String repName, int repNum, int repCount, String extName) {
    	super(repName, 0, 0);  // name of this signal has reps  
    	this.indexedName = indexedName;  // name with indexed reps included will be used for local name truncation and intf inst name gen
		this.from = from;
		this.to = to;
		this.repNum = repNum;
		this.repCount = repCount;
		this.extIntfName = extName;
		intf = new SystemVerilogInterface(this, repCount);
		//System.out.println("SystemVerilogIOSignal intf constructor: repName=" + repName + ", idx name=" + indexedName + ", no rep name=" + getNoRepName());
	}

	/** return true if location specified is in from locations for this signal */
	public Boolean isFrom(Integer loc) {
		if (loc == null) return true;
		return ((this.from & loc) > 0);
	}

	/** set from
	 *  @param from the from to set
	 */
	public void setFrom(Integer from) {
		this.from = from;
	}

    /** return true if location specified is in to locations for this signal */
	public Boolean isTo(Integer loc) {
		if (loc == null) return true;
		return ((this.to & loc) > 0);
	}

	/** set to
	 *  @param to the to to set
	 */
	public void setTo(Integer to) {
		this.to = to;
	}

	/** return true if an interface that is externally defined
	 *  @return the extDefined
	 */
	private Boolean isExtDefined() {
		return (extIntfName != null);
	}

	/** get extIntfName
	 *  @return the extIntfName
	 */
	public String getExtIntfName() {
		return extIntfName;
	}

	/** return true if this IOsignal is an interface that does not use an external interface def */
	public Boolean isLocalDefinedInterface() {
		return isIntfSig() && ! isExtDefined();
	}

	/** get repCount */
	public int getRepCount() {
		return repCount;
	}

	/** get repNum */
	public int getRepNum() {
		return repNum;
	}
	
	public boolean isReplicated() {
		return (repCount > 1);
	}
	
	public boolean isFirstRep() {
		return (repNum == 0);
	}

	/** return true if this signal represents an interface */
	public boolean isIntfSig() {
		return (intf != null);
	}

	/** return the name of this signal as interface */
	public String getInterfaceDefName() {
		if (isExtDefined()) return getExtIntfName();  
		return getNoRepName() + "_intf";
	}

	/** return the array def string for this interface */
	public String getIntfDefArray() {
		return isReplicated() ? "[" + intf.getRepCount() + "]" : "";
	}
	
	/** return the array string used for definitions */
	@Override
	public String getDefArray() {
		if (isIntfSig()) return getIntfDefArray();
		return super.getDefArray();
	}

	/** return the intfList for this IOSignal */
	public SystemVerilogInterface getIntfList() {
		return intf;
	}

	/** return all signals encapsulated in this intfSig */
	public List<SystemVerilogSignal> getSignalList(Integer fromLoc, Integer toLoc) {
		List<SystemVerilogSignal> outList = new ArrayList<SystemVerilogSignal>();
	    outList.addAll(intf.getSignalList(fromLoc, toLoc));
		//System.out.println("  SystemVerilogIOSignal getSignalList: intfmap size=" + intfMappings.size() + ", output size=" + outList.size());
		return outList;
	}
	/** return all signals encapsulated in this intfSig */
	public List<SystemVerilogIOSignal> getIOSignalList(Integer fromLoc, Integer toLoc) {
		List<SystemVerilogIOSignal> outList = new ArrayList<SystemVerilogIOSignal>();
		outList.addAll(intf.getIOSignalList(fromLoc, toLoc));
		//System.out.println("  SystemVerilogIOSignal getSignalList: intfmap size=" + intfMappings.size() + ", output size=" + outList.size());
		return outList;
	}

	/** return a list of all defined interfaces leaf first  */
	public List<SystemVerilogIOSignal> getIntfsToBeDefined() {
		return intf.getIntfsToBeDefined();
	}

	/** return a list define strings for an interface from intfList element) */
	public List<String> getIntfDefStrings() {
		//System.out.println("  SystemVerilogIOSignal getIntfDefStrings: intfmap size=" + intfMappings.size() +
		//		", sig name" + getName() + ", map name" + intfMappings.get(0).getName());
		List<String> outList = new ArrayList<String>();
		outList.addAll(intf.getIntfDefStrings());
		return outList;
	}

	/** return a list of strings assigning interfaces to corresponding logic IO */ 
	public List<String> getInterfaceAssignStrList(int insideLocations, String intfPrefix) {
		List<String> outList = new ArrayList<String>();
		outList.addAll(intf.getInterfaceAssignStrList(insideLocations, intfPrefix));
		//System.out.println("  SystemVerilogIOSignal getSignalList: intfmap size=" + intfMappings.size() + ", output size=" + outList.size());
		return outList;
	}
	
	/** return the name used for definitions (includes array string) */
	@Override
	public String getDefName() {
		if (!isIntfSig()) return super.getDefName();  // if a logic signal
		// otherwise build interface def name
		Integer size = intf.getRepCount();
		String arrStr = (size > 1) ? " [" + size + "]" : "";
		return getName() + arrStr;
	}

	/** get noRepName
	 *  @return the noRepName
	 */
	public String getNoRepName() {
		return indexedName.replaceAll("\\[\\d+\\]", "");  // remove indexed
	}

	/** get indexedName */
	public String getIndexedName() {
		return indexedName;  //indexedName
	}

	public static void main (String[] args) {
		final Integer PIO = 1;
		final Integer DECODE = 2;
		final Integer LOGIC = 4;
		final Integer HW = 8;

    	SystemVerilogIOSignal sig = new SystemVerilogIOSignal(HW, DECODE|LOGIC, "newSig", 0, 7);
    	System.out.println("null,   from:" + sig.isFrom(null) + ",  to:" + sig.isTo(null));
    	System.out.println("PIO,    from:" + sig.isFrom(PIO) + ",  to:" + sig.isTo(PIO));
    	System.out.println("DECODE, from:" + sig.isFrom(DECODE) + ",  to:" + sig.isTo(DECODE));
    	System.out.println("LOGIC,  from:" + sig.isFrom(LOGIC) + ",  to:" + sig.isTo(LOGIC));
    	System.out.println("HW,     from:" + sig.isFrom(HW) + ",  to:" + sig.isTo(HW));
    }

}
