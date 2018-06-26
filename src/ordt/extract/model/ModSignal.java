/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved. 
 */
package ordt.extract.model;

import java.util.HashSet;

import ordt.output.OutputBuilder;
import ordt.output.SignalProperties;

public class ModSignal extends ModComponent {
	
	public ModSignal() {
		super();
		compType = CompType.SIGNAL;
	}
	
	/** create a new indexed model instance of this component */
	@Override
	public ModIndexedInstance createNewInstance() {
		ModIndexedInstance newInst = new ModIndexedInstance();  // create the new instance
		newInst.setRegComp(this);  // set component type of new instance
		addInstanceOf(newInst);  // add instance to list for this comp
		return newInst;
	}

    /** recursively build list of hierarchical signal names that will be created by this ModSignal */
	public void getDefinedSignalNames(HashSet<String> nameList) {
		// loop through instances of this ModSignal
		for (ModInstance inst: instancesOf)
			inst.getDefinedSignalNames("", nameList);  // start with null suffix
		return;
	}

	// ------------------------------------ code gen templates ----------------------------------------
	
	/* generate output */
	@Override
	public void generateOutput(ModInstance callingInst, OutputBuilder outputBuilder) {
		if (callingInst == null) return;
		
		SignalProperties signalProperties = new SignalProperties(callingInst);  // extract properties
		outputBuilder.pushInstance(signalProperties);  // instance path is valid after this

		outputBuilder.addSignal(signalProperties);  // add signal to verilog structures
		outputBuilder.popInstance();
	}
	

}
