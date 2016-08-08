package ordt.output.systemverilog.io;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import ordt.output.systemverilog.SystemVerilogDefinedSignals;
import ordt.output.systemverilog.SystemVerilogSignal;
import ordt.output.systemverilog.SystemVerilogDefinedSignals.DefSignalType;


/** this is the main class used to hold IO lists for building sv modules.
 * maintains a stack of active signalsets where new signals/sets will be added by svbuilder */
public class SystemVerilogIOSignalList extends SystemVerilogIOSignalSet {

	public SystemVerilogIOSignalList() {
		super(null, null, 1);  // IOsignalList has no tag, no name and one rep
	}

	private Stack<SystemVerilogIOSignalSet> activeSetStack = new Stack<SystemVerilogIOSignalSet>();  // track active signalsets for inserts 
	private Stack<Boolean> inhibitInsertStack = new Stack<Boolean>();  // track insert inhibited signalsets (inhibit subsequent reps) 
	
	// --------------- child signal/interface add methods 
	
	/** return true if a child element add should be inhibited due to non-first rep count */
	private boolean inhibitAdd() {
		return (!inhibitInsertStack.isEmpty() && inhibitInsertStack.peek());
	}

	/** add a new vector signal to the active list */
	@Override
	public void addVector(Integer from, Integer to, String tagPrefix, String name, int lowIndex, int size) {
		if (inhibitAdd()) return;
		if (activeSetStack.isEmpty()) super.addVector(from, to, tagPrefix, name, lowIndex, size);  // empty stack, so add to this list
		else activeSetStack.peek().addVector(from, to, tagPrefix, name, lowIndex, size);  // otherwise add to active list
		//System.out.println("SystemVerilogIOSignalList addVector: adding " + name + ", from=" + from + ", to=" + to + ", stack empty=" + activeSetStack.isEmpty());
	}

	/** add a new vector signal to the active list */
	public void addVector(Integer from, Integer to, DefSignalType sigType, int lowIndex, int size) {
		String prefix = SystemVerilogDefinedSignals.getPrefix(sigType);
		String name = SystemVerilogDefinedSignals.getSuffix(sigType);
		addVector(from, to, prefix, name, lowIndex, size);
	}
	
	/** add a new scalar signal to the active list */
	public void addScalar(Integer from, Integer to, DefSignalType sigType) {
		String prefix = SystemVerilogDefinedSignals.getPrefix(sigType);
		String name = SystemVerilogDefinedSignals.getSuffix(sigType);
		addVector(from, to, prefix, name, 0, 1);
	}

	/** add a new vector signal to the active list using defined signal locations */
	public void addVector(DefSignalType sigType, int lowIndex, int size) {
		Integer from = SystemVerilogDefinedSignals.getFrom(sigType);
		Integer to = SystemVerilogDefinedSignals.getTo(sigType);
		addVector(from, to, sigType, lowIndex, size);
		//System.out.println("SystemVerilogIOSignalList addVector: adding " + sigType + ", from=" + from + ", to=" + to + ", stack empty=" + activeSetStack.isEmpty());
	}
	
	/** add a new scalar signal to the active list  using defined signal locations */
	public void addScalar(DefSignalType sigType) {
		Integer from = SystemVerilogDefinedSignals.getFrom(sigType);
		Integer to = SystemVerilogDefinedSignals.getTo(sigType);
		addVector(from, to, sigType, 0, 1);
		//System.out.println("SystemVerilogIOSignalList addVector: adding " + sigType + ", from=" + from + ", to=" + to + ", stack empty=" + activeSetStack.isEmpty());
	}
	
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
	 * @param extType - externally defined type of this set, if null interface type is defined by path
	 * @return
	 */
	public SystemVerilogIOSignalSet pushIOSignalSet(Integer from, Integer to, String namePrefix, String name, int reps, boolean isFirstRep, boolean isIntf, String extType) {
		// first determine if push is inhibited
		boolean inhibitAdd = inhibitAdd() || !isFirstRep;
		inhibitInsertStack.push(inhibitAdd);  // always push to the inhibit stack 
		if (inhibitAdd) return null;
		// else no inhibit then create the set and push to active stack
		SystemVerilogIOSignalSet newSigSet;
		boolean hasExtType = (extType !=null);
		String newType = hasExtType? extType : getCurrentStackPath();  // if internally defined, pass in the current path
		if (isIntf) newSigSet = new SystemVerilogIOInterface (from, to, namePrefix, name, reps, newType, hasExtType);  // create a new interface
		else newSigSet = new SystemVerilogIOSignalSet (namePrefix, name, reps);  // otherwise create default set
		// add to active element or root level
		if (activeSetStack.isEmpty()) super.addSignalSet(newSigSet);  // add new set at root
		else activeSetStack.peek().addSignalSet(newSigSet);  // otherwise add to active set
		// push this set onto the active definition stack (must push here always so pops match)
		activeSetStack.push(newSigSet);  
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
	public SystemVerilogIOSignalSet pushIOSignalSet(Integer from, Integer to, DefSignalType sigType, int reps, boolean isFirstRep, String extType) {
		String prefix = SystemVerilogDefinedSignals.getPrefix(sigType);
		String name = SystemVerilogDefinedSignals.getSuffix(sigType);
		boolean isIntf = (sigType == DefSignalType.LH_INTERFACE);
		return pushIOSignalSet(from, to, prefix, name,  reps,  isFirstRep,  isIntf,  extType);  
	}

	/** done defining signalset, so pop the stacks */
	public void popIOSignalSet() {
		if (!inhibitAdd()) activeSetStack.pop();  // pop this set if push wasn't inhibited
		inhibitInsertStack.pop();  // always pop the inhibit stack 
	}
	 
	// -------

	/** return a list of flat simple signals that are encapsulated in interfaces (no root sigs) - recursive *  was getIntfEncapsulatedSignalList
	 *  <--- this is from SVIOSignalList, gets list of full signals in interfaces
	 * @param insideLocations - encapsulated signals from and to this location are returned
	 */
	public List<SystemVerilogSignal> getEncapsulatedSignalList(int insideLocations) {
		List<SystemVerilogSignal> outList = new ArrayList<SystemVerilogSignal>();
		for (SystemVerilogIOElement ioElem: childList) {
			// if this element is a sigset, call recursively to get all encapsulated signals
			if (ioElem.isSignalSet()) {
				SystemVerilogIOSignalSet ioSigSet = (SystemVerilogIOSignalSet) ioElem;
				//System.err.println("   SystemVerilogIOSignalList getEncapsulatedSignalList: name=" + ioElem.getName());
				List<SystemVerilogSignal> newList = ioSigSet.getEncapsulatedSignalList(null, insideLocations);  // get inputs
				outList.addAll(newList);
				newList = ioSigSet.getEncapsulatedSignalList(insideLocations, null);  // get outputs
				outList.addAll(newList);
			}
		}
		return outList;
	}
	
	/** return a list of strings assigning interface children to corresponding simple IO signal */   
	public List<String> getNonVirtualAssignStrings(int insideLocations) {
		List<String> outList = new ArrayList<String>();
		for (SystemVerilogIOElement ioElem: childList) {
			// if this element is a set, call recursively to get all encapsulated signal assigns
			if (ioElem.isSignalSet()) {
				//System.err.println("   SystemVerilogIOSignalList getInterfaceAssignStrList: name=" + sig.getName());
				List<String> newList = ((SystemVerilogIOSignalSet) ioElem).getNonVirtualAssignStrings(insideLocations, true, null, null, false, false);
				outList.addAll(newList);
			}
		}
		return outList;
	}
	
    public static void main (String[] args) {
        // create some locations
    	Integer HW = 1;
    	Integer LOGIC = 2;
    	Integer DECODE = 4;
    	Integer PIO = 8;
    	
    	// create a list
    	SystemVerilogIOSignalList list1 = new SystemVerilogIOSignalList();
    	list1.addScalar(DECODE, HW, DefSignalType.D2H_ADDR);
    	list1.addScalar(DefSignalType.D2H_DATA);
    	list1.addVector(DefSignalType.H2D_DATA, 3, 5);
    	
    	// add a signalset
    	list1.pushIOSignalSet(null, null, null, "sigset1", 2, true, false, null);
    	list1.addScalar(DefSignalType.L2H_OVERFLOW);
    	list1.popIOSignalSet();
    	
    	// add an interface
    	SystemVerilogIOInterface intf1 = (SystemVerilogIOInterface) list1.pushIOSignalSet(LOGIC, HW, "lh_", "intf1", 1, true, true, null);  // TODO add from/set method using type
    	list1.addScalar(DefSignalType.L2H_UNDERFLOW);
    	// with a child signalset
    	//list1.pushIOSignalSet(null, null, null, "childsigset", 1, true, false, null);
    	//list1.addScalar(DefSignalType.H2L_INCR);
    	//list1.popIOSignalSet();
    	list1.popIOSignalSet();
    	
    	// get a list of child elems
    	List<SystemVerilogIOElement> elemList = list1.getChildList();
    	System.out.println("Children:");
    	for (SystemVerilogIOElement ioElem : elemList) System.out.println("  " + ioElem /*+ ", local inst=" + ioElem.getInstanceString() */);  // FIXME no SvBuilder defined
    	
    	// get a list of non-virtual child elems
    	elemList = list1.getDescendentIOElementList(null, null);  //DECODE, HW
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
    	//List<String> strList = intf1.getDefStrings();   // FIXME doesnt work here, no SvBuilder defined
    	//System.out.println("Define strings:");
    	//for (String str : strList) System.out.println("  " + str);
    	
    	// get assign strings
    	List<String> strList = list1.getNonVirtualAssignStrings(DECODE|LOGIC); 
    	System.out.println("Assign strings:");
    	for (String str : strList) System.out.println("  " + str);
    }

}
