package ordt.output.systemverilog.io;

import java.util.ArrayList;
import java.util.List;

public class SystemVerilogIOSignalSet extends SystemVerilogIOElement {
	protected List<SystemVerilogIOElement> childList = new ArrayList<SystemVerilogIOElement>(); // signals/signalsets in this signalset
	protected String type;   // type of this signalset  

	public SystemVerilogIOSignalSet(Integer from, Integer to, String name, int reps) { // TODO - need to set prefix, do we need from/to for virtual
		// TODO Auto-generated constructor stub
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
	public void addScalar(Integer from, Integer to, String name) {
		this.addVector(from, to, name, 0, 1);
	}
	
	/** add a new vector signal to the child list 
	 */
	public void addVector(Integer from, Integer to, String name, int lowIndex, int size) {
		SystemVerilogIOSignal sig = new SystemVerilogIOSignal(from, to, name, lowIndex, size);
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

}
