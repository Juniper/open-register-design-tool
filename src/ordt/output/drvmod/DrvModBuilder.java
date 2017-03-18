/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved.
 */
package ordt.output.drvmod;

import java.io.BufferedWriter;
import java.util.HashSet;
import java.util.Stack;

import ordt.extract.RegModelIntf;
import ordt.output.FieldProperties;
import ordt.output.OutputBuilder;

/** builder class for creating reg driver data structures - language independent */
public class DrvModBuilder extends OutputBuilder {
	
	protected HashSet<DrvModRegSetInstance> uniqueRegSets = new HashSet<DrvModRegSetInstance>();
	protected HashSet<DrvModRegInstance> uniqueRegs = new HashSet<DrvModRegInstance>();
	private Stack<DrvModRegSetInstance> currentRegSetStack = new Stack<DrvModRegSetInstance>();
	
	private static HashSet<String> reservedWords = getReservedWords();
	
    //---------------------------- constructor ----------------------------------

    public DrvModBuilder(RegModelIntf model) {
	    this.model = model;  // store the model ref
	    setVisitEachReg(false);   // only need to call once for replicated reg groups
	    setVisitEachRegSet(false);   // only need to call once for replicated reg set groups
	    setVisitExternalRegisters(true);  // we will visit externals 
	    setVisitEachExternalRegister(false);	    // handle externals as a group
	    setAllowLocalMapInternals(true);  // cascaded addrmaps will result in local non-ext regions   
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
	public void finishRegister() {  // TODO toInteger wont work, need toLong
		//	System.out.println("DrvModBuilder finishRegister: " + regProperties.getInstancePath() + ", base=" + regProperties.getBaseAddress());	
		// create new reg instance
		DrvModRegInstance newReg = new DrvModRegInstance(regProperties.getId(), 0, regProperties.getRelativeBaseAddress().toLong(), regProperties.getRepCount(), regProperties.getAddrStride().toLong());
		uniqueRegs.add(newReg);
		// add field info to this reg from sorted fieldList
		for (FieldProperties fld: fieldList) 
			newReg.addField(fld.getPrefixedId(), fld.getLowIndex(), fld.getFieldWidth(), fld.isSwReadable(), fld.isSwWriteable());
		// add this reg to its parent
		currentRegSetStack.peek().addChild(newReg);
	}

	@Override
	public void addRegSet() {
		System.out.println("DrvModBuilder addRegSet: basename=" + regSetProperties.getBaseName() + ", id=" + regSetProperties.getId() + ", reps=" + regSetProperties.getRepCount() + ", base=" + regSetProperties.getFullBaseAddress() + ", high=" + regSetProperties.getFullHighAddress() + ", alignedSize=" + regSetProperties.getAlignedSize());
		// create new regset instance
		DrvModRegSetInstance newRegSet = new DrvModRegSetInstance(regSetProperties.getId(), 0, regSetProperties.getRelativeBaseAddress().toLong(), regSetProperties.getRepCount(), regSetProperties.getAlignedSize().toLong());
		uniqueRegSets.add(newRegSet);
		// add this regset to its parent
		currentRegSetStack.peek().addChild(newRegSet);
		// update current instance
		currentRegSetStack.push(newRegSet);
	}

	@Override
	public void finishRegSet() {    
		//System.out.println("DrvModBuilder finishRegSet: " + regSetProperties.getBaseName() + ", id=" + regSetProperties.getId() + ", reps=" + regSetProperties.getRepCount() + ", base=" + regSetProperties.getFullBaseAddress() + ", high=" + regSetProperties.getFullHighAddress() + ", stride=" + regSetProperties.getExtractInstance().getAddressIncrement());
		currentRegSetStack.pop();
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
