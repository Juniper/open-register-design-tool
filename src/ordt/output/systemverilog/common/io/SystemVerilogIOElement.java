package ordt.output.systemverilog.common.io;

import java.util.ArrayList;
import java.util.List;

import ordt.output.systemverilog.common.SystemVerilogSignal;

/** io signal/signalset elements */
public abstract class SystemVerilogIOElement {
	
	protected String name;       // local name of this element
	protected String tagPrefix;       // prefix to be added to full generated name (eg l2h_)
	protected int reps = 1;  // number of times this element is replicated  
	Integer from, to = 0;  // direction of this signal/signalset

	/** returns true if this element is a set */
	public boolean isSignalSet() { return false; }
	
	/** returns true if this element is virtual (ie not an actually group in systemverilog output).
	 *  This method is overridden in child classes */
	public boolean isVirtual() { return false; }

	/** return true if location specified is in from locations for this io element */
	public Boolean isFrom(Integer loc) {
		if ((loc == null) || (this.from == null)) return true;
		return ((this.from & loc) != 0);
	}

	/** get from location(s) for this io element */
	public Integer getFrom() {
		return from;
	}

	/** set from location(s) for this io element */
	public void setFrom(Integer from) {
		this.from = from;
	}

    /** return true if location specified is in to locations for this io element */
	public Boolean isTo(Integer loc) {
		if ((loc == null) || (this.to == null)) return true;
		return ((this.to & loc) != 0);
	}

	/** get to location(s) for this io element */
	public Integer getTo() {
		return to;
	}

	/** set to location(s) for this io element */
	public void setTo(Integer to) {
		this.to = to;
	}

	public String getName() {
		return name;
	}
	
	/** returns true if element name is null or empty */
	public boolean hasNoName() {
		return (name == null) || name.isEmpty();
	}

	public String getTagPrefix() {
		return tagPrefix;
	}

	/** get replication count for this io element */
	public int getReps() {
		return reps;
	}
	
	/** return true if more than one rep of this io element */
	public boolean isReplicated() {
		return (reps > 1);
	}

	/** by default return local element definition (no name prefix) */
	public String toString () {
		return getFullName("", true);
	}
	
	/** return the full name of this io element including any tagPrefix, pathPrefix, and local name 
	 * used for top-down recursive name generation */ 
	public String getFullName(String pathPrefix, boolean addTagPrefix) {   
		String newTagPrefix = ((tagPrefix == null) || !addTagPrefix)? "" : tagPrefix;
		String newPathPrefix = (pathPrefix == null)? "" : pathPrefix;
		return newTagPrefix + newPathPrefix + name;
	}
	
	/** return the full name of this io element assuming no addl pathPrefix and addition of tagPrefix  
	 * used for top-down recursive name generation */ 
	public String getFullName() {   
		return getFullName("", true);
	}

	/** convert a flat list of SystemVerilogIOElement to a list of SystemVerilogSignal with full generated names for this signalset. 
	 * @return - list of SystemVerilogSignal
	 */
	public static List<SystemVerilogSignal> getSignalList(List<SystemVerilogIOElement> ioElemList) {
		List<SystemVerilogSignal> outList = new ArrayList<SystemVerilogSignal>();
		//System.out.println("  SystemVerilogInterface getSignalList: sigs size=" + sigs.size());
		for (SystemVerilogIOElement ioElem : ioElemList) {
			if (ioElem.isSignalSet())
			    outList.add(new SystemVerilogSignal(ioElem.getFullName(null, true), 0, 1));
			else {
				SystemVerilogIOSignal ioSig = (SystemVerilogIOSignal) ioElem;
			    outList.add(new SystemVerilogSignal(ioSig.getFullName(null, true), ioSig.getLowIndex(), ioSig.getSize()));
			}
		}
		//System.out.println("  SystemVerilogIOSignalSet getSignalList: output size=" + outList.size());
		return outList;
	}	
		
	// abstract methods
	
	/** return type string for this element */
	public abstract String getType();
	
	/** return sv string instancing this element - assumes element name is full instance name 
	 *   includes type, name, array, semi eol */
	public abstract String getInstanceString(boolean addTagPrefix);
	
	/** return sv string used in definition of this element in input/output lists - assumes element name is full instance name 
	 *   includes type if non-virtual sigset, name, and array *
	 *   @param addTagPrefix - if true signal tag prefix will be added to name
	 *   @param sigIOType - this string will be used as IO define type for IOSignals */
	public abstract String getIODefString(boolean addTagPrefix, String sigIOType);

	/** return a simple IOSignal with full generated name for this element */
	public abstract SystemVerilogIOElement getFullNameIOElement(String pathPrefix, boolean addTagPrefix);

	public abstract void display(int indentLvl);

	// hashCode/Equals overrides - name, reps omitted in match
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((from == null) ? 0 : from.hashCode());
		result = prime * result + ((tagPrefix == null) ? 0 : tagPrefix.hashCode());
		result = prime * result + ((to == null) ? 0 : to.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SystemVerilogIOElement other = (SystemVerilogIOElement) obj;
		if (from == null) {
			if (other.from != null)
				return false;
		} else if (!from.equals(other.from))
			return false;
		if (tagPrefix == null) {
			if (other.tagPrefix != null)
				return false;
		} else if (!tagPrefix.equals(other.tagPrefix))
			return false;
		if (to == null) {
			if (other.to != null)
				return false;
		} else if (!to.equals(other.to))
			return false;
		return true;
	}

	protected int hashCode(boolean includeReps) {
		return hashCode();  // ignore includeReps option by default
	}

	protected boolean equals(Object obj, boolean includeReps) {
		return equals(obj);  // ignore includeReps option by default
	}
	
}
