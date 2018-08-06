/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved. 
 */
package ordt.extract.model;

import ordt.output.common.MsgUtils;
import ordt.output.FieldProperties;
import ordt.output.OutputBuilder;

/** register field class extracted from definition lang */
public class ModField extends ModComponent {
	
	public ModField() {
		super();
		compType = CompType.FIELD;
	}
	
	/** create a new indexed model instance of this component */
	@Override
	public ModIndexedInstance createNewInstance() {
		ModIndexedInstance newInst = new ModIndexedInstance();  // create the new instance
		newInst.setRegComp(this);  // set component type of new instance
		addInstanceOf(newInst);  // add instance to list for this comp
		return newInst;
	}
	
	/** return a string representing this sub-class for messages - overridden by child types */
    @Override
	protected String getBaseComponentTypeName() {
		return "field";
	}

	// ------------------------------------ code gen templates ----------------------------------------
	
	/* generate field output */
	public void generateOutput(ModInstance callingInst, OutputBuilder outputBuilder) {
	
		// each subclass should override to create appropriate code based on calling instance
		if (callingInst == null) return;
		
		//System.out.println(RegExtractor.repeat(' ', depth) + "---> field " + callingInst.getId());
		FieldProperties fieldProperties = new FieldProperties(callingInst);  // init and set id, calling instance
		outputBuilder.pushInstance(fieldProperties);  // instance path is valid after this

		// add field to output structures
		outputBuilder.addField(fieldProperties);  // instance id is picked up here
		
		// generate each direct instance in this component
		for (ModInstance regInst : getChildInstances()) {
			MsgUtils.errorExit("Rdl fields are not allowed to have child components, field=" + callingInst.getId());
			regInst.generateOutput(outputBuilder);
		}
		outputBuilder.popInstance();
	}

}
