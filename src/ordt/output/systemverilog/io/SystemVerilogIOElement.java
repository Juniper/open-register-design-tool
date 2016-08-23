package ordt.output.systemverilog.io;

/** io signal/signalset elements */
public abstract class SystemVerilogIOElement {
	
	protected String name;       // local name of this element
	protected String tagPrefix;       // prefix to be added to full generated name
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
		return ((this.from & loc) > 0);
	}

	/** set from location(s) for this io element */
	public void setFrom(Integer from) {
		this.from = from;
	}

    /** return true if location specified is in to locations for this io element */
	public Boolean isTo(Integer loc) {
		if ((loc == null) || (this.to == null)) return true;
		return ((this.to & loc) > 0);
	}

	/** set to location(s) for this io element */
	public void setTo(Integer to) {
		this.to = to;
	}

	public String getName() {
		return name;
	}
	
	/** returns true if element name is null of empty */
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
		String newTagPrefix = addTagPrefix? tagPrefix : "";
		String newPathPrefix = (pathPrefix == null)? "" : pathPrefix;
		return newTagPrefix + newPathPrefix + name;
	}
	
	/** return the full name of this io element assuming no addl pathPrefix and addition of tagPrefix  
	 * used for top-down recursive name generation */ 
	public String getFullName() {   
		return getFullName("", true);
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
	
}
