/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved. 
 */
package ordt.extract.model;

import java.util.HashSet;
import java.util.Iterator;

import ordt.extract.Ordt;
import ordt.extract.Ordt.InputType;
import ordt.output.FieldSetProperties;
import ordt.output.OutputBuilder;


public class ModFieldSet extends ModComponent {
	boolean union = false;
	
	public ModFieldSet() {
		super();
		compType = CompType.FIELDSET;
	}
	
	/** create a new indexed model instance of this component */
	@Override
	public ModIndexedInstance createNewInstance() {
		ModIndexedInstance newInst = new ModIndexedInstance();  // create the new instance
		newInst.setRegComp(this);  // set component type of new instance
		addInstanceOf(newInst);  // add instance to list for this comp
		return newInst;
	}
	
	/** default check for implicit default property assignments - overridden by child types */
	@Override
	protected boolean isImplicitDefaultProperty(String propertyName) {
		HashSet<String> implicitDefaultProperties = new HashSet<String>();
		// implicit default properties...
		implicitDefaultProperties.add("dontcompare");
		if (Ordt.hasInputType(InputType.JSPEC)) {
			implicitDefaultProperties.add("js_attributes");
			implicitDefaultProperties.add("sub_category");			
		}
		return implicitDefaultProperties.contains(propertyName);
	}

    /** return true if a union fieldset */
	public boolean isUnion() {
		return union;
	}
	
	/** return a string representing this sub-class for messages - overridden by child types */
    @Override
	protected String getBaseComponentTypeName() {
		return Ordt.hasInputType(InputType.RDL)? "fieldstruct" : "field_set";
	}

    /** set union fieldset boolean */
	public void setUnion(boolean union) {
		this.union = union;
	}

	/** remove all except first child instance and return size of first instance */  // TODO - js specific move into extractor
	public Integer cleanupUnion() {
		if (getChildInstances().size() > 1) {
			for (int idx=getChildInstances().size()-1; idx>0; idx--) getChildInstances().remove(idx);
			ModComponent child = getChildInstances().get(0).getRegComp();
			if (child.isField()) return child.getIntegerProperty("fieldwidth");
			if (child.isFieldSet()) return child.getIntegerProperty("fieldstructwidth");
		}
		return this.getIntegerProperty("fieldstructwidth");
	}

	// ------------------------------------ code gen templates ----------------------------------------
	
	/* generate output for regset */
	@Override
	public void generateOutput(ModInstance callingInst, OutputBuilder outputBuilder) {
	
		// each subclass should override to create appropriate code based on calling instance
		if (callingInst == null) return;

		// get replication count
		int repCount = callingInst.getRepCount();  // get non-null repCount
		
		//System.out.println("--- ModFieldSet " + callingInst.getId() + ", reps=" + repCount + ", hi=" + callingInst.getHighIndex() + ", lo=" + callingInst.getLowIndex());
	    
		// call once per replicated field set
		FieldSetProperties fieldSetProperties = null;  
		for (int rep=0; rep<repCount; rep++) {

			fieldSetProperties = new FieldSetProperties(callingInst);  // extract basic properties
		    fieldSetProperties.setId(fieldSetProperties.getId() + getRepSuffix(rep, repCount)); // update name based on rep #  // TODO fix to allow []?
			outputBuilder.pushInstance(fieldSetProperties);  
			outputBuilder.addFieldSet(fieldSetProperties, rep);  

			// generate each direct instance in this component 
			Iterator<ModInstance> it = getChildInstances().iterator();
			boolean processNextChild = it.hasNext(); 
			while (processNextChild) { 
				it.next().generateOutput(outputBuilder);
				processNextChild = it.hasNext(); 
			}			
			outputBuilder.finishFieldSet(fieldSetProperties);    
			outputBuilder.popInstance();
		}			

	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (union ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		ModFieldSet other = (ModFieldSet) obj;
		if (union != other.union)
			return false;
		return true;
	}
}
