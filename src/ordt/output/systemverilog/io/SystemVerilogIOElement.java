package ordt.output.systemverilog.io;

import java.util.List;

/** io signal/signalset elements */
public abstract class SystemVerilogIOElement {
	
	protected String name;       // local name of this element
	protected String namePrefix;       // prefix to be added to full generated name
	protected int repCount = 1;  // number of times this element is replicated  
	Integer from, to = 0;  // direction of this signal/signalset

	/** returns true if this element is a set */
	protected boolean isSignalSet() { return false; }
	
	/** returns true if this element is virtual (ie not an actually group in systemverilog output).
	 *  This method is overridden in child classes */
	protected boolean isVirtual() { return false; }

	/** return true if location specified is in from locations for this io element */
	public Boolean isFrom(Integer loc) {
		if (loc == null) return true;
		return ((this.from & loc) > 0);
	}

	/** set from location(s) for this io element */
	public void setFrom(Integer from) {
		this.from = from;
	}

    /** return true if location specified is in to locations for this io element */
	public Boolean isTo(Integer loc) {
		if (loc == null) return true;
		return ((this.to & loc) > 0);
	}

	/** set to location(s) for this io element */
	public void setTo(Integer to) {
		this.to = to;
	}

	public String getName() {
		return name;
	}

	public String getNamePrefix() {
		return namePrefix;
	}

	/** get replication count for this io element */
	public int getRepCount() {
		return repCount;
	}
	
	/** return true if more than one rep of this io element */
	public boolean isReplicated() {
		return (repCount > 1);
	}

	/** by default return local element definition (no name prefix) */
	public String toString () {
		return getFullName("");
	}
	
	/** return the full name of this io element including prefix, path, and local name 
	 * used for top-down recursive name generation */ 
	public String getFullName(String pathPrefix) {   // TODO need one w/o name_prefix for child name gen
		return namePrefix + pathPrefix + name;
	}
	
	// abstract methods
	
	/** return sv string instancing this element */
	public abstract String getInstanceString();

	/** return all io elements at root level of this io element *   TODO - move to IOSignalSet
	public List<SystemVerilogIOElement> getIOElements(Integer fromLoc, Integer toLoc) {
		List<SystemVerilogSignal> outList = new ArrayList<SystemVerilogSignal>();
	    outList.addAll(intf.getSignalList(fromLoc, toLoc));
		//System.out.println("  SystemVerilogIOSignal getSignalList: intfmap size=" + intfMappings.size() + ", output size=" + outList.size());
		return outList;
	}*/
	
	/** return a list of all io signals in this io element */
	public abstract List<SystemVerilogIOSignal> getIOSignals(Integer fromLoc, Integer toLoc);

}
