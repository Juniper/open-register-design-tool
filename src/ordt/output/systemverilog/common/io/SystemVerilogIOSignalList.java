package ordt.output.systemverilog.common.io;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import ordt.output.systemverilog.common.RemapRuleList;
import ordt.output.systemverilog.common.SystemVerilogDefSignalTypeIntf;
import ordt.output.systemverilog.common.SystemVerilogDefinedSignalMap;
import ordt.output.systemverilog.common.SystemVerilogSignal;
import ordt.output.systemverilog.common.wrap.SystemVerilogWrapModule.WrapperSignalMap;


/** this is the main class used to hold IO lists for building sv modules.
 * maintains a stack of active signalsets where new signals/sets will be added by svbuilder */
public class SystemVerilogIOSignalList extends SystemVerilogIOSignalSet {
	private String listName;
	private Stack<SystemVerilogIOSignalSet> activeSetStack = new Stack<SystemVerilogIOSignalSet>();  // track active signalsets for inserts 
	private Stack<Boolean> inhibitInsertStack = new Stack<Boolean>();  // track insert inhibited signalsets (inhibit subsequent reps) 

	public SystemVerilogIOSignalList(String listName) {
		super(null, null, 1);  // IOsignalList has no tag, no name and one rep
		this.listName = listName;
	}

	/** copy an IOSignalList, keeping elements matching specified rules and not in a unique name list.
	 * virtual elements will be added w/o rule matching  
	 * 
	 * @param origList - IOSignalList be copied
	 * @param rules - RemapRuleList to be applied for matching
	 * @param uniqueList - if non-null, only elements with names not in the set will be copied
	 */
	public SystemVerilogIOSignalList(SystemVerilogIOSignalList origList, RemapRuleList rules, HashSet<String> uniqueList) {
		super(origList, rules, uniqueList, null);
		this.listName = origList.listName;
	}

	public String getListName() {
		return listName;
	}

	private Stack<SystemVerilogIOSignalSet> getActiveSetStack() {
		return activeSetStack;
	}

	/*
	public String listStack() {
		String outStr = "[";
		for (SystemVerilogIOSignalSet sigSet: activeSetStack) outStr += " " + sigSet.getName();
		return outStr += " ]";
	}*/

	/** copy the activeStack contents from specified list to the current list 
	 * @param sigSet - signalList whose active stack will be recreated in current signalList
	 * @param sigSetType - signal type that represents a general signal set in this application 
	 */
	public void copyActiveSetStack(SystemVerilogIOSignalList sigList, SystemVerilogDefSignalTypeIntf sigSetType) {
		for (SystemVerilogIOSignalSet sigSet: sigList.getActiveSetStack()) {
			//System.out.println("SystemVerilogIOSIgnalList copyActiveSetStack: adding sigSet " + sigSet.getName());
			this.pushIOSignalSet(sigSetType, sigSet.getName(), 1, true, null, null);
		}
	}
	
	// --------------- child signal/interface add methods 

	/** return true if a child element add should be inhibited due to non-first rep count */
	private boolean inhibitAdd() {
		return (!inhibitInsertStack.isEmpty() && inhibitInsertStack.peek());
	}

	// --- add vectors
	
	/** add a new vector signal to the active list */
	@Override
	public void addVector(Integer from, Integer to, String tagPrefix, String name, int lowIndex, int size) {
		if (inhibitAdd()) return;
		if (activeSetStack.isEmpty()) super.addVector(from, to, tagPrefix, name, lowIndex, size);  // empty stack, so add to this list
		else activeSetStack.peek().addVector(from, to, tagPrefix, name, lowIndex, size);  // otherwise add to active list
		//System.out.println("SystemVerilogIOSignalList addVector: adding " + name + ", from=" + from + ", to=" + to + ", list=" + getListName() + ", stack empty=" + activeSetStack.isEmpty());
	}

	/** add a new vector signal to the active list */
	public void addVector(Integer from, Integer to, SystemVerilogDefSignalTypeIntf sigType, int lowIndex, int size) {
		String prefix = SystemVerilogDefinedSignalMap.getPrefix(sigType);
		String name = SystemVerilogDefinedSignalMap.getSuffix(sigType);
		addVector(from, to, prefix, name, lowIndex, size);
		//System.out.println("SystemVerilogIOSignalList addVector: adding " + sigType + ", from=" + from + ", to=" + to + ", stack empty=" + activeSetStack.isEmpty());
	}

	/** add a new vector signal to the active list using defined signal locations */
	public void addVector(SystemVerilogDefSignalTypeIntf sigType, int lowIndex, int size) {
		Integer from = SystemVerilogDefinedSignalMap.getFrom(sigType);
		Integer to = SystemVerilogDefinedSignalMap.getTo(sigType);
		addVector(from, to, sigType, lowIndex, size);
		//System.out.println("SystemVerilogIOSignalList addVector: adding " + sigType + ", from=" + from + ", to=" + to + ", stack empty=" + activeSetStack.isEmpty());
	}

	/** add a new simple vector to the root child list (no prefix) */
	public void addSimpleVector(Integer from, Integer to, String name, int lowIndex, int size, boolean signed) {
		super.addVector(from, to, null, name, lowIndex, size, signed);
	}
	
	/** add a new simple unsigned vector to the root child list (no prefix) */
	public void addSimpleVector(Integer from, Integer to, String name, int lowIndex, int size) {
		addSimpleVector(from, to, name, lowIndex, size, false);
		//System.out.println("SystemVerilogIOSignalList addSimpleVector: adding " + name + ", from=" + from + ", to=" + to + ", list=" + getListName());
	}

	/** add a new simple vector with freeform slice string to the root child list (no prefix) */
	public void addSimpleVector(Integer from, Integer to, String name, String slice) {
		super.addVector(from, to, null, name, slice);
	}

	/** add a new simple vector to the root child list (no prefix) */
	public void addSimpleVector(SystemVerilogDefSignalTypeIntf sigType, String namePrefix, int lowIndex, int size) {
		Integer from = SystemVerilogDefinedSignalMap.getFrom(sigType);
		Integer to = SystemVerilogDefinedSignalMap.getTo(sigType);
		String fullName = SystemVerilogDefinedSignalMap.getFullName(sigType, namePrefix, true);
		addSimpleVector(from, to, fullName, lowIndex, size);
	}

	/** add a new simple 2d vector array with freeform slice strings to the root child list (no prefix) */
	public void addSimpleVectorArray(Integer from, Integer to, String name, String packedSlice, String unpackedSlice) {
		super.addVectorArray(from, to, null, name, packedSlice, unpackedSlice);
	}
	
	// --- add scalars
	
	/** add a new scalar signal to the active list */
	@Override
	protected void addScalar(Integer from, Integer to, String namePrefix, String name) {
		this.addVector(from, to, namePrefix, name, 0, 1);
	}
	
	/** add a new scalar signal to the active list */
	public void addScalar(Integer from, Integer to, SystemVerilogDefSignalTypeIntf sigType) {
		String prefix = SystemVerilogDefinedSignalMap.getPrefix(sigType);
		String name = SystemVerilogDefinedSignalMap.getSuffix(sigType);
		addVector(from, to, prefix, name, 0, 1);
	}
	
	/** add a new scalar signal to the active list  using defined signal locations */
	public void addScalar(SystemVerilogDefSignalTypeIntf sigType) {
		Integer from = SystemVerilogDefinedSignalMap.getFrom(sigType);
		Integer to = SystemVerilogDefinedSignalMap.getTo(sigType);
		addVector(from, to, sigType, 0, 1);
		//System.out.println("SystemVerilogIOSignalList addVector: adding " + sigType + ", from=" + from + ", to=" + to + ", stack empty=" + activeSetStack.isEmpty());
	}

	/** add a new simple scalar to the root child list (no prefix) */
	public void addSimpleScalar(Integer from, Integer to, String name) {
		addSimpleVector(from, to, name, 0, 1);
	}

	/** add a new simple vector to the root child list (no prefix) */
	public void addSimpleScalar(SystemVerilogDefSignalTypeIntf sigType, String namePrefix) {
		addSimpleVector(sigType, namePrefix, 0, 1);
	}

	// --- add lists/signal sets
	
	/** add children of another IOsignallist to this list - use only in post signal build stage
	 * as the active stack is cleared
	 * @param sigSet - signalset to be added
	 */
	@Override
	public void addList(SystemVerilogIOSignalSet sigSet) {
		activeSetStack.clear();  // clear stack so all are added to signalList
		if (inhibitAdd()) return;
		super.addList(sigSet);  
	}

	/** if initial rep, create a new signal set class, add it to active stack
	 * @param from - from locations for this set
	 * @param to - to locations for this set
	 * @param namePrefix - prefix to be added to full set name
	 * @param name - local name of this set
	 * @param reps - number of replications
	 * @param isFirstRep - true if first rep is being processed
	 * @param isIntf - true if set is an interface
	 * @param isStruct - true if set is a struct
	 * @param extType - externally defined type of this set, if null interface type is defined by path
	 * @param compId - optional component ID for name generation
	 * @return
	 */
	public SystemVerilogIOSignalSet pushIOSignalSet(Integer from, Integer to, String namePrefix, String name, int reps, boolean isFirstRep, boolean isIntf, boolean isStruct, String extType, String compId) {
		// first determine if push is inhibited
		boolean inhibitAdd = inhibitAdd() || !isFirstRep;
		inhibitInsertStack.push(inhibitAdd);  // always push to the inhibit stack 
		if (inhibitAdd) return null;
		// else no inhibit then create the set and push to active stack
		SystemVerilogIOSignalSet newSigSet;
		boolean hasExtType = (extType !=null);
		String newType = hasExtType? extType : getCurrentStackPath();  // if internally defined, pass in the current path
		// create appropriate signalset
		if (isIntf) newSigSet = new SystemVerilogIOInterface (from, to, namePrefix, name, reps, newType, hasExtType, true, compId);  // create a new interface
		else if (isStruct) newSigSet = new SystemVerilogIOStruct (from, to, namePrefix, name, reps, newType, hasExtType, true, compId);  // create a new struct
		else newSigSet = new SystemVerilogIOSignalSet (namePrefix, name, reps);  // otherwise create default set
		// add to active element or root level
		if (activeSetStack.isEmpty()) super.addSignalSet(newSigSet);  // add new set at root
		else activeSetStack.peek().addSignalSet(newSigSet);  // otherwise add to active set
		// push this set onto the active definition stack (must push here always so pops match)
		activeSetStack.push(newSigSet);  
		//System.out.println("SystemVerilogIOSignalList pushIOSignalSet: adding " + name + ", from=" + from + ", to=" + to + ", list=" + getListName() + ", stack empty=" + activeSetStack.isEmpty());
		return newSigSet;
	}
	
	/** return a string of catenated sigset names on the activestack (no replication counts) */
	private String getCurrentStackPath() {
		String outName = "";
		Iterator<SystemVerilogIOSignalSet> iter = activeSetStack.iterator();
		while (iter.hasNext()) {
			outName += iter.next().getName();
			if (iter.hasNext()) outName += "_";
		}
		return outName;
	}

	/** if initial rep, create a new signal set class, add it to active stack */
	public SystemVerilogIOSignalSet pushIOSignalSet(SystemVerilogDefSignalTypeIntf sigType, String name, int reps, boolean isFirstRep, String extType, String compId) {
		Integer from = SystemVerilogDefinedSignalMap.getFrom(sigType);
		Integer to = SystemVerilogDefinedSignalMap.getTo(sigType);
		String prefix = SystemVerilogDefinedSignalMap.getPrefix(sigType);
		return pushIOSignalSet(from, to, prefix, name,  reps,  isFirstRep,  sigType.isInterface(), sigType.isStruct(), extType, compId);  
	}

	/** done defining signalset, so pop the stacks */
	public void popIOSignalSet() {
		//System.out.println("SystemVerilogIOSignalList popIOSignalSet: inhibitAdd=" + inhibitAdd());
		if (!inhibitAdd() && !activeSetStack.isEmpty()) {
			//System.out.println("SystemVerilogIOSignalList popIOSignalSet: popping " + activeSetStack.peek().getName() + " from active stack");
			activeSetStack.pop();  // pop this set if push wasn't inhibited
		}
		if (!inhibitInsertStack.isEmpty()) inhibitInsertStack.pop();  // always pop the inhibit stack 
	}
	 
	// -------

	/** return a flat list of simple SystemVerilogIOElement with full generated names for this signalset - recursively builds names top down 
	 *  and should be called from root of signal list (assumes addTagPrefix=true, stopOnNonVirtualSets=false, validEncap=true, skipSets = false) 
	 * @param fromLoc - only signals matching from will be returned
	 * @param toLoc - only signals matching to will be returned
	 * @return - list of SystemVerilogIOSignal
	 */
	public List<SystemVerilogIOElement> getIOElementList(Integer fromLoc, Integer toLoc) {
		return getFlatIOElementList(fromLoc, toLoc, null, true, false, true, false, false, false);
	}
	
	/** return a flat list of simple SystemVerilogSignal with full generated names for this signalset. 
	 *  Generates a list of SystemVerilogIOElement and then converts to SystemVerilogSignal -
	 *  should be called from root of signal list
	 * @param fromLoc - only signals matching from will be returned
	 * @param toLoc - only signals matching to will be returned
	 * @return - list of SystemVerilogSignal
	 */
	public List<SystemVerilogSignal> getSignalList(Integer fromLoc, Integer toLoc) {
		List<SystemVerilogIOElement> IOElems = getIOElementList(fromLoc, toLoc);
		return getSignalList(IOElems);
	}

	/** return a list of flat simple signals that are encapsulated in interfaces (no root sigs) - recursive 
	 * @param insideLocations - encapsulated signals from and to this location are returned
	 */
	public List<SystemVerilogSignal> getEncapsulatedSignalList(int insideLocations) {
		List<SystemVerilogSignal> outList = new ArrayList<SystemVerilogSignal>();
		for (SystemVerilogIOElement ioElem: childList) {
			// if this element is a sigset, call recursively to get all encapsulated signals
			if (ioElem.isSignalSet()) {
				SystemVerilogIOSignalSet ioSigSet = (SystemVerilogIOSignalSet) ioElem;
				//System.err.println("   SystemVerilogIOSignalList getEncapsulatedSignalList: name=" + ioElem.getName());
				List<SystemVerilogIOElement> newList = ioSigSet.getEncapsulatedIOElementList(null, insideLocations);  // get inputs
				outList.addAll(getSignalList(newList));
				newList = ioSigSet.getEncapsulatedIOElementList(insideLocations, null);  // get outputs
				outList.addAll(getSignalList(newList));
			}
		}
		return outList;
	}
		
	/** add matching leaf elements of this SystemVerilogSignalList to a wrapper signal map as sources.
	 *  this is root call of a recursive walk of the signal set hierarchy 
	 * @param sigMap - WrapperSignalMap that will be modified
	 * @param rules - set of remapping rules that will be used to match signals 
	 * @param useHierSignalNames - if true, hierarchical names will be set for encapsulated signals
	 * @param isInput - if true, sources added will be tagged as input ports
	 */   
	public void loadWrapperMapSources(WrapperSignalMap sigMap, RemapRuleList rules, boolean useHierSignalNames, boolean isInput) {
		this.loadWrapperMapInfo(sigMap, rules, useHierSignalNames, true, isInput, null, null, false);  // recursive call
	}

	/** add matching leaf elements of this SystemVerilogSignalList to a wrapper signal map as destinations.
	 *  this is root call of a recursive walk of the signal set hierarchy 
	 * @param sigMap - WrapperSignalMap that will be modified
	 * @param rules - set of remapping rules that will be used to match signals 
	 * @param useHierSignalNames - if true, hierarchical names will be set for encapsulated signals
	 * @param isOutput - if true, destinations added will be tagged as output ports
	 */   
	public void loadWrapperMapDestinations(WrapperSignalMap sigMap, RemapRuleList rules,
			boolean useHierSignalNames, boolean isOutput) {
		this.loadWrapperMapInfo(sigMap, rules, useHierSignalNames, false, isOutput, null, null, false);  // recursive call
	}

	public void display() {
		System.out.println("SystemVerilogIOSignalList: " + listName + ", activeSetStack depth=" + activeSetStack.size());
		super.display(0);
	}
	
	/*
    public static void main (String[] args) {
        // create some locations
    	Integer HW = 1;
    	Integer LOGIC = 2;
    	Integer DECODE = 4;
    	//Integer PIO = 8;
    	
    	// create a list
    	SystemVerilogIOSignalList list1 = new SystemVerilogIOSignalList("");
    	list1.addScalar(DECODE, HW, DefSignalType.D2H_ADDR);
    	list1.addScalar(DefSignalType.D2H_DATA);
    	list1.addVector(DefSignalType.H2D_DATA, 3, 5);
    	
    	// add a signalset
    	list1.pushIOSignalSet(null, null, null, "sigset1", 2, true, false, false, null, null);
    	list1.addScalar(DefSignalType.L2H_OVERFLOW);
    	list1.popIOSignalSet();
    	
    	// add an interface
    	SystemVerilogIOInterface intf1 = (SystemVerilogIOInterface) list1.pushIOSignalSet(LOGIC, HW, "lh_", "intf1", 1, true, true, false, null, null);  // TODO add from/set method using type
    	list1.addScalar(DefSignalType.L2H_UNDERFLOW);
    	// with a child signalset
    	//list1.pushIOSignalSet(null, null, null, "childsigset", 1, true, false, null);
    	//list1.addScalar(DefSignalType.H2L_INCR);
    	//list1.popIOSignalSet();
    	list1.popIOSignalSet();
    	
    	// get a list of child elems
    	List<SystemVerilogIOElement> elemList = list1.getChildList();
    	System.out.println("Children:");
    	for (SystemVerilogIOElement ioElem : elemList) System.out.println("  " + ioElem + ", local inst=" + ioElem.getInstanceString(true) ); 
    	
    	// get a list of non-virtual child elems
    	elemList = list1.getDescendentIOElementList(null, null, true);  //DECODE, HW
    	System.out.println("Non-virtual child elems:");
    	for (SystemVerilogIOElement ioElem : elemList) System.out.println("  " + ioElem);
    	
    	// get a list of local children
    	elemList = list1.getLocalDescendentIOElementList(); 
    	System.out.println("Local Non-virtual child elems:");
    	for (SystemVerilogIOElement ioElem : elemList) System.out.println("  " + ioElem);
    	
    	// get a list of encapsulated children
    	elemList = list1.getEncapsulatedIOElementList(null, null); 
    	System.out.println("Encapsulated child elems:");
    	for (SystemVerilogIOElement ioElem : elemList) System.out.println("  " + ioElem);
    	
    	// get intf define string
    	List<String> strList = intf1.getDefStrings();
    	System.out.println("Define strings:");
    	for (String str : strList) System.out.println("  " + str);
    	
    }*/

}
