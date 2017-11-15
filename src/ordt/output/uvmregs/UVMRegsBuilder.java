/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved. 
 */
package ordt.output.uvmregs;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import ordt.extract.Ordt;
import ordt.extract.PropertyList;
import ordt.extract.RegModelIntf;
import ordt.extract.RegNumber;
import ordt.extract.RegNumber.NumBase;
import ordt.extract.RegNumber.NumFormat;
import ordt.output.FieldProperties;
import ordt.output.InstanceProperties;
import ordt.output.OutputBuilder;
import ordt.output.OutputLine;
import ordt.output.RegProperties;
import ordt.output.RegSetProperties;
import ordt.output.RhsReference;
import ordt.output.FieldProperties.RhsRefType;
import ordt.output.systemverilog.common.SystemVerilogFunction;
import ordt.parameters.ExtParameters;

public class UVMRegsBuilder extends OutputBuilder {

	protected List<OutputLine> outputList = new ArrayList<OutputLine>();
	protected List<OutputLine> pkgOutputList = new ArrayList<OutputLine>();  // define a separate list for package info
	
	protected subComponentLists subcompDefList = new subComponentLists();   // lists of subcomponent define statements (per block)
	protected subComponentLists subcompBuildList = new subComponentLists();  // lists of subcomponent build statements (per block)
	protected subComponentLists subcompAddrCoverGroupList = new subComponentLists();  // lists of subcomponent address covergroup statements (per block)
	protected subComponentLists regCbsDefineStatements = new subComponentLists();  // list of register callback defines (per block)
	protected subComponentLists regCbsAssignStatements = new subComponentLists();  // list of register callback assign statements (per block)
	protected List<SystemVerilogFunction> externMethods = new ArrayList<SystemVerilogFunction>();  // list of functions to be defined outside class scope
	
	protected int indentLvl = 0;
	
	protected static HashSet<String> reservedWords = getReservedWords();
	
	protected AliasGroups aliasGroups = new AliasGroups();  // list of alias groups (per block class)
	
	protected Stack<Boolean> regSetHasCallback = new Stack<Boolean>();  // stack of flags indicating block needs add_callback overridden
	protected Stack<Integer> activeRegisterCount = new Stack<Integer>(); // stack of non-pruned register counts in active regset
	
	// search state for use in field callbacks
	protected static int lastCBDepth = -1;
	protected static int lastCBCount = 0;
	
	// unique uvm reg and block class
	protected HashMap<RegProperties, String> uniqueRegClasses = new HashMap<RegProperties, String>(); // hashmap since even equal RegProperties can have dif't instance paths 
	protected HashMap<RegSetProperties, String> uniqueBlockClasses = new HashMap<RegSetProperties, String>();  // hashmap since even equal RegSetProperties can have dif't instance paths
	//protected int regClassCount = 0;
	//protected int blockClassCount = 0;

    //---------------------------- constructor ----------------------------------

    public UVMRegsBuilder(RegModelIntf model) {
	    this.model = model;  // store the model ref
	    setVisitEachReg(false);   // only need to call once for replicated reg groups
	    setVisitEachRegSet(false);   // only need to call once for replicated reg set groups
	    setVisitExternalRegisters(true);  // we will visit externals 
	    setVisitEachExternalRegister(false);	    // handle externals as a group
	    model.getRoot().generateOutput(null, this);   // generate output structures recursively starting at model root
    }

    /** load systemverilog reserved words to be escaped */
	protected static HashSet<String> getReservedWords() {
		HashSet<String> reservedWords = new HashSet<String>();
		reservedWords.add("bit");
		reservedWords.add("begin");
		reservedWords.add("byte");
		reservedWords.add("cell");
		reservedWords.add("checker");
		reservedWords.add("config");
		reservedWords.add("context");
		reservedWords.add("disable");
		reservedWords.add("end");
		reservedWords.add("event");
		reservedWords.add("local");
		reservedWords.add("packed");
		reservedWords.add("priority");
		reservedWords.add("ref");
		reservedWords.add("reg");
		reservedWords.add("repeat");
		reservedWords.add("table");
		reservedWords.add("time");
		reservedWords.add("type");
		return reservedWords;
	}
	
    /** escape string if a reserved words  */
	protected String escapeReservedString(String word) {
		if (reservedWords.contains(word)) return ("\\" + word + " ");
		return word;
	}

    //---------------------------- OutputBuilder methods to load structures ----------------------------------------

	@Override
	public void addField() {
		//System.out.println("UVMRegsBuilder: addField id=" + fieldProperties.getPrefixedId() + ", reset=" + fieldProperties.getReset());
	}

	@Override
	public void addAliasField() {    
		// handle same as non-aliased field
		addField();
	}

	@Override
	public void addRegister() {
		//if ("sopcf1_ctl".equals(regProperties.getId())) 
		//	System.out.println("UVMRegsBuilder addRegister: " + regProperties.getInstancePath() + ", base=" + regProperties.getBaseAddress());
	}

	@Override
	public void finishRegister() {
		//if ("sopcf1_ctl".equals(regProperties.getId())) 
		//	System.out.println("UVMRegsBuilder finishRegister: " + regProperties.getInstancePath() + ", base=" + regProperties.getBaseAddress());
		
		// only add the register if it is sw accessible and not explicitly pruned from model
		if ((regProperties.isSwWriteable() || regProperties.isSwReadable()) && !regProperties.uvmRegPrune()) {
			//if (regProperties.isReplicated()) System.out.println("UVMRegsBuilder finishRegister: replicated reg id=" + regProperties.getId() + ", reps=" + regProperties.getRepCount() + ", thold=" + ExtParameters.uvmregsIsMemThreshold());
			// if a memory, then add info to parent uvm_reg_block
			if (regProperties.isMem() || (regProperties.getRepCount() >= ExtParameters.uvmregsIsMemThreshold())) {   // check is_mem threshold vs reps
				//System.out.println("UVMRegsBuilder finishRegister: replicated MEM reg id=" + regProperties.getId() + ", reps=" + regProperties.getRepCount() + ", thold=" + ExtParameters.uvmregsIsMemThreshold());
                // if debug mode 3 then no wrapper created (old behavior - bad address stride generated in uvm1.1d)
				if (ExtParameters.hasDebugMode("uvmregs_no_mem_wrap")) {
					// save info for this memory and virtual regs to be used in parent uvm_reg_block
					saveMemInfo(false);  // no wrapper used
					// build the virtual register class definition
					buildVRegClass();  									
                }
				// otherwise default behavior - create a wrapper block with same create_map width as mem and encapsulate mem/vreg
				else {
					// save wrapper block in parent
					String newBlockName = regProperties.getId();  // use reg name as wrapper block name
					String uvmBlockClassName = getUVMBlockID(newBlockName);
					saveRegSetInfo(uvmBlockClassName, newBlockName, regProperties.getRelativeBaseAddress()); 
					// save info for this memory and virtual regs in the wrapper
					saveMemInfo(true);  // save info in wrapper block
					// build the virtual register class definition
					buildVRegClass();  									
                	// build the wrapper using reg byte width in map and reg id as suffix
					buildMemWrapperBlockClass(uvmBlockClassName, newBlockName, regProperties.getRegByteWidth());
					//System.out.println("UVMRegsBuilder finishRegister wrapper: " + regProperties.getInstancePath() + ", newBlockName=" + newBlockName);
				}
			}
			// otherwise model as a register
			else {
				String uvmRegClassName = getUVMRegID();  // generate class name based on current register
				// if class reuse is specified, store new class names and reuse repeats
				boolean createNewRegClass = true;
				if (ExtParameters.uvmregsReuseUvmClasses()) {
					/*System.out.println("UVMRegsBuilder finishRegister: reg " + regProperties.getInstancePath() + " with class name=" + uvmRegClassName + ", hash=" + regProperties.hashCode() + ", containsKey=" + uniqueRegClasses.containsKey(regProperties));
					for(RegProperties reg: uniqueRegClasses.keySet()) {
						System.out.println("    unique reg " + reg.getInstancePath()  + ", hash=" + reg.hashCode()  + ", equals=" + regProperties.equals(reg));
					}*/
					if (!uniqueRegClasses.containsKey(regProperties)) uniqueRegClasses.put(regProperties, uvmRegClassName);
					else {
						//System.out.println("UVMRegsBuilder finishRegister: reg=" + uvmRegClassName + " is a copy of reg=" + uniqueRegClasses.get(regProperties));
						uvmRegClassName = uniqueRegClasses.get(regProperties);  // use existing reg class
						createNewRegClass = false;
					}
				}
				// save info for this register to be used in parent uvm_reg_block
				String uvmBlockClassName = getUVMBlockID();
			    saveRegInfo(uvmRegClassName, uvmBlockClassName);
				// build the register class definition
				if (createNewRegClass) buildRegClass(uvmRegClassName);		
			}
			// bump defined reg count on the stack
			activeRegisterCount.push(activeRegisterCount.pop() + 1);
		}
	}

	@Override
	public void addRegSet() {
		regSetHasCallback.push(false);
		activeRegisterCount.push(0);  // init to no defined regs
	}

	@Override
	public void finishRegSet() {    
		Boolean hasCallback = regSetHasCallback.pop();
		// if regset has defined registers, add it to the model
		if (activeRegisterCount.peek()>0) {
			// save info for this register set to be used in parent uvm_reg_block (do in finishRegSet so size is computed) - no overrides
			String uvmBlockClassName = getUVMBlockID();  
			//blockClassCount++;
			// if class reuse is specified, store new class names and reuse repeats
			boolean createNewBlockClass = true;
			if (ExtParameters.uvmregsReuseUvmClasses()) {
				if (!uniqueBlockClasses.containsKey(regSetProperties)) uniqueBlockClasses.put(regSetProperties, uvmBlockClassName);
				else {
					//System.out.println("UVMRegsBuilder finishRegSet: block=" + uvmBlockClassName + " is a copy of reg=" + uniqueBlockClasses.get(regSetProperties));
					uvmBlockClassName = uniqueBlockClasses.get(regSetProperties);  // use existing block class
					createNewBlockClass = false;
				}
			}
			saveRegSetInfo(uvmBlockClassName, null, null);
			// build the block class definition
			if (createNewBlockClass) buildBlockClass(uvmBlockClassName, hasCallback);  
		}
		// parent also needs override if has callack
		//if (regSetProperties.isRootInstance()) System.out.println("UVMRegsBuilder finishRegSet: root instance detected, id=" + regSetProperties.getId() + ", addrmap=" + getAddressMapName());
		if (hasCallback && !regSetHasCallback.isEmpty()) {
			regSetHasCallback.pop();
			regSetHasCallback.push(true);
		}
		//System.out.println("UVMRegsBuilder finishRegSet: " + regSetProperties.getInstancePath() + ", activeRegisterCount=" + activeRegisterCount.peek());
		Integer regCount = activeRegisterCount.pop();  // pop the count
		// bump defined reg count on the stack
		if (!activeRegisterCount.isEmpty()) activeRegisterCount.push(activeRegisterCount.pop() + regCount);
	}

	/** process root address map */
	@Override
	public void addRegMap() { 
		regSetHasCallback.push(false);
		activeRegisterCount.push(0);  // init to no defined regs
		generatePkgImports();
	}

	/** finish root address map  */
	@Override
	public  void finishRegMap() {	
		// build the register class definition
		Boolean hasCallback = regSetHasCallback.pop();
		String uvmBlockClassName = getUVMBlockID();
		buildBaseBlockClass(uvmBlockClassName, hasCallback); 
		//System.out.println("UVMRegsBuilder finishRegMap: " + regSetProperties.getInstancePath() + ", activeRegisterCount=" + activeRegisterCount.peek());
		//System.out.println("UVMRegsBuilder finishRegMap: regs=" + regClassCount + ", unique regs=" + uniqueRegClasses.size());
		//System.out.println("UVMRegsBuilder finishRegMap: blocks=" + blockClassCount + ", unique blocks=" + uniqueBlockClasses.size());
		
		activeRegisterCount.pop();  // pop the count
	}

    //---------------------------- helper methods --------------------------------------

	/** generate package import statements */
	protected void generatePkgImports() {
		outputList.add(new OutputLine(indentLvl, "import uvm_pkg::*;"));
		outputList.add(new OutputLine(indentLvl, "import ordt_uvm_reg_pkg::*;"));
	}
	
    //---------------------------- helper methods saving child info for parent build --------------------------------------

	/** save register info for use in parent uvm_reg_block class */
	protected void saveRegInfo(String uvmRegClassName, String uvmBlockClassName) {
		// get parent name
		String parentID = this.getParentInstancePath().replace('.', '_');
		// escape id and alias names
		String regId = escapeReservedString(regProperties.getId());
		//String aliasedId = escapeReservedString(regProperties.getAliasedId());
		// save register define statements
		String repStr = (regProperties.isReplicated()) ? "[" + regProperties.getRepCount() + "]" : "";
		subcompDefList.addStatement(parentID, "rand " + uvmRegClassName + " " + regId + repStr + ";");
		
		// save cbs define statements (may not be needed - determine in block finish once alias groups are known)
		regCbsDefineStatements.addStatement(parentID, "rdl_alias_reg_cbs " + getUVMRegCbsID() + ";", uvmBlockClassName, regProperties.getId(), regProperties.getAliasedId());
		// save cbs assign statements
		String cbsName = getUVMRegCbsID();
		regCbsAssignStatements.addStatement(parentID, "GENERATE ALIAS GROUP", uvmBlockClassName, regProperties.getId(), regProperties.getAliasedId()); // special line  
		regCbsAssignStatements.addStatement(parentID, cbsName + " = new(\"" + cbsName + "\");", uvmBlockClassName, regProperties.getId(), regProperties.getAliasedId());  
		regCbsAssignStatements.addStatement(parentID, cbsName + ".set_alias_regs(alias_group);", uvmBlockClassName, regProperties.getId(), regProperties.getAliasedId());  
		regCbsAssignStatements.addStatement(parentID, "uvm_reg_cb::add(" + regProperties.getId() + ", " + cbsName + ");", uvmBlockClassName, regProperties.getId(), regProperties.getAliasedId());  

		// save register coverage statements
		if (ExtParameters.uvmregsIncludeAddressCoverage()) addRegToAddrCoverageList(parentID, regId, false);  // false = not in a uvm_mem wrapper
		// save register build statements
		addRegToBuildList(uvmRegClassName, parentID, regId);
		//if (regProperties.isAlias()) System.out.println("UVMRegsNuilder: alias reg hdl using " + regProperties.getAliasedId() + " not " + regProperties.getId());
		// if an alias register build a cbs class and register callback   
		if (regProperties.isAlias()) {  // also need isAliased()   // just load alias/aliased register structure by block here
			aliasGroups.addGroupReg(uvmBlockClassName, regProperties.getAliasedId(), regProperties.getId());  
		}
		// issue warning if no category defined
		if (!regProperties.hasCategory() && !ExtParameters.uvmregsSuppressNoCategoryWarnings()) 
			Ordt.warnMessage("register " + regProperties.getInstancePath() + " has no category defined");
		//System.out.println("UVMBuild saveRegInfo: " + regProperties.getBaseName() + ", parent=" + parentID + ", rel addr=" + regProperties.getRelativeBaseAddress());
	}
	
	/** create block addr covergroup statements adding current register */
	protected void addRegToAddrCoverageList(String parentID, String regId, boolean parentIsWrapper) {
		RegNumber baseAddress = parentIsWrapper? new RegNumber(0) : regProperties.getRelativeBaseAddress();
		String addrString = baseAddress.toFormat(RegNumber.NumBase.Hex, RegNumber.NumFormat.NoLengthVerilog);
		if (regProperties.isReplicated()) {
			// compute high address for reg range
			RegNumber offset = new RegNumber(regProperties.getAddrStride());
			offset.multiply(regProperties.getRepCount());
			offset.add(regProperties.getRelativeBaseAddress());
			offset.subtract(1);
			String endAddr = offset.toFormat(RegNumber.NumBase.Hex, RegNumber.NumFormat.NoLengthVerilog);
			// if bin threshold is exceeded then set bins to 8 
			int bins = (regProperties.getRepCount() > ExtParameters.uvmregsMaxRegCoverageBins())? 8 : regProperties.getRepCount();
			subcompAddrCoverGroupList.addStatement(parentID, "  " + regId + ": coverpoint m_offset");  
			subcompAddrCoverGroupList.addStatement(parentID, "      { bins offset[" + bins + "] = { [" + addrString + " : " + endAddr + "] }; }"); 
		}
		else {
			subcompAddrCoverGroupList.addStatement(parentID, "  " + regId + ": coverpoint m_offset");  
			subcompAddrCoverGroupList.addStatement(parentID, "      { bins hit = { " + addrString + " }; }"); 
		}	
	}

	/** create block build statements adding current register */
	protected void addRegToBuildList(String uvmRegClassName, String parentID, String regId) {
		// set hdl path component
		String hdlPath = regProperties.isAlias() ? regProperties.getAliasedId() : regProperties.getId();
		String addr = "`UVM_REG_ADDR_WIDTH" + regProperties.getRelativeBaseAddress().toFormat(RegNumber.NumBase.Hex, RegNumber.NumFormat.NoLengthVerilog);
		if (regProperties.isReplicated()) {
			subcompBuildList.addStatement(parentID, "foreach (this." + regId + "[i]) begin");
			if (ExtParameters.uvmregsRegsUseFactory()) subcompBuildList.addStatement(parentID, "  this." + regId + "[i] = " + uvmRegClassName + "::type_id::create($psprintf(\"" + regId + " [%0d]\",i));");
			else subcompBuildList.addStatement(parentID, "  this." + regId + "[i] = new($psprintf(\"" + regProperties.getId() + " [%0d]\",i));");  
			subcompBuildList.addStatement(parentID, "  this." + regId + "[i].configure(this, null, \"\");");  
			subcompBuildList.addStatement(parentID, "  this." + regId + "[i].set_rdl_tag($psprintf(\"" + hdlPath + "_%0d_\",i));");
			if (regProperties.isLocalExternal()) 
				subcompBuildList.addStatement(parentID, "  this." + regId + "[i].set_external(1);");
			subcompBuildList.addStatement(parentID, "  this." + regId + "[i]" + getUvmRegTestModeString());  
			// add any user defined properties
			addUserDefinedPropertyElements(parentID, regProperties, regId + "[i]");
			subcompBuildList.addStatement(parentID, "  this." + regId + "[i].build();");
			subcompBuildList.addStatement(parentID, "  this.default_map.add_reg(this." + regId + "[i], " + addr + "+i*" + getRegAddrIncr() + ", \"" + getRegAccessType() + "\", 0);");						
			subcompBuildList.addStatement(parentID, "end");
		}
		else {
		   if (ExtParameters.uvmregsRegsUseFactory()) subcompBuildList.addStatement(parentID, "this." + regId + " = " + uvmRegClassName + "::type_id::create(\"" + regId + "\");");
		   else subcompBuildList.addStatement(parentID, "this." + regId + " = new(\"" + regProperties.getId() + "\");");  
		   subcompBuildList.addStatement(parentID, "this." + regId + ".configure(this, null, \"\");"); 
		   subcompBuildList.addStatement(parentID, "this." + regId + ".set_rdl_tag(\"" + hdlPath + "_\");");
		   if (regProperties.isLocalExternal()) 
			   subcompBuildList.addStatement(parentID, "this." + regId + ".set_external(1);");
		   subcompBuildList.addStatement(parentID, "this." + regId + getUvmRegTestModeString());  
			// add any user defined properties
			addUserDefinedPropertyElements(parentID, regProperties, regId);
		   subcompBuildList.addStatement(parentID, "this." + regId + ".build();");
		   subcompBuildList.addStatement(parentID, "this.default_map.add_reg(this." + regId + ", " + addr + ", \"" + getRegAccessType() + "\", 0);");			
		}	
	}

	/* return a string with a call to set_reg_test_info */
	protected String getUvmRegTestModeString() {
		String dontTestBit = regProperties.isDontTest()? "1" : "0";
		String dontCompareBit = regProperties.isDontCompare()? "1" : "0";
		return ".set_reg_test_info(" + dontTestBit + ", " + dontCompareBit + ", " + regProperties.getCategory().getValue() + ");";
	}

	/** save memory info for use in parent uvm_reg_block class
	 * @param parentIsWrapper - if true, info will be saved to a wrapper block and reg/mem instance names will be changed
	 */
	protected void saveMemInfo(boolean parentIsWrapper) {
		// get parent block name (use reg name if a wrapper is being used)
		String parentID = parentIsWrapper? regProperties.getInstancePath().replace('.', '_') : this.getParentInstancePath().replace('.', '_'); 
		// set vreg instance id
		String regId = parentIsWrapper? "vregs" : regProperties.getId();
		// escape id name
		String escapedRegId = escapeReservedString(regId);
		// build the memory Id
		String memId = parentIsWrapper? "mem" : "MEM_" + regProperties.getId();
		// save memory and vreg define statements
		subcompDefList.addStatement(parentID, "rand uvm_mem_rdl " + memId + ";");   // the memory 
		subcompDefList.addStatement(parentID, "rand " + getUVMVRegID() + " " + escapedRegId + ";");   // virtual regs
		//System.out.println("UVMRegsBuilder saveMemInfo: saving statements into wrapper parentID=" + parentID);
		// save register coverage statements
		if (ExtParameters.uvmregsIncludeAddressCoverage()) addRegToAddrCoverageList(parentID, escapedRegId, parentIsWrapper);  
		// save virtual register and mem build statements
		addMemToBuildList(parentID, memId, escapedRegId);

		// issue warning if no category defined
		if (!regProperties.hasCategory() && !ExtParameters.uvmregsSuppressNoCategoryWarnings()) 
			Ordt.warnMessage("register " + regProperties.getInstancePath() + " has no category defined");

		// add the memory to the address map  (no offset if in a wrapper block)
		String addrString = parentIsWrapper? "`UVM_REG_ADDR_WIDTH'h0" :
		                                     "`UVM_REG_ADDR_WIDTH" + regProperties.getRelativeBaseAddress().toFormat(RegNumber.NumBase.Hex, RegNumber.NumFormat.NoLengthVerilog);
		subcompBuildList.addStatement(parentID, "this.default_map.add_mem(this." + memId + ", " + addrString + ");");						
	}
	
	/** create block build statements adding current register */
	protected void addMemToBuildList(String parentID, String memId, String escapedRegId) {
		// save mem build statements for uvm_reg_block
		subcompBuildList.addStatement(parentID, "this." + memId + " = new(\"" + memId + "\", " + regProperties.getRepCount() + ", " + regProperties.getRegWidth() + ");");  
		subcompBuildList.addStatement(parentID, "this." + memId + ".configure(this);");  
		
		// compute a reg level reset value from fields (null if no reset fields)
		RegNumber reset = getFullRegReset();
		
		// save vreg build statements for uvm_reg_block  
		subcompBuildList.addStatement(parentID, "this." + escapedRegId + " = new;");  
		subcompBuildList.addStatement(parentID, "this." + escapedRegId + ".configure(this, " + memId + ", " + regProperties.getRepCount() + ");"); 
		// if this vreg group has a reset defined, then add it
		if (reset != null) {
			subcompBuildList.addStatement(parentID, "this." + escapedRegId + ".set_reset_value(" + reset.toFormat(NumBase.Hex, NumFormat.Verilog) + ");"); 
		}
		
		//System.out.println("UVMRegsBuilder saveMemInfo: regid=" + regId + ", memid=" + memId + ", reg reps=" + regProperties.getRepCount() + ", inst reps=" + getVRegReps());
		subcompBuildList.addStatement(parentID, "this." + escapedRegId + getUvmRegTestModeString());  
		// add any user defined properties
		addUserDefinedPropertyElements(parentID, regProperties, escapedRegId);
		subcompBuildList.addStatement(parentID, "this." + escapedRegId + ".build();");		
	}

	/** compute a register level reset from the current list of fields (or null if none defined) */
	protected RegNumber getFullRegReset() {
		RegNumber reset = new RegNumber(0);  // default to reset of zero
		reset.setVectorLen(regProperties.getRegWidth());
		boolean hasFieldReset = false;
		// add each field reset
		for (FieldProperties fld : fieldList) {
			RegNumber fldReset = new RegNumber(fld.getReset());  // copy reset value
			if (fldReset.isDefined()) {
				hasFieldReset = true;
				fldReset.lshift(fld.getLowIndex());
				reset.add(fldReset);
			}
		}
		// return null if no field resets found
		//System.out.println("UVMRegsBuilder getVRegReset: id=" + regProperties.getId() + ", hasFldRest=" + hasFieldReset + ", reset=" + reset);	
		return hasFieldReset? reset : null;
	}

	/** get total number of replications for this vreg by multiplying reps on reg set stack
	 */
	public Integer getVRegReps() {
		Integer retVal = 1;
		Iterator<RegSetProperties> iter = regSetPropertyStack.iterator();
		while (iter.hasNext()) {
			RegSetProperties inst = iter.next();
			retVal *= inst.getRepCount();
		}
		return retVal;
	}

	/** save register set info for use in parent uvm_reg_block class   
	 * @param uvmBlockClassName - name of uvm block class
	 * @param blockIdOverride - if non null, specified name will be used as the block instance rather then regset id
	 *        - regSetProperties (replication, is addrmap) will be ignored if a name override is specified
	 *        (this is used for uvm_mem wrapper block gen)
	 * @param addrOffsetOverride - if non null, specified address offset will be used rather than current regset offset
	 */
	protected void saveRegSetInfo(String uvmBlockClassName, String blockIdOverride, RegNumber addrOffsetOverride) {
		// get parent name
		String parentID = this.getParentInstancePath().replace('.', '_');
		// block id
		boolean hasInstanceNameOverride = (blockIdOverride != null);
		String blockId = hasInstanceNameOverride? blockIdOverride : regSetProperties.getId();
		// escaped block id 
		String escapedBlockId = escapeReservedString(blockId);
		// save block define statements
		String repStr = (!hasInstanceNameOverride && regSetProperties.isReplicated()) ? "[" + regSetProperties.getRepCount() + "]" : "";
		subcompDefList.addStatement(parentID, "rand " + uvmBlockClassName + " " + escapedBlockId + repStr + ";");
		// set address offset for new block
		RegNumber addr = (addrOffsetOverride != null)? addrOffsetOverride : regSetProperties.getRelativeBaseAddress();
		String addrStr = "`UVM_REG_ADDR_WIDTH" + addr.toFormat(RegNumber.NumBase.Hex, RegNumber.NumFormat.NoLengthVerilog);
		// save register build statements
		if (!hasInstanceNameOverride && regSetProperties.isReplicated()) {  
			subcompBuildList.addStatement(parentID, "foreach (this." + escapedBlockId + "[i]) begin");
			subcompBuildList.addStatement(parentID, "  this." + escapedBlockId + "[i] = " + uvmBlockClassName + "::type_id::create($psprintf(\"" + blockId + " [%0d]\",i),, get_full_name());");
			subcompBuildList.addStatement(parentID, "  this." + escapedBlockId + "[i].configure(this, \"\");");  
			if (regSetProperties.isAddressMap()) {
				subcompBuildList.addStatement(parentID, "  this." + escapedBlockId + "[i].set_rdl_address_map(1);");  // tag block as an address map
				subcompBuildList.addStatement(parentID, "  this." + escapedBlockId + "[i].set_rdl_address_map_hdl_path({`" + getParentAddressMapName().toUpperCase() + "_PIO_INSTANCE_PATH, \".pio_logic\"});");  // TODO
			}
			else
				subcompBuildList.addStatement(parentID, "  this." + escapedBlockId + "[i].set_rdl_tag($psprintf(\"" + blockId + "_%0d_\",i));");
			// add any user defined properties
			addUserDefinedPropertyElements(parentID, regSetProperties, escapedBlockId + "[i]");
			subcompBuildList.addStatement(parentID, "  this." + escapedBlockId + "[i].build();");
			subcompBuildList.addStatement(parentID, "  this.default_map.add_submap(this." + escapedBlockId + "[i].default_map, " + addrStr + "+i*" + getRegSetAddrIncrString() + ");");						
			subcompBuildList.addStatement(parentID, "end");
		}
		else {
		   subcompBuildList.addStatement(parentID, "this." + escapedBlockId + " = " + uvmBlockClassName + "::type_id::create(\"" + blockId + "\",, get_full_name());");
		   subcompBuildList.addStatement(parentID, "this." + escapedBlockId + ".configure(this, \"\");"); 
		   if (!hasInstanceNameOverride && regSetProperties.isAddressMap()) {
				subcompBuildList.addStatement(parentID, "this." + escapedBlockId + ".set_rdl_address_map(1);");  // tag block as an address map
				subcompBuildList.addStatement(parentID, "this." + escapedBlockId + ".set_rdl_address_map_hdl_path({`" + getParentAddressMapName().toUpperCase() + "_PIO_INSTANCE_PATH, \".pio_logic\"});");  
		   }
		   else
			   subcompBuildList.addStatement(parentID, "this." + escapedBlockId + ".set_rdl_tag(\"" + blockId + "_\");");
		   // add any user defined properties
		   addUserDefinedPropertyElements(parentID, regSetProperties, escapedBlockId);
		   subcompBuildList.addStatement(parentID, "this." + escapedBlockId + ".build();");
		   subcompBuildList.addStatement(parentID, "this.default_map.add_submap(this." + escapedBlockId + ".default_map, " + addrStr + ");");			
		}		
		//System.out.println("UVMBuild saveRegSetInfo: " + regSetProperties.getBaseName() + ", parent=" + parentID + ", rel addr=" + regSetProperties.getRelativeBaseAddress());
	}

	/** get the increment string for this group of regs */
	protected String getRegAddrIncr() {
		RegNumber incr = regProperties.getAddrStride();
		String addr = "`UVM_REG_ADDR_WIDTH" + incr.toFormat(RegNumber.NumBase.Hex, RegNumber.NumFormat.NoLengthVerilog);
		return addr;
	}

	/** get the increment string for this regset */
	protected String getRegSetAddrIncrString() {
		RegNumber incr = getRegSetAddressStride(true);
		String addr = "`UVM_REG_ADDR_WIDTH" + incr.toFormat(RegNumber.NumBase.Hex, RegNumber.NumFormat.NoLengthVerilog);
		return addr;
	}
	
	// ----------------------- uvm reg class builder methods -------------------------

	/** build reg class definition for current register instance */   
	protected void buildRegClass(String uvmRegClassName) {
		// create text name and description if null
		String id = regProperties.getId();
		String textName = regProperties.getTextName();
		if (textName == null) textName = "Register " + id;
		else textName = textName.replace('\n', ' ');
		
		// generate register header 
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl, "// " + textName));
		outputList.add(new OutputLine(indentLvl++, "class " + uvmRegClassName + " extends uvm_reg_rdl;")); 
		
		// tag used for rdl name generation
		outputList.add(new OutputLine(indentLvl, "string m_rdl_tag;")); 
				
		// create field definitions
		buildRegFieldDefines();
		
		// create new function
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function new(string name = \"" + uvmRegClassName + "\");"));
		outputList.add(new OutputLine(indentLvl, "super.new(name, " + regProperties.getRegWidth() + ", UVM_NO_COVERAGE);"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: new"));

		// create build function
		buildRegBuildFunction();
		
		// create add_callbacks function
		buildRegAddCallbacksFunction();
		
		// if this register has interrupt fields, add interrupt get methods
		if (regProperties.hasInterruptFields()) buildRegIntrFunctions();
				
		// close out the register definition
		outputList.add(new OutputLine(indentLvl, ""));	
		if (ExtParameters.uvmregsRegsUseFactory()) outputList.add(new OutputLine(indentLvl, "`uvm_object_utils(" + uvmRegClassName + ")"));
		outputList.add(new OutputLine(--indentLvl, "endclass : " + uvmRegClassName));
	}

	/** add interrupt field get methods */
	protected void buildRegIntrFunctions() {
		// these override defaults in uvm_reg_rdl
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "virtual function void get_intr_fields(ref uvm_reg_field fields[$]); // return all source interrupt fields"));
		outputList.add(new OutputLine(indentLvl,     "uvm_reg_field f[$];"));
		outputList.add(new OutputLine(indentLvl,     "uvm_reg_field_rdl rdl_f;"));
		outputList.add(new OutputLine(indentLvl,     "uvm_reg_field_rdl_interrupt rdl_intr_f;"));
		outputList.add(new OutputLine(indentLvl,     "get_fields(f);")); 
		outputList.add(new OutputLine(indentLvl++,     "foreach(f[i]) begin"));
		outputList.add(new OutputLine(indentLvl,         "$cast(rdl_f, f[i]);"));  // cast so rdl methods are visible
		outputList.add(new OutputLine(indentLvl++,       "if (rdl_f.is_interrupt()) begin"));
		outputList.add(new OutputLine(indentLvl,           "$cast(rdl_intr_f, rdl_f);"));  // cast so intr methods are visible
		outputList.add(new OutputLine(indentLvl,           "rdl_intr_f.get_intr_fields(fields);"));
		outputList.add(new OutputLine(--indentLvl,       "end"));
		outputList.add(new OutputLine(--indentLvl,     "end"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: get_intr_fields"));
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "virtual task get_active_intr_fields(ref uvm_reg_field fields[$], input bit is_halt, input uvm_path_e path = UVM_DEFAULT_PATH); // return all active source intr/halt fields"));
		outputList.add(new OutputLine(indentLvl,     "uvm_reg_field f[$];"));
		outputList.add(new OutputLine(indentLvl,     "uvm_reg_field_rdl rdl_f;"));
		outputList.add(new OutputLine(indentLvl,     "uvm_reg_field_rdl_interrupt rdl_intr_f;"));
		outputList.add(new OutputLine(indentLvl,     "uvm_status_e status;"));
		outputList.add(new OutputLine(indentLvl,     "uvm_reg_data_t value;"));
		outputList.add(new OutputLine(indentLvl,     "read(status, value, path);"));
		outputList.add(new OutputLine(indentLvl,     "get_fields(f);")); 
		outputList.add(new OutputLine(indentLvl++,     "foreach(f[i]) begin"));
		outputList.add(new OutputLine(indentLvl,         "$cast(rdl_f, f[i]);"));  // cast so rdl methods are visible
		outputList.add(new OutputLine(indentLvl++,       "if (rdl_f.is_interrupt() && rdl_f.get()) begin"));
		outputList.add(new OutputLine(indentLvl,           "$cast(rdl_intr_f, rdl_f);"));  // cast so intr methods are visible
		outputList.add(new OutputLine(indentLvl,           "rdl_intr_f.get_active_intr_fields(fields, is_halt, path);"));
		outputList.add(new OutputLine(--indentLvl,       "end"));
		outputList.add(new OutputLine(--indentLvl,     "end"));
		outputList.add(new OutputLine(--indentLvl, "endtask: get_active_intr_fields"));
	}

	/** build vreg class definition for current register instance */   
	protected void buildVRegClass() {   
		// create text name and description if null
		String id = regProperties.getId();
		String fullId = getUVMVRegID();
		String textName = regProperties.getTextName();
		if (textName == null) textName = "Virtual Register " + id;
		else textName = textName.replace('\n', ' ');
		
		// generate register header 
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl, "// " + textName));
		outputList.add(new OutputLine(indentLvl++, "class " + fullId + " extends uvm_vreg_rdl;")); 
						
		// create field definitions
		buildVRegFieldDefines();   
		
		// create new function
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function new(string name = \"" + fullId + "\");"));
		outputList.add(new OutputLine(indentLvl,     "super.new(name, " + regProperties.getRegWidth() + ");"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: new"));

		// create build function
		buildVRegBuildFunction();
				
		// close out the register definition
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(--indentLvl, "endclass : " + fullId));
	}

	/** build block class definition for current regset instance 
	 * @param hasCallback - indicates block children have callbacks */   
	protected void buildBlockClass(String uvmBlockClassName, Boolean hasCallback) {
		// create text name and description if null
		String id = regSetProperties.getId();
		String refId = regSetProperties.getBaseName();  // ref used for block structure lookup
		
		String textName = regSetProperties.getTextName();
		if (textName == null) textName = "Block " + id;
		
		// generate register header 
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl, "// " + textName));
		outputList.add(new OutputLine(indentLvl++, "class " + uvmBlockClassName + " extends uvm_reg_block_rdl;")); 

		// create field definitions  
		buildBlockDefines(refId);
		
		// build any specified coverage defines
		buildBlockCoverageDefines(refId);
		
		// if this block has alias groups then add a define 
		if (aliasGroups.blockExists(uvmBlockClassName)) {
			outputList.add(new OutputLine(indentLvl, "local uvm_reg alias_group[$];"));			
		}
		
		// create new function
		buildBlockNewDefine(uvmBlockClassName, ExtParameters.uvmregsIncludeAddressCoverage());
		
		// if child callbacks, override add_callbacks
		if (hasCallback) buildBlockAddCallbacksMethod();
		
		// create build function  
		Integer mapWidthOverride = (ExtParameters.hasDebugMode("uvmregs_maps_use_max_width"))?  regSetProperties.getMaxRegByteWidth()  : null; // if debug mode 1 use regset property width
		buildBlockBuildFunction(uvmBlockClassName, refId, mapWidthOverride, false);
		
		// close out the class definition
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl, "`uvm_object_utils(" + uvmBlockClassName + ")"));
		outputList.add(new OutputLine(--indentLvl, "endclass : " + uvmBlockClassName));
	}

	/** build uvm_mem/uvm_vreg wrapper block class definition as child of current regset block 
	 *  callbacks, aliases not allowed in this block type 
	 * @param b */   
	protected void buildMemWrapperBlockClass(String uvmBlockClassName, String nameSuffix, Integer mapWidthOverride) {
		// create block name + id with suffix
		String refId = ((regSetProperties == null) || regSetProperties.getBaseName().isEmpty()) ? nameSuffix : regSetProperties.getBaseName() + "_" + nameSuffix;  // id used for structure lookup
		//System.out.println("UVMRegsBuilder buildMemWrapperBlockClass: fullId=" + uvmBlockClassName + ", refId=" + refId);
		
		// generate register header 
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl, "// Uvm_mem wrapper block " + refId));
		outputList.add(new OutputLine(indentLvl++, "class " + uvmBlockClassName + " extends uvm_reg_block_rdl;")); 

		// create field definitions  
		buildBlockDefines(refId);
		
		// build any specified coverage defines
		buildBlockCoverageDefines(refId);
		
		// create new function
		buildBlockNewDefine(uvmBlockClassName, ExtParameters.uvmregsIncludeAddressCoverage());
		
		// create build function ising width of underlying virtual regs/mem
		buildBlockBuildFunction(uvmBlockClassName, refId, mapWidthOverride, true);
		
		// close out the class definition
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl, "`uvm_object_utils(" + uvmBlockClassName + ")"));
		outputList.add(new OutputLine(--indentLvl, "endclass : " + uvmBlockClassName));
	}

	/** build block class definition for current regset instance 
	 * @param hasCallback  - indicates block children have callbacks*/ 
	protected void buildBaseBlockClass(String uvmBlockClassName, Boolean hasCallback) {
		//System.out.println("UVMRegsBuilder buildBaseBlockClass: fullId=" + uvmBlockClassName + ", getUVMBlockID()=" + getUVMBlockID());
		String refId = "";  // ref used for base block structure lookup
		
		// generate register header 
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl, "// Base block"));
		outputList.add(new OutputLine(indentLvl++, "class " + uvmBlockClassName + " extends uvm_reg_block_rdl;")); 
		
		//String uvmBlockClassName, 
		// create field definitions  
		buildBlockDefines(refId);
		
		// build any specified coverage defines
		buildBlockCoverageDefines(refId);
		
		// if this block has alias groups then add a define 
		if (aliasGroups.blockExists(uvmBlockClassName)) {
			outputList.add(new OutputLine(indentLvl, "local uvm_reg alias_group[$];"));			
		}
		
		// create new function
		buildBlockNewDefine(uvmBlockClassName, ExtParameters.uvmregsIncludeAddressCoverage());
		
		// if child callbacks, override add_callbacks
		if (hasCallback) buildBlockAddCallbacksMethod();
		
		// create build function (no width override) 
		buildBlockBuildFunction(uvmBlockClassName, refId, null, false);
		
		// close out the class definition
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl, "`uvm_object_utils(" + uvmBlockClassName + ")"));
		outputList.add(new OutputLine(--indentLvl, "endclass : " + uvmBlockClassName));
	}

	// ---------

	/** return address map prefix string  */
	protected String getAddrMapPrefix() {
		String map = getAddressMapName();
		if (map.isEmpty()) return "";
		return "_" + map;
	}

	/** return uvm_reg class name */
	protected String getUVMRegCbsID() {
		String fullId = "alias_cbs" + getAddrMapPrefix() + "_" + regProperties.getBaseName();
		return fullId;
	}

	/** return uvm_reg class name */
	protected String getUVMRegID() {
		String fullId = "reg" + getAddrMapPrefix() + "_" + regProperties.getBaseName();  
		return fullId;
	}

	/** return uvm_vreg class name */
	protected String getUVMVRegID() {
		String fullId = "vreg" + getAddrMapPrefix() + "_" + regProperties.getBaseName();  
		return fullId;
	}

	/** return uvm_reg_block class name
	 * @param regSetProps - currently active regset properties
	 * @param nameSuffix - optional block name suffix
	 */
	protected String getUVMBlockID(RegSetProperties regSetProps, String nameSuffix) {
		String regSetStr = ((regSetProps == null) || regSetProps.getBaseName().isEmpty()) ? "" : "_" + regSetProps.getBaseName();
		if (nameSuffix != null) regSetStr += "_" + nameSuffix;
		String fullId = "block" + getAddrMapPrefix() + regSetStr;
		//if (fullId.contains("__")) System.out.println("getUVMBlockID: prefix=" + getAddrMapPrefix() + " regSetStr=" + regSetStr);
		return fullId;
	}

	/** return uvm_reg_block class name */
	protected String getUVMBlockID() {
		return getUVMBlockID(regSetProperties, null);
	}

	/** return uvm_reg_block class name
	 * @param nameSuffix - optional block name suffix 
	 */
	protected String getUVMBlockID(String nameSuffix) {
		return getUVMBlockID(regSetProperties, nameSuffix);
	}
	
	/** create add_callbacks methods for uvm_reg_block derived class   
	 * @param outputList - list of output lines to be updated
	 * @param indentLvl */
	protected void buildBlockAddCallbacksMethod() {
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "virtual function void add_callbacks();"));
		outputList.add(new OutputLine(indentLvl, "uvm_reg m_regs[$];"));
		outputList.add(new OutputLine(indentLvl, "uvm_reg_block  m_blks[$];"));
		outputList.add(new OutputLine(indentLvl, "uvm_reg_rdl rg;"));
		outputList.add(new OutputLine(indentLvl, "uvm_reg_block_rdl blk;"));
		// loop through child regs and add_callbacks
		outputList.add(new OutputLine(indentLvl, "this.get_registers(m_regs, UVM_NO_HIER);"));
		outputList.add(new OutputLine(indentLvl++, "foreach (m_regs[rg_]) begin"));  
		outputList.add(new OutputLine(indentLvl, "$cast(rg, m_regs[rg_]);"));
		outputList.add(new OutputLine(indentLvl, "rg.add_callbacks();"));
		outputList.add(new OutputLine(--indentLvl, "end"));
		// loop through child blocks and add_callbacks
		outputList.add(new OutputLine(indentLvl, "this.get_blocks(m_blks, UVM_NO_HIER);"));
		outputList.add(new OutputLine(indentLvl++, "foreach (m_blks[blk_]) begin"));  
		outputList.add(new OutputLine(indentLvl, "$cast(blk, m_blks[blk_]);"));
		outputList.add(new OutputLine(indentLvl, "blk.add_callbacks();"));
		outputList.add(new OutputLine(--indentLvl, "end"));

		outputList.add(new OutputLine(--indentLvl, "endfunction: add_callbacks"));		
	}

	/** build field definitions for current register */
	protected void buildRegFieldDefines() {
		Iterator<FieldProperties> iter = fieldList.iterator();
		// traverse field list
		while (iter.hasNext()) {
			FieldProperties field = iter.next();
			String fieldId = escapeReservedString(field.getPrefixedId());  
			// define field class by type
			String fieldClass = "uvm_reg_field_rdl";
			if (field.isCounter()) fieldClass = "uvm_reg_field_rdl_counter";
			else if (field.isInterrupt()) fieldClass = "uvm_reg_field_rdl_interrupt";
			//System.out.println("UVMRegsBuilder: buildRegFieldDefines def=" + "rand " + fieldClass + " " + fieldId + ";");
			outputList.add(new OutputLine(indentLvl, "rand " + fieldClass + " " + fieldId + ";"));
		}
	}

	/** build field definitions for current virtual register */
	protected void buildVRegFieldDefines() {
		Iterator<FieldProperties> iter = fieldList.iterator();
		// traverse field list
		while (iter.hasNext()) {
			FieldProperties field = iter.next();
			String fieldId = escapeReservedString(field.getPrefixedId());  
			// define field class by type
			String fieldClass = "uvm_vreg_field";
			//System.out.println("UVMRegsBuilder: buildVRegFieldDefines def=" + "rand " + fieldClass + " " + fieldId + ";");
			outputList.add(new OutputLine(indentLvl, "rand " + fieldClass + " " + fieldId + ";"));
		}
	}

	/** build field hdl paths for current register */
	protected void buildRegHdlPaths() {
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl, "rdl_reg_name = get_rdl_name(\"rg_\");"));	// get register name
		
		Iterator<FieldProperties> iter = fieldList.iterator();
		// traverse field list
		while (iter.hasNext()) {
			FieldProperties field = iter.next();
			outputList.add(new OutputLine(indentLvl, "add_hdl_path_slice({rdl_reg_name, \"" + field.getPrefixedId() + "\"}, " + field.getLowIndex() + ", " + field.getFieldWidth() + ");"));
		}		
	}

	/** build subcomponent definitions for current block 
	 * @param block - getBaseName of current regSet used as hash key*/
	protected void buildBlockDefines(String block) {  
		List<SpecialLine> defList = subcompDefList.getStatements(block);
		if (defList == null) return;
		Iterator<SpecialLine> iter = defList.iterator();
		while (iter.hasNext()) {
			SpecialLine line = iter.next();
			line.setIndent(indentLvl);
			outputList.add(line);
		}
		// need to add a callback definition for each reg in an alias group 
		List<SpecialLine> cbsDefList = regCbsDefineStatements.getStatements(block);
		// if a list found (will be null if block has no regs directly defined)
        if (cbsDefList != null) {
    		iter = cbsDefList.iterator();
    		while (iter.hasNext()) {
    			SpecialLine line = iter.next();
    			line.setIndent(indentLvl);
    			if (line.isInAliasGroup()) outputList.add(line);  // add define to output if in an alias group
    		}
        }
	}
	
	/** build subcomponent coverage definitions for current block 
	 * @param block - getBaseName of current regSet used as hash key*/
	protected void buildBlockCoverageDefines(String block) {
        // if address coverage is specified then init coverage vars
        if (ExtParameters.uvmregsIncludeAddressCoverage()) {
       	    outputList.add(new OutputLine(indentLvl, "local uvm_reg_addr_t m_offset;"));
        	outputList.add(new OutputLine(indentLvl, ""));
        	outputList.add(new OutputLine(indentLvl++, "covergroup cg_addr;"));
    		List<SpecialLine> defList = subcompAddrCoverGroupList.getStatements(block);
    		if (defList != null) {
        		Iterator<SpecialLine> iter = defList.iterator();
        		while (iter.hasNext()) {
        			SpecialLine line = iter.next();
        			line.setIndent(indentLvl);
        			outputList.add(line);
        		}
    		}
        	outputList.add(new OutputLine(--indentLvl, "endgroup"));   			
        }
	}

	/** build new function and coverage sample methods 
	 * @param includeCoverage */
	protected void buildBlockNewDefine(String fullId, boolean includeCoverage) {
		outputList.add(new OutputLine(indentLvl, ""));	
		// if address coverage specified, add to new() and create sample
		if (includeCoverage) { 
			outputList.add(new OutputLine(indentLvl++, "function new(string name = \"" + fullId + "\");"));
			outputList.add(new OutputLine(indentLvl,     "super.new(name, build_coverage(UVM_CVR_ADDR_MAP));"));
			outputList.add(new OutputLine(indentLvl,     "if (has_coverage(UVM_CVR_ADDR_MAP))"));	
			outputList.add(new OutputLine(indentLvl,     "  cg_addr = new();"));	
			outputList.add(new OutputLine(--indentLvl, "endfunction: new"));
			// add add coverage sample method
			outputList.add(new OutputLine(indentLvl, ""));	
			outputList.add(new OutputLine(indentLvl++, "virtual function void sample(uvm_reg_addr_t offset, bit is_read, uvm_reg_map map);"));
			outputList.add(new OutputLine(indentLvl,     "if (get_coverage(UVM_CVR_ADDR_MAP)) begin"));	
			outputList.add(new OutputLine(indentLvl,     "  m_offset  = offset;"));	
			outputList.add(new OutputLine(indentLvl,     "  cg_addr.sample();"));	
			outputList.add(new OutputLine(indentLvl,     "end"));	
			outputList.add(new OutputLine(--indentLvl, "endfunction: sample"));
		}
		// otherwise just simple new()
		else {
			outputList.add(new OutputLine(indentLvl++, "function new(string name = \"" + fullId + "\");"));
			outputList.add(new OutputLine(indentLvl,     "super.new(name);"));
			outputList.add(new OutputLine(--indentLvl, "endfunction: new"));			
		}
	}
	
	/** build the build function for current register */
	protected void buildRegBuildFunction() {
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "virtual function void build();"));
		outputList.add(new OutputLine(indentLvl, "string rdl_reg_name;"));
		
		// traverse field list and create field init statements
		Iterator<FieldProperties> iter = fieldList.iterator();
		while (iter.hasNext()) {
			FieldProperties field = iter.next();
			String fieldId = escapeReservedString(field.getPrefixedId()); 
			
			// create appropriate field class   
			if (field.isCounter()) {
				outputList.add(new OutputLine(indentLvl, "this." + fieldId +  " = new(\"" + field.getPrefixedId() + "\");"));  
				addHWAccessInfo(field);  // add call to init hw access methods
				addCounterInitInfo(field);  // add call to counter init methods				
			}
			else if (field.isInterrupt()) {
				outputList.add(new OutputLine(indentLvl, "this." + fieldId +  " = new(\"" + field.getPrefixedId() + "\");"));  
				addHWAccessInfo(field);  // add call to init hw access methods
				addInterruptInitInfo(field);  // add call to interrupt init methods 
			}
			else {    
				outputList.add(new OutputLine(indentLvl, "this." + fieldId +  " = new(\"" + field.getPrefixedId() + "\");"));  
				addHWAccessInfo(field);  // add call to init hw access methods
			}
			
			// add field configure
			String isVolatile = (field.hwChangesValue() || field.isDontCompare()) ? "1" : "0";
			//if (field.isDontCompare()) System.out.println("UVMRegsBuilder: buildRegBuildFunction volatile set for field=" + field.getInstancePath() + ";");
			String isRand = field.isSwWriteable() ? "1" : "0";  
			outputList.add(new OutputLine(indentLvl, "this." + fieldId + ".configure(this, " + field.getFieldWidth() + 
					", " + field.getLowIndex() + ", \"" + getFieldAccessType(field) + "\", " + isVolatile + ", " + getFieldResetParameters(field) + 
					", " + isRand + ", " + isOnlyField() + ");"));		
			
			// remove reset for uninitialized fields if db update is being skipped
			if (fieldNeedsResetRemoval(field)) outputList.add(new OutputLine(indentLvl, "void'(this." + fieldId + ".has_reset(.delete(1)));"));
				
			// add explicit field dontcompare so reset test can be inhibited even if volatile is ignored
			if (field.isDontCompare())
				outputList.add(new OutputLine(indentLvl, "this." + fieldId +  ".set_dontcompare();"));  

			// add a subcategory
			if (field.hasSubCategory())
				outputList.add(new OutputLine(indentLvl, "this." + fieldId +  ".set_js_subcategory(" + field.getSubCategory().getValue() + ");")); 
			
			// add any user defined properties
			addUserDefinedPropertyElements(indentLvl, field, fieldId);
			
		} // while
		
		// add backdoor path to generated rtl
		if (!regProperties.isLocalExternal()) buildRegHdlPaths();
				
		outputList.add(new OutputLine(--indentLvl, "endfunction: build"));
	}
	
	/** build the add_callbacks function for current register 
	 *  add_callbacks is called recursively after build completes and sets field dynamic assign linkages and associated callbacks */
	protected void buildRegAddCallbacksFunction() {
		// create a new function define
		SystemVerilogFunction func = new SystemVerilogFunction("void", "add_callbacks", this.getUVMRegID());  // only have callbacks on regs
		func.addComment("Add model callbacks associated with " + regProperties.getInstancePath() + ".");
		func.setVirtual();
		
		// find any fields needing callbacks/reference assignment and define callback vars
		boolean intrMaskDetected = false;  // detect intr enable or masks
		boolean intrCascadeDetected = false;   // detect cascaded interrupt fields
		Iterator<FieldProperties> iter = fieldList.iterator();
		while (iter.hasNext()) {
			FieldProperties field = iter.next();
			if (field.isInterrupt() && (field.hasRef(RhsRefType.INTR_ENABLE) || field.hasRef(RhsRefType.INTR_MASK))) {
				intrMaskDetected = true;
				if (field.isMaskIntrBits())  // if maskintrbits then define a callback
					func.addStatement("rdl_mask_intr_field_cbs " + field.getPrefixedId() + "_mask_cb;"); // define mask field callback
			}
			// intr cascade (callback disabled) 
			if (field.isInterrupt() && (field.hasRef(RhsRefType.NEXT) || (field.hasRef(RhsRefType.INTR) && (field.getIntrType() == FieldProperties.IntrType.LEVEL)))) {
				intrCascadeDetected = true;
				//func.addStatement("rdl_cascade_intr_field_cbs " + field.getPrefixedId() + "_cascade_cb;"); // define cascade field callback
			}
		} 
		
		// if fields require add_callbacks() to be called,
		boolean fieldCallbackNeeded  = intrMaskDetected || intrCascadeDetected;  // detect intr bit mask using enable or mask value 
		if (fieldCallbackNeeded) {
			// define vars used for extracting reg/fields and setting references in uvm model
			func.addPreStatement("uvm_reg_block m_root_cb_block;" );  // found from assign depth
			func.addPreStatement("uvm_reg m_cb_reg;");  // find from m_cb_block using reg name
			func.addPreStatement("uvm_reg_field m_cb_field;");  // find from m_cb_reg using field name
			regSetHasCallback.pop();
			regSetHasCallback.push(true);			

			// traverse field list and add any field callbacks
			lastCBDepth = -1;
			lastCBCount = 0;
			iter = fieldList.iterator();
			while (iter.hasNext()) {
				FieldProperties field = iter.next();
				String fieldId = escapeReservedString(field.getPrefixedId()); 
					
				// generate field intr mask assign and callback if maskintrbits
				if (field.isInterrupt() && (field.hasRef(RhsRefType.INTR_ENABLE) || field.hasRef(RhsRefType.INTR_MASK))) {  
					// set reference and ref type
					Boolean isEnable = null;
					RhsReference eRef = null;
					if (field.hasRef(RhsRefType.INTR_ENABLE)) { 
						eRef = field.getRef(RhsRefType.INTR_ENABLE);
						isEnable = true;
					}
					else if (field.hasRef(RhsRefType.INTR_MASK)) {
						eRef = field.getRef(RhsRefType.INTR_MASK);
						isEnable = false;
					}
					// check for invalid form
					if ((isEnable == null) || eRef.hasDeRef() || (eRef.getPathCount() < 2)) {  // rhs must reference a field so reflen must be >= 2
						if (isEnable != null) {
							String enTypeStr = isEnable ? "enable" : "mask";
							String maskIntBitsStr = field.isMaskIntrBits()? "maskintrbits " : "";
							Ordt.warnMessage("UVM model does not support " + maskIntBitsStr + "property assign for " + enTypeStr + "=" + eRef.getRawReference() + " in field=" + field.getInstancePath());							
						}
					}
					else {
						// build register search statements (sets m_cb_field)
						buildFieldCbAssign(eRef, field, func);
						
						// set the enable/mask field reference in this field
						String isEnableStr = isEnable ? "1" : "0";  
						func.addStatement(fieldId + ".set_intr_mask_field(m_cb_field, " + isEnableStr + ");");  
						
						//System.out.println("UVMRegsBuilder buildRegAddCallbacksFunction: enable maskStr=" + maskStr + ", depth=" + eRef.getDepth() );
						// if maskintrbits then also need a callback to change mirror value
						if (field.isMaskIntrBits()) {
							func.addStatement(field.getPrefixedId() + "_mask_cb = new(\"" +
						                        field.getPrefixedId() + "_mask_cb\"," + fieldId + ");");
							func.addStatement("uvm_reg_field_cb::add(this." + fieldId + ", " + field.getPrefixedId() + "_mask_cb);"); 						
						}
					}
				}
				
				// generate field halt mask assign
				if (field.isInterrupt() && (field.hasRef(RhsRefType.HALT_ENABLE) || field.hasRef(RhsRefType.HALT_MASK))) {  
					// set reference and ref type
					Boolean isEnable = null;
					RhsReference eRef = null;
					if (field.hasRef(RhsRefType.HALT_ENABLE)) { 
						eRef = field.getRef(RhsRefType.HALT_ENABLE);
						isEnable = true;
					}
					else if (field.hasRef(RhsRefType.HALT_MASK)) {
						eRef = field.getRef(RhsRefType.HALT_MASK);
						isEnable = false;
					}
					// check for invalid form
					if ((isEnable == null) || eRef.hasDeRef() || (eRef.getPathCount() < 2)) {  // rhs must reference a field so reflen must be >= 2
						if (isEnable != null) {
							String enTypeStr = isEnable ? "enable" : "mask";
							Ordt.warnMessage("UVM model does not support property assign for " + enTypeStr + "=" + eRef.getRawReference() + " in field=" + field.getInstancePath());							
						}
					}
					else {
						// build register search statements (sets m_cb_field)
						buildFieldCbAssign(eRef, field, func);

						// set the enable/mask field reference in this field
						String isEnableStr = isEnable ? "1" : "0";  
						func.addStatement(fieldId + ".set_halt_mask_field(m_cb_field, " + isEnableStr + ");");  
					}
				}
				
				//* (disabled) interrupt cascade callback, but still need set_cascade intr reg call  
				if (field.isInterrupt() && (field.hasRef(RhsRefType.NEXT) || (field.hasRef(RhsRefType.INTR) && (field.getIntrType() == FieldProperties.IntrType.LEVEL)))) {
					// set reference 
					RhsReference eRef = null;
					if (field.hasRef(RhsRefType.NEXT)) eRef = field.getRef(RhsRefType.NEXT);
					else if (field.hasRef(RhsRefType.INTR)) eRef = field.getRef(RhsRefType.INTR);
					// check for invalid form
					if (!(eRef.hasDeRef("intr") || eRef.hasDeRef("halt")) || (eRef.getPathCount() < 1)) {  // rhs must reference a reg so reflen must be >= 1
							Ordt.warnMessage("UVM model does not support property assign " + eRef.getRawReference() + " in field=" + field.getInstancePath());							
							//System.out.println("UVMRegsBuilder buildRegAddCallbacksFunction: eRef=" + eRef.getRegName() + ", depth=" + eRef.getDepth() );
					}
					else {
						// build register search statements (sets m_cb_reg)
						buildRegCbAssign(eRef, func);

						// set the cascaded reg reference in this field
						String cascadeIsHalt = eRef.hasDeRef("halt")? "1" : "0";
						func.addStatement(fieldId + ".set_cascade_intr_reg(m_cb_reg, " + cascadeIsHalt + ");");  
						
						/* disable callback
						//System.out.println("UVMRegsBuilder buildRegAddCallbacksFunction: cascade reg=" + eRef.getRegName() + ", depth=" + eRef.getDepth() );
						func.addStatement(field.getPrefixedId() + "_cascade_cb = new(\"" +
					                        field.getPrefixedId() + "_cascade_cb\", " + fieldId + ");")); 
						func.addStatement("uvm_reg_field_cb::add(this." + fieldId + ", " + field.getPrefixedId() + "_cascade_cb);"));
						*/ 										
					}   
				}
				
			} // while		
		}
		// save the function and add its extern signature
		if (!func.isEmpty()) {
			externMethods.add(func);
			outputList.add(new OutputLine(indentLvl, ""));	
			outputList.add(new OutputLine(indentLvl, func.genSignature(true, false)));	
		}
	}
	
	/** generate statements to assign a rhs field reference in uvm model (sets m_cb_field) */
	protected void buildFieldCbAssign(RhsReference eRef, FieldProperties field, SystemVerilogFunction func) {
		// get root ancestor for assign
		String rootBlockName = buildBlockCbAssign(eRef, func);
		// now get the field
		String fieldStr = eRef.getFieldName();
		if (fieldStr.equals("*")) fieldStr = field.getPrefixedId();
		// now add field assignment
		String regSetPath = (eRef.getRegSetPathLength() == 0)? "" : "." + eRef.getRegSetPath();
		func.addStatement("m_cb_field = " + rootBlockName + regSetPath + "." + eRef.getRegName() + "." + fieldStr + ";");
	}

	/** generate statements to assign for a rhs reg reference in uvm model (sets m_cb_reg) */
	protected void buildRegCbAssign(RhsReference eRef, SystemVerilogFunction func) {
		// get root ancestor for assign
		String rootBlockName = buildBlockCbAssign(eRef, func);
		// now add reg assignment
		String regSetPath = (eRef.getRegSetPathLength() == 0)? "" : "." + eRef.getRegSetPath();
		func.addStatement("m_cb_reg = " + rootBlockName + regSetPath + "." + eRef.getRegName() +";");
	}

	/** generate statements to assign/cast root cb block to its specific child class and return the name of the assigned block instance variable
	 * 
	 * @param eRef - right hand assign reference
	 * @param func - function to which statements will be added
	 * @return - name of the assigned root block variable
	 */
	private String buildBlockCbAssign(RhsReference eRef, SystemVerilogFunction func) {
		int newCBDepth = eRef.getDepth() - 1;  // determine the ancestor depth of this callback reference
		// if depth changed, assign and cast a new root cb block
		if (newCBDepth != lastCBDepth) {
			// generate the ancestor class name so generic block can be cast to specific child
			RegSetProperties ancRegset = getRegSetAncestor(newCBDepth);
            String ancestorType = getUVMBlockID(ancRegset, null);
            String ancestorName = "m_root_cb_block_" + ++lastCBCount;
			func.addStatement("m_root_cb_block = this.get_ancestor(" + newCBDepth + ");"); // get root ancestor for assign (if it has changed)
			func.addPreStatement(ancestorType + " " + ancestorName + ";");
			func.addStatement("$cast(" + ancestorName + ", m_root_cb_block);");         
			lastCBDepth = newCBDepth;
			return ancestorName;  // return new ancestor name
		}
		return "m_root_cb_block_" + lastCBCount;  // no change, so use last name
	}

	/** build the build function for current virtual register */
	protected void buildVRegBuildFunction() {
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "virtual function void build();"));
		Iterator<FieldProperties> iter = fieldList.iterator();
		// traverse field list
		while (iter.hasNext()) {
			FieldProperties field = iter.next();
			String fieldId = escapeReservedString(field.getPrefixedId());  
			// create appropriate uvm_reg_vfield class   
			outputList.add(new OutputLine(indentLvl, "this." + fieldId +  " = new(\"" + field.getPrefixedId() + "\");"));  
			outputList.add(new OutputLine(indentLvl, "this." + fieldId + ".configure(this, " + field.getFieldWidth() + 
					", " + field.getLowIndex() + ");")); 
			// add any user defined properties
			addUserDefinedPropertyElements(indentLvl, field, fieldId);
		} // while		
		outputList.add(new OutputLine(--indentLvl, "endfunction: build"));
	}
	
	/** generate set_rdl_access_info call */
	protected void addHWAccessInfo(FieldProperties field) {
		String fieldId = escapeReservedString(field.getPrefixedId());  
		// function void set_rdl_access_info(bit is_sw_readable, bit is_sw_writeable, bit is_hw_readable, bit is_hw_writeable, bit has_hw_we, bit has_hw_wel)
		char isSwReadable = field.isSwReadable() ? '1' : '0';
		char isSwWriteable = field.isSwWriteable() ? '1' : '0';
		char isHwReadable = field.isHwReadable() ? '1' : '0';
		char isHwWriteable = field.isHwWriteable() ? '1' : '0';
		char hasHwWe = field.hasWriteEnableH() ? '1' : '0';
		char hasHwWel = field.hasWriteEnableL() ? '1' : '0';
		outputList.add(new OutputLine(indentLvl, "this." + fieldId + ".set_rdl_access_info(" + isSwReadable + ", " + isSwWriteable + ", " + isHwReadable + ", " + isHwWriteable + ", " + hasHwWe + ", " + hasHwWel + ");"));  
	}
	
	/** generate add_intr, add_halt calls */
	protected void addInterruptInitInfo(FieldProperties field) {
		String fieldId = escapeReservedString(field.getPrefixedId());  
		// add intr info
		//function void add_intr(int intr_level_type = 0, int intr_sticky_type = 0,
		//            string intr_sig = \"\", bit mask_intr_bits = 0)
		String intrRef = "";
	    if (field.hasRef(RhsRefType.INTR) && field.getRefRtlExpression(RhsRefType.INTR, false).startsWith("l2h_")) // only save intr ref if an input
	    	intrRef = field.getRefRtlExpression(RhsRefType.INTR, true);  
	    String maskBitsStr = (field.isMaskIntrBits()) ? "1" : "0";
		outputList.add(new OutputLine(indentLvl, "this." + fieldId + ".add_intr(" + field.getIntrType().getValue() + ", " + field.getIntrStickyType().getValue() + 
				", \"" + intrRef + "\", " + maskBitsStr + ");"));  
		
		// if halt, add info
		//function void add_halt();")
		if (field.isHalt()) {
			outputList.add(new OutputLine(indentLvl, "this." + fieldId + ".add_halt();"));  
		}
	}

	/** add user defined property assigns (for fields) */
	protected void addUserDefinedPropertyElements(int indentLvl, FieldProperties instProperties, String instName) {
		if (!instProperties.hasUserDefinedProperties()) return;  // done if no external properties
		PropertyList pList = instProperties.getUserDefinedProperties();
		for (String name : pList.getProperties().keySet()) {
			String value = (pList.getProperty(name) == null)? "" : pList.getProperty(name);
			outputList.add(new OutputLine(indentLvl, "this." + instName +  ".add_def_property(\"" + name + "\", \"" + value + "\");")); 
		}
	}
	
	/** add user defined property assigns (for regs, vregs, regsets) */
	protected void addUserDefinedPropertyElements(String parentID, InstanceProperties instProperties, String instName) {
		if (!instProperties.hasUserDefinedProperties()) return;  // done if no external properties
		PropertyList pList = instProperties.getUserDefinedProperties();
		for (String name : pList.getProperties().keySet()) {
			String value = (pList.getProperty(name) == null)? "" : pList.getProperty(name);
			subcompBuildList.addStatement(parentID, "  this." + instName + ".add_def_property(\"" + name + "\", \"" + value + "\");"); 
		}
	}
//				subcompBuildList.addStatement(parentID, "  this." + regId + "[i].set_external(1);");
	
	/** generate add_incr, add_decr calls */
	protected void addCounterInitInfo(FieldProperties field) {
		String fieldId = escapeReservedString(field.getPrefixedId());  
		// if incrementing counter, add info
		if (field.isIncrCounter()) {
			// create add_incr calls
			String incrValueStr = "0";
			RegNumber value = field.getIncrValue();
			if ((value != null) && (value.isGreaterThan(new RegNumber(0))) )   
				incrValueStr = value.toFormat(RegNumber.NumBase.Dec, RegNumber.NumFormat.Int);
			String incrRef = "";
		    if (field.hasRef(RhsRefType.INCR) && field.getRefRtlExpression(RhsRefType.INCR, false).startsWith("l2h_")) // only save incr if an input signal
		    	incrRef = field.getRefRtlExpression(RhsRefType.INCR, true);  
			String incrValueRef = "";
		    if (field.hasRef(RhsRefType.INCR_VALUE) && field.getRefRtlExpression(RhsRefType.INCR_VALUE, false).startsWith("rg_")) // only save incrvalue ref if a field value
		    	incrValueRef = field.getRefRtlExpression(RhsRefType.INCR_VALUE, true); 
		    String incrValueWidthStr = (field.getIncrWidth() != null)? ", " + field.getIncrWidth() : ", 0"; 
			outputList.add(new OutputLine(indentLvl, "this." + fieldId + ".add_incr(" + incrValueStr + ", \"" + incrRef + "\", \"" + incrValueRef + "\"" + incrValueWidthStr + ");")); 
			
			// create add_incr_sat call
			value = field.getIncrSatValue();
			if (value != null) {
				//value.se
				String satValueStr = value.toFormat(RegNumber.NumBase.Dec, RegNumber.NumFormat.Verilog);
				String incrSatValueRef = "";
			    if (field.hasRef(RhsRefType.INCR_SAT_VALUE) && field.getRefRtlExpression(RhsRefType.INCR_SAT_VALUE, false).startsWith("rg_")) // only save incrsat ref if a field value
			    	incrSatValueRef = field.getRefRtlExpression(RhsRefType.INCR_SAT_VALUE, true);  
				outputList.add(new OutputLine(indentLvl, "this." + fieldId + ".add_incr_sat(" + satValueStr + ", \"" + incrSatValueRef + "\");")); 
			}
			
			// create add_incr_thold call
			value = field.getIncrTholdValue();
			if (value != null) {
				String tholdValueStr = value.toFormat(RegNumber.NumBase.Dec, RegNumber.NumFormat.Verilog);
				String incrTholdValueRef = "";
			    if (field.hasRef(RhsRefType.INCR_THOLD_VALUE) && field.getRefRtlExpression(RhsRefType.INCR_THOLD_VALUE, false).startsWith("rg_")) // only save incrthold ref if a field value
			    	incrTholdValueRef = field.getRefRtlExpression(RhsRefType.INCR_THOLD_VALUE, true);  
				outputList.add(new OutputLine(indentLvl, "this." + fieldId + ".add_incr_thold(" + tholdValueStr + ", \"" + incrTholdValueRef + "\");"));
			}		
		}
		// if decrementing counter, add info
		if (field.isDecrCounter()) {
			// create add_decr calls
			String decrValueStr = "0";
			RegNumber value = field.getDecrValue();
			if ((value != null) && (value.isGreaterThan(new RegNumber(0))) )   
				decrValueStr = value.toFormat(RegNumber.NumBase.Dec, RegNumber.NumFormat.Int);
			String decrRef = "";
		    if (field.hasRef(RhsRefType.DECR) && field.getRefRtlExpression(RhsRefType.DECR, false).startsWith("l2h_")) // only save intr ref if an input
		    	decrRef = field.getRefRtlExpression(RhsRefType.DECR, true);  
			String decrValueRef = "";
		    if (field.hasRef(RhsRefType.DECR_VALUE) && field.getRefRtlExpression(RhsRefType.DECR_VALUE, false).startsWith("rg_")) // only save decrvalue ref if a field value
		    	decrValueRef = field.getRefRtlExpression(RhsRefType.DECR_VALUE, true);  
		    String decrValueWidthStr = (field.getDecrWidth() != null)? ", " + field.getDecrWidth() : ", 0"; 
			outputList.add(new OutputLine(indentLvl, "this." + fieldId + ".add_decr(" + decrValueStr + ", \"" + decrRef + "\", \"" + decrValueRef + "\"" + decrValueWidthStr + ");")); 
			
			// create add_decr_sat call
			value = field.getDecrSatValue();
			if (value != null) {
				String satValueStr = value.toFormat(RegNumber.NumBase.Dec, RegNumber.NumFormat.Verilog);
				String decrSatValueRef = "";
			    if (field.hasRef(RhsRefType.DECR_SAT_VALUE) && field.getRefRtlExpression(RhsRefType.DECR_SAT_VALUE, false).startsWith("rg_")) // only save decrsat ref if a field value
			    	decrSatValueRef = field.getRefRtlExpression(RhsRefType.DECR_SAT_VALUE, true);  
				outputList.add(new OutputLine(indentLvl, "this." + fieldId + ".add_decr_sat(" + satValueStr + ", \"" + decrSatValueRef + "\");")); 
			}
			
			// create add_decr_thold call
			value = field.getDecrTholdValue();
			if (value != null) {
				String tholdValueStr = value.toFormat(RegNumber.NumBase.Dec, RegNumber.NumFormat.Verilog);
				String decrTholdValueRef = "";
			    if (field.hasRef(RhsRefType.DECR_THOLD_VALUE) && field.getRefRtlExpression(RhsRefType.DECR_THOLD_VALUE, false).startsWith("rg_")) // only save decrthold ref if a field value
			    	decrTholdValueRef = field.getRefRtlExpression(RhsRefType.DECR_THOLD_VALUE, true);  
				outputList.add(new OutputLine(indentLvl, "this." + fieldId + ".add_decr_thold(" + tholdValueStr + ", \"" + decrTholdValueRef + "\");"));
			}		
		}
		
	}
	
	/** build the virtual build function for current block
	 * @param uvmBlockClassName - name of current uvm block class
	 * @param block - block id used for structure lookup
	 * @param mapWidthOverrride - if non-null, the created default map for this block will use specified value
	 * @param isMemWrapper - if true, created block is a wrapper so will avoid regSetProperty references
	 */
	protected void buildBlockBuildFunction(String uvmBlockClassName, String block, Integer mapWidthOverride, boolean isMemWrapper) {
		outputList.add(new OutputLine(indentLvl, "")); 	
		outputList.add(new OutputLine(indentLvl++, "virtual function void build();"));
		// set access width of block to max of full addrmap by default (<MAX_REG_BYTE_WIDTH> will be replaced with final max value)
		String byteWidthString = "<MAX_REG_BYTE_WIDTH>"; 
		if (mapWidthOverride != null) byteWidthString = mapWidthOverride.toString();
		
		// if base block then include in map with base address offset and set hdl path
		Boolean isBaseBlock = block.equals("");
		String endianness = "UVM_LITTLE_ENDIAN";
		if (isBaseBlock) {
			String addr = "`UVM_REG_ADDR_WIDTH" + ExtParameters.getPrimaryBaseAddress().toFormat(RegNumber.NumBase.Hex, RegNumber.NumFormat.NoLengthVerilog);
			OutputLine oLine = new OutputLine(indentLvl, "this.default_map = create_map(\"\", " + addr + ", " + byteWidthString + ", " + endianness + ", 1);");
			oLine.setHasTextReplacements(true);
			outputList.add(oLine);
			outputList.add(new OutputLine(indentLvl, "this.set_rdl_address_map(1);"));  			
			outputList.add(new OutputLine(indentLvl, "this.set_rdl_address_map_hdl_path({`" + getParentAddressMapName().toUpperCase() + "_PIO_INSTANCE_PATH, \".pio_logic\"});"));  			
		}
		else {
			OutputLine oLine = new OutputLine(indentLvl, "this.default_map = create_map(\"\", 0, " + byteWidthString + ", " + endianness + ", 1);");
			oLine.setHasTextReplacements(true);
			outputList.add(oLine);
		}
		
		// add subcomponent build statements
		List<SpecialLine> buildList = subcompBuildList.getStatements(block);
		if (buildList != null) {
			Iterator<SpecialLine> iter = buildList.iterator();
			// traverse subcomponent list
			while (iter.hasNext()) {
				SpecialLine line = iter.next();
				line.setIndent(indentLvl);
				outputList.add(line);
			}  
		}
		// need to add a callback assignment for each reg in an alias group 
		List<SpecialLine> cbsList = regCbsAssignStatements.getStatements(block);
		// verify list exists (null if block has no directly instanced regs)
		if (cbsList != null) {
			Iterator<SpecialLine> iter = cbsList.iterator();
			while (iter.hasNext()) {
				SpecialLine line = iter.next();
				line.setIndent(indentLvl);
				if (line.isInAliasGroup()) {
					// if special tag line is detected
					if (line.getLine().equals("GENERATE ALIAS GROUP")) {
						// create the aliased register list
						HashSet<String> aliasGrp = aliasGroups.getGroup(uvmBlockClassName, line.getBaseRegId());
						outputList.add(new OutputLine(indentLvl, "alias_group.delete();"));
						Iterator<String> regIter = aliasGrp.iterator();
						while (regIter.hasNext()) {
							outputList.add(new OutputLine(indentLvl, "alias_group.push_back(this." + regIter.next() + ");"));
						}
		
					}
					else outputList.add(line);  // add define to output if in an alias group
				}
			}	// while	
		}
				
		// now that all subcomponents have been built, add callbacks
		if (!isMemWrapper && isBaseBlock)
			outputList.add(new OutputLine(indentLvl, "this.add_callbacks();"));
		
		outputList.add(new OutputLine(--indentLvl, "endfunction: build"));
	}
	
	/** return "1" if this is the only field so can be accessed individually */
	protected String isOnlyField() {
		return (fieldList.size() == 1) ? "1" : "0";
	}

	/** generate field reset string and has_reset indication 
	 * @param field */
	protected String getFieldResetParameters(FieldProperties field) {
		String retStr = ExtParameters.uvmregsSkipNoResetDbUpdate()? "0, 1" : "0, 0";  // if skip db update then config with a reset and remove after, else default to no reset
		if (field.getReset() != null) {
			RegNumber resetVal = new RegNumber(field.getReset());
			if (resetVal.isDefined()) {
				retStr = resetVal.toFormat(RegNumber.NumBase.Hex, RegNumber.NumFormat.Verilog) + ", 1";
			}
		}
		return retStr;
	}

	/** return true if field has no reset and skip db update is specified  
	 * @param field */
	protected boolean fieldNeedsResetRemoval(FieldProperties field) {
		if (!ExtParameters.uvmregsSkipNoResetDbUpdate()) return false;
		if (field.getReset() != null) {
			RegNumber resetVal = new RegNumber(field.getReset());
			if (resetVal.isDefined()) return false;
		}
		return true;
	}
	
	/** generate field access type string */
	protected String getRegAccessType() {
		String accessMode = "RO";
		if (regProperties.isSwWriteable()) {
			accessMode = regProperties.isSwReadable() ? "RW" : "WO";
		}
		return accessMode;
	}

	/** generate field access type string 
	 * @param field */
	protected String getFieldAccessType(FieldProperties field) {
		String accessMode = "RO";
		// if read clear
		if (field.isRclr()) {
			if (field.isWoset()) accessMode = "W1SRC"; 
			else if (field.isSwWriteable()) accessMode = "WRC";
		    else accessMode = "RC";
		}
		// if read set
		else if (field.isRset()) {
			if (field.isWoclr()) accessMode = "W1CRS"; 
			else if (field.isSwWriteable()) accessMode = "WRS";
		    else accessMode = "RS";
		}
		// no read set/clr
		else {
			if (field.isWoclr()) accessMode = "W1C";   
			else if (field.isWoset()) accessMode = "W1S";
			else if (field.isSwWriteable()) {
				if (field.isSwReadable()) accessMode = "RW"; 
				else accessMode = "WO";
			}
		}
		return accessMode;
	}
	
    // ------------------------ inner classes ---------------------------
	
	/** class of statement lists for a uvm_block */
    protected class subComponentLists {
    	private HashMap<String,List<SpecialLine>> subCompList = new HashMap<String,List<SpecialLine>>();   // hashmap of subcomponent statements
    	
    	protected void addStatement(String block, String statement) {
    		// if this hash isnt used then create a new list
    		if (!subCompList.containsKey(block)) {
    			subCompList.put(block, new ArrayList<SpecialLine>());
    		}
    		// add to the list
    		subCompList.get(block).add(new SpecialLine(statement));
    	}
    	
    	protected void addStatement(String block, String statement, String cbsId, String regId, String aliasedId) {
    		// if this hash isnt used then create a new list
    		if (!subCompList.containsKey(block)) {
    			subCompList.put(block, new ArrayList<SpecialLine>());
    		}
    		// add to the list
    		subCompList.get(block).add(new SpecialLine(0, statement, cbsId, regId, aliasedId));
    	}
    	
    	
    	protected List<SpecialLine> getStatements(String block) {
    		// if this hash isnt used then create a new list
    		if (subCompList.containsKey(block)) {
    			return subCompList.get(block);
    		}
    		return null;
    	}
    }
    
    /** class of alias regs in a group */
    protected class AliasGroup {
    	private HashMap<String, HashSet<String>> group = new HashMap<String, HashSet<String>>();
       	
    	// return true if alias group exists for this base reg
    	protected boolean baseRegExists(String id) {
    		return group.containsKey(id);
    	}
 
		/** get group
		 *  @return an alias group for the given base register
		 */
    	protected HashSet<String> getGroup(String baseReg) {
			if (group.containsKey(baseReg)) return group.get(baseReg);
			return null;
		}

		/** add add a new alias group
		 *  @param baseReg - name of base register being aliased 
		 */
    	protected void addNewGroup(String baseReg) {
			//System.out.println("UVMRegsBuilder:    added base reg:" + baseReg);
			HashSet<String> newGrp = new HashSet<String>();
			newGrp.add(baseReg);  // also add the base reg to the set
			group.put(baseReg, newGrp);
		}
		
		/** add add a new register to an alias group
		 *  @param baseReg - name of base register being aliased 
		 *  @param aliasReg - name of register being added 
		 */
    	protected void addGroupReg(String baseReg, String aliasReg) {
			// create the alias group if it doesn't exist
			if (!group.containsKey(baseReg)) addNewGroup(baseReg);
			//System.out.println("   added alias reg:" + aliasReg);
			group.get(baseReg).add(aliasReg);
		}
    }
     
    /** class of all alias groups in a block/reg 
     *    structure is loaded during instance unroll - only valid at block completion */
    protected class AliasGroups {
    	HashMap<String, AliasGroup> groups = new HashMap<String, AliasGroup>();
    	
    	// return true if alias group exists for this block
    	protected boolean blockExists(String id) {
    		return groups.containsKey(id);
    	}
    	
    	/** return true if alias group exists for this block/reg */
		public boolean baseRegExists(String id, String reg) {
			boolean retVal = false;
			if (blockExists(id)) {
				AliasGroup grp = groups.get(id);
				if (grp.baseRegExists(reg)) retVal = true;
			}
			//System.out.println("UVMRegsBuilder: baseRegExists: blk id=" + id + ", reg=" + reg + ", exists=" + retVal);
			return retVal;
		}

		/** get group of registers for block/base reg
		 *  @param block - name of block  
		 *  @param baseReg - name of base register being aliased 
		 */
		protected HashSet<String> getGroup(String id, String reg) {
			if (groups.containsKey(id)) {
				AliasGroup grp = groups.get(id);
				if ((reg != null) && grp.baseRegExists(reg)) return grp.getGroup(reg);  
			}
			return new HashSet<String>();
		}
    	
		/** add add a new block alias group
		 *  @param block - name of block  
		 */
		protected void addNewGroup(String id) {
			//System.out.println("UVMRegsBuilder:    added block:" + id);
			AliasGroup newGrp = new AliasGroup();
			groups.put(id, newGrp);
		}
		
		/** add add a new register to an alias group
		 *  @param block - name of block  
		 *  @param baseReg - name of base register being aliased 
		 *  @param aliasReg - name of register being added 
		 */
		protected void addGroupReg(String id, String baseReg, String aliasReg) {
			//System.out.println("UVMRegsBuilder: alias group add blk=" + id + ", reg=" + baseReg + ", alias=" + aliasReg);
			// create the alias group if it doesn't exist
			if (!groups.containsKey(id)) addNewGroup(id);
			groups.get(id).addGroupReg(baseReg, aliasReg);
		}
    	
    }
    

    /** special line class for post-processing output
     * in addition to line holds info for alias list generation at block finish (once groups are known) */
    protected class SpecialLine extends OutputLine {
    	private String blockId;
    	private String RegId;
    	private String AliasedId;
    	
		/**
		 * @param indent
		 * @param line
		 * @param reg 
		 * @param block 
		 */
		public SpecialLine(int indent, String line, String block, String reg, String aliased) {
			super(indent, line);
			setSpecialType();  // make as an overloaded line
			this.setBlockId(block);
			this.setRegId(reg);
			this.setAliasedId(aliased);
			//System.out.println("UVMRegsBuilder: special line: " + line + ", blk=" + block + ", reg=" + reg + ", alias=" + aliased);
		}
		
		public SpecialLine(String line) {
			this(0, line, null, null, null);
		}

		/** return the base register id of this register */
		public String getBaseRegId() {
			// aliased id if it exists
			return (hasAliasedId() ? getAliasedId() : getRegId());
		}

		/** return true if block/register is in an alias group */
		public boolean isInAliasGroup() {
			return ( hasAliasedId()	|| aliasGroups.baseRegExists(getBlockId(), getRegId()));
		}
		
		/** return true if register has a valid aliased id */
		public boolean hasAliasedId() {
			return ((this.getAliasedId() != null) && (!this.getAliasedId().isEmpty()));
		}

		/** get blockId
		 *  @return the blockId
		 */
		public String getBlockId() {
			return blockId;
		}

		/** set blockId
		 *  @param blockId the blockId to set
		 */
		public void setBlockId(String blockId) {
			this.blockId = blockId;
		}

		/** get regId
		 *  @return the regId
		 */
		public String getRegId() {
			return RegId;
		}

		/** set regId
		 *  @param regId the regId to set
		 */
		public void setRegId(String regId) {
			RegId = regId;
		}

		/** get aliasedId
		 *  @return the aliasedId
		 */
		public String getAliasedId() {
			return AliasedId;
		}

		/** set aliasedId
		 *  @param aliasedId the aliasedId to set
		 */
		public void setAliasedId(String aliasedId) {
			AliasedId = aliasedId;
		}

    }
    //---------------------------- output write methods ----------------------------------------

	@Override
	public void write(BufferedWriter bw) {
		bufferedWriter = bw;
		
		// create text replacement set to set final max width  
    	HashMap<String, String> repStrings = new HashMap<String, String>();
    	repStrings.put("<MAX_REG_BYTE_WIDTH>", String.valueOf(this.getMaxRegByteWidth())); // replace with max reg size   	
    	OutputLine.setReplacements(repStrings);

		// write the output for each output group
		for (OutputLine rLine: outputList) {
			writeStmt(rLine.getIndent(), rLine.getLine());  
		}
		
		// write any extern methods	
		for (SystemVerilogFunction func: externMethods)
			writeStmts(func.genOutputLines(indentLvl, true));  

	}
	
	/** write pkg output to specified output file  
	 * @param outName - output file or directory
	 * @param description - text description of file generated
	 * @param commentPrefix - comment chars for this file type */
	public void writePkg(String outName, String description) {
    	BufferedWriter bw = openBufferedWriter(outName, description);
    	if (bw != null) {
	    	writePkg(bw);
    		closeBufferedWriter(bw);
    	}
	}
	
	/** write the package definition statements */
	public void writePkg(BufferedWriter bw) {
		bufferedWriter = bw;

		writeStmt(0, "//   Ordt " + Ordt.getVersion() + " autogenerated file ");
		writeStmt(0, "//   Date: " + new Date());
		writeStmt(0, "//");
		writeStmt(0, "");
		
		// generate the package output
		buildRdlPackage();
		
		// write the output for each output group
		for (OutputLine rLine: pkgOutputList) {
			writeStmt(rLine.getIndent(), rLine.getLine());  
		}
	}

	/** generate package info (overridden by child uvm builder classes) */
	protected void buildRdlPackage() {
		UVMRdlClasses.buildRdlPackage(pkgOutputList, 0);		
	}
}
