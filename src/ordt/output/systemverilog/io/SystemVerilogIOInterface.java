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
		List<String> intfDefStrings = getDefStrings(null);  // get child definitions for this intf
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

    /** return a list define strings for child elements in this interface */
    private List<String> getDefStrings(String pathPrefix) {  // TODO - need to get elements, not 
    	List<String> outList = new ArrayList<String>();
    	// get a list of child elements
    	// loop through the list and generate an instance
  	return outList;
    }

/*
/** return a list of strings assigning interfaces to corresponding logic IO * 
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
