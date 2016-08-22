/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved.
 */
package ordt.output.systemverilog.oldio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ordt.output.systemverilog.SystemVerilogSignal;

/** class defining a a SV interface containing list of child signals/interfaces.  This class is
 * used as part of composite class SystemVerilogIOSignal to represent SV interface hierarchy  */
public class SystemVerilogInterface {
	private SystemVerilogIOSignal parent = null;  // parent signal containing this list
	protected List<SystemVerilogIOSignal> signalList = new ArrayList<SystemVerilogIOSignal>(); // signals/interfaces in this interface
	private HashMap<String, Integer> localIntfSigReps = new HashMap<String, Integer>();  // reps of each locally instanced intf 
	
	/** create a new interface with same name  */
	public SystemVerilogInterface(SystemVerilogIOSignal parent, int repCount) {
		this.parent = parent;
	}

	// ------ name and replication info are stored in parent IOSignal
	
	/** get name from parent IOSignal */
	public String getName() {
		if (parent==null) return "";
		return parent.getName();
	}

	/** get repCount from parent IOSignal*/
	public int getRepCount() {
		if (parent==null) return 1;
		return parent.getRepCount();
	}

	/** get repNum from parent IOSignal*/
	public int getRepNum() {
		if (parent==null) return 0;
		return parent.getRepNum();
	}
	
	/** return isReplicated() from parent IOSignal*/
	public boolean isReplicated() {
		if (parent==null) return false;
		return parent.isReplicated();
	}
	
	/** return isFirstRep() from parent IOSignal*/
	public boolean isFirstRep() {
		if (parent==null) return true;
		return parent.isFirstRep();
	}
	
	// ---------------  
	
	public Integer size() {
		return signalList.size();
	}
	
	/** return the full signal list  
	 */
	public List<SystemVerilogIOSignal> getIOSignalList() {
		return signalList;
	}
	
	// --------------- child signal/interface add methods 
	
	/** add a new scalar signal to the list (extended w/ to/from info)
	 */
	public void addScalar(Integer from, Integer to, String name) {
		this.addVector(from, to, name, 0, 1);
	}
	
	/** add a new vector signal to the list 
	 */
	public void addVector(Integer from, Integer to, String name, int lowIndex, int size) {
		SystemVerilogIOSignal sig = new SystemVerilogIOSignal(from, to, name, lowIndex, size);
		signalList.add(sig);
		//System.out.println("SystemVerilogInterface addVector: adding " + name + " to interface " + getName());
	}

	/** add a nested interface signal to this interface map 
	 * @param extName */
	public SystemVerilogInterface addInterface(Integer from, Integer to, String indexedName, String repName, int repCount, String extName) {
		String noRepName = indexedName.replaceAll("\\[\\d+\\]", "");  // create norep name from indexed name
		// if a new interface sig, reset rep count, else increment it
		int rep = 0;
		if (!localIntfSigReps.containsKey(noRepName)) localIntfSigReps.put(noRepName, 0);  // localIntfSigReps
		else {
			rep = localIntfSigReps.get(noRepName) + 1;  // bump the rep
			localIntfSigReps.put(noRepName, rep);
		}
		//System.out.println("SystemVerilogInterface addInterface: indexedName=" + indexedName + ", repName=" + repName + ", repCount=" + repCount + ", rep=" + rep);
		// create a new interface
		SystemVerilogIOSignal intfSig = new SystemVerilogIOSignal(from, to, indexedName, repName, rep, repCount, extName);  // create intfSig to hold mappings for array instances
		signalList.add(intfSig);  // add interface to signal list
		//System.out.println("  SystemVerilogInterface addInterface: new intf=" + intfSig.getName() + " to " + getName());
		return intfSig.getIntfList();
	}

	// -------------------
	
	/** return all encapsulated non-intf signals in this interface - recursive */   
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
	
	/** return all encapsulated non-intf signals in this interface - recursive */  
	public List<SystemVerilogIOSignal> getIOSignalList(Integer fromLoc, Integer toLoc) {
		List<SystemVerilogIOSignal> outList = new ArrayList<SystemVerilogIOSignal>();
		//System.out.println("  SystemVerilogInterface getSignalList: sigs size=" + sigs.size());
		for (SystemVerilogIOSignal ioSig : signalList) {
			// if this signal is an interface get all encapsulated signals
			if (ioSig.isIntfSig()) {
				List<SystemVerilogIOSignal> newList = ioSig.getIOSignalList(fromLoc, toLoc);
				outList.addAll(newList);
			}
			// otherwise add simple signal to list if from/to match
			else if (ioSig.isFrom(fromLoc) && ioSig.isTo(toLoc)) {
				outList.add(ioSig);
			}
		}
		//System.out.println("  SystemVerilogIntfList getIOSignalList: output size=" + outList.size());
		return outList;
	}
	
	/** get a signalList including intrface encaps matching specified from/to params - no recursion
	 * @param fromLoc
	 * @param toLoc 
	 */
	public List<SystemVerilogIOSignal> getEncapsulatedIOSignalList(Integer fromLoc, Integer toLoc) {
		List<SystemVerilogIOSignal> outList = new ArrayList<SystemVerilogIOSignal>();
		for (SystemVerilogIOSignal sig: signalList) {
			if (sig.isFrom(fromLoc) && sig.isTo(toLoc)) outList.add(sig);
		}
		return outList;
	}

	/** return a list of all internally defined interfaces leaf first */
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
	
	// ----------------- 

	/** return local name of signals after stripping off intf hierarchy */
	private String getChildName(SystemVerilogIOSignal sig) {
		String outName = null;
		// just use names for non-intf child extract
		String childFullName = sig.getName();  
		String baseName = getName();
		// if new signal is an interface, compare child and base indexed names
		if (sig.isIntfSig()) {
			childFullName = sig.getIndexedName(); 
			baseName = parent.getIndexedName();			
			baseName = baseName.replaceAll("\\[", "\\\\\\[");  // need to escape brackets
			baseName = baseName.replaceAll("\\]", "\\\\\\]");  
		}
		//System.out.println("SystemVerilogIntfList getChildName: basename=" + baseName );
		if (baseName.isEmpty()) return outName;
		baseName = baseName.replaceFirst("[dl]h_", "");  // remove interface prefix for compare
		// extract the child name by removing signal prefix and interface basename
		Pattern p = Pattern.compile("([a-z][a-z]?2?[a-z][a-z]?_)" + baseName + "_(.*)");  
		Matcher m = p.matcher(childFullName);
		if (m.matches()) {
			outName = m.group(2);
			if (sig.isIntfSig()) outName = outName.replaceFirst("\\[\\d+\\]", ""); // remove reps from interface child name
		}
		if (outName == null) {
			//System.out.println("SystemVerilogInterface getChildName: no match for sig=" + childFullName + " using basename=" + baseName);
		}
		return outName;
	}
	
	// ----------------- methods returning sv formatted strings for output
	
	/** return a list define strings for this interface map */
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

	/** return a list of strings assigning interfaces to corresponding logic IO */   
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
	
}
