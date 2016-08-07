package ordt.output.systemverilog.io;

import java.util.ArrayList;
import java.util.List;

import ordt.output.systemverilog.SystemVerilogSignal;

public class SystemVerilogIOSignalSet extends SystemVerilogIOElement {
	protected List<SystemVerilogIOElement> childList = new ArrayList<SystemVerilogIOElement>(); // signals/signalsets in this signalset
	protected String type = null;   // type of this signalset  
	protected boolean hasExtType = false;   // type of this signalset  

	public SystemVerilogIOSignalSet(String tagPrefix, String name, int reps) { 
		this.name = (name == null)? "" : name;
		this.tagPrefix = tagPrefix;
		this.reps = reps; 
	}

	/** returns true if this element is a set */
    @Override
	protected boolean isSignalSet() { return true; }
	
	/** returns true if this element is virtual (ie not an actually group in systemverilog output).
	 *  This method is overridden in child classes */
    @Override
	protected boolean isVirtual() { return true; }
    	
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
		
	/** return a list of definitions for this sigset - overridden in SignalSet child classses  */  
	public List<String> getDefStrings() {
	   return null;
    }
	
	/** return a list of assign statements for this sigset mapping from simple signals to hierarchical- overriden in SignalSet child classses */  
	public List<String> getAssignMapStrings(String pathPrefix) {
	   return null;
    }

	/** get rep array string */
	protected String getRepArray() {
		return isReplicated()? "[" + getReps() + "] " : "";
	}
    // ------------ methods overriding super

    /** return type of this signal */
	@Override
	public String getType() {
		return type;
	}

	/** return sv string instancing this element - assumes element name is full instance name */
	@Override
	public String getInstanceString() {
		return getType() + " " + getRepArray() + getFullName();
	}

	/** return a simple IOElement with full generated name */
	@Override
	public SystemVerilogIOElement getFullNameIOElement(String pathPrefix, boolean addTagPrefix) {
		String newTagPrefix = addTagPrefix? tagPrefix : "";
		return new SystemVerilogIOSignalSet(newTagPrefix, pathPrefix + name, reps);
	}
		
	// ------ IO model output methods
	
	/** return a flat list of IOelements with generated names for this signalset - recursively builds names top down * 
	 * @param fromLoc - only signals matching from will be returned
	 * @param toLoc - only signals matching to will be returned
	 * @param pathPrefix - prefix from ancestor levels that will be used to create child name
	 * @param addTagPrefix - if true, defined signal prefixes will be added to names
	 * @param stopOnNonVirtualSets - if true, recursion stops when a non-virtual signalset is hit (eg an interface)
	 * @param inhibitVirtualEncaps - if true, only signals in at least one non-virtual signalset are returned
	 * @return - list of SystemVerilogIOSignal
	 */
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
	
	/** return a flat list of simple SystemVerilogIOElement with full generated names for this signalset - recursively builds names top down 
	 * and should be called from root of signal list
	 * @param fromLoc - only signals matching from will be returned
	 * @param toLoc - only signals matching to will be returned
	 * @return - list of SystemVerilogIOSignal
	 */
	public List<SystemVerilogIOElement> getIOElementList(Integer fromLoc, Integer toLoc) {
		return getIOElementList(fromLoc, toLoc, null, true, false, false);
	}
	
	/** return a flat list of simple SystemVerilogIOElement with full generated names for this signalset - recursively builds names top down 
	 * and should be called from root of signal list
	 * @param fromLoc - only signals matching from will be returned
	 * @param toLoc - only signals matching to will be returned
	 * @return - list of SystemVerilogIOSignal
	 */
	public List<SystemVerilogIOElement> getEncapsulatedIOElementList(Integer fromLoc, Integer toLoc) {
		return getIOElementList(fromLoc, toLoc, null, true, false, true);
	}
	 
	/** return a flat list of simple SystemVerilogSignal with full generated names for this signalset. 
	 * Generates a list of SystemVerilogIOElement and then converts to SystemVerilogSignal -
	 * should be called from root of signal list
	 * @param fromLoc - only signals matching from will be returned
	 * @param toLoc - only signals matching to will be returned
	 * @return - list of SystemVerilogSignal
	 */
	public List<SystemVerilogSignal> getSignalList(Integer fromLoc, Integer toLoc) {
		List<SystemVerilogIOElement> IOElems = getIOElementList(fromLoc, toLoc);
		return getSignalList(IOElems);
	}
	
	/** return a flat list of simple SystemVerilogSignal with full generated names for this signalset  
	 * Generates a list of SystemVerilogIOElement and then converts to SystemVerilogSignal -
	 * and should be called from root of signal list
	 * @param fromLoc - only signals matching from will be returned
	 * @param toLoc - only signals matching to will be returned
	 * @return - list of SystemVerilogIOSignal
	 */
	public List<SystemVerilogSignal> getEncapsulatedSignalList(Integer fromLoc, Integer toLoc) {
		List<SystemVerilogIOElement> IOElems = getEncapsulatedIOElementList(fromLoc, toLoc);
		return getSignalList(IOElems);
	}

	/** convert a flat list of SystemVerilogIOElement to a list of SystemVerilogSignal with full generated names for this signalset. 
	 * @return - list of SystemVerilogSignal
	 */
	public List<SystemVerilogSignal> getSignalList(List<SystemVerilogIOElement> ioElemList) {
		List<SystemVerilogSignal> outList = new ArrayList<SystemVerilogSignal>();
		//System.out.println("  SystemVerilogInterface getSignalList: sigs size=" + sigs.size());
		for (SystemVerilogIOElement ioElem : ioElemList) {
			if (ioElem.isSignalSet())
			    outList.add(new SystemVerilogSignal(ioElem.getFullName(null, true), 0, 1));
			else {
				SystemVerilogIOSignal ioSig = (SystemVerilogIOSignal) ioElem;
			    outList.add(new SystemVerilogSignal(ioSig.getFullName(null, true), ioSig.getLowIndex(), ioSig.getSize()));
			}
		}
		//System.out.println("  SystemVerilogIOSignalSet getSignalList: output size=" + outList.size());
		return outList;
	}
	

	/** return a simple IOelement list of this signalset's children matching specified from/to params (non virtual children).  
	 * Assumes no pathPrefix. Called by module input/output gen methods
	 */
	public List<SystemVerilogIOElement> getChildIOElementList(Integer fromLoc, Integer toLoc) {  // was getEncapsulatedIOSignalList
		List<SystemVerilogIOElement> outList = getIOElementList(fromLoc, toLoc, null, true, true, false);
		return outList;
	}
	
	/** return a simple IOelement list of this signalset's non-virtual children  
	 * Assumes no pathPrefix. Called in Non-virtual signalset definition generation
	 */
	public List<SystemVerilogIOElement> getLocalChildIOElementList() {  
		List<SystemVerilogIOElement> outList = getIOElementList(null, null, null, false, true, false);
		return outList;
	}
	
 
	/** return a list of all non virtual signalsets in current - return list leaf first *  was getIntfsToBeDefined()
	 * @param noExtDefined - if true, only return signalsets that are not externally defined
	 */
	public List<SystemVerilogIOSignalSet> getNonVirtualSignalSets(boolean noExtDefined) {
		List<SystemVerilogIOSignalSet> outList = new ArrayList<SystemVerilogIOSignalSet>();
		for (SystemVerilogIOElement ioElem: childList) {
			// only add to list is intf is locally defined and first rep
			if (ioElem.isSignalSet()) {
				SystemVerilogIOSignalSet ioSigSet = (SystemVerilogIOSignalSet) ioElem;
				outList.addAll(ioSigSet.getNonVirtualSignalSets(noExtDefined));  // call recursively on children
				if (!ioSigSet.isVirtual() && (ioSigSet.hasExtType() || !noExtDefined)) outList.add(ioSigSet);
			}
		}
		//System.out.println("SystemVerilogIOSignalSet getNonVirtualSignalSets: n=" + outList.size());
		return outList;
	}
		
}