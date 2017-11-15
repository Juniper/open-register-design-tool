/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved. 
 */
package ordt.output.systemverilog.common;

import java.util.ArrayList;
import java.util.List;

/** class to hold a list of simple verilog signal info (SystemVerilogSignal) */  
public  class SystemVerilogSignalList {
	protected List<SystemVerilogSignal> signalList = new ArrayList<SystemVerilogSignal>();
		
	/** get signalList
	 *  @return the signalList
	 */
	public List<SystemVerilogSignal> getSignalList() {
		return signalList;
	}
	
	/** return list of signal definition strings
	 *  @return the signalList
	 */
	public List<String> getDefNameList() {
		List<String> outList = new ArrayList<String>();
		for (SystemVerilogSignal sig : signalList) {
			outList.add(sig.getDefName());
		}
		return outList;
	}
	
	/** add a new scalar signal to the list
	 */
	public void addScalar(String name) {
		this.addVector(name, 0, 1);
	}
	
	/** add a new vector signal to the list
	 */
	public void addVector(String name, int lowIndex, int size) {
		//this.add(name, genDefArrayString(lowIndex, size));
		signalList.add(new SystemVerilogSignal(name, lowIndex, size));
	}
	
	/** add a list of signals to this list
	 */
	public void addAll(SystemVerilogSignalList addList) {
		signalList.addAll(addList.getSignalList());
	}

	public void addAll(List<SystemVerilogSignal> addList) {
		signalList.addAll(addList);		
	}

}

