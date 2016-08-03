package ordt.output.systemverilog.io;

import java.util.List;

/** io signal/signalset elements */
public abstract class SystemVerilogIOElement {
	
	protected String name;       // local name of this element
	protected int repCount = 1;  // number of times this element is replicated  
	Integer from, to = 0;  // direction of this signal/signalset

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
		return getDefName("");
	}
	
	// abstract methods
	
	/** return the definition string for this io element */
	public abstract String getDefName(String prefix);
	
	/** return a list of definitions *  TODO - child definitions for IOSignalSet?
	public abstract List<String> getDefStrings(String prefix); */

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
