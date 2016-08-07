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
		this.type = hasExtType? type : getFullName(type, true);  // if not an ext type then build from path
	}

	/** returns true if this element is virtual (ie not an actually group in systemverilog output).
	 *  This method is overridden in child classes */
    @Override
	protected boolean isVirtual() { return false; }

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

    /** return a list of assign statements for this sigset mapping from simple signals to hierarchical- overriden in SignalSet child classses */  
    @Override
    public List<String> getAssignMapStrings(String pathPrefix) {
    	return null;
    }
	

    // ----------------- methods returning sv formatted strings for output 

    /** return a list of instance strings for child elements in this interface */
    private List<String> getChildInstanceStrings() { 
    	List<String> outList = new ArrayList<String>();
    	// get a list of child elements
    	List<SystemVerilogIOElement> children = getLocalChildIOElementList();
    	// loop through the list and generate an instance
    	for (SystemVerilogIOElement child: children)
    		outList.add(child.getInstanceString());
  	    return outList;
    }

/*

	/** return a flat list of IOelements with generated names for this signalset - recursively builds names top down * 
	 * @param fromLoc - only signals matching from will be returned
	 * @param toLoc - only signals matching to will be returned
	 * @param pathPrefix - prefix from ancestor levels that will be used to create child name
	 * @param addTagPrefix - if true, defined signal prefixes will be added to names
	 * @param stopOnNonVirtualSets - if true, recursion stops when a non-virtual signalset is hit (eg an interface)
	 * @param inhibitVirtualEncaps - if true, only signals in at least one non-virtual signalset are returned
	 * @return - list of SystemVerilogIOSignal
	 *
	public List<SystemVerilogIOElement> getIOElementList(Integer fromLoc, Integer toLoc, String pathPrefix, boolean addTagPrefix, 
			boolean stopOnNonVirtualSets, boolean inhibitVirtualEncaps) {
		List<SystemVerilogIOElement> outList = new ArrayList<SystemVerilogIOElement>();
		//System.out.println("  SystemVerilogInterface getSignalList: sigs size=" + sigs.size());
		for (SystemVerilogIOElement ioElem : childList) {
			String prefix = ((pathPrefix == null) || pathPrefix.isEmpty())? "" : ioElem.getName() + "_";
			boolean childInhibitVirtualEncaps = inhibitVirtualEncaps && !isVirtual();  // no inhibit in children if a real set is encountered
		    // process each rep of this elem
			for (int idx=0; idx<ioElem.getReps(); idx++) {
				String suffix = ioElem.isReplicated()? "_" + idx : "";
				// if this is leaf element then return it
				boolean validLeaf = !(ioElem.isVirtual() || (ioElem.isSignalSet() && !stopOnNonVirtualSets) || (isVirtual() && inhibitVirtualEncaps));
				boolean validLoc = ioElem.isFrom(fromLoc) && ioElem.isTo(toLoc);
				if (validLeaf && validLoc) {
					outList.add(ioElem.getFullNameIOElement(prefix + suffix, addTagPrefix));  // create a new IOElem and add to list
				}		
				// otherwise if a signalset, make recursive call 
				else if (ioElem.isSignalSet()) {
					List<SystemVerilogIOElement> newList = ((SystemVerilogIOSignalSet) ioElem).getIOElementList(fromLoc, toLoc, prefix + suffix, addTagPrefix, stopOnNonVirtualSets, childInhibitVirtualEncaps);
					outList.addAll(newList);
  			    }
			}
		}
		//System.out.println("  SystemVerilogIOSignalSet getIOSignalList: output size=" + outList.size());
		return outList;
	}


/** return a list of strings assigning interfaces to corresponding logic IO *   // TODO
public List<String> getInterfaceAssignStrList(int insideLocations, String intfPrefix) {
	List<String> outList = new ArrayList<String>();
	//System.out.println("SystemVerilogIntfList getInterfaceAssignStrList: name=" + getName() + ", rep=" + getRepNum() + ", reps=" + getRepCount() + ", sigList size=" + size());
	for (SystemVerilogIOSignal sig: signalList) {
		String arrayStr = (isReplicated()) ? "[" + getRepNum() + "]" : "";  // generate rep string for current list
		String localName = getChildName(sig);  
		//System.out.println("SystemVerilogIntfList getInterfaceAssignStrList: sig name=" + sig.getName()  + ", localName=" + localName);
		if (localName != null) {
			// if this signal is an interface, call recursively to get all encapsulated signal assigns
			if (sig.isIntfSig()) {  
				String intfSigName = intfPrefix + arrayStr + "." + localName;  
				List<String> newList = sig.getInterfaceAssignStrList(insideLocations, intfSigName);
				outList.addAll(newList);
			}
			// otherwise if logic signal, output the assigns
			else {
				String intfSigName = intfPrefix + arrayStr + "." + localName;  
			   // input assigns
			   if (sig.isTo(insideLocations)) outList.add(sig.getName() + " = " + intfSigName + ";");
			   else if (sig.isFrom(insideLocations)) outList.add(intfSigName + " = " + sig.getName() + ";");
		    }
		}
	}
	//System.out.println("SystemVerilogIntfList getInterfaceAssignStrList: name=" + getName() + ", outList size=" + outList.size());
	return outList;
}

	
/** return a list of strings assigning interface children to corresponding simple IO signal *  <----- this is from IOSignalList 
public List<String> getInterfaceAssignStrList(int insideLocations) {
	List<String> outList = new ArrayList<String>();
	for (SystemVerilogIOSignal sig: signalList) {
		// if this signal is an interface, call recursively to get all encapsulated signal assigns
		if (sig.isIntfSig()) {
			//System.err.println("   SystemVerilogIOSignalList getInterfaceAssignStrList: name=" + sig.getName());
			List<String> newList = sig.getInterfaceAssignStrList(insideLocations, sig.getNoRepName());  // base list adds prefix
			outList.addAll(newList);
		}
	}
	return outList;
}
*/

}
