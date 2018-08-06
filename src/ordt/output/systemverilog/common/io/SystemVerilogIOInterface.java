package ordt.output.systemverilog.common.io;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import ordt.output.systemverilog.common.RemapRuleList;

public class SystemVerilogIOInterface extends SystemVerilogIOSignalSet {

	protected String compId;
	
	/** create a new SystemVerilogIOInterface
	 * @param from
	 * @param to
	 * @param tagPrefix
	 * @param name - name of this interface
	 * @param reps - number of reps in this set
	 * @param type - if hasExtType is true, this is the external type, otherwise it is the path string for an internally defined type
	 * @param hasExtType - if true, interface has an externally defined type
	 * @param genNewType - if true, interface type will generated for internal types, else type will be passed thru as-is
	 * @param compId - optional component ID for name generation
	 */
	public SystemVerilogIOInterface(Integer from, Integer to, String tagPrefix, String name, int reps, String type, boolean hasExtType, boolean genNewType, String compId) { 
		super(tagPrefix, name, reps);
		signalSetType = SignalSetType.INTERFACE;
		this.from = from;
		this.to = to;
		this.hasExtType = hasExtType;
		this.compId = compId;
		String suffix = ((type==null) || type.isEmpty())? "" : "_";
		this.type = (hasExtType || !genNewType)? type : getFullName(type + suffix, true) + "_intf";  // if not an ext type then build from path
		//System.out.println("SystemVerilogIOInterface: name=" + name + ", compId=" + compId + ", old type=" + type + ", new type=" + this.type + ", hasExtType=" + hasExtType);
	}

	/** create copy of an IOInterface w/ hierarchy, keeping elements matching specified rules and not in a unique name list.
	 * virtual elements will be added w/o rule matching  
	 * 
	 * @param origSet - IOInterface be copied
	 * @param rules - RemapRuleList to be applied for matching
	 * @param uniqueList - if non-null, only elements with names not in the set will be copied
	 * @param namePrefix - string prefix from ancestors that is used for hier name creation (recursion parameter)
	 */
	protected SystemVerilogIOInterface(SystemVerilogIOInterface origSet, RemapRuleList rules, HashSet<String> uniqueList, String namePrefix) {
		super(origSet, rules, uniqueList, namePrefix);
		this.compId = origSet.compId;
	}

	/** return a copy of this IOInterface w/ hierarchy, keeping elements matching specified rules and not in a unique name list.
	 * virtual elements will be added w/o rule matching  
	 * 
	 * @param rules - RemapRuleList to be applied for matching
	 * @param uniqueList - if non-null, only elements with names not in the set will be copied
	 * @param namePrefix - string prefix from ancestors that is used for hier name creation (recursion parameter)
	 */
	@Override
	public SystemVerilogIOInterface createCopy(RemapRuleList rules, HashSet<String> uniqueList, String namePrefix) {
		//System.out.println("SystemVerilogIOInterface createCopy: name=" + name + ", compId=" + compId);
	   return new SystemVerilogIOInterface(this, rules, uniqueList, namePrefix);
	}

	/** return a simple IOElement with full generated name */
	@Override
	public SystemVerilogIOElement getFullNameIOElement(String pathPrefix, boolean addTagPrefix) {
		String newTagPrefix = addTagPrefix? tagPrefix : "";
		return new SystemVerilogIOInterface(from, to, newTagPrefix, pathPrefix + name, reps, type, hasExtType, false, null);
	}

    /** return a list of definitions for this sigset - overridden in SignalSet child classses  */  
    @Override
    public List<String> getDefStrings() {
    	List<String> outList = new ArrayList<String>();
		List<String> intfDefStrings = getChildInstanceStrings();  // get child definitions for this intf
		// write the interface
		if (intfDefStrings.size() > 0) {
			outList.add("//");
			outList.add("//---------- interface " + getType());
			outList.add("//");
			outList.add("interface " + getType() + ";");		
			outList.addAll(intfDefStrings);   
			outList.add("endinterface\n");	
		}
    	return outList;
    }	

    // ----------------- methods returning sv formatted strings for output 

    /** return a list of instance strings for child elements in this interface */
    private List<String> getChildInstanceStrings() { 
    	List<String> outList = new ArrayList<String>();
    	// get a list of child elements
    	List<SystemVerilogIOElement> children = getLocalDescendentIOElementList();
    	// loop through the list and generate an instance
    	for (SystemVerilogIOElement child: children) {
    		//System.out.println("SystemVerilogIOInterface getChildInstanceStrings: calling getInstanceString");
    		outList.add("  " + child.getInstanceString(false));  // do not add tagPrefix
    	}
  	    return outList;
    }

    @Override
	public String getCompId() {
		return compId;
	}
    
    @Override
	public String getCompIdType() { 
    	return getTagPrefix() + getCompId() + "_intf"; 
    }

}
