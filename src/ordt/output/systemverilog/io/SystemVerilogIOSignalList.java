package ordt.output.systemverilog.io;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import ordt.output.systemverilog.SystemVerilogDefinedSignals;
import ordt.output.systemverilog.SystemVerilogSignal;
import ordt.output.systemverilog.SystemVerilogDefinedSignals.DefSignalType;


/** this is the main class used to hold IO lists for building sv modules.
 * maintains a stack of active signalsets where new signals/sets will be added by svbuilder */
public class SystemVerilogIOSignalList extends SystemVerilogIOSignalSet {

	public SystemVerilogIOSignalList(String name) {
		super(null, name, 1);
	}

	private Stack<SystemVerilogIOSignalSet> activeSetStack = new Stack<SystemVerilogIOSignalSet>();  // track active signalsets for inserts 
	private Stack<Boolean> inhibitInsertStack = new Stack<Boolean>();  // track insert inhibited signalsets (inhibit subsequent reps) 
	
	// --------------- child signal/interface add methods 
	
	/** return true if a child element add should be inhibited due */
	private boolean inhibitAdd() {
		return (!inhibitInsertStack.isEmpty() && inhibitInsertStack.peek());
	}

	/** add a new vector signal to the active list */
	@Override
	public void addVector(Integer from, Integer to, String namePrefix, String name, int lowIndex, int size) {
		if (inhibitAdd()) return;
		if (activeSetStack.isEmpty()) super.addVector(from, to, namePrefix, name, lowIndex, size);  // empty stack, so add to this list
		else activeSetStack.peek().addVector(from, to, namePrefix, name, lowIndex, size);  // otherwise add to active list
		//System.out.println("SystemVerilogIOSignalList addVector: adding sig=" + name + ", intf stack empty=" + activeSetStack.isEmpty());
	}

	/** add a new vector signal to the active list */
	public void addVector(Integer from, Integer to, DefSignalType sigType, int lowIndex, int size) {
		String prefix = SystemVerilogDefinedSignals.getPrefix(sigType);
		String name = SystemVerilogDefinedSignals.getSuffix(sigType);
		addVector(from, to, prefix, name, lowIndex, size);
	}
	
	/** add a new scalar signal to the active list */
	public void addScalar(Integer from, Integer to, DefSignalType sigType, int lowIndex, int size) {
		String prefix = SystemVerilogDefinedSignals.getPrefix(sigType);
		String name = SystemVerilogDefinedSignals.getSuffix(sigType);
		addVector(from, to, prefix, name, 0, 1);
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
	 * @param extType - externally defined type of this set
	 * @return
	 */
	public SystemVerilogIOSignalSet pushIOSignalSet(Integer from, Integer to, String namePrefix, String name, int reps, boolean isFirstRep, boolean isIntf, String extType) {
		// first determine if push is inhibited
		boolean inhibitAdd = inhibitAdd() || !isFirstRep;
		inhibitInsertStack.push(inhibitAdd);  // always push to the inhibit stack 
		if (inhibitAdd) return null;
		// else no inhibit then create the set and push to active stack
		SystemVerilogIOSignalSet newSigSet;
		if (isIntf) newSigSet = new SystemVerilogIOInterface (from, to, namePrefix, name, reps, extType);  // create a new interface
		else newSigSet = new SystemVerilogIOSignalSet (namePrefix, name, reps);  // otherwise create default set
		// add to active element or root level
		if (activeSetStack.isEmpty()) super.addSignalSet(newSigSet);  // add new set at root
		else activeSetStack.peek().addSignalSet(newSigSet);  // otherwise add to active set
		// push this set onto the active definition stack (must push here always so pops match)
		activeSetStack.push(newSigSet);  
		return newSigSet;
	}
	
	/** if initial rep, create a new signal set class, add it to active stack */
	public SystemVerilogIOSignalSet pushIOSignalSet(Integer from, Integer to, DefSignalType sigType, int reps, boolean isFirstRep, String extType) {
		String prefix = SystemVerilogDefinedSignals.getPrefix(sigType);
		String name = SystemVerilogDefinedSignals.getSuffix(sigType);
		boolean isIntf = (sigType == DefSignalType.LOGIC_INTERFACE);
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

}
