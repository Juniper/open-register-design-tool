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

public class ModRegSet extends ModComponent {
	
	protected ModRegSet() {
		super();
		compType = CompType.REGSET;
	}
	
	/** create a new addressable model instance of this component */
	@Override
	public ModAddressableInstance createNewInstance() {
		ModAddressableInstance newInst = new ModAddressableInstance();  // create the new instance
		newInst.setRegComp(this);  // set component of new instance
		addInstanceOf(newInst);  // add instance to list for this comp
		return newInst;
	}

	/** default check on valid property assignments - overridden by child types */
	@Override
	protected boolean isValidProperty(String propertyName) {
		return DefinedProperties.isRegsetProperty(propertyName);
	}
	
	/** default check for implicit default property assignments - overridden by child types */
    @Override
	protected boolean isImplicitDefaultProperty(String propertyName) {
		HashSet<String> implicitDefaultProperties = new HashSet<String>();
		// implicit default properties...
		implicitDefaultProperties.add("donttest");
		implicitDefaultProperties.add("dontcompare");
		// if jspec, interpret as implicit default
		if (Ordt.hasInputType(InputType.JSPEC)) {
			implicitDefaultProperties.add("js_attributes");
			implicitDefaultProperties.add("sub_category");
			//implicitDefaultProperties.add("js_superset_check");
			implicitDefaultProperties.add("category");
			implicitDefaultProperties.add("regwidth");
		}
		return implicitDefaultProperties.contains(propertyName);
	}
	
	/** return a string representing this sub-class for messages - overridden by child types */
    @Override
	protected String getBaseComponentTypeName() {
		return Ordt.hasInputType(InputType.RDL)? "regfile" : "register_set";
	}
	
	/** recursively compute size of this component assuming js align rules
	 * returned value is independent of subcomp alignment/base addr 
	 */
	@Override
	public void setAlignedSize(int defaultRegWidth) {
		// if already computed then exit
		if (alignedSize != null) return;
		// add all child sizes
		RegNumber newAlignedSize = new RegNumber(0);
		for (ModInstance regInst : getChildInstances()) {  
			// only consider addressable instances
			if (regInst.isAddressable()) {
				ModAddressableInstance childInst= (ModAddressableInstance) regInst;
				int newDefaultRegWidth = childInst.hasDefaultProperty("regwidth") ? childInst.getDefaultIntegerProperty("regwidth") :   // use instance default if defined
				   this.hasDefaultProperty("regwidth") ? this.getDefaultIntegerProperty("regwidth") : defaultRegWidth;  // else use current regset default if defined
				childInst.regComp.setAlignedSize(newDefaultRegWidth);  // recursively set size of individual child component 
				if (childInst.getAddress() != null) newAlignedSize = new RegNumber(childInst.getAddress());     // if child has a defined address, bump the running size
				if (childInst.getAddressShift() != null) newAlignedSize.add(childInst.getAddressShift());     // if child has a defined address shift, bump the running size
				if (childInst.getAddressModulus() != null) newAlignedSize.roundUpToModulus(childInst.getAddressModulus()); // if child has a defined modulus then bump size
				RegNumber childSize = (childInst.getAddressIncrement() != null) ? new RegNumber(childInst.getAddressIncrement()) : 
					                                                       new RegNumber(childInst.regComp.getAlignedSize());  // compute size of this instance or use increment if specified
				childSize.multiply(childInst.getRepCount());  
				newAlignedSize.add(childSize);
			}
		}
		newAlignedSize.setNextHighestPowerOf2();  // round size to next power of 2  
		this.alignedSize = newAlignedSize;
	}

	/** sortRegisters - fix simple out of order address order issues */
	@Override
	public void sortRegisters() {
		if (needsAddressSort()) sortChildrenByAddress();
		// now process children
		for (ModInstance regInst : getChildInstances()) regInst.regComp.sortRegisters();
	}

	// ------------------------------------ code gen templates ----------------------------------------
	
	/* generate output for regset */
	@Override
	public void generateOutput(ModInstance callingInst, OutputBuilder outputBuilder) {
		RegSetProperties regSetProperties = null;
	
		// each subclass should override to create appropriate code based on calling instance
		if (callingInst == null) return;
		
		// get replication count
		int repCount = callingInst.getRepCount();  // get non-null repCount  
		// check for replicated addrmaps
		if (isAddressMap() && (repCount > 1)) {
			Ordt.errorMessage("Replicated address maps are not allowed (map name = " + callingInst.getId() + ")");
			repCount = 1;
		}
		
		//System.err.println("--- ModRegSet " + callingInst.getId() + ", reps=" + repCount);
		
		// if this is the first address map instance encountered (in each builder) treat special (no builder instance stack push/pop)
		if (isAddressMap() && outputBuilder.isFirstAddressMap()) {  
			//System.out.println("--- ModRegSet.generateOutput: root address map, mod instance=" + callingInst.getId() + ", root=" + callingInst.isRootInstance() + ", this is amap=" + isAddressMap() + ", rs amap=" + ", bid=" + outputBuilder.getBuilderID());
			outputBuilder.addRegMap(callingInst);  // regSetProperties will be created for root map in this builder, indicate we're done with first map 

			// generate each direct instance in this component
			for (ModInstance regInst : getChildInstances()) {
				regInst.generateOutput(outputBuilder);
			}

			outputBuilder.finishRegMap(callingInst); 	
		}
		// otherwise, treat as a typical reg set
		else {
	    
			// call once per replicated register set
			for (int rep=0; rep<repCount; rep++) {
				//System.out.println("--- ModRegSet.generateOutput: non-root address map or regfile, null instance=" + callingInst.getId() + ", rep=" + rep);

				regSetProperties = new RegSetProperties(callingInst);  // extract basic properties
				outputBuilder.setExternalInstanceProperties(regSetProperties, isAddressMap());  // set external inst properties
				// set addr map
				if (isAddressMap()) {
					//System.out.println("ModRegSet generateOutput: addrmap instance=" + regSetProperties.getId() + ", ext type=" + regSetProperties.getExternalType());
					regSetProperties.setAddressMap(true);  // mark this as an address map
				}
				// else use rep number in regset name if output is visiting each
				else if (outputBuilder.visitEachRegSet() && (outputBuilder.visitExternalRegisters() || !regSetProperties.isExternal())) 
					regSetProperties.setId(regSetProperties.getId() + getRepSuffix(rep, repCount)); // update name based on rep #  
				
				outputBuilder.pushInstance(regSetProperties);  // root external is set here
				outputBuilder.addRegSet(regSetProperties, rep);  // FIXME - previous regSetProperties stuff could be pushed into addRegSet and return regSetProperties

				// generate each direct instance in this component
				for (ModInstance regInst : getChildInstances()) {  
					regInst.generateOutput(outputBuilder);
				}	
				
				// update current regSet address and stride now that all children are processed
				outputBuilder.updateLastRegSetAddress(regSetProperties);    // final next address is available after this for size compute

				if (regSetProperties.isLastRep()) {   
					// if this is a root external instance (addrmaps included), then add the external reg interface
					// if generation of external address maps is specified then just add to builder list for processing
					if (regSetProperties.isRootExternal() || (ExtParameters.sysVerGenerateChildAddrmaps() && isAddressMap())) { 
						//if (isAddressMap()) System.out.println("ModRegSet: adding addrmap, id=" + regSetProperties.getId() + ", root ext=" + regSetProperties.isRootExternal());  
						// create empty regProperties so external set can be created
						RegProperties regProperties = new RegProperties(callingInst, outputBuilder.fieldOffsetsFromZero());  // FIXME - dont need to use reg any more??
						if (isAddressMap()) regProperties.setAddressMap(true);  // set address map indicator
						if (!regProperties.isExternal()) regProperties.setExternalType(regSetProperties.getExternalType()); // pick up ext type if instance didnt carry it
						regProperties.setRegWidth(regSetProperties.getMaxRegWidth());  // use max width for this addrmap 
						//System.out.println("ModRegSet: adding ext regset with width=" + regProperties.getRegWidth() + ", ext type=" + regProperties.getExternalType()+ ", rs ext type=" + regSetProperties.getExternalType());  
						if (regSetProperties.isRootExternal()) outputBuilder.addRootExternalRegisters(regProperties);  // generate ext interface 
						else outputBuilder.addNonRootExternalAddressMap(regProperties);  // generate a non-root addrmap	(used in sv builder)					
					}
				}
				outputBuilder.finishRegSet(regSetProperties);    // pop the active regset
				outputBuilder.popInstance();
			}	
		}
	}
}
