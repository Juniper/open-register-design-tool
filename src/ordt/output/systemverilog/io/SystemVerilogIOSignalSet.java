package ordt.output.systemverilog.io;

import java.util.ArrayList;
import java.util.List;

public class SystemVerilogIOSignalSet extends SystemVerilogIOElement {
	protected List<SystemVerilogIOElement> childList = new ArrayList<SystemVerilogIOElement>(); // signals/signalsets in this signalset
	protected String type;   // type of this signalset  
	protected boolean hasExtType = false;   // type of this signalset  

	public SystemVerilogIOSignalSet(String tagPrefix, String name, int reps) { 
		this.name = (name == null)? "" : name;
		this.tagPrefix = tagPrefix;
		this.reps = reps; 
	}

	/** returns true if this element is a set */
    @Override
	public boolean isSignalSet() { return true; }
	
	/** returns true if this element is virtual (ie not an actually group in systemverilog output).
	 *  This method is overridden in child classes */
    @Override
	public boolean isVirtual() { return true; }
    	
    /** return true if this signalset is externally defined (has type) */
	public boolean hasExtType() {
		return hasExtType;
	}

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
	protected void addScalar(Integer from, Integer to, String namePrefix, String name) {
		this.addVector(from, to, namePrefix, name, 0, 1);
	}
	
	/** add a new vector signal to the child list 
	 */
	protected void addVector(Integer from, Integer to, String namePrefix, String name, int lowIndex, int size) {
		SystemVerilogIOSignal sig = new SystemVerilogIOSignal(from, to, namePrefix, name, lowIndex, size);
		childList.add(sig);
		//System.out.println("SystemVerilogIOSignalSet addVector: adding " + name + " to set " + getName());
	}
	
	/** add children of another IOsignallist to this list
	 * @param sigSet - signalset whose children will be added
	 */
	protected void addList(SystemVerilogIOSignalSet sigSet) {
		childList.addAll(sigSet.getChildList());  
	}

	/** add a nested signalset to this signalset */
	protected void addSignalSet(SystemVerilogIOSignalSet newSigSet) {
		childList.add(newSigSet);  // add set to signal list 
		//System.out.println("  SystemVerilogIOSignalSet addSignalSet: new intf=" + newSigSet.getName() + " to " + getName());
	}

	// ----------- methods to be overriden by child classes
		
	/** return a list of definitions for this sigset - overridden in SignalSet child classses  */  
	public List<String> getDefStrings() {
	   return null;
    }

	// ------------ methods overriding super

    /** return type of this signal */
	@Override
	public String getType() {
		return type;
	}

	/** return sv string instancing this element - assumes element name is full instance name */
	@Override
	public String getInstanceString(boolean addTagPrefix) {
        //System.out.println("SystemVerilogIOSignalSet getInstanceString: addTagPrefix=" + addTagPrefix + ", fullName=" + getFullName(null, addTagPrefix));
		return getType() + " " + getFullName(null, addTagPrefix) + getRepArray() + "();";
	}
	
	/** return sv string used in definition of this element in input/output lists - assumes element name is full instance name 
	 *   includes type if non-virtual sigset, name, and array *
	 *   @param addTagPrefix - if true signal tag prefix will be added to name
	 *   @param sigIOType - this string will be used as IO define type for IOSignals */
	@Override
	public String getIODefString(boolean addTagPrefix, String sigIOType) {
		//System.out.println("SystemVerilogIOSignalSet getIODefString: type=" + getType() + ", name=" + getFullName(null, addTagPrefix));
		return getType() + " " + getFullName(null, addTagPrefix) + getRepArray();
	}

	/** get rep array string */
	protected String getRepArray() {
		return isReplicated()? "[" + getReps() + "] " : "";
	}

	/** return a simple IOElement with full generated name */
	@Override
	public SystemVerilogIOElement getFullNameIOElement(String pathPrefix, boolean addTagPrefix) {
		String newTagPrefix = addTagPrefix? tagPrefix : "";
		return new SystemVerilogIOSignalSet(newTagPrefix, pathPrefix + name, reps);
	}
		
	// ------ methods returning IOElement lists
	
	/** return a flat list of IOelements with generated names for this signalset - recursively builds names top down * 
	 * @param fromLoc - only signals matching from will be returned
	 * @param toLoc - only signals matching to will be returned
	 * @param pathPrefix - prefix from ancestor levels that will be used to create child name
	 * @param addTagPrefix - if true, defined signal prefixes will be added to names
	 * @param stopOnNonVirtualSets - if true, recursion stops when a non-virtual signalset is hit (eg an interface)
	 * @param validEncap - if true, any matching elements in this signalset should be added, otherwise only those encapsulated in a non-virtual set
	 * @param omitCurrentName - if true, the name of the root set will not be appended to the prefix
	 * @param skipSets - if true, do not return any signalsets in output list
	 * @return - list of SystemVerilogIOSignal
	 */
	public List<SystemVerilogIOElement> getIOElementList(Integer fromLoc, Integer toLoc, String pathPrefix, boolean addTagPrefix, 
			boolean stopOnNonVirtualSets, boolean validEncap, boolean omitCurrentName, boolean skipSets) {
		List<SystemVerilogIOElement> outList = new ArrayList<SystemVerilogIOElement>();
		//System.out.println("SystemVerilogIOSignalSet getIOElementList: from=" + fromLoc + ", to=" + toLoc + ", pPrefix=" + pathPrefix + ", addTagPrefix=" + addTagPrefix + ", stopOnNonVirtualSets=" + stopOnNonVirtualSets + ", validEncap=" + validEncap + ", omitCurrentName=" + omitCurrentName);
		String prefix = omitCurrentName? pathPrefix : getFullName(pathPrefix, false);  // add current name to prefix
		String suffixChar = hasNoName()? "" : "_";
	    // process each rep of this elem
		for (int idx=0; idx<getReps(); idx++) {
			String repSuffix = isReplicated()? "_" + idx : ""; 
			String fullPrefix = (prefix == null)? "" : prefix + repSuffix + suffixChar;
			//System.out.println("SystemVerilogIOSignalSet getIOElementList: idx=" + idx + ", fullPrefix=" + fullPrefix);
			for (SystemVerilogIOElement ioElem : childList) {
				boolean newValidEncap = validEncap || !isVirtual();  // child elems are valid if this element in non-virtual
				boolean validLeaf = !(ioElem.isVirtual() || (ioElem.isSignalSet() && !stopOnNonVirtualSets) || !newValidEncap);
				boolean validLoc = ioElem.isSignalSet() || (ioElem.isFrom(fromLoc) && ioElem.isTo(toLoc)); // only consider leaf location
				//System.out.println("SystemVerilogIOSignalSet getIOElementList child: name=" + ioElem.getName() + ", newValidEncap=" + newValidEncap + ", validLeaf=" + validLeaf + ", validLoc=" + validLoc);
				// if this is leaf element then return it
				if (validLeaf && validLoc) {
					if (!(ioElem.isSignalSet() && skipSets)) outList.add(ioElem.getFullNameIOElement(fullPrefix, addTagPrefix));  // create a new IOElem and add to list
				}		
				// otherwise if a signalset, make recursive call 
				else if (ioElem.isSignalSet()) {
					List<SystemVerilogIOElement> newList = ((SystemVerilogIOSignalSet) ioElem).getIOElementList(fromLoc, toLoc, fullPrefix, addTagPrefix, stopOnNonVirtualSets, newValidEncap, false, skipSets);
					outList.addAll(newList);
				}
			}
		}
		//System.out.println("  SystemVerilogIOSignalSet getIOSignalList: output size=" + outList.size());
		return outList;
	}

	/** return a simple IOelement list of this signalset's first-level non-virtual descendents that match specified from/to params.  
	 *  Assumes no pathPrefix and addTagPrefix=true. Called by module input/output gen methods
	 * @param fromLoc - only signals matching from will be returned
	 * @param toLoc - only signals matching to will be returned
	 * @param skipSets - if true, do not return any signalsets in output list
	 */
	public List<SystemVerilogIOElement> getDescendentIOElementList(Integer fromLoc, Integer toLoc, boolean skipSets) {
		List<SystemVerilogIOElement> outList = getIOElementList(fromLoc, toLoc, null, true, true, true, false, skipSets);
		//System.out.println("SystemVerilogIOSignalSet getDescendentIOElementList: name=" + getName() + ", from=" + fromLoc + ", to=" + toLoc);
		//for (SystemVerilogIOElement elem : outList) System.out.println("  type=" + elem.getType() + ", name=" + elem.getName());
		return outList;
	}
	
	/** return a simple IOelement list of this signalset's first-level non-virtual descendents (all)
	 *  Assumes no pathPrefix and addTagPrefix=false. Called in Non-virtual signalset instance string generation
	 */
	public List<SystemVerilogIOElement> getLocalDescendentIOElementList() {  
		List<SystemVerilogIOElement> outList = getIOElementList(null, null, null, false, true, true, true, false); // omitCurrentName=true
		return outList;
	}
	
	/** return a flat list of simple SystemVerilogIOElement with full generated names for this signalset - recursively builds names top down 
	 *  and should be called from root of signal list
	 * @param fromLoc - only signals matching from will be returned
	 * @param toLoc - only signals matching to will be returned
	 * @return - list of SystemVerilogIOSignal
	 */
	public List<SystemVerilogIOElement> getEncapsulatedIOElementList(Integer fromLoc, Integer toLoc) {
		return getIOElementList(fromLoc, toLoc, null, true, false, false, false, false);
	}

 
	// ------- methods returning IOSignalSets
	
	/** return a list of all non virtual signalsets in current - return list leaf first
	 * @param noExtDefined - if true, only return signalsets that are not externally defined
	 */
	public List<SystemVerilogIOSignalSet> getNonVirtualSignalSets(boolean noExtDefined) {
		List<SystemVerilogIOSignalSet> outList = new ArrayList<SystemVerilogIOSignalSet>();
		for (SystemVerilogIOElement ioElem: childList) {
			// only add to list is intf is locally defined and first rep
			if (ioElem.isSignalSet()) {
				SystemVerilogIOSignalSet ioSigSet = (SystemVerilogIOSignalSet) ioElem;
				outList.addAll(ioSigSet.getNonVirtualSignalSets(noExtDefined));  // call recursively on children
				if (!ioSigSet.isVirtual() && !(ioSigSet.hasExtType() && noExtDefined)) outList.add(ioSigSet);
			}
		}
		//System.out.println("SystemVerilogIOSignalSet getNonVirtualSignalSets: n=" + outList.size());
		return outList;
	}

	// ------ string output methods
	
	/** return a list of assignment strings for this signalset - recursively builds names top down * 
	 * @param insideLocations - only assigns to/from this location will be returned
	 * @param sigsOnInside - true if signals are used inside insideLocations, hierarchy outside
	 * @param pathPrefix - prefix from ancestor levels that will be used to create child name
	 * @param hierPathPrefix - hierarchical path prefix from ancestor levels that will be used to create hier child name
	 * @param validEncap - if true, any matching elements in this signalset should be added, otherwise only those encapsulated in a non-virtual set
	 * @param foundFirstNonVirtual - if true, first non-virtual element in hierarchy was already found, so no name prefix in hier
	 * @return - list of SystemVerilogIOSignal
	 */
	public List<String> getNonVirtualAssignStrings(Integer insideLocations, boolean sigsOnInside, String pathPrefix, String hierPathPrefix, 
			boolean validEncap, boolean foundFirstNonVirtual) {
		List<String> outList = new ArrayList<String>();
		System.out.println("SystemVerilogIOSignalSet getNonVirtualAssignStrings: hierPathPrefix=" + hierPathPrefix);
		String prefix = getFullName(pathPrefix, false);  // add current name to prefix
		String suffixChar = hasNoName()? "" : "_";
		String hierSuffixChar = hasNoName()? "" : isVirtual()? "_" : ".";
		String newHierPrefix = getFullName(hierPathPrefix, !(isVirtual() || foundFirstNonVirtual));  // first non-virtual sets the prefix
	    // process each rep of this elem
		for (int idx=0; idx<getReps(); idx++) {
			// build hier and non-hier name prefixes
			String repSuffix = isReplicated()? "_" + idx : ""; 
			String fullPrefix = prefix + repSuffix + suffixChar;
			String hierRepSuffix = (isReplicated() && !isVirtual())? "[" + idx + "]" : repSuffix;
			String fullHierPrefix = newHierPrefix + hierRepSuffix + hierSuffixChar;
			boolean newValidEncap = validEncap || !isVirtual();  // mark encap as valid if not set already
			boolean newFoundFirstNonVirtual = foundFirstNonVirtual || !isVirtual();  // mark first non-virtual if not set already
			for (SystemVerilogIOElement ioElem : childList) {
				boolean validLeaf = !(ioElem.isVirtual() || ioElem.isSignalSet() || !newValidEncap); 
				// if this is leaf element then return it
				if (validLeaf) {
					String signalName = ioElem.getFullName(fullPrefix, true);
					String hierName = ioElem.getFullName(fullHierPrefix, false);
					// assignments for signals into insideLocations
					if (ioElem.isTo(insideLocations)) {
						if (sigsOnInside) outList.add(signalName + " = " + hierName + ";");
						else outList.add(hierName + " = " + signalName + ";");
					}
					else if (ioElem.isFrom(insideLocations)) {
						if (sigsOnInside) outList.add(hierName + " = " + signalName + ";");
						else outList.add(signalName + " = " + hierName + ";");
					}
				}		
				// otherwise if a signalset, make recursive call 
				else if (ioElem.isSignalSet()) {
					List<String> newList = ((SystemVerilogIOSignalSet) ioElem).getNonVirtualAssignStrings(insideLocations, sigsOnInside, fullPrefix, fullHierPrefix, newValidEncap, newFoundFirstNonVirtual);
					outList.addAll(newList);
				    }
			}
		}
		//System.out.println("  SystemVerilogIOSignalSet getIOSignalList: output size=" + outList.size());
		return outList;
	}
		
}