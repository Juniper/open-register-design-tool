/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved.
 */
package ordt.output.drvmod;

import java.io.BufferedWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;

import ordt.extract.RegModelIntf;
import ordt.output.FieldProperties;
import ordt.output.OutputBuilder;

/** builder class for creating reg driver data structures - language independent */
public class DrvModBuilder extends OutputBuilder {
	
	// use hashmap mapped w/ same key/value for storing unique instances so values can be extracted later
	protected HashMap<DrvModRegSetInstance, DrvModRegSetInstance> uniqueRegSets = new HashMap<DrvModRegSetInstance, DrvModRegSetInstance>();
	protected HashMap<DrvModRegInstance, DrvModRegInstance> uniqueRegs = new HashMap<DrvModRegInstance, DrvModRegInstance>();

	int addedInstances, uniqueInstances = 0;
	private Stack<DrvModRegSetInstance> currentRegSetStack = new Stack<DrvModRegSetInstance>();
	protected int overlayCount = 0;
	
	private static HashSet<String> reservedWords = getReservedWords();
	
    //---------------------------- constructor ----------------------------------

    public DrvModBuilder(RegModelIntf model) {
	    this.model = model;  // store the model ref
	    setVisitEachReg(false);   // only need to call once for replicated reg groups
	    setVisitEachRegSet(false);   // only need to call once for replicated reg set groups
	    setVisitExternalRegisters(true);  // we will visit externals 
	    setVisitEachExternalRegister(false);	    // handle externals as a group
	    setAllowLocalMapInternals(true);  // cascaded addrmaps will result in local non-ext regions   
	    setSupportsOverlays(true);	    // support overlay files
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

    /** load C++ reserved words to be escaped */
	private static HashSet<String> getReservedWords() {
		HashSet<String> reservedWords = new HashSet<String>();
		//reservedWords.add("bit");
		return reservedWords;
	}
	
    /** escape string if a reserved words  */
	@SuppressWarnings("unused")
	private String escapeReservedString(String word) {
		if (reservedWords.contains(word)) return ("\\" + word + " ");  
		return word;
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
		DrvModRegInstance newReg = new DrvModRegInstance(regProperties.getId(), overlayCount, regProperties.getRegWidth(), regProperties.getRelativeBaseAddress().toLong(), regProperties.getRepCount(), regProperties.getAddrStride().toLong());
		// add field info to this reg before uniqueRegs check so has is valid
		for (FieldProperties fld: fieldList) 
			newReg.addField(fld.getPrefixedId(), fld.getLowIndex(), fld.getFieldWidth(), fld.isSwReadable(), fld.isSwWriteable());
        addedInstances++;
		if (!uniqueRegs.containsValue(newReg)) {
			uniqueRegs.put(newReg, newReg);
        	uniqueInstances++;
    		// add this reg to its parent
    		currentRegSetStack.peek().addChild(newReg, overlayCount);  // if newReg is unique, add to parent's child list
    		//System.out.println("DrvModBuilder finishRegister:    unique id=" + regProperties.getId() + ", reps=" + regProperties.getRepCount() + ", base=" + regProperties.getFullBaseAddress() );
        }
		// otherwise add original instance as child  
		else { 
    		currentRegSetStack.peek().addChild(uniqueRegs.get(newReg), overlayCount); // if a dup, bump map encoding in parent's child list
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
		Long relativeAddr = (regSetProperties.getRelativeBaseAddress() == null)? regSetProperties.getFullBaseAddress().toLong() : regSetProperties.getRelativeBaseAddress().toLong();
		//System.out.println("DrvModBuilder addRegSet: basename=" + regSetProperties.getBaseName() + ", id=" + regSetProperties.getId() + ", reps=" + regSetProperties.getRepCount() + ", relAddr=" + relativeAddr + ", alignedSize=" + regSetProperties.getAlignedSize());
		DrvModRegSetInstance newRegSet = new DrvModRegSetInstance(regSetProperties.getId(), overlayCount, relativeAddr, regSetProperties.getRepCount(), regSetProperties.getAlignedSize().toLong());
		// update current instance
		currentRegSetStack.push(newRegSet);
	}

	@Override
	public void finishRegSet() {    
		//System.out.println("DrvModBuilder finishRegSet: " + regSetProperties.getBaseName() + ", id=" + regSetProperties.getId() + ", reps=" + regSetProperties.getRepCount() + ", base=" + regSetProperties.getFullBaseAddress() + ", high=" + regSetProperties.getFullHighAddress() + ", stride=" + regSetProperties.getExtractInstance().getAddressIncrement());
		DrvModRegSetInstance newRegSet = currentRegSetStack.pop();
		// now that all children are added, test for uniqueness
        addedInstances++;
		//if ("rx_memctl".equals(newRegSet.getName())) {
		//	System.out.println("DrvModBuilder finishRegSet:        pre id=" + newRegSet.getName() + ", mapId=" + newRegSet.getMapId() + ", reps="  + newRegSet.getReps() + ", base=" + newRegSet.getAddressOffset() + ", stride=" + newRegSet.getAddressStride() + ", hashCode=" + newRegSet.hashCode() + ", containsKey=" + uniqueRegSets.containsKey(newRegSet)+ ", containsValue=" + uniqueRegSets.containsValue(newRegSet));
		//}
		if (!uniqueRegSets.containsValue(newRegSet)) {
        	uniqueRegSets.put(newRegSet, newRegSet);
        	uniqueInstances++;
    		//if (/*(overlayCount>0) ||*/ "rx_memctl".equals(newRegSet.getName()) ) 
    		//	System.out.println("DrvModBuilder finishRegSet:    unique id=" + newRegSet.getName() + ", mapId=" + newRegSet.getMapId() + ", reps="  + newRegSet.getReps() + ", base=" + newRegSet.getAddressOffset() + ", stride=" + newRegSet.getAddressStride() + ", hashCode=" + newRegSet.hashCode() + ", containsKey=" + uniqueRegSets.containsKey(newRegSet)+ ", containsValue=" + uniqueRegSets.containsValue(newRegSet));
    		// add this regset to its parent
    		if (!currentRegSetStack.isEmpty()) currentRegSetStack.peek().addChild(newRegSet, overlayCount); // if unique, add to parent's child list
        }
		// otherwise add original instance as child
		else { 
			// transfer newRegSet children into unique version
			for (DrvModBaseInstance inst: newRegSet.getChildren()) {
				uniqueRegSets.get(newRegSet).addChild(inst, overlayCount);
			}
    		if (!currentRegSetStack.isEmpty()) currentRegSetStack.peek().addChild(uniqueRegSets.get(newRegSet), overlayCount); // if a dup, bump map encoding in parent's child list
	    	//	System.out.println("DrvModBuilder finishRegSet: duplicate id=" + regSetProperties.getId() + ", reps=" + regSetProperties.getRepCount() + ", base=" + regSetProperties.getFullBaseAddress() + ", high=" + regSetProperties.getFullHighAddress() + ", stride=" + regSetProperties.getExtractInstance().getAddressIncrement());
		}
		if (regSetProperties.isRootInstance())
			System.out.println("DrvModBuilder finishRegSet: added instances=" + addedInstances + ", unique instances=" + uniqueInstances + ", duplicate instances=" + (addedInstances - uniqueInstances));
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

    //---------------------------- output write methods ----------------------------------------

    /** required default write method - not used in DrvModBuilder */
	@Override
	public void write(BufferedWriter bw) {
	}
		
}
