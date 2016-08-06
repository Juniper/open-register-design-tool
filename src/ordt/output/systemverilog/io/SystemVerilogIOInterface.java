package ordt.output.systemverilog.io;

import java.util.ArrayList;
import java.util.List;

public class SystemVerilogIOInterface extends SystemVerilogIOSignalSet {

	public SystemVerilogIOInterface(Integer from, Integer to, String namePrefix, String name, int reps, String extType) { 
		super(namePrefix, name, reps);
		this.from = from;
		this.to = to;
		this.extType = extType;
	}

	/** returns true if this element is virtual (ie not an actually group in systemverilog output).
	 *  This method is overridden in child classes */
    @Override
	protected boolean isVirtual() { return false; }

    /** return a list of definitions for this sigset - overriden in SignalSet child classses  */  
    @Override
    public List<String> getDefStrings() {  // TODO
    	List<String> outList = new ArrayList<String>();
		List<String> intfDefStrings = getDefStrings(null);  // get child definitions for this intf
		// write the interface
		if (intfDefStrings.size() > 0) {
			/*writeInterfaceBegin(indentLevel++, intfName);  // need to generate 
			outList.addAll(intfDefStrings);   
			writeInterfaceEnd(--indentLevel);*/
		}
    	return outList;
    }

    /** return a list of assign statements for this sigset mapping from simple signals to hierarchical- overriden in SignalSet child classses */  
    @Override
    public List<String> getAssignMapStrings(String pathPrefix) {
    	return null;
    }
	

    // ----------------- methods returning sv formatted strings for output 

    /** return a list define strings for this interface */
    private List<String> getDefStrings(String pathPrefix) {  // TODO
    	List<String> outList = new ArrayList<String>();
	/*	List<String> intfDefStrings = getIntfDefStrings();
		// write the interface
		if (intfDefStrings.size() > 0) {
			writeInterfaceBegin(indentLevel++, intfName);
			for (String defStr: intfDefStrings) writeStmt(indentLevel, defStr);   
			writeInterfaceEnd(--indentLevel);
		}*/
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
