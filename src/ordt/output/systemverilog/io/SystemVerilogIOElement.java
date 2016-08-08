package ordt.output.systemverilog.io;

/** io signal/signalset elements */
public abstract class SystemVerilogIOElement {
	
	protected String name;       // local name of this element
	protected String tagPrefix;       // prefix to be added to full generated name
	protected int reps = 1;  // number of times this element is replicated  
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
	
	/** return sv string instancing this element */
	public String getInstanceString(String pathPrefix, boolean addTagPrefix) {
		return getType() + " " + getFullName(pathPrefix, addTagPrefix) + ";";
	}
		
	// abstract methods
	
	/** return type string for this element */
	public abstract String getType();
	
	/** return sv string instancing this element - assumes element name is full instance name */
	public abstract String getInstanceString();

	/** return a simple IOSignal with full generated name for this element */
	public abstract SystemVerilogIOElement getFullNameIOElement(String pathPrefix, boolean addTagPrefix);  
	
}
