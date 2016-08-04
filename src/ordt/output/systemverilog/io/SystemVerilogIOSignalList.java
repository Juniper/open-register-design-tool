package ordt.output.systemverilog.io;

import java.util.Stack;


/** this is the main class used to hold IO lists for building sv modules.
 * maintains a stack of active signalsets where new signals/sets will be added by svbuilder */
public class SystemVerilogIOSignalList extends SystemVerilogIOSignalSet {

	public SystemVerilogIOSignalList(String name) {
		super(0, 0, name, 1);
		// TODO Auto-generated constructor stub
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
	public void addVector(Integer from, Integer to, String name, int lowIndex, int size) {
		if (inhibitAdd()) return;
		if (activeSetStack.isEmpty()) super.addVector(from, to, name, lowIndex, size);  // empty stack, so add to this list
		else activeSetStack.peek().addVector(from, to, name, lowIndex, size);  // otherwise add to active list
		//System.out.println("SystemVerilogIOSignalList addVector: adding sig=" + name + ", intf stack empty=" + activeSetStack.isEmpty());
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
	 * @param from - from locations for this group
	 * @param to 
	 * @param name
	 * @param repCount
	 * @param isFirstRep
	 * @param isIntf
	 * @param extType
	 * @return
	 */
	public SystemVerilogIOSignalSet pushIOSignalSet(Integer from, Integer to, String name, int reps, boolean isFirstRep, boolean isIntf, String extType) {
		// first determine if push is inhibited
		boolean inhibitAdd = inhibitAdd() || !isFirstRep;
		inhibitInsertStack.push(inhibitAdd);  // always push to the inhibit stack 
		if (inhibitAdd) return null;
		// else no inhibit then create the set and push to active stack
		SystemVerilogIOSignalSet newSigSet;
		if (isIntf) newSigSet = new SystemVerilogIOInterface (from, to, name, reps, extType);  // create a new interface
		else newSigSet = new SystemVerilogIOSignalSet (from, to, name, reps);  // otherwise create default set
		// add to active element or root level
		if (activeSetStack.isEmpty()) super.addSignalSet(newSigSet);  // add new set at root
		else activeSetStack.peek().addSignalSet(newSigSet);  // otherwise add to active set
		// push this set onto the active definition stack (must push here always so pops match)
		activeSetStack.push(newSigSet);  
		return newSigSet;
	}

	/** done defining signalset, so pop the stacks */
	public void popIOSignalSet() {
		if (!inhibitAdd()) activeSetStack.pop();  // pop this set if push wasn't inhibited
		inhibitInsertStack.pop();  // always pop the inhibit stack 
	}

}
