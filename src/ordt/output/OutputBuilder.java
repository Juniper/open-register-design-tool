/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved. 
 */
package ordt.output;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Stack;

import ordt.output.common.MsgUtils;
import ordt.extract.RegModelIntf;
import ordt.extract.RegNumber;
import ordt.extract.Ordt;
import ordt.extract.Ordt.InputType;
import ordt.extract.RegNumber.NumBase;
import ordt.extract.RegNumber.NumFormat;
import ordt.extract.model.ModInstance;
import ordt.output.common.OutputLine;
import ordt.output.common.OutputWriterIntf;
import ordt.parameters.ExtParameters;
import ordt.parameters.Utils;

/**
 *  @author snellenbach      
 *  Jan 27, 2014
 *
 */
public abstract class OutputBuilder implements OutputWriterIntf{

	protected RegModelIntf model;  // model containing rdl extract info

	protected BufferedWriter bufferedWriter;
	
	// unique instance ID
	private static int nextBuilderID = 0;
	private int builderID = 0;
	
	private RegNumber nextAddress = new RegNumber("0x0");   // initialize to address 0
	private RegNumber baseAddress = new RegNumber("0x0");   // initialize to address 0
	
	private int maxRegWidth = ExtParameters.getMinDataSize();  // maximum sized register found in this addrmap - default to min pio data width  // TODO - can replace with precalculated value in model

	protected Stack<InstanceProperties> instancePropertyStack = new Stack<InstanceProperties>();  // track currently active instance path
	
	private String addressMapName = (ExtParameters.defaultBaseMapName().isEmpty())? "" : ExtParameters.defaultBaseMapName();  // name of the base address map in this builder
	private boolean firstAddressMap = true;  // indication of first address map visited
	
	// parse rules for this output
	private boolean visitEachReg = true;   // should each register in a replicated group be visited
	private boolean visitEachRegSet = true;  // should each regset in a replicated group be visited
	private boolean visitExternalRegisters = false;  // should any register group/regset in an external group be visited
	private boolean visitEachExternalRegister = false;  // should each register in an external group be visited (treated as internal)
	private boolean supportsOverlays = false;  // if true, builder supports processing of overlay models

	private RegNumber externalBaseAddress;  // starting address of current external reg group
		
	// active rdl component info
	protected  SignalProperties signalProperties;  // output-relevant active signal properties 
	protected PriorityQueue<FieldProperties> fieldList = new PriorityQueue<FieldProperties>(  // fields in current register ordered by idx
	    	128, new Comparator<FieldProperties>(){
            public int compare(FieldProperties a, FieldProperties b) {
                if (a.getLowIndex() > b.getLowIndex()) return 1;  // ascending list
                if (a.getLowIndex() == b.getLowIndex()) return 0;
                return -1;
            }
        });
			
	protected  FieldProperties fieldProperties;  // output-relevant active field properties  
	protected  FieldSetProperties fieldSetProperties;  // output-relevant active field properties  
	protected Stack<FieldSetProperties> fieldSetPropertyStack = new Stack<FieldSetProperties>();  // field sets are nested so store stack
	protected  RegProperties regProperties;  // output-relevant active register properties  
	protected boolean regIsActive = false;  // indicator that regProperties is currently active
	protected Stack<RegSetProperties> regSetPropertyStack = new Stack<RegSetProperties>();  // reg sets are nested so store stack
	protected  RegSetProperties regSetProperties;  // output-relevant active register set properties  
	protected  RegSetProperties rootMapProperties;  // properties of root address map (separate from regSetProperties since root map is handled differently wrt rs stack) 
	
	/** reset builder state */
	protected void resetBuilder() {
		nextAddress = new RegNumber("0x0");   // initialize to address 0
		baseAddress = new RegNumber("0x0");   // initialize to address 0
		firstAddressMap = true;  // indication of first address map visited
		instancePropertyStack.clear();
		fieldSetPropertyStack.clear();
		regSetPropertyStack.clear();
	}
	
	/** get builderID
	 *  @return the builderID
	 */
	public int getBuilderID() {
		return builderID; 
	}

	/** set a new builderid */
	protected void setNewBuilderID() {
		this.builderID = nextBuilderID++;   // set unique ID of this instance
	}

	/** set a new builderid of 0 for this instance */
	protected void setBaseBuilderID() {
		nextBuilderID = 0;
		this.builderID = nextBuilderID++;   // set unique ID of this instance
	}

	/** return true if this is the root VerilogBuilder
	 */
	public boolean isBaseBuilder() {
		return (this.builderID == 0);
	}
	
	/** return fieldOffsetsFromZero state from the model **/
	public boolean fieldOffsetsFromZero() {
		return model.fieldOffsetsFromZero();
	}

	/** process an overlay file */
	public void processOverlay(RegModelIntf model) {
	}
		
	//---------------------------- methods to load verilog structures ----------------------------------------
	
	/** add a signal to this output */
	public  void addSignal(SignalProperties sProperties) {  
		if (sProperties != null) {
		   signalProperties = sProperties;   
		   // set instance path and instance property assigns
		   signalProperties.updateInstanceInfo(getInstancePath());
		   addSignal();
		}
	}
	
	/** add a signal for a particular output - concrete since most builders do not use */
	protected  void addSignal() {};
		
	/** add a field to this output */
	public  void addField(FieldProperties fProperties) {
		if (fProperties != null) {
		   
		   fieldProperties = fProperties;
			
		   // set instance path and instance property assigns
		   fieldProperties.updateInstanceInfo(getInstancePath());
		   
		   if (regProperties == null ) {   //FIXME
			   //System.out.println("OutputBuilder: addField: NO REGPROPERTIES DEFINED, path=" + getInstancePath() + ", id=" + fieldProperties.getId());
               return;  // exit if no active register
		   }
		   // get index range of this field in the register
		   Integer computedLowIndex = regProperties.addField(fieldProperties); // get field array indices from reg
		   fieldProperties.setLowIndex(computedLowIndex); // save the computed index back into fieldProperties  // TODO - updateIndexInfo here or in addField?
		   
		   // set slice ref string for this field
		   String fieldArrayString = "";
		   if (fieldProperties.getFieldWidth() != regProperties.getRegWidth())  
			   fieldArrayString = genRefArrayString(computedLowIndex, fieldProperties.getFieldWidth());
		   fieldProperties.setFieldArrayString(fieldArrayString);
		   // set fieldset prefix of this field instance
		   if (!fieldSetPropertyStack.isEmpty()) {
			   String prefix = getFieldSetPrefix(); // extract from fieldset instance values
			   //System.err.println("OutputBuilder: addField, setting prefix to  " + prefix);
			   //System.out.println("  field path=" + fieldProperties.getInstancePath() + ", id=" + fieldProperties.getId());
			   //System.out.println("    reg path=" + regProperties.getInstancePath() + ", id=" + regProperties.getId());
			   fieldProperties.setFieldSetPrefixString(prefix);			   
		   }
		   
		   // check for invalid field parameters
		   if (fieldProperties.isInvalid()) MsgUtils.errorMessage("invalid rw access to field " + fieldProperties.getInstancePath());
		   		    
		   if (regProperties != null) {
			   // handle field output for alias vs non-alias regs
			   if (visitEachReg() || (regProperties.isFirstRep() && firstRegSetRep())) { // qualify add calls by valid register options	
					fieldList.add(fieldProperties);  // add field to list for current register
				   // inhibit child builder field call if external and not visiting externals
				   if (!regProperties.isExternal() || visitExternalRegisters()) {
					   if (regProperties.isAlias()) 
						   addAliasField();
					   else
						   addField();
				   }
			   }
			   //System.out.println("path=" + getInstancePath() + ", id=" + regInst.getId());
		   }
		}
	}
	
	/** add a field for a particular output */
	abstract public  void addField();
		
	/** add an alias register field for a particular output */
	abstract public  void addAliasField();

	/** add a field setr to this output 
	 * @param rep - replication count
     */
	public void addFieldSet(FieldSetProperties fsProperties, int rep) {
		if (fsProperties != null) {
			   //System.out.println("OutputBuilder: addFieldSet, path=" + getInstancePath() + ", id=" + fsProperties.getId());

			   // extract properties from instance/component
			   fieldSetProperties = fsProperties; 
			   // set instance path and instance property assigns
			   fieldSetProperties.updateInstanceInfo(getInstancePath());  
			   // set rep number of this iteration
			   fieldSetProperties.setRepNum(rep);
			   
			   Integer parentFsOffset = getCurrentFieldSetOffset();  // get parent's offset in reg			   
			   int newFsOffset = regProperties.addFieldSet(fieldSetProperties, parentFsOffset); // get relative fieldset position from reg
			   fieldSetProperties.setOffset(newFsOffset);  // from actual reg add fs call so its always non-null
			   
			   fieldSetPropertyStack.push(fieldSetProperties);  // push active fieldset onto stack
			   
			   // inhibit child builder field call if external and not visiting externals
			   if (visitEachReg() || (regProperties.isFirstRep() && firstRegSetRep())) { // qualify add calls by valid register options	
				   if (!regProperties.isExternal() || visitExternalRegisters()) 
				       addFieldSet();
			   }
			}
	}

	/** add a fieldset for a particular output - concrete since most builders do not use */
	protected void addFieldSet() {}

	public void finishFieldSet(FieldSetProperties fsProperties) {
		if (fsProperties != null) {
			//System.out.println("OutputBuilder: finishFieldSet, path=" + getInstancePath() + ", id=" + fsProperties.getId());
			if (visitEachReg() || (regProperties.isFirstRep() && firstRegSetRep())) { // qualify add calls by valid register options
				// inhibit child builder field call if external and not visiting externals
				if (!regProperties.isExternal() || visitExternalRegisters()) 
					finishFieldSet();
			}
			// save min allowed offset prior to popping from the stack
			Integer newMinOffset = getCurrentFieldSetOffset() + fieldSetProperties.getFieldSetWidth();

			// pop from fieldset stack and restore parent as current
			fieldSetPropertyStack.pop();  // done, so pop from stack
			if (fieldSetPropertyStack.isEmpty()) fieldSetProperties = null;
			else fieldSetProperties = fieldSetPropertyStack.peek();  // restore parent as active fieldset
			// restore parent fieldset offset post pop
			regProperties.restoreFieldSetOffsets(getCurrentFieldSetOffset(), newMinOffset);
		}
	}

	/** finish a fieldset for a particular output - concrete since most builders do not use */
	protected void finishFieldSet() {}


	/** add a register to this output.
	 *  called for internal regs and externals where visitEachExternalRegister is set.
	 * @param regAddress - address of this register
	 * @param rep - replication count
      */
	public  void addRegister(RegProperties rProperties, int rep) {  
		if (rProperties != null) {
			regIsActive = true;
		   //System.out.println("OutputBuilder " + getBuilderID() + " addRegister, path=" + getInstancePath() + ", id=" + rProperties.getId() + ", addr=" + rProperties.getExtractInstance().getAddress());

		   // extract properties from instance/component
		   regProperties = rProperties; 
		   // set instance path and instance property assigns
		   regProperties.updateInstanceInfo(getInstancePath());
		   
		   updateMaxRegWidth(regProperties.getRegWidth());  // check for largest register width
		   
		   // set rep number of this iteration
		   regProperties.setRepNum(rep);
		   
		   // set register base address
		   updateRegBaseAddress();
		   
		   // now bump the running address count by regsize or increment value
		   RegNumber addressIncrement = regProperties.getExtractInstance().getAddressIncrement();
		   updateNextAddress(addressIncrement);  
		   
		   fieldList.clear();  // clear fields list for active register
		   
		   // only visit once if specified by this output type
		   if (visitEachReg() || (regProperties.isFirstRep() && firstRegSetRep())) {
			   addRegister();   
		   }
		}
	}

	/** add a register for a particular output */
	abstract public  void addRegister();

	/** process register info after all fields added */  
	public  void finishRegister(RegProperties rProperties) {  
		if (rProperties != null) {
			// only visit once if specified by this output type
			//System.out.println("OutputBuilder finishRegister: " + regProperties.getInstancePath() + ", visit each reg=" + visitEachReg() + ", isFirstRep=" + regProperties.isFirstRep() + ", firstRegSetRep=" + firstRegSetRep());
			if (visitEachReg() || (regProperties.isFirstRep() && firstRegSetRep())) {
			    updateFinishRegProperties(regProperties);  // update regprops post field processing
			    regSetProperties.updateChildHash(regProperties.hashCode(true)); // add this reg's hashcode to parent including id
			    // issue warning if register has no fields or isn't sw accessible
			    if (!regProperties.hasFields())
					MsgUtils.warnMessage("register "+ regProperties.getInstancePath() + " has no fields");
			    else if (!regProperties.isSwWriteable() && !regProperties.isSwReadable()) 
					MsgUtils.warnMessage("register "+ regProperties.getInstancePath() + " is neither readable nor writeable");
				finishRegister();   
			}
			regIsActive = false;

			// if visiting each external reg then may need to bump next addr when done
			if (visitEachExternalRegister() && regProperties.isLocalRootExternal() && regProperties.isLastRep()) {
				int padRegs = Utils.getNextHighestPowerOf2(regProperties.getRepCount()) - regProperties.getRepCount();
				if (padRegs > 0) {
					//System.out.println("OutputBuilder finishRegister: " + regProperties.getInstancePath() + ", ext=" + regProperties.isExternal() + ", local root ext=" + regProperties.isLocalRootExternal() + ", reps=" + regProperties.getRepCount() + ", pow 2 size=" + Utils.getNextHighestPowerOf2(regProperties.getRepCount()));
					updateNextAddress(padRegs);
				}
			}
		}
		//else System.out.println("OutputBuilder finishRegister: null regProperties");
	}

	/** finish a register for a particular output */
	abstract public  void finishRegister();
	
	/** add an external register group to this output (called in ModRegister if external and not visiting each reg) // TODO combine with addRegisters
	 * @param rProperties - extracted register properties */
	public  void addExternalRegisters(RegProperties rProperties) {  
		if (rProperties != null) {
		   regIsActive = true;  // regProperties is valid
		   
		   //if (rProperties.isRootExternal() != rProperties.isLocalRootExternal())
		   //   System.out.println("OutputBuilder addExternalRegisters: root mismath for inst=" + getInstancePath() + ", rootExt=" + rProperties.isRootExternal() + ", localRootExt=" + rProperties.isLocalRootExternal());

		   // extract properties from instance/component
		   regProperties = rProperties; 
		   // set instance path from instance stack, and init property info
		   regProperties.updateInstanceInfo(getInstancePath());
		   
		   updateMaxRegWidth(regProperties.getRegWidth());  // check for largest register width
		   
		   // set register base address
		   updateRegBaseAddress();
		   
		   if (regProperties.isRootExternal()) {
			   //System.out.println("OutputBuilder: addExternalRegisters: root external path=" + regProperties.getInstancePath() + ", base addr=" + regProperties.getBaseAddress()  + ", next/new ext base addr=" + nextAddress);
			   setExternalBaseAddress(nextAddress);  // save address of this reg if root external
		   }

		   // now bump the running address count by regsize or increment value times reg count
		   RegNumber addressIncrement = regProperties.getExtractInstance().getAddressIncrement();
		   updateNextAddress(addressIncrement, regProperties.getRepCount()); 
		   
		   //System.out.println("addExternalRegisters: path=" + regProperties.getInstancePath() + ", base=" + regProperties.getBaseAddress()  + ", next=" + nextAddress);

		   fieldList.clear();  // clear fields in current register

		   // if this output type will process external register output
		   if (visitExternalRegisters() && firstRegSetRep()) { // only first rset rep here (only one call for all reg reps)
			   addRegister();   
		   }
		}
	}

	/** process ext register info after all fields added */  
	public  void finishExternalRegisters(RegProperties rProperties) {
		if (rProperties != null) {
		    updateFinishRegProperties(regProperties);  // update regprops post field processing
			// only visit once if specified by this output type
			if (visitExternalRegisters() && firstRegSetRep()) {
			    regSetProperties.updateChildHash(regProperties.hashCode(true)); // add this reg's hashcode to parent including id
			    // issue warning if register has no fields or isn't sw accessible
			    if (!regProperties.hasFields())
					MsgUtils.warnMessage("register "+ regProperties.getInstancePath() + " has no fields");
			    else if (!regProperties.isSwWriteable() && !regProperties.isSwReadable()) 
					MsgUtils.warnMessage("register "+ regProperties.getInstancePath() + " is neither readable nor writeable");
				finishRegister();   // only first rset rep here (only one call for all reg reps)
			}
			regIsActive = false;	

			// pad the the running address if this is a local-only root external with non-power 2 count
			int padReps = Utils.getNextHighestPowerOf2(regProperties.getRepCount()) - regProperties.getRepCount();
			if (regProperties.isLocalRootExternal() && !regProperties.isRootExternal() && (padReps > 0)) {
				RegNumber addressIncrement = regProperties.getExtractInstance().getAddressIncrement();
				updateNextAddress(addressIncrement, padReps); 
			}
		}
	}

	/** add a set of external registers to this output 
	 * @param newRegProperties - if non-null (is ext regset, not reg), this will be set as external and used as static regProperties for output gen */
	public void addRootExternalRegisters(RegProperties newRegProperties) { 
		regIsActive = true;  // regProperties is valid
		int reservedRange = updateRootExternalRegProperties(newRegProperties, false);

		addRootExternalRegisters();  // note getExternalRegBytes() is usable by child

		// now bump the running address count by the reserved range
		RegNumber newNext = new RegNumber(getExternalBaseAddress());
		newNext.add(new RegNumber(reservedRange * getMinRegByteWidth()));
		setNextAddress(newNext);   
		//System.out.println("addRootExternalRegisters   base=" + getExternalBaseAddress() + ", next=" + getNextAddress() + ", delta=" + extSize);
		regIsActive = false;
	}

	/** add a register for a particular output  - concrete since most builders do not use. 
	 *  called when entering a root external register/registerset region */
	protected void addRootExternalRegisters() {}
	
	/** add a non-root address map to this output - used to generate verilog for non-root child maps such as ring leaf decoders
	 * @param newRegProperties - if non-null (is ext regset, not reg), this will be set as external and used as static regProperties for output gen */
	public void addNonRootExternalAddressMap(RegProperties newRegProperties) { 		
		updateRootExternalRegProperties(newRegProperties, true);

		addNonRootExternalAddressMap();  // note getExternalRegBytes() is usable by child
	}
	
	/** add a non-root addressmap - concrete since most builders do not use */
	protected void addNonRootExternalAddressMap() {}

	/** update static regProperties for child builder processing and return address range of regset
	 * @param newRegProperties - properties that will be updated
	 * @param isNonRootExternal - if true, base address will be set to that of current regset else builder ext base addr will be used
	 */
	private int updateRootExternalRegProperties(RegProperties newRegProperties, boolean isNonRootExternal) {
		// if new regProperties, then init 
		if (newRegProperties != null) {
			if (!newRegProperties.isExternal()) {
				newRegProperties.setExternalTypeFromString("DEFAULT");  // insure external is set
				//System.out.println("OutputBuilder addRootExternalRegisters: setting DEFAULT external for path=" + getInstancePath() +", orig ext=" + newRegProperties.getExternalType());
			}
			//System.out.println("OutputBuilder updateRootExternalRegProperties: updating base addr for path=" + getInstancePath() + ", old base=" + newRegProperties.getBaseAddress() + ", new base=" + getExternalBaseAddress() + ", rs base=" + regSetProperties.getBaseAddress());
			if (isNonRootExternal) newRegProperties.setBaseAddress(regSetProperties.getBaseAddress());   //  use current base to support multiple child scenarios
			else newRegProperties.setBaseAddress(getExternalBaseAddress());   //  use ext base address stored by builder
			newRegProperties.updateInstanceInfo(getInstancePath());  		   // set instance path and instance property assigns
			newRegProperties.setSwReadable(true);  // this is a regset external, so set readable/writeable to true
			newRegProperties.setSwWriteable(true);
			regProperties = newRegProperties;
		}
		//System.out.println("OutputBuilder addRootExternalRegisters: adding external reg set, path=" + getInstancePath()); // + ", reps=" + repCount);
		// load external rep_level parameters
		if (regProperties.hasExternalRepLevel()) {
			// issue warning if this is a regfile ext region
			//if (newRegProperties != null)
			//	MsgUtils.warnMessage("External rep_level option is only supported on registers currently. Will be ignored in instance " + regProperties.getInstancePath());
            // otherwise extract info needed for rep_level processing
			//else
				extractExternalRepLevelInfo();
		}

		// compute size of the external group from next and stored base address
		RegNumber extSize = regProperties.getExtractInstance().getAlignedSize();
		//System.out.println("OutputBuilder updateRootExternalRegProperties: " + regProperties.getInstancePath() + ", base=" + getExternalBaseAddress() + ", next=" + getNextAddress() + ", delta=" + extSize);

		int lowBit = getAddressLowBit();  // same low bit as overall address range
		regProperties.setExtLowBit(lowBit);  // save the low bit in external address
		int range = getAddressWidth(extSize);
		regProperties.setExtInstAddressWidth(range);  // set width of the external address for this group
		int reservedRange = 1 << range;  // calc 2^n
		//if (!extSize.isEqualTo(regProperties.getExtractInstance().getAlignedSize())) 
		//   System.err.println("OutputBuilder updateRootExternalRegProperties:   ext addr width=" + regProperties.getExtAddressWidth() + ", lo bit=" + lowBit + ", reservedRange=" + reservedRange + ", extSize=" + extSize + ", reps=" + regProperties.getRepCount() + ", inst aligned size=" + regProperties.getExtractInstance().getAlignedSize() + ", comp aligned size=" + regProperties.getExtractInstance().getRegComp().getAlignedSize());
		
		return reservedRange;
	}

	/** extract ancestor info needed for rep_level processing of this instance */
	private void extractExternalRepLevelInfo() {
		// get rep_level
		int repLevel = regProperties.getExternalType().getRepLevel();
		// check for valid repLevel
		int maxRepLevel = instancePropertyStack.size() - 1; 
		if ((repLevel > maxRepLevel) || (repLevel < 1)) {
			regProperties.getExternalType().removeParm("rep_level");
			MsgUtils.warnMessage("Invalid external rep_level option specified in instance " + regProperties.getInstancePath() + ".  rep_level will be ignored.");
			return;
		}
		// inhibit field_data option and issue a warning since rep_level and field_data are not supported concurrently TODO
		//if (regProperties.useExtFieldData()) {
			//regProperties.getExternalType().removeParm("field_data");
			//MsgUtils.warnMessage("External rep_level and field_data options are not supported concurrently.  field_data will be ignored in instance " + regProperties.getInstancePath()+ ".");
		//}
		// extract new path name and ancestor lists for address generation
		String repLevelPathStr = "";
		List<Integer> repLevelAddrBits = new ArrayList<Integer>(repLevel); // address bits of each ancestor
		List<Integer> repLevelIdx = new ArrayList<Integer>(repLevel); // rep index of each ancestor
		//regProperties.getExternalType().getRepLevelAddrBits(); regProperties.getExternalType().getRepLevelIdx();
		int ancestorLevel = maxRepLevel;
		boolean hasBits = false;
		for (InstanceProperties inst: instancePropertyStack) {
			if (inst != null) {
				// save rep info and remove reps from path if one of ancestors used for to determine replication
				if ((ancestorLevel > 0) && (ancestorLevel <= repLevel)) {
					int addrBits = Utils.getBits(inst.getRepCount());
					if (addrBits > 0) hasBits = true;
					repLevelAddrBits.add(addrBits);
					repLevelIdx.add(inst.getRepNum());
					repLevelPathStr += "_" + inst.getNoRepId();
				}
				else
					repLevelPathStr += "_" + inst.getId();
				ancestorLevel--;
			}
		}
		if (repLevelPathStr.length()<2) repLevelPathStr = "";
		else repLevelPathStr = repLevelPathStr.substring(1);
		// reverse list order
		Collections.reverse(repLevelAddrBits);
		Collections.reverse(repLevelIdx);
        // if no replicated ancestors or wrong ancestor count then exit
		if (!hasBits || (repLevelAddrBits.size() != repLevel)) {
			regProperties.getExternalType().removeParm("rep_level");
			return;
		}
		// save results
		regProperties.getExternalType().setRepLevelBasename(repLevelPathStr);
		regProperties.getExternalType().setRepLevelAddrBits(repLevelAddrBits);
		regProperties.getExternalType().setRepLevelIndices(repLevelIdx);
		//System.out.println("OutputBuilder extractExternalRepLevelInfo: " + regProperties.getInstancePath() + " has external rep_level option, rep_level=" + repLevel + ", newname=" + repLevelPathStr + ", repLevelAddrBits=" + repLevelAddrBits + ", repLevelIdx=" + repLevelIdx);
	}

	/** add a register set to this output 
	 * @param rep - replication count */
	public  void addRegSet(RegSetProperties rsProperties, int rep) {  
		
		if (rsProperties != null) {
			//System.out.println("OutputBuilder addRegSet: path=" + getInstancePath() + ", builder=" + builderID); // + ", id=" + regSetInst.getId());

		   regSetProperties = rsProperties; 
		   
		   // set instance path and instance property assigns
		   regSetProperties.updateInstanceInfo(getInstancePath());
		   
			// get address info from instance
		   RegNumber regSetAddress = regSetProperties.getExtractInstance().getAddress(); 
		   RegNumber addressModulus = regSetProperties.getExtractInstance().getAddressModulus();     
		   RegNumber addressShift = regSetProperties.getExtractInstance().getAddressShift(); 
		   
		   // set replication number
		   regSetProperties.setRepNum(rep);
		   //if (regSetProperties.getInstancePath().endsWith("cas_sop_1")) System.out.println("OutputBuilder addRegSet: " + regSetProperties.getInstancePath() + ", rep=" + rep + ", regSetAddress=" + regSetAddress + ", addressModulus=" + addressModulus + ", addressShift=" + addressShift  + ", nextAddress=" + nextAddress);
 		   
		   // if first rep, set the next address to new regset base register
		   boolean baseAddressSpecified = false;
		   if (regSetProperties.isFirstRep()) {
			   // if an explicit address is set for this regset, then use it  
			   if (regSetAddress != null) {
				   baseAddressSpecified = true;
				   //System.out.println("OutputBuilder addRegSet: --- rep=" + rep + ", regSetAddress=" + regSetAddress + ", incAddress=" + addressIncrement  + ", nextAddress=" + nextAddress);
				   
				   // compute relative address by adding to parent base
				   RegNumber newBaseAddr = new RegNumber(regSetAddress);  
				   newBaseAddr.add(getRegSetParentAddress());
				   //System.out.println("OutputBuilder addRegSet:   parent=" + getRegSetParentAddress() + ", abs=" + newBaseAddr);

				   // detect address errors and save next address
				   if (newBaseAddr.isLessThan(nextAddress)) {  // check for bad address here
					   MsgUtils.errorExit("out of order register set address specified in " + getInstancePath() );
				   }
				   setNextAddress(newBaseAddr);  //  save computed next address   
			    }
			   
			   // else if base address shift is specified
			   else if (addressShift != null) {
				   baseAddressSpecified = true;
				   updateNextAddress(addressShift);
			   }

			   // otherwise adjust if a modulus is specified
			   else if (addressModulus != null) {
				   baseAddressSpecified = true;
				   updateNextAddressModulus(addressModulus);  // adjust the base address if a modulus is defined					   
			   }
			   //if (regSetProperties.getInstancePath().endsWith("cas_sop_1")) System.out.println("OutputBuilder addRegSet: post-mod " + regSetProperties.getInstancePath() + ", rep=" + rep + ", regSetAddress=" + regSetAddress + ", addressModulus=" + addressModulus + ", addressShift=" + addressShift  + ", nextAddress=" + nextAddress);

			   // if js alignment is specified or a root external regset, shift base address of first rep 
			   if (ExtParameters.useJsAddressAlignment() || regSetProperties.isLocalRootExternal())
				   alignRegSetAddressToSize(regSetProperties, true, baseAddressSpecified);
		   }
		   
		   // for subsequent reps, get estimated size from model and check base address alignment (warnings only, no addr shift)
		   else if (ExtParameters.useJsAddressAlignment()) 
			   alignRegSetAddressToSize(regSetProperties, false, false);
	   
		   // save the base address of this reg set
		   regSetProperties.setBaseAddress(nextAddress); 
		   // save address of this reg set if root external 
		   if (regSetProperties.isRootExternal() && regSetProperties.isFirstRep()) setExternalBaseAddress(nextAddress); 
		   // compute the relative base address (vs parent) and save it
		   RegNumber relAddress = new RegNumber(regSetProperties.getBaseAddress());  // start with current
		   relAddress.subtract(getRegSetParentAddress());  // subtract parent base from current
		   regSetProperties.setRelativeBaseAddress(relAddress);  // store in regset
		   //System.out.println("OutputBuilder addRegSet:   saved  base=" + regSetProperties.getBaseAddress() + ", saved rel=" + regSetProperties.getRelativeBaseAddress() + ", saved ext base=" + getExternalBaseAddress());
		   // push onto regset properties stack
		   regSetPropertyStack.push(regSetProperties);    

		   // determine if this rep should be visited 
		   boolean visitThisRep = visitEachRegSet() || (regSetProperties.isFirstRep() && firstRegSetRep());
		   // if valid visit and specified reg set type then call finishRgSet 
		   if (visitThisRep && (!regSetProperties.isExternal() || 
				   (regSetProperties.isExternal() && visitExternalRegisters()))) {  
			   //System.out.println("addRegSet: rep=" + rep + ", vistEachRegSet=" + visitEachRegSet() + ", isFirstRep=" + regSetProperties.isFirstRep()  + ", firstRegSetRep=" + firstRegSetRep());
			   addRegSet();
		   }
		}
	}

	/** add a register set for a particular output */
	abstract public  void addRegSet();

	/** process regset/regmap info after all subsets,regs added 
	 * @param rsProperties - register set properties for active instance
	 */
	public  void finishRegSet(RegSetProperties rsProperties) {  
		// determine if this rep should be visited 
		boolean visitThisRep = visitEachRegSet() || (rsProperties.isFirstRep() && firstRegSetRep());
		// if valid visit and specified reg set type then call finishRgSet 
		if (visitThisRep && (!rsProperties.isExternal() || 
				(rsProperties.isExternal() && visitExternalRegisters()))) {  
			finishRegSet();
			//System.out.println("finishRegSet: external=" + rsProperties.isExternal() + ", visitThisRep=" + visitThisRep + ", vistEachRegSet=" + visitEachRegSet() + ", isFirstRep=" + regSetProperties.isFirstRep()  + ", firstRegSetRep=" + firstRegSetRep());		
		}

		// done with this regset, so pop stack to restore parent regset properties
		regSetPropertyStack.pop();

		if (regSetPropertyStack.isEmpty()) { regSetProperties = rootMapProperties;} 
		else {
			regSetPropertyStack.peek().updateChildHash(regSetProperties.hashCode(true)); // add popped regset's hashcode to parent including id
			regSetProperties = regSetPropertyStack.peek();
		}
	}
	
	/** finish a register set for a particular output */
	abstract public  void finishRegSet();
	
	/** add the root address map to this output - addRegMap is only called on root addrmap in Builder */
	public  void addRegMap(ModInstance regMapInst) {  
		if (regMapInst != null) {
		   regSetProperties = new RegSetProperties(regMapInst);  // extract basic properties  (maxregwidth, id, external, default properties)
		   regSetProperties.setAddressMap(true);
		   regSetProperties.setInternal();   // first map in a new builder is treated as internal (overrides RegSetProperties constructor)
		   rootMapProperties = regSetProperties;  // save these so can restore if empty rs stack
		   //System.out.println("OutputBuilder addRegMap: adding regmap,  path=" + getInstancePath() + ", id=" + regMapInst.getId()+ ", comp=" + regMapInst.getRegComp().getId());
		   // extract basic info for root map
		   regSetProperties.updateRootInstanceInfo();
		   //regSetProperties.display();
		   // if instance has an id then use it for modulename
		   if (regMapInst.getId() != null) setAddressMapName(regMapInst.getId());  
		   // only base addrmap instance is first
		   setFirstAddressMap(false);
		   addRegMap();
		}
	}
	
	/** add root address map for a particular output */
	abstract public  void addRegMap();
	
	/** process root address map info after all sub components added */
	public  void finishRegMap(ModInstance regMapInst) {
		if (regMapInst != null) {
			// save the highest address 
			RegNumber highAddress = new RegNumber(nextAddress);
			highAddress.subtract(1);
			regSetProperties.setHighAddress(highAddress); 			
		}
		finishRegMap();
	}
	
	/** finish root address map for a particular output */
	abstract public  void finishRegMap();


	//---------------------------- end of add/finish methods  ----------------------------------------
	
	/** update next address after last regset/regmap rep if an increment/align is specified (called by ModRegSet generate after child processing)
	 * @param rsProperties - register set properties for active instance
	 */
	public  void updateLastRegSetAddress(RegSetProperties rsProperties) {   
		// save the highest address of this reg set (last child address used)
		RegNumber highAddress = new RegNumber(nextAddress);
		highAddress.subtract(1);
		rsProperties.setHighAddress(highAddress); 
		//System.out.println("OutputBuilder updateLastRegSetAddress: id=" + rsProperties.getId() + ", next=" + nextAddress + ", highAddress=" + highAddress );	
		
		// if a replicated regset with increment, then set nextAddress to end of range when done
		RegNumber regSetBaseAddress = rsProperties.getBaseAddress();  // base address of last regset rep (rs stack is not yet popped)
		
		RegNumber addressIncrement = rsProperties.getExtractInstance().getAddressIncrement();
		if (regSetBaseAddress != null) {
			RegNumber newAddr = new RegNumber(regSetBaseAddress);
			if (addressIncrement != null) {  // if increment is specified, then pad (even if first rep)
			   newAddr.add(addressIncrement);
			   setNextAddress(newAddr);
			}
			
			// else if no incr, get estimated size from model and align base address if root external or align is specified
			else if ((ExtParameters.useJsAddressAlignment() && rsProperties.isReplicated())   // no address padding if not a replicated regset
					|| (rsProperties.isLocalRootExternal() && rsProperties.isLastRep()))   // pad if last iteration of a root external
				   alignRegSetAddressToSize(regSetProperties, true, false);  // shift address without warning
		}		
	}

	/** check this register set's current address and shift address if not a mod of its stored alignedSize
	 * @param regSetProperties - regsetProperties to be evaluated
	 * @param shiftAddress - if true, actually shift the address (otherwise only warning will be issued on misalignment)
	 * @param warnOnShift - if true, a warning is generated when address is shifted
	 */
	private void alignRegSetAddressToSize(RegSetProperties regSetProperties, boolean shiftAddress, boolean warnOnShift) {
		RegNumber alignBytes = regSetProperties.getExtractInstance().getRegComp().getAlignedSize();
		alignBytes.setNumBase(NumBase.Hex);
		alignBytes.setNumFormat(NumFormat.Address);
		if ((alignBytes != null) && alignBytes.isNonZero()) { 
			//if (regSetProperties.getInstancePath().contains("dest_credit_cnt")) System.out.println("OutputBuilder alignRegSetAddressToSize: regset=" + this.getInstancePath() + ", align==" + alignBytes + ", addr=" + nextAddress + ", rep=" + regSetProperties.getRepCount());
			// if this regset isn't in an external decoder and is misaligned, then align it
			if (!nextAddress.isModulus(alignBytes) && !regSetProperties.isExternalDecode()) {
				if (shiftAddress) {
					if (warnOnShift && !ExtParameters.suppressAlignmentWarnings()) 
						MsgUtils.warnMessage("base address for register set " + regSetProperties.getInstancePath() + " shifted to be " + alignBytes + "B aligned.");
					updateNextAddressModulus(new RegNumber(alignBytes));  // adjust the address to align register set					   
				}
				else if (!ExtParameters.suppressAlignmentWarnings()) 
						   MsgUtils.warnMessage("base address for register set " + regSetProperties.getInstancePath() + " is not " + alignBytes + "B aligned.");
			}
		}
		
	}
	
	/** returns true if active AddressableInstance is external */
	protected boolean activeInstanceIsExternal() { // TODO - may need to explicity set reg vs regset to handle rootExtRegs case, signal case needs autoselect tho
		if (regIsActive) return regProperties.isExternal();
		if (!regSetPropertyStack.isEmpty()) return regSetPropertyStack.peek().isExternal();
		return false;  // neither reg or regset is external
	}
	
	/** update reg properties now that fields are captured
	 * 
	 * @param rProperties - properties object that will be updated
	 */
	private void updateFinishRegProperties(RegProperties rProperties) {
        // update reg info now that field processing is complete  
		boolean regIsSwReadable = false, regIsSwWriteable = false, allFieldsSwReadable = true, allFieldsSwWriteable = true; 
		boolean regIsHwReadable = false, regIsHwWriteable = false;
		boolean regHasCounter = false, regHasInterrupt = false;
		for (FieldProperties field : fieldList) {  // note that pq iterator, so no order
			   if (field.isSwReadable())  regIsSwReadable = true; 
			   else allFieldsSwReadable = false;
			   if (field.isSwWriteable()) regIsSwWriteable = true;
			   else allFieldsSwWriteable = false;
			   if (field.isHwReadable()) regIsHwReadable = true;  
			   if (field.isHwWriteable()) regIsHwWriteable = true;				
			   if (field.isCounter()) regHasCounter = true;  
			   if (field.isInterrupt()) regHasInterrupt = true;				
		}
		//System.out.println("OutputBuilder updateFinishRegProperties: " + regProperties.getInstancePath() + ", sw r=" + regIsSwReadable+ ", sw w=" + regIsSwWriteable);
		// set reg sw access
		rProperties.setSwReadable(regIsSwReadable);  
		rProperties.setSwWriteable(regIsSwWriteable);
		
		// set field hash for this reg - need to convert pq to list
		rProperties.setFieldHash(getOrderedFieldList().hashCode()); 
		// set reg category if input is rdl
		if (ExtParameters.rdlResolveRegCategory() && Ordt.hasInputType(InputType.RDL) && !rProperties.hasCategory()) {
			boolean isStatus = allFieldsSwReadable && !regIsHwReadable && regIsHwWriteable && !regHasInterrupt;
			boolean isConfig = allFieldsSwWriteable && regIsHwReadable && !regIsHwWriteable && !regHasCounter && !regHasInterrupt;
			if (isStatus) rProperties.setCategory("STATE");
			else if (isConfig) rProperties.setCategory("DYNAMIC_CONFIG");
		}
	}

	/** return ordered list from fieldList priority queue */
	protected List<FieldProperties> getOrderedFieldList() {
		PriorityQueue<FieldProperties> temp = new PriorityQueue<FieldProperties>(fieldList);
		ArrayList<FieldProperties> outList = new ArrayList<FieldProperties>();
		while (!temp.isEmpty()) outList.add(temp.remove());
		return outList;
	}

	/** set the base address of a register group (internal or external) */
	private void updateRegBaseAddress() {
		   RegNumber regAddress = regProperties.getExtractInstance().getAddress();
		   RegNumber addressModulus = regProperties.getExtractInstance().getAddressModulus(); 
		   RegNumber addressShift = regProperties.getExtractInstance().getAddressShift(); 
		   
		   // save register address info - if explicit address then use it 
		   if (regAddress != null) {
			   // if a relative address is used, compute by adding to parent base
			   RegNumber newBaseAddress = new RegNumber(regAddress);
			   newBaseAddress.add(getRegSetParentAddress());
			   /*if (regProperties.getInstancePath().startsWith("msg")) {  
				   System.out.println("OutputBuilder updateRegBaseAddress:   path=" + regProperties.getInstancePath() + 
					   	      ", regAddress=" + regAddress  + ", parent addr=" + getRegSetParentAddress() + ", nextAddress=" + nextAddress + ", newBaseAddress=" + newBaseAddress);
                   regProperties.getExtractInstance().display(true);
			   }*/
			   
			   if (regProperties.isFirstRep()) {
				   if (newBaseAddress.isLessThan(nextAddress)) {  // check for bad address here
					   MsgUtils.errorMessage("out of order register address specified in " + regProperties.getInstancePath());
					   //System.out.println("OutputBuilder updateRegBaseAddress:   path=" + regProperties.getInstancePath() + 
					   //	      ", regAddress=" + regAddress  + ", parent addr=" + getRegSetParentAddress() + ", nextAddress=" + nextAddress + ", newBaseAddress=" + newBaseAddress);
					   //System.exit(0);
				   }
				   setNextAddress(newBaseAddress);  // explicit start address for this group of regs, so save it   
			   }
		   }
		   // else adjust base if address shift or modulus is specified
		   else if (regProperties.isFirstRep()) {
			   if (addressShift != null) updateNextAddress(addressShift);
			   else updateNextAddressModulus(addressModulus);  // adjust the address if a modulus is defined
		   }
		   
		   // verify that address is aligned correctly   
		   int regBytes = regProperties.getRegByteWidth();
		   int alignBytes = !Utils.isPowerOf2(regProperties.getRegWidth()) ? (Utils.getNextHighestPowerOf2(regProperties.getRegWidth())/8) : regBytes; 
		   // if an external reg then need to align based on total size
		   boolean isFirstReplicatedExternal = regProperties.isRootExternal() && regProperties.isReplicated() && regProperties.isFirstRep(); // TODO - child map root external reg arrays are not realigned - issue warning for now 
           int arrayAlignBytes = alignBytes * Utils.getNextHighestPowerOf2(regProperties.getRepCount());
		   if (isFirstReplicatedExternal) alignBytes = arrayAlignBytes;
		   // inhibit address align shift if in an external region and jsAlign is disabled
		   boolean doNotShift = regProperties.isExternal() && !regProperties.isRootExternal() && !ExtParameters.useJsAddressAlignment();

		   //System.out.println("OutputBuilder updateRegBaseAddress: wide reg=" + this.getInstancePath() + " with width=" + regBytes + " at addr=" + nextAddress + ", ext=" + regProperties.isExternal()+ ", rep=" + regProperties.getRepCount());
		   // if this reg isn't in an external decoder and misaligned, then align it
		   if (!nextAddress.isModulus(alignBytes) && !regProperties.isExternalDecode()) {
			   if (!ExtParameters.suppressAlignmentWarnings()) {
				   if (isFirstReplicatedExternal) 
					   MsgUtils.warnMessage("base address for external " + regBytes + "B register group " + regProperties.getInstancePath() + " shifted to be " + alignBytes + "B aligned.");
				   else if (doNotShift)   
					   MsgUtils.warnMessage("base address for " + regBytes + "B external register " + regProperties.getInstancePath() + " is not " + alignBytes + "B aligned.");
				   else 
					   MsgUtils.warnMessage("base address for " + regBytes + "B register " + regProperties.getInstancePath() + " shifted to be " + alignBytes + "B aligned.");
			   }
			   // if an external and not root external, just display a warn message
			   if (!doNotShift) updateNextAddressModulus(new RegNumber(alignBytes));  // adjust the address to align register					   
		   }
		   
		   // issue warning for non aligned reg arrays
		   boolean doReplicatedAlignCheck = ExtParameters.useJsAddressAlignment() && regProperties.isReplicated() && regProperties.isFirstRep() && !regProperties.isRootExternal(); 
		   if (doReplicatedAlignCheck && !ExtParameters.suppressAlignmentWarnings() && !nextAddress.isModulus(arrayAlignBytes) && !regProperties.isExternalDecode()) {
		      MsgUtils.warnMessage("base address for " + regProperties.getRepCount() + " x " + regBytes + "B register array " + regProperties.getInstancePath() + " is not " + arrayAlignBytes + "B aligned.");
		   }

		   // ----
		   regProperties.setBaseAddress(nextAddress);  // save the address of this reg
		   // compute the relative base address (vs parent regset base address)
		   RegNumber relAddress = new RegNumber(regProperties.getBaseAddress());  // start with current
		   relAddress.subtract(getRegSetParentAddress());  // subtract parent base from current
		   regProperties.setRelativeBaseAddress(relAddress);  // store in reg properties		   
	}

	/** get the base address of the top regset on the stack */
	private RegNumber getRegSetParentAddress() {
		RegNumber retVal = new RegNumber(0);  // default to 0
		if (!regSetPropertyStack.isEmpty()) {
			if (regSetPropertyStack.peek().getBaseAddress().isDefined())
				retVal = regSetPropertyStack.peek().getBaseAddress();
		}
		return retVal;
	}

	// ----------- public methods
	
	/** get name of root map in this builder
	 *  @return the addressMapName
	 */
	public  String getAddressMapName() {
		return addressMapName;
	}

	/** set name of root map in this builder
	 *  @param addressMapName the addressMapName to set
	 */
	public  void setAddressMapName(String moduleName) {
		this.addressMapName = moduleName;
	}

	/** get firstAddressMap - indication of first addrmap visited in this builder
	 *  @return the firstAddressMap
	 */
	public boolean isFirstAddressMap() {
		return firstAddressMap;
	}

	/** set firstAddressMap
	 *  @param firstAddressMap the firstAddressMap to set
	 */
	public void setFirstAddressMap(boolean firstAddressMap) {
		this.firstAddressMap = firstAddressMap;
	}

	// ----------------- instance stack methods
	
	/** push an instance onto instanceStack
	 */
	public  void pushInstance(InstanceProperties inst) {
		/*boolean l3_bregs = false;
		int targetBuilder = 2;
		if (getBuilderID() == targetBuilder) {  // 0=base 1=l3child 2=l2_r16_child
			if (inst.isExternal()) 
				System.out.println("OutputBuilder " + getBuilderID() + ": pushInstance, external inst " + inst.getId() + " found, stack depth=" + instancePropertyStack.size());  
			else 
				System.out.println("OutputBuilder " + getBuilderID() + ": pushInstance, internal inst " + inst.getId() + " found, stack depth=" + instancePropertyStack.size());
			l3_bregs = "base_regs".equals(inst.getId()) && (!inst.isExternal());
		}*/

		// if stack isn't empty get parent instance default properties
		if (!instancePropertyStack.isEmpty()) inst.updateDefaultProperties(instancePropertyStack.peek().getInstDefaultProperties());
		// push this instance onto the stack
		instancePropertyStack.push(inst);
	}
	
	/** pop an instance from instanceStack
	 */
	public  InstanceProperties popInstance() {
		InstanceProperties inst = instancePropertyStack.pop();
		//System.out.println("popped inst=" + inst + " from stack");
		return inst;
	}

	/** peek top instance from instanceStack
	 */
	public  InstanceProperties peekInstance() {
		if ((instancePropertyStack == null) || instancePropertyStack.isEmpty()) return null;
		InstanceProperties inst = instancePropertyStack.peek();
		//System.out.println("peeked inst=" + inst + " from stack");
		return inst;
	}
	
	/** generate current instance path string
	 */
	public  String getInstancePath() {
		String retStr = "";
		for (InstanceProperties inst: instancePropertyStack) {
			if (inst != null) retStr += "." + inst.getId(); 
		}
		if (retStr.length()<2) return "";
		else return retStr.substring(1);
	}
		
	/** generate current instance path string with indexed rep suffixes
	 */
	protected String getIndexedInstancePath() {
		String retStr = "";
		for (InstanceProperties inst: instancePropertyStack) {
			if (inst != null) retStr += "." + inst.getIndexedId(); 
		}
		if (retStr.length()<2) return "";
		else return retStr.substring(1);
	}
	
	/** generate instance path string for parent of stack top
	 */
	protected String getParentInstancePath() {
		String retStr = "";
		Iterator<InstanceProperties> iter = instancePropertyStack.iterator();
		while (iter.hasNext()) {
			InstanceProperties inst = iter.next();
			if ((inst !=null) && (iter.hasNext())) retStr += "." + inst.getId();  // skip top elem
		}
		if (retStr.length()<2) return "";
		else return retStr.substring(1);
	}

	/** get baseName with indexed reps */
	protected String getIndexedBaseName() {
		return getIndexedInstancePath().replace('.', '_');
	}

	// ----------------- regset stack methods
	
	/** set external state of a newly created AddressableInstance prior to builder push/add
	 * @param inst - instance that will be modified based on properties and parent instance state
	 * @param isAddressMap - true if component being instanced is an addrmap
	 */
	public  void setExternalInstanceProperties(AddressableInstanceProperties inst, boolean isAddressMap) {

		// -- set external type and local external indicator as appropriate (AddressableInstance constructor already extracts extractInstance value) 
		
		// if model inst already has an external property assign, then use it
		if (inst.isExternal()) {
			//System.out.println("OutputBuilder " + getBuilderID() + ": setExternalInstanceProperties, already set external type for inst=" + inst.getId() + " to " + inst.getExternalType());
			inst.setLocalExternal(!isAddressMap);  // also set local extern if not an addrmap
		}
		// else if instance has a default external property assigned then use it (eg, js will set external default for large replicated typedefs)
		else if (inst.hasDefaultProperty("external")) {
			inst.setExternalTypeFromString(inst.getDefaultProperty("external"));
			inst.setLocalExternal(!isAddressMap);  // also set local extern if not an addrmap
			//System.out.println("OutputBuilder " + getBuilderID() + ": setExternalInstanceProperties, setting external type for inst=" + inst.getId() + " via default to " + inst.getExternalType());
		}
		// else if parent is external, this instance is external 
		else if (!regSetPropertyStack.isEmpty()) {
			// cascade external type to child
			if (regSetPropertyStack.peek().isExternal())
				inst.setExternalType(regSetPropertyStack.peek().getExternalType());
			// cascade local external state to child
			if (regSetPropertyStack.peek().isLocalExternal())
				inst.setLocalExternal(!isAddressMap);
		}
		
		// treat addr maps w/ no explicit external type assigned as a default external reg set
		if (isAddressMap && (!inst.isExternal())) {
				inst.setExternalTypeFromString("DEFAULT");  // address map is treated as external reg set	
		}
		
		//if (inst.isExternal() && !inst.isLocalExternal())
		//	MsgUtils.errorMessage("OutputBuilder " + getBuilderID() + ": setExternalInstanceProperties, found local internal type for inst=" + inst.getId() + " : " + inst.getExternalType());
		
		// -- now determine if instance is root external
		
		// if added instance is external and parent isnt then mark as root
		if ((regSetPropertyStack.isEmpty() || !regSetPropertyStack.peek().isExternal()) && inst.isExternal()) {
			inst.setRootExternal(true);
			//System.out.println("OutputBuilder " + getBuilderID() + ": setExternalInstanceProperties, setting external root for inst=" + inst.getId());
		}
		
		// if added instance is local external and parent isnt then mark as root
		if ((regSetPropertyStack.isEmpty() || !regSetPropertyStack.peek().isLocalExternal()) && inst.isLocalExternal()) {
			inst.setLocalRootExternal(true);
			//System.out.println("OutputBuilder " + getBuilderID() + ": setExternalInstanceProperties, setting local external root for inst=" + inst.getId());
		}	
		
		//if (!inst.isRootExternal() && inst.isLocalRootExternal())
		//	MsgUtils.errorMessage("OutputBuilder " + getBuilderID() + ": setExternalInstanceProperties, found local root external type for inst=" + inst.getId() + " : " + inst.getExternalType());
	}
	
	/** return number of addressmaps in current hierarchy
	 */
	protected int currentMapLevel() {
		int mlevel = 0;
		Iterator<RegSetProperties> iter = regSetPropertyStack.iterator();
		while (iter.hasNext()) {
			RegSetProperties inst = iter.next();
			if (inst.isAddressMap()) {
				mlevel++;
				//System.out.println("OutputBuilder currentMapLevel: level=" + mlevel + ", inst=" + inst.getInstancePath());
			}
		}
		return (builderID == 0)? mlevel + 1: mlevel;  // root isn't included in stack, so add 1
	}
	
	/** return true if all reg sets on stack are first rep
	 */
	protected boolean firstRegSetRep() {
		Iterator<RegSetProperties> iter = regSetPropertyStack.iterator();
		while (iter.hasNext()) {
			RegSetProperties inst = iter.next();
			if (!inst.isFirstRep()) return false;
		}
		return true;
	}
	
	/** return RegSetProperties instance of current active address map from the regset stack
	 */
	protected RegSetProperties getParentAddressMap() {
		Iterator<RegSetProperties> iter = regSetPropertyStack.iterator();
		while (iter.hasNext()) {
			RegSetProperties inst = iter.next();
			if (inst.isAddressMap()) return inst;
		}
		return null;
	}
	
	/** return nth RegSetProperties ancestor from the regset stack
	 * 
	 * @param ancLevel - ancestor level (1=top of stack, 2=1's parent, etc)
	 */
	protected RegSetProperties getRegSetAncestor(int ancLevel) {
		int stackSize = regSetPropertyStack.size();
		if (stackSize<ancLevel) return null;  // no parent for this level
		return regSetPropertyStack.get(stackSize-ancLevel);
	}
	
	/** return parent of active RegSetProperties from the regset stack */
	protected RegSetProperties getRegSetParent() {
		return getRegSetAncestor(2);
	}
	
	/** return name of current active address map from the regset stack
	 */
	protected String getParentAddressMapName() {
		RegSetProperties addrMapInst = getParentAddressMap();  // get first addrmap on the stack
		if (addrMapInst == null) return getAddressMapName();  // return the root name
		return getAddressMapName() + "_" + addrMapInst.getBaseName();  // return the catenated name
	}

	// ----------------- fieldset stack methods

	/** get the accumulated fieldset offset by adding offsets properties found on the stack 
	 * this is used to update the offset used in regProperties when fields are added */
	private int getCurrentFieldSetOffset() {
		Integer offset = 0;
		//System.out.println("-- OutputBuilder getCurrentFieldSetOffset: getting fs offset");
		Iterator<FieldSetProperties> iter = fieldSetPropertyStack.iterator();
		while (iter.hasNext()) {
			FieldSetProperties inst = iter.next();
			offset += inst.getOffset(); 
		}
		return offset;
	}

	/** get the fieldset prefix to be appended to field names in some outputs (rdl eg) */
	private String getFieldSetPrefix() {
		String prefix = "";
		Iterator<FieldSetProperties> iter = fieldSetPropertyStack.iterator();
		while (iter.hasNext()) {
			InstanceProperties inst = iter.next();
			prefix += inst.getId() + "_";
		}
		return prefix;
	}
	
	// ------------
	
	/** get signalProperties
	 *  @return the signalProperties
	 */
	public  SignalProperties getSignalProperties() {
		return signalProperties;
	}

	/** get fieldProperties
	 *  @return the fieldProperties
	 */
	public  FieldProperties getFieldProperties() {
		return fieldProperties;
	}

	/** get regProperties
	 *  @return the regProperties
	 */
	public  RegProperties getRegProperties() {
		return regProperties;
	}

	/** get regSetProperties
	 *  @return the regSetProperties
	 */
	public  RegSetProperties getRegSetProperties() {
		return regSetProperties;
	}

	// -------------------------------- address mod methods ----------------------------------
	
	/** get visitEachReg
	 *  @return the visitEachReg
	 */
	public boolean visitEachReg() {
		return visitEachReg;
	}

	/** set visitEachReg
	 *  @param visitEachReg the visitEachReg to set
	 */
	public void setVisitEachReg(boolean visitEachReg) {
		this.visitEachReg = visitEachReg;
	}

	/** get visitEachRegSet
	 *  @return the visitEachRegSet
	 */
	public boolean visitEachRegSet() {
		return visitEachRegSet;
	}

	/** set visitEachRegSet
	 *  @param visitEachRegSet the visitEachRegSet to set
	 */
	public void setVisitEachRegSet(boolean visitEachRegSet) {
		this.visitEachRegSet = visitEachRegSet;
	}

	/** get visitExternalRegisters
	 *  @return the visitExternalRegisters
	 */
	public boolean visitExternalRegisters() {
		return visitExternalRegisters;
	}

	/** set visitExternalRegisters
	 *  @param visitExternalRegisters the visitExternalRegisters to set
	 */
	public void setVisitExternalRegisters(boolean visitExternalRegisters) {
		this.visitExternalRegisters = visitExternalRegisters;
	}

	/** get visitEachExternalRegister
	 *  @return the visitEachExternalRegister
	 */
	public boolean visitEachExternalRegister() {
		return visitEachExternalRegister;
	}

	/** set visitEachExternalRegister
	 *  @param visitEachExternalRegister the visitEachExternalRegister to set
	 */
	public void setVisitEachExternalRegister(boolean visitEachExternalRegister) {
		this.visitEachExternalRegister = visitEachExternalRegister;
	}

	public boolean supportsOverlays() {
		return supportsOverlays;
	}

	public void setSupportsOverlays(boolean supportsOverlays) {
		this.supportsOverlays = supportsOverlays;
	}

	/** get next address  */
	protected  RegNumber getNextAddress() {
		return nextAddress;
	}
	
	/** set next address value */
	protected  void setNextAddress(RegNumber regAddress) {
		nextAddress.setValue(regAddress.getValue());  // update value but keep format
	}

	/** get base address of root in this builder  */
	protected  RegNumber getBaseAddress() {
		return baseAddress;
	}
	
	/** set base address of root in this builder */
	protected  void setBaseAddress(RegNumber regAddress) {
		baseAddress.setValue(regAddress.getValue());  // update value but keep format
	}
	
	/** get externalBaseAddress
	 *  @return the externalBaseAddress
	 */
	public RegNumber getExternalBaseAddress() {
		return externalBaseAddress;
	}

	/** set externalBaseAddress
	 *  @param externalBaseAddress the externalBaseAddress to set
	 */
	public void setExternalBaseAddress(RegNumber externalBaseAddress) {
		//System.out.println("Setting ext base address to " + externalBaseAddress);
		this.externalBaseAddress = new RegNumber(externalBaseAddress);
	}
		
	/** get MinRegWidth in bytes */
	public  int getMinRegByteWidth() {
		return ExtParameters.getMinDataSize()/8;
	}
	
	/** get maxRegWidth
	 *  @return the maxRegWidth
	 */
	public int getMaxRegWidth() {
		return maxRegWidth;
	}
	
	/** get maxRegWidth in bytes
	 */
	public int getMaxRegByteWidth() {
		return getMaxRegWidth() / 8;
	}
	
	/** get maxRegWidth in words */
	public int getMaxRegWordWidth() {
		return getMaxRegWidth() / ExtParameters.getMinDataSize();
	}

	/** set maxRegWidth
	 *  @param maxRegWidth the maxRegWidth to set
	 */
	private void setMaxRegWidth(int maxRegWidth) {
		this.maxRegWidth = maxRegWidth;
	}
	
	/** update maxRegWidth if new value is higher
	 *  @param maxRegWidth the maxRegWidth to set
	 */
	private void updateMaxRegWidth(int maxRegWidth) {
		if (maxRegWidth > getMaxRegWidth()) setMaxRegWidth(maxRegWidth);  // update max value for this addrmap
	}

	/** get high index of pio address based on max address in map 
	 *  @return the getAddressHighBit
	 */
	public  int getAddressHighBit(RegNumber regNum) {   
		return regNum.getMinusOneHighestBit();
	}
	
	/** get low index of pio address based on register size 
	 *  @return the getAddressLowBit
	 */
	public  int getAddressLowBit() {   
		return new RegNumber(getMinRegByteWidth()).getHighestBit();
	}
	
	/** get bit width of pio address  
	 *  @return the getAddressHighBit
	 */
	public  int getAddressWidth(RegNumber regNum) {   
		return getAddressHighBit(regNum) - getAddressLowBit() + 1;
	}
	
	/** generate a (little endian) array reference string given a starting bit and size */
	public static String genRefArrayString(int lowIndex, int size) {
		if (size < 1) return " ";
		if (size == 1) return " [" + lowIndex + "] ";
	   	return " [" + (size + lowIndex - 1) + ":" + lowIndex + "] ";
	}

	/** return the current accumulated size of the register map in bytes  **/
	public RegNumber getCurrentMapSize() {
		RegNumber mapSize = new RegNumber(getNextAddress());
		mapSize.subtract(getBaseAddress());
		//System.out.println("OutputBuilder getCurrentMapSize: " + builderID + ", size=" + mapSize + " next=" + getNextAddress()+ " base=" + getBaseAddress());
		return mapSize;
	}

	// ------------- protected regset size accumulation methods

	/** get the increment stride for this regset - must be run in finishRegSet after sub components for non-aligned case 
	 */
	protected RegNumber getRegSetAddressStride() {
		// if address increment is specified, use it
		RegNumber stride = regSetProperties.getExtractInstance().getAddressIncrement();
		if (stride != null) {
			stride.setNumFormat(NumFormat.Address);
			stride.setNumBase(NumBase.Hex);
			// error if increment specified is too small
			if (regSetProperties.isReplicated() && stride.isLessThan(regSetProperties.getAlignedSize()))
				MsgUtils.errorMessage("register set " + regSetProperties.getInstancePath() + 
						" address increment (" + stride +") is smaller than its min size (" + regSetProperties.getAlignedSize().toFormat(NumBase.Hex, NumFormat.Address) +")"); 
		}
		// otherwise use computed regset size
		else if ((regSetProperties.isReplicated() && ExtParameters.useJsAddressAlignment())
			|| (regSetProperties.isLocalRootExternal() && regSetProperties.isLastRep())) {
			return regSetProperties.getAlignedSize();
		}
	    // otherwise use computed regset size
		else {
			stride = new RegNumber(getNextAddress());
			stride.subtract(regSetProperties.getBaseAddress());
			//System.out.println("OutputBuilder getRegSetSize: " + regSetProperties.getInstancePath() + 
			//				" computed size = " + incr);
			stride.setNumFormat(NumFormat.Address);
			stride.setNumBase(NumBase.Hex);
		}
		/*
		RegNumber calcStride = new RegNumber(getNextAddress());
		calcStride.subtract(regSetProperties.getBaseAddress());
		calcStride.setNumFormat(NumFormat.Address);
		calcStride.setNumBase(NumBase.Hex);
		System.out.println("OutputBuilder getRegSetAddressStride: " + regSetProperties.getInstancePath() + 
				" base = " + regSetProperties.getBaseAddress() + ", computed size = " + calcStride + ", aligned size = " + regSetProperties.getAlignedSize() + ", returned size = " + stride);
		*/
		return stride;
	}

	// ------------------------ next address calc methods -----------------
	
	/** set next address based on size of current register */
	private  void updateNextAddress() {
		int regBytes = regProperties.getRegByteWidth();
		int incBytes = !Utils.isPowerOf2(regProperties.getRegWidth()) ? (Utils.getNextHighestPowerOf2(regProperties.getRegWidth())/8) : regBytes; 
		//RegNumber inc = new RegNumber(regProperties.getRegByteWidth()); //register bytes
		//System.out.println("updating address, inc=" + incBytes);
		nextAddress.add(incBytes);
	}

	/** bump the next address by specified number of register width words */
	private  void updateNextAddress(int reps) {
		int regBytes = regProperties.getRegByteWidth();
		int incBytes = !Utils.isPowerOf2(regProperties.getRegWidth()) ? (Utils.getNextHighestPowerOf2(regProperties.getRegWidth())/8) : regBytes; 
		RegNumber inc = new RegNumber(reps * incBytes); //register bytes
		//System.out.println("updating address, inc=" + inc);
		nextAddress.add(inc);		
	}

	/**  bump the running address count by an increment value **/
	private void updateNextAddress(RegNumber addressIncrement) {
		// if no increment specified increase by reg width
		if (addressIncrement == null) updateNextAddress();  
		else {
			nextAddress.add(addressIncrement);
		}
	}

	/**  bump the running address count by an increment value for multiple registers **/
	private void updateNextAddress(RegNumber addressIncrement, int repCount) {
		// if no increment specified increase by reg width
		if (addressIncrement == null) updateNextAddress(repCount);  
		else {
			RegNumber totalIncrement = new RegNumber(addressIncrement);
			totalIncrement.multiply(repCount);
			nextAddress.add(totalIncrement);
		}
	}
	
	/** adjust the next address if a modulus is defined **/
	private void updateNextAddressModulus(RegNumber addressModulus) {
		if (addressModulus == null) return;
		nextAddress.roundUpToModulus(addressModulus);
	}

	//---------------------------- message/output stmt generation  ----------------------------------------

	/** write a stmt to the specified BufferedWriter */
	public  void writeStmt(BufferedWriter bw, int indentLevel, String stmt) {
		   //System.out.println("OutputBuilder: bufnull=" + (bufferedWriter == null) + ", indent=" + ",Stmt=" + stmt);
		   try {
			bw.write(MsgUtils.repeat(' ', indentLevel*2) + stmt +"\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/** write multiple stmts to the specified BufferedWriter */
	public void writeStmts(BufferedWriter bw, int indentLevel, List<String> outputLines) {
		Iterator<String> iter = outputLines.iterator();
		while (iter.hasNext()) writeStmt(bw, indentLevel, iter.next());	
	}

	/** write an OutputLine to the specified BufferedWriter */
	public  void writeStmt(BufferedWriter bw, OutputLine outputLine) {
		writeStmt(bw, outputLine.getIndent(), outputLine.getLine());  
	}
	
	/** write an OutputLine to the default BufferedWriter */
	public  void writeStmt(OutputLine outputLine) {
		writeStmt(bufferedWriter, outputLine);
	}

	/** write a list of OutputLines to the specified BufferedWriter */
	protected void writeStmts(BufferedWriter bw, List<OutputLine> outputList) {
		for (OutputLine rLine: outputList) {
			writeStmt(bw, rLine.getIndent(), rLine.getLine());  
		}
	}

	/** write a list of OutputLines to the default BufferedWriter */
	protected void writeStmts(List<OutputLine> outputList) {
		for (OutputLine rLine: outputList) {
			writeStmt(bufferedWriter, rLine.getIndent(), rLine.getLine());  
		}
	}
	
	// -------- OutputWriterIntf methods

	@Override
	public String getWriterName() {
		return getAddressMapName();  // use address map as name of this writer
	}
	
	/** write a stmt to the default BufferedWriter */
	@Override
	public  void writeStmt(int indentLevel, String stmt) {
		writeStmt(bufferedWriter, indentLevel, stmt);
	}
	
	/** write a multiple stmts to the default BufferedWriter */
	@Override
	public  void writeStmts(int indentLevel, List<String> outputLines) {
		writeStmts(bufferedWriter, indentLevel, outputLines);
	}

	/** return true if open was successful */
	@Override
	public boolean isOpen() {
		return (bufferedWriter != null);
	}

	/** close this writer */
	@Override
	public void close() {
		OutputBuilder.closeBufferedWriter(bufferedWriter);
	}

	//---------------------------- methods to write output ----------------------------------------

	/** write output to an already open bufferedWriter 
	 * @param bw */
	abstract protected void write(BufferedWriter bw);
	
	/** write output to specified output file - this is called by ordt main and can be
	 *  overridden by child builders if multiple file outputs are needed.
	 *  sets the default bufferdWriter for this OutputBuilder if file is opened successfully. 
	 * @param outName - output file or directory
	 * @param description - text description of file generated
	 * @param commentPrefix - comment chars for this file type */
	public void write(String outName, String description, String commentPrefix) {
    	BufferedWriter bw = openBufferedWriter(outName, description);
    	if (bw != null) {
    		// set bw as default
    		bufferedWriter = bw;

    		// write the file header
    		writeHeader(commentPrefix);
    		
    		// now write the output
	    	write(bw);
    		closeBufferedWriter(bw);
    	}
	}
	
	/** write specified list of output statements to specified output file. File is opened/created on entry and closed on return.
	 *  The default bufferedWriter for this OutputBuilder is not affected. 
	 * @param outName - output file or directory
	 * @param description - text description of file generated
	 * @param commentPrefix - comment chars for this file type 
	 * @param simple list of lines to be written */
	public void writeStatementsToFile(String outName, String description, String commentPrefix, List<String> stmts) {
    	BufferedWriter bw = openBufferedWriter(outName, description);
    	if (bw != null) {
    		// write the file header
    		writeHeader(bw, commentPrefix); 		
    		// now write the output
    		for (String stmt:stmts) writeStmt(bw, 0, stmt);	
    		// close buffer
    		closeBufferedWriter(bw);
    	}
	}
	
	/** write a file header to specified BufferedWriter
	 * @param commentPrefix
	 */
	protected void writeHeader(BufferedWriter bw, String commentPrefix) {
		if (commentPrefix == null) return;  // no header if commentPrefix is null (eg json)
		boolean isXml = commentPrefix.equals("<!--");
		String midCommentChar = (commentPrefix.equals("/*")) ? " *" : 
			                    isXml ? "    " : commentPrefix;
		String lastCommentChar = (commentPrefix.equals("/*")) ? " */" : 
			                     isXml ? " -->" : commentPrefix;
		if (isXml) writeStmt(bw, 0, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"); // prefix line for xml
		writeStmt(bw, 0, commentPrefix + "   Ordt " + Ordt.getVersion() + " autogenerated file ");
		writeStmt(bw, 0, midCommentChar + "   Input: " + model.getOrdtInputFile());
		for (String parmFile: ExtParameters.getParmFiles()) {
			writeStmt(bw, 0, midCommentChar + "   Parms: " + parmFile);
		}
		writeStmt(bw, 0, midCommentChar + "   Date: " + new Date());
		writeStmt(bw, 0, lastCommentChar);
		writeStmt(bw, 0, "");
	}
	
	/** write a file header to teh default BufferedWriter
	 * @param commentPrefix
	 */
	protected void writeHeader(String commentPrefix) {
		writeHeader(bufferedWriter, commentPrefix);
	}
	
	/** set buffered writer directly
     */
    public void setBufferedWriter(BufferedWriter bw) {
    	bufferedWriter = bw;
    }

	/** validate output file and create buffered writer
     */
    public static BufferedWriter openBufferedWriter(String outName, String description) {
    	File outFile = null;
    	try {	  			
    		outFile = new File(outName); 

    		System.out.println(MsgUtils.getProgName() + ": writing " + description + " file " + outFile + "...");

    		// if file doesnt exists, then create it
    		if (!outFile.exists()) {
    			outFile.createNewFile();
    		}

    		FileWriter fw = new FileWriter(outFile.getAbsoluteFile());
    		BufferedWriter bw = new BufferedWriter(fw);
    		return bw;

    	} catch (IOException e) {
    		MsgUtils.errorMessage("Create of " + description + " file " + outFile.getAbsoluteFile() + " failed.");
    		return null;
    	}
    }
    
    /** validate output file and create buffered writer
     */
    public static void closeBufferedWriter(BufferedWriter bw) {
    	try {	  
    		bw.close();

    	} catch (IOException e) {
    		MsgUtils.errorMessage("File close failed.");
    	}
    }

		
}