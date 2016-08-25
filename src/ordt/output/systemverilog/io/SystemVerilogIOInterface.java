package ordt.output.systemverilog.io;

import java.util.ArrayList;
import java.util.List;

public class SystemVerilogIOInterface extends SystemVerilogIOSignalSet {

	/** create a new SystemVerilogIOInterface
	 * @param from
	 * @param to
	 * @param tagPrefix
	 * @param name - name of this interface
	 * @param reps - number of reps in this set
	 * @param type - if hasExtType is true, this is the external type, otherwise it is the path string for an internally defined type
	 * @param hasExtType - if true, interface has an externally defined type
	 */
	public SystemVerilogIOInterface(Integer from, Integer to, String tagPrefix, String name, int reps, String type, boolean hasExtType) { 
		super(tagPrefix, name, reps);
		this.from = from;
		this.to = to;
		this.hasExtType = hasExtType;
		this.type = hasExtType? type : getFullName(type, true) + "_intf";  // if not an ext type then build from path
		//System.out.println("SystemVerilogIOInterface: name=" + name + ", type=" + type + ", hasExtType=" + hasExtType);
	}

	/** returns true if this element is virtual (ie not an actually group in systemverilog output).
	 *  This method is overridden in child classes */
    @Override
	public boolean isVirtual() { return false; }

	/** return a simple IOElement with full generated name */
	@Override
	public SystemVerilogIOElement getFullNameIOElement(String pathPrefix, boolean addTagPrefix) {
		String newTagPrefix = addTagPrefix? tagPrefix : "";
		return new SystemVerilogIOInterface(from, to, newTagPrefix, pathPrefix + name, reps, type, hasExtType);
	}

    /** return a list of definitions for this sigset - overriden in SignalSet child classses  */  
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

}
