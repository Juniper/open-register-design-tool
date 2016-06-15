/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved.
 */
package ordt.output.systemverilog;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/** extended SV interface list for defining SVbuilder level IO signal lists - tracks the active SV interface for adds */  
public class SystemVerilogIOSignalList extends SystemVerilogInterface { 

	private Stack<SystemVerilogInterface> activeIntfStack = new Stack<SystemVerilogInterface>();  // track active interface list 
	
	public SystemVerilogIOSignalList() {
		super(null, 1);  // root list has no parent and only 1 rep
	}

	/** add a new vector signal to the active list */
	public void addVector(Integer from, Integer to, String name, int lowIndex, int size) {
		if (activeIntfStack.isEmpty()) super.addVector(from, to, name, lowIndex, size);  // empty stack, so add to this list
		else activeIntfStack.peek().addVector(from, to, name, lowIndex, size);  // otherwise add to active list
		//System.out.println("SystemVerilogIOSignalList addVector: adding sig=" + name + ", intf stack empty=" + activeIntfStack.isEmpty());
	}
	
	/** add IOsignal list to this IOSignalList - only used post signal load stage */
	public void addList(SystemVerilogIOSignalList sigList) {
		activeIntfStack.clear();  // clear stack so all are added to signalList
		for (SystemVerilogIOSignal sig: sigList.getIOSignalList()) {
			signalList.add(sig);  
			//if (sig.isIntfSig()) localIntfSigs.put(sig.getName(), sig);  // add to local intf set 
		}
	}
	
	/** add basic signal list to this IOSignalList - only used post signal load stage*/
	public void addList(SystemVerilogSignalList sigList, Integer from, Integer to) {
		activeIntfStack.clear();  // clear stack so all are added to signalList
		for (SystemVerilogSignal sig: sigList.getSignalList()) {
			addVector(from, to, sig.getName(), sig.getLowIndex(), sig.getSize());
		}
	}

	/** create a new interface, add it to active stack  
	 * @param extName 
	 * @param name */
	@Override
	public SystemVerilogInterface addInterface(Integer from, Integer to, String indexedName, String repName, int repCount, String extName) {
		 SystemVerilogInterface intfList;
		if (activeIntfStack.isEmpty()) intfList = super.addInterface(from, to, indexedName, repName, repCount, extName);  // create a new interface/intfList at root
		else intfList = activeIntfStack.peek().addInterface(from, to, indexedName, repName, repCount, extName);  // otherwise add to active list
		// push this interface onto the active definition stack (must push here always so pops match)
		activeIntfStack.push(intfList);  
		return intfList;
	}

	/** done defining interface, so remove it from active stack */
	public void popInterface() {
		activeIntfStack.pop();  // pop this interface 
	}

	/** return a list of signals encapsulated in interfaces (no root sigs) - recursive */
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

	// --------------- methods returning sv string lists for output 
	
	/** return a list of strings assigning interface children to corresponding simple IO signal */ 
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

}
