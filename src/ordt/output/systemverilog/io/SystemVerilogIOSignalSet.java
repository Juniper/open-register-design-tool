package ordt.output.systemverilog.io;

import java.util.ArrayList;
import java.util.List;

public class SystemVerilogIOSignalSet extends SystemVerilogIOElement {
	protected List<SystemVerilogIOElement> childList = new ArrayList<SystemVerilogIOElement>(); // signals/signalsets in this signalset
	protected String type = null;   // type of this signalset  

	public SystemVerilogIOSignalSet(String namePrefix, String name, int reps) { // TODO - need to set prefix, do we need from/to for virtual
		this.name = name;
		this.namePrefix = namePrefix;
		this.repCount = reps;  // signal repcount is always 1
	}

	/** returns true if this element is a set */
    @Override
	protected boolean isSignalSet() { return true; }
	
	/** returns true if this element is virtual (ie not an actually group in systemverilog output).
	 *  This method is overridden in child classes */
    @Override
	protected boolean isVirtual() { return true; }
	
	/** returns true if this signalset has no children
	 *  This method is overridden in child classes
	 */
	protected boolean isEmpty() {
		return childList.isEmpty();
	}
	
	public List<SystemVerilogIOElement> getChildList() {
		return childList;
	}
	
	// --------------- child signal/interface add methods 

	/** add a new scalar signal to the child list (extended w/ to/from info)
	 */
	public void addScalar(Integer from, Integer to, String namePrefix, String name) {
		this.addVector(from, to, namePrefix, name, 0, 1);
	}
	
	/** add a new vector signal to the child list 
	 */
	public void addVector(Integer from, Integer to, String namePrefix, String name, int lowIndex, int size) {
		SystemVerilogIOSignal sig = new SystemVerilogIOSignal(from, to, namePrefix, name, lowIndex, size);
		childList.add(sig);
		//System.out.println("SystemVerilogIOSignalSet addVector: adding " + name + " to set " + getName());
	}
	
	/** add children of another IOsignallist to this list
	 * @param sigSet - signalset whose children will be added
	 */
	public void addList(SystemVerilogIOSignalSet sigSet) {
		childList.addAll(sigSet.getChildList());  
	}

	/** add a nested signalset to this signalset */
	public void addSignalSet(SystemVerilogIOSignalSet newSigSet) {
		childList.add(newSigSet);  // add set to signal list 
		//System.out.println("  SystemVerilogIOSignalSet addSignalSet: new intf=" + newSigSet.getName() + " to " + getName());
	}

	// ----------- methods to be overriden by child classes
	
	
	/** return a list of definitions for this element */
	public List<String> getDefStrings(String pathPrefix) {
	   return null;
    }

    // ------------ methods overriding super
	
	@Override
	public String getInstanceString() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<SystemVerilogIOSignal> getIOSignals(Integer fromLoc, Integer toLoc) {
		// TODO Auto-generated method stub
		return null;
	}
	
	// ------ output generation methods
/*
 
	/** return all encapsulated non-intf signals in this interface - recursive *   TODO generate this from getIOSignalList
	public List<SystemVerilogSignal> getSignalList(Integer fromLoc, Integer toLoc) {
		List<SystemVerilogSignal> outList = new ArrayList<SystemVerilogSignal>();
		//System.out.println("  SystemVerilogInterface getSignalList: sigs size=" + sigs.size());
		for (SystemVerilogIOSignal ioSig : signalList) {
			// if this signal is an interface get all encapsulated signals
			if (ioSig.isIntfSig()) {
				List<SystemVerilogSignal> newList = ioSig.getSignalList(fromLoc, toLoc);
				outList.addAll(newList);
			}
			// otherwise add simple signal to list if from/to match
			else if (ioSig.isFrom(fromLoc) && ioSig.isTo(toLoc)) {
				outList.add(ioSig);
			}
		}
		//System.out.println("  SystemVerilogIntfList getSignalList: output size=" + outList.size());
		return outList;
	}
*/	
	
	/** return a flat list of IOsignals in this signalset - recursive */ 
	public List<SystemVerilogIOSignal> getIOSignalList(Integer fromLoc, Integer toLoc, String pathPrefix) {
		List<SystemVerilogIOSignal> outList = new ArrayList<SystemVerilogIOSignal>();
		//System.out.println("  SystemVerilogInterface getSignalList: sigs size=" + sigs.size());
		for (SystemVerilogIOElement ioElem : childList) {
			String prefix = ((pathPrefix == null) || pathPrefix.isEmpty())? "" : ioElem.getName() + "_";
			// if this elem is an set, get all encapsulated signals
			if (ioElem.isSignalSet()) {
				//List<SystemVerilogIOSignal> newList = ioElem.getIOSignalList(fromLoc, toLoc, prefix);  TODO --- move to IOElement
				//outList.addAll(newList);
			}
			// otherwise add simple signal to list if from/to match
			else if (ioElem.isFrom(fromLoc) && ioElem.isTo(toLoc)) {
				//outList.add(new SystemVerilogIOSignal(ioElem, );  // use same IOElem defined abstract method / create new IOSig and add to list
			}
		}
		//System.out.println("  SystemVerilogIOSignalSet getIOSignalList: output size=" + outList.size());
		return outList;
	}
	
/*	
	/** get a signalList including intrface encaps matching specified from/to params - no recursion  <-- gets the local list, need to cast sigsets to sigs
	 * @param fromLoc
	 * @param toLoc 
	 *
	public List<SystemVerilogIOSignal> getEncapsulatedIOSignalList(Integer fromLoc, Integer toLoc) {
		List<SystemVerilogIOSignal> outList = new ArrayList<SystemVerilogIOSignal>();
		for (SystemVerilogIOSignal sig: signalList) {
			if (sig.isFrom(fromLoc) && sig.isTo(toLoc)) outList.add(sig);
		}
		return outList;
	}

	/** return a list of all internally defined interfaces leaf first *  <--- used in intr wrapper gen
	public List<SystemVerilogIOSignal> getIntfsToBeDefined() {
		List<SystemVerilogIOSignal> outList = new ArrayList<SystemVerilogIOSignal>();
		for (SystemVerilogIOSignal sig: signalList) {
			// only add to list is intf is locally defined and first rep
			if (sig.isIntfSig() && sig.isFirstRep()) {
				outList.addAll(sig.getIntfsToBeDefined());  // call recursively on children
				if (sig.isLocalDefinedInterface()) outList.add(sig);
			}
		}
		//System.out.println("SystemVerilogIntfList getDefinedIntfSignals: n=" + outList.size());
		//if (outList.size()>0) for (SystemVerilogIOSignal sig: outList) System.out.println("  SystemVerilogIntfList getDefinedIntfSignals: sig=" + sig.getInterfaceDefName());
		return outList;
	}
 
	/** return a list of signals encapsulated in interfaces (no root sigs) - recursive *   <--- this is from SVIOSignalList, gets list of full signals in interfaces
	public List<SystemVerilogSignal> getIntfEncapsulatedSignalList(int insideLocations) {
		List<SystemVerilogSignal> outList = new ArrayList<SystemVerilogSignal>();
		for (SystemVerilogIOSignal sig: signalList) {
			// if this signal is an interface, call recursively to get all encapsulated signals
			if (sig.isIntfSig()) {
				//System.err.println("   SystemVerilogIOSignalList getInterfaceAssignStrList: name=" + sig.getName());
				List<SystemVerilogSignal> newList = sig.getSignalList(null, insideLocations);  // get inputs
				outList.addAll(newList);
				newList = sig.getSignalList(insideLocations, null);  // get outputs
				outList.addAll(newList);
			}
		}
		return outList;
	}

 */
	// ------ output generation methods
	
/*

	// ----------------- methods returning sv formatted strings for output
	
	/** return a list define strings for this interface map *
	public List<String> getIntfDefStrings() {
		List<String> outList = new ArrayList<String>();
		//System.out.println("SystemVerilogInterface getIntfDefStrings: name=" + getName() + ", sigs n=" + signalList.size());
		for (SystemVerilogIOSignal sig: signalList) {
			// extract sig string  
			//String newName = getLocalName(sig);  
			String newName = getChildName(sig);  
			if (newName != null) {
				// if logic use std def
				if (!sig.isIntfSig()) {
					String defString = "logic " + sig.getDefArray() + newName + ";";
					outList.add(defString);
					//System.out.println("   SystemVerilogInterface getIntfDefStrings: sig=" + defString);
				}
				// else if an interface use it's name and array size
				else if (sig.isFirstRep()) {
					String defString = sig.getInterfaceDefName() + " " + newName + sig.getDefArray() + "();";
					outList.add(defString);
					//System.out.println("   SystemVerilogInterface getIntfDefStrings: intf=" + defString);
				}
			}
			// replicated interface sigs will fail to match and so will not be added
			else {
				//System.err.println("   SystemVerilogInterface getIntfDefStrings: elem name=" + sig.getName()); 
				//System.out.println("---SystemVerilogInterface getIntfDefStrings: match failed for " + sig.getName());
			}
		}
		return outList;
	}

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
