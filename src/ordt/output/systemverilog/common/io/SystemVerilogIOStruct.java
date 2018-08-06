package ordt.output.systemverilog.common.io;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import ordt.output.systemverilog.common.RemapRuleList;

public class SystemVerilogIOStruct extends SystemVerilogIOSignalSet {  // FIXME - update for struct support / fix getIODefString - use inout wire type name form in inputs

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
	public SystemVerilogIOStruct(Integer from, Integer to, String tagPrefix, String name, int reps, String type, boolean hasExtType, boolean genNewType, String compId) { 
		super(tagPrefix, name, reps);
		signalSetType = SignalSetType.STRUCT;
		this.from = from;
		this.to = to;
		this.hasExtType = hasExtType;
		this.compId = compId;
		String suffix = ((type==null) || type.isEmpty())? "" : "_";
		this.type = (hasExtType || !genNewType)? type : getFullName(type + suffix, true) + "_struct";  // if not an ext type then build from path
		//if (!hasExtType) System.out.println("SystemVerilogIOStruct: name=" + name + ", old type=" + type + ", new type=" + this.type + ", hasExtType=" + hasExtType);
	}

	/** create copy of an IOStruct w/ hierarchy, keeping elements matching specified rules and not in a unique name list.
	 * virtual elements will be added w/o rule matching  
	 * 
	 * @param origSet - IOStruct be copied
	 * @param rules - RemapRuleList to be applied for matching
	 * @param uniqueList - if non-null, only elements with names not in the set will be copied
	 * @param namePrefix - string prefix from ancestors that is used for hier name creation (recursion parameter)
	 */
	protected SystemVerilogIOStruct(SystemVerilogIOStruct origSet, RemapRuleList rules, HashSet<String> uniqueList, String namePrefix) {
		super(origSet, rules, uniqueList, namePrefix);
		this.compId = origSet.compId;
	}

	/** return a copy of this IOStruct w/ hierarchy, keeping elements matching specified rules and not in a unique name list.
	 * virtual elements will be added w/o rule matching  
	 * 
	 * @param rules - RemapRuleList to be applied for matching
	 * @param uniqueList - if non-null, only elements with names not in the set will be copied
	 * @param namePrefix - string prefix from ancestors that is used for hier name creation (recursion parameter)
	 */
	@Override
	public SystemVerilogIOStruct createCopy(RemapRuleList rules, HashSet<String> uniqueList, String namePrefix) {
	   return new SystemVerilogIOStruct(this, rules, uniqueList, namePrefix);
	}

	/** return a simple IOElement with full generated name */
	@Override
	public SystemVerilogIOElement getFullNameIOElement(String pathPrefix, boolean addTagPrefix) {
		String newTagPrefix = addTagPrefix? tagPrefix : "";
		return new SystemVerilogIOStruct(from, to, newTagPrefix, pathPrefix + name, reps, type, hasExtType, false, null);
	}

    /** return a list of definitions for this sigset - overridden in SignalSet child classses  */  
    @Override
    public List<String> getDefStrings() {
    	List<String> outList = new ArrayList<String>();
		List<String> structDefStrings = getChildInstanceStrings();  // get child definitions for this intf
		// write the struct
		if (structDefStrings.size() > 0) {
			outList.add("//");
			outList.add("//---------- struct " + getType());
			outList.add("//");
			outList.add("typedef struct {");		
			outList.addAll(structDefStrings);   
			outList.add("} " + getType() + ";\n");	
		}
    	return outList;
    }	
    
	/** return sv string used in definition of this element in input/output lists - assumes element name is full instance name 
	 *   includes type if non-virtual sigset, name, and array *
	 *   @param addTagPrefix - if true signal tag prefix will be added to name
	 *   @param sigIOType - this string will be used as IO define type for IOSignals */
	@Override
	public String getIODefString(boolean addTagPrefix, String sigIOType) {
		//System.out.println("SystemVerilogIOSignalSet getIODefString: type=" + getType() + ", name=" + getFullName(null, addTagPrefix));
		return "ref " + getType() + " " + getFullName(null, addTagPrefix) + getRepArray();
	}

    // ----------------- methods returning sv formatted strings for output 

    /** return a list of instance strings for child elements in this struct */
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
	/** return sv string instancing this element - assumes element name is full instance name */
	@Override
	public String getInstanceString(boolean addTagPrefix) {
        //System.out.println("SystemVerilogIOStruct getInstanceString: addTagPrefix=" + addTagPrefix + ", fullName=" + getFullName(null, addTagPrefix));
		return getType() + " " + getFullName(null, addTagPrefix) + getRepArray() + ";";
	}

    @Override
	public String getCompId() {
		return compId;
	}
    
    @Override
	public String getCompIdType() { 
    	return getTagPrefix() + getCompId() + "_struct"; 
    }

}
