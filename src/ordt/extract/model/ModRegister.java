/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved.
 */
package ordt.extract.model;

import java.util.HashSet;

import ordt.extract.DefinedProperties;
import ordt.extract.Ordt;
import ordt.extract.RegNumber;
import ordt.extract.Ordt.InputType;
import ordt.output.OutputBuilder;
import ordt.output.RegProperties;
import ordt.output.RegSetProperties;
import ordt.parameters.ExtParameters;

/** register class extracted from definition lang */
public class ModRegister extends ModComponent  {
		
	private int padBits = 0;  // number of unused bits in this reg / used to compute field offsets for inputs that allow pad (jspec)   
	
	public ModRegister() {
		super();
		compType = CompType.REG;
	}

	/** default check on valid property assignments - overridden by child types */
	@Override
	protected boolean isValidProperty(String propertyName) {
		return DefinedProperties.isRegProperty(propertyName);
	}
	
	/** default check for implicit default property assignments - overridden by child types */
    @Override
	protected boolean isImplicitDefaultProperty(String propertyName) {
		HashSet<String> implicitDefaultProperties = new HashSet<String>();
		// implicit default properties...
		implicitDefaultProperties.add("dontcompare");
		// if jspec, interpret as implicit default
		if (Ordt.hasInputType(InputType.JSPEC)) {
			implicitDefaultProperties.add("js_attributes");
			implicitDefaultProperties.add("sub_category");
		}
		return implicitDefaultProperties.contains(propertyName);
	}
	
	/** return a string representing this sub-class for messages - overridden by child types */
    @Override
	protected String getBaseComponentTypeName() {
		return "register";
	}
	
	/** create a new addressable model instance of this component */
	@Override
	public ModAddressableInstance createNewInstance() {
		ModAddressableInstance newInst = new ModAddressableInstance();  // create the new instance
		newInst.setRegComp(this);  // set component type of new instance
		addInstanceOf(newInst);  // add instance to list for this comp
		return newInst;
	}

	/** write info to stdout */
	@Override
	public void display () {
		super.display();
		// display reg properties
		System.out.println("    register properties:");
		System.out.println("        pad bits=" + this.getPadBits());
	}
	
	/** compute aligned size of this reg in bytes*/
	@Override
	public void setAlignedSize() {
		// if already computed then exit
		if (alignedSize != null) return;
		// get reg width
		int regWidth = hasProperty("regwidth") ? getIntegerProperty("regwidth")/8 : ExtParameters.getMinDataSize()/8;
		// add all child sizes
		RegNumber newMinSize = new RegNumber(regWidth);
		//System.out.println("ModRegister setMinSize, register instance=" + getId() + ", regwidth=" + newMinSize);
		newMinSize.setNextHighestPowerOf2();  // round to next power of 2
		//System.out.println("ModRegister setMinSize, register instance=" + getId() + ", regwidth=" + newMinSize);
		this.alignedSize = newMinSize;
	}
	
	public int getPadBits() {
		return padBits;
	}

	public void setPadBits(int padBits) {
		this.padBits = padBits;
	}

	// ------------------------------------ code gen templates ----------------------------------------

	/* generate output */   
	@Override
	public void generateOutput(ModInstance callingInst, OutputBuilder outputBuilder) {
	
		// each subclass should override to create appropriate code based on calling instance
		if (callingInst == null) return;

		// get replication count
		int repCount = callingInst.getRepCount(); // get non-null repCount 
		
		//System.out.println("ModRegister generateOutput: id=" + callingInst.getId() + ", reps=" + repCount);

     	RegSetProperties rsProps = outputBuilder.getRegSetProperties();
     	boolean regIsExternal = callingInst.hasProperty("external") || ((rsProps != null) && rsProps.isLocalMapExternal(outputBuilder.allowLocalMapInternals()));  
     	
     	//if (regIsExternal && rsProps.isAddressMap())
     	//System.out.println("ModRegister generateOutput: register instance=" + callingInst.getId() + ",  regIsExternal=" + regIsExternal + ", rs id=" + rsProps.getId() + ", rs addrmap=" + rsProps.isAddressMap() + ", rs isLocalMapExternal=" + rsProps.isLocalMapExternal(outputBuilder.allowLocalMapInternals()) + ", bid=" + outputBuilder.getBuilderID());
     	
	    // if an extern register, call once per replicated set
		if (regIsExternal && !outputBuilder.visitEachExternalRegister()) {
			//Boolean rsIsExt = (rsProps == null) ? null : rsProps.isExternal();
			//System.out.println("Register: generateOutput, register instance=" + callingInst.getId() + " is external, parent ext=" + rsIsExt + ", reps=" + repCount);
			
			RegProperties regProperties = new RegProperties(callingInst, outputBuilder.fieldOffsetsFromZero());  // extract basic properties
			// cascade external type from instance or regset
			if (callingInst.hasProperty("external")) regProperties.setExternal(callingInst.getProperty("external"));  
			else if ((rsProps != null) && rsProps.isLocalMapExternal(outputBuilder.allowLocalMapInternals())) regProperties.setExternalType(rsProps.getExternalType());
			else {
				regProperties.setExternal("DEFAULT");
				//System.out.println("ModRegister generateOutput, register instance=" + regProperties.getId() + " set to DEFAULT external");
			}
			outputBuilder.pushInstance(regProperties);  // push instance and set isRootExternal
			//System.out.println("ModRegister generateOutput: register instance=" + regProperties.getId() + " post push, ext=" + regProperties.isLocalMapExternal() + ", root=" + regProperties.isRootExternal());
			// add external registers to output structures  
			outputBuilder.addExternalRegisters(regProperties);     // <----- note that all inst properties are extracted here
			
			// visit children (fieldsets/fields) if externals are being processed
			//if (outputBuilder.visitExternalRegisters()) {
				// generate each direct instance in this component
				for (ModInstance regInst : getChildInstances()) {
				   regInst.generateOutput(outputBuilder);
				}
			//}
			// wrap up register add after all sub-fields added
			outputBuilder.finishExternalRegisters(regProperties);  
			
			// if this is a root external instance, then add the external reg interface
			if (regProperties.isRootExternal()) {
				outputBuilder.addRootExternalRegisters(null); // use static regProperties since already created 
			}

			outputBuilder.popInstance();  
		}
		// otherwise loop through each instance
		else {
			// check for invalid internal reps
			if ((repCount > ExtParameters.getMaxInternalRegReps()) && !regIsExternal) Ordt.errorExit("Register replication exceeded max for internal register, instance=" + callingInst.getId() + ", reps=" + repCount);
			//else Ordt.infoMessage("generateVerilog: register replication for internal register, instance=" + callingInst.getId() + ", reps=" + repCount);
				
			// call once per replicated register
		    for (int rep=0; rep<repCount; rep++) {
				
				RegProperties regProperties = new RegProperties(callingInst, outputBuilder.fieldOffsetsFromZero());  // extract basic properties
				// cascade external type from instance or regset (compunents of regIsExternal)
				if (callingInst.hasProperty("external")) regProperties.setExternal(callingInst.getProperty("external"));  
				else if ((rsProps != null) && rsProps.isLocalMapExternal(outputBuilder.allowLocalMapInternals())) regProperties.setExternalType(rsProps.getExternalType());
				//regProperties.setExternal(regIsExternal);  // set external here to cascade from external parent
				//System.out.println("ModRegister generate: register instance=" + regProperties.getId() + ",  is external=" + regProperties.getExternalType());  

				if (outputBuilder.visitEachReg()) regProperties.setId(regProperties.getId() + getRepSuffix(rep, repCount)); // update name based on rep #
				outputBuilder.pushInstance(regProperties);
				
				outputBuilder.addRegister(regProperties, rep);   // add register to verilog output structures  <----- note that all inst properties are extracted here 
		     			     	
				// generate each direct instance in this component
				for (ModInstance regInst : getChildInstances()) {
					regInst.generateOutput(outputBuilder);
				}
				
				outputBuilder.finishRegister(regProperties);  // wrap up register add after all sub-fields added
				//System.out.println("Register: adding ext reg with width=" + regProperties.getRegWidth());  // still default of 32 here
				outputBuilder.popInstance();
		    }	
		}
	}

	@Override
	// NOTE: currently used for uvm class reuse
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + padBits;
		return result;
	}

	@Override
	// NOTE: currently used for uvm class reuse
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		ModRegister other = (ModRegister) obj;
		if (padBits != other.padBits)
			return false;
		return true;
	}

}
