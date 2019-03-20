/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved.
 */
package ordt.output.drvmod;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import ordt.output.common.MsgUtils;
import ordt.extract.RegModelIntf;
import ordt.output.FieldProperties;
import ordt.output.OutputBuilder;
import ordt.output.RhsReference;
import ordt.output.drvmod.DrvModRegSetInstance.DrvModRegSetChildInfo;

/** builder class for creating reg driver data structures - language independent */
public abstract class DrvModBuilder extends OutputBuilder {
		
	// use hashmap mapped w/ same key/value for storing unique instances so values can be extracted later
	protected HashMap<DrvModRegSetInstance, DrvModRegSetInstance> uniqueRegSets = new HashMap<DrvModRegSetInstance, DrvModRegSetInstance>();
	protected HashMap<DrvModRegInstance, DrvModRegInstance> uniqueRegs = new HashMap<DrvModRegInstance, DrvModRegInstance>();

	int addedInstances, uniqueInstances = 0;  // counts per overlay
	private Stack<DrvModRegSetInstance> currentRegSetStack = new Stack<DrvModRegSetInstance>();
	protected int overlayCount = 0;
	protected List<RootInstanceInfo> rootInstances = new ArrayList<RootInstanceInfo>();
	
    //---------------------------- constructor ----------------------------------

    public DrvModBuilder(RegModelIntf model) {
	    this.model = model;  // store the model ref
	    setVisitEachReg(false);   // only need to call once for replicated reg groups
	    setVisitEachRegSet(false);   // only need to call once for replicated reg set groups
	    setVisitExternalRegisters(true);  // we will visit externals 
	    setVisitEachExternalRegister(false);	    // handle externals as a group
	    setSupportsOverlays(true);	    // support overlay files
	    DrvModBaseInstance.setBuilder(this);  // save this builder in overlay model
		RhsReference.setInstancePropertyStack(instancePropertyStack);  // update pointer to the instance stack for rhs reference evaluation
	    model.getRoot().generateOutput(null, this);   // generate output structures recursively starting at model root
    }
    
	/** process an overlay file */
    @Override
	public void processOverlay(RegModelIntf model) {
    	overlayCount++;
	    this.model = model;  // store the model ref
	    resetBuilder();
	    model.getRoot().generateOutput(null, this);   // generate output structures recursively starting at model root
	}

    //---------------------------- OutputBuilder methods to load structures ----------------------------------------

	@Override
	public void addField() {}

	@Override
	public void addAliasField() {    
		// handle same as non-aliased field
		addField();
	}

	@Override
	public void addRegister() {
	}

	@Override
	public void finishRegister() {  
		//	System.out.println("DrvModBuilder finishRegister: " + regProperties.getInstancePath() + ", base=" + regProperties.getBaseAddress());	
		// create new reg instance
		DrvModRegInstance newReg = new DrvModRegInstance(regProperties.getId(), overlayCount, regProperties.getRegWidth());
		// add field info to this reg before uniqueRegs check so has is valid
		while (fieldList.size() > 0) {
			FieldProperties fld = fieldList.remove();  // get next field
			newReg.addField(fld.getPrefixedId(), fld.getLowIndex(), fld.getFieldWidth(), fld.isSwReadable(), fld.isSwWriteable(), fld.getReset());
		}
        addedInstances++;
		if (!uniqueRegs.containsValue(newReg)) {
			uniqueRegs.put(newReg, newReg);
        	uniqueInstances++;
    		// add this reg to its parent
    		currentRegSetStack.peek().addChild(newReg, regProperties.getRelativeBaseAddress().toLong(), regProperties.getRepCount(), regProperties.getAddrStride().toLong(), overlayCount);  // if newReg is unique, add to parent's child list
    		//System.out.println("DrvModBuilder finishRegister:    unique id=" + regProperties.getId() + ", reps=" + regProperties.getRepCount() + ", base=" + regProperties.getFullBaseAddress() );
        }
		// otherwise add original instance as child  
		else { 
    		currentRegSetStack.peek().addChild(uniqueRegs.get(newReg), regProperties.getRelativeBaseAddress().toLong(), regProperties.getRepCount(), regProperties.getAddrStride().toLong(), overlayCount); // if a dup, bump map encoding in parent's child list
        	//	System.out.println("DrvModBuilder finishRegister: duplicate id=" + regProperties.getId() + ", reps=" + regProperties.getRepCount() + ", base=" + regProperties.getFullBaseAddress() );
		}
	}

	@Override
	public void addRegSet() {
		// clear tracking counters
		if (regSetProperties.isRootInstance()) {
			addedInstances = 0;
			uniqueInstances = 0;
		}
		// create new regset instance
		//System.out.println("DrvModBuilder addRegSet: basename=" + regSetProperties.getBaseName() + ", id=" + regSetProperties.getId() + ", reps=" + regSetProperties.getRepCount() + ", relAddr=" + relativeAddr + ", alignedSize=" + regSetProperties.getAlignedSize());
		DrvModRegSetInstance newRegSet = new DrvModRegSetInstance(regSetProperties.getId(), overlayCount);
		// update current instance
		currentRegSetStack.push(newRegSet);
		// save root instance for this overlay
		if (regSetProperties.isRootInstance()) {
			Long relativeAddr = (regSetProperties.getRelativeBaseAddress() == null)? regSetProperties.getFullBaseAddress().toLong() : regSetProperties.getRelativeBaseAddress().toLong();
			rootInstances.add(new RootInstanceInfo(newRegSet, relativeAddr));
        }
	}

	@Override
	public void finishRegSet() {   
		Long relativeAddr = (regSetProperties.getRelativeBaseAddress() == null)? regSetProperties.getFullBaseAddress().toLong() : regSetProperties.getRelativeBaseAddress().toLong();
		//System.out.println("DrvModBuilder finishRegSet: " + regSetProperties.getBaseName() + ", id=" + regSetProperties.getId() + ", reps=" + regSetProperties.getRepCount() + ", base=" + regSetProperties.getFullBaseAddress() + ", high=" + regSetProperties.getFullHighAddress() + ", stride=" + regSetProperties.getExtractInstance().getAddressIncrement());
		DrvModRegSetInstance newRegSet = currentRegSetStack.pop();
		// now that all children are added, test for uniqueness
        addedInstances++;
        // add newRegSet if it's unique
		if (!uniqueRegSets.containsKey(newRegSet)) {  // same as containsValue in this case
        	uniqueRegSets.put(newRegSet, newRegSet);
        	uniqueInstances++;
    		// unique, so add this regset to its parent
    		if (!currentRegSetStack.isEmpty()) currentRegSetStack.peek().addChild(newRegSet, relativeAddr, regSetProperties.getRepCount(), regSetProperties.getAlignedSize().toLong(), overlayCount); 
        }
		// otherwise add newRegSet instance as a duplicate
		else { 
			// transfer newRegSet children into the unique version
			for (DrvModRegSetChildInfo cInfo: newRegSet.getChildren()) {
				//System.out.println("DrvModBuilder finishRegSet: updating child=" + cInfo.child.getName() + "of newRegSet " + newRegSet.getName() + ", null uniqueRegSets.get(newRegSet)?=" + (uniqueRegSets.get(newRegSet)==null) + ", uniqueRegSets.containsKey(newRegSet)=" + uniqueRegSets.containsKey(newRegSet) + ", newRegSet.hash=" + newRegSet.hashCode());
				uniqueRegSets.get(newRegSet).updateChild(cInfo, overlayCount, false);  // no adds are allowed here
			}
			// if a dup, bump map encoding in parent's child list
    		if (!currentRegSetStack.isEmpty()) currentRegSetStack.peek().addChild(uniqueRegSets.get(newRegSet), relativeAddr, regSetProperties.getRepCount(), regSetProperties.getAlignedSize().toLong(), overlayCount); 
	    	//	System.out.println("DrvModBuilder finishRegSet: duplicate id=" + regSetProperties.getId() + ", reps=" + regSetProperties.getRepCount() + ", base=" + regSetProperties.getFullBaseAddress() + ", high=" + regSetProperties.getFullHighAddress() + ", stride=" + regSetProperties.getExtractInstance().getAddressIncrement());
		}
		if (regSetProperties.isRootInstance())  
			MsgUtils.infoMessage("Overlay " + overlayCount + " total processed instances=" + addedInstances + ", unique instances=" + uniqueInstances + ", duplicate instances=" + (addedInstances - uniqueInstances));
	}

	/** process root address map */
	@Override
	public void addRegMap() { 
		//System.out.println("DrvModBuilder addRegMap: root found, ipath=" + regSetProperties.getBaseName() + ", id=" + regSetProperties.getId());
		addRegSet();
	}

	/** finish root address map  */
	@Override
	public  void finishRegMap() {
		finishRegSet();
	}
	
    //---------------------------- model process methods ----------------------------------------

	public abstract void processRegSetInstance(DrvModRegSetInstance drvModRegSetInstance); 
	public abstract void processRegInstance(DrvModRegInstance drvModRegInstance);

	// inner clas to save root instance/offset
	public class RootInstanceInfo {
		public DrvModRegSetInstance instance;
		public long relativeAddress;
		private RootInstanceInfo(DrvModRegSetInstance instance, long relativeAddress) {
			super();
			this.instance = instance;
			this.relativeAddress = relativeAddress;
		}
	}
	
    //---------------------------- output write methods ----------------------------------------

    /** required default write method - not used in DrvModBuilder */
	@Override
	public void write(BufferedWriter bw) {
	}
		
}
