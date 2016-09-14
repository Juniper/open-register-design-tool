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
import ordt.output.RegSetProperties;
import ordt.output.RhsReference;
import ordt.output.FieldProperties.RhsRefType;
import ordt.parameters.ExtParameters;

public class UVMRegsBuilder extends OutputBuilder {

	private List<OutputLine> outputList = new ArrayList<OutputLine>();
	private List<OutputLine> pkgOutputList = new ArrayList<OutputLine>();  // define a separate list for package info
	private subComponentLists subcompDefList = new subComponentLists();   // lists of subcomponent define statements (per block)
	private subComponentLists subcompBuildList = new subComponentLists();  // lists of subcomponent build statements (per block)
	private subComponentLists subcompAddrCoverGroupList = new subComponentLists();  // lists of subcomponent address covergroup statements (per block)
	private subComponentLists regCbsDefineStatements = new subComponentLists();  // list of register callback defines (per block)
	private subComponentLists regCbsAssignStatements = new subComponentLists();  // list of register callback assign statements (per block)
	
	//private PriorityQueue<FieldProperties> fieldList;   // fields in current register
	private int indentLvl = 0;
	
	private static HashSet<String> reservedWords = getReservedWords();
	
	private AliasGroups aliasGroups = new AliasGroups();
	
	private Stack<Boolean> regSetHasCallback = new Stack<Boolean>();  // stack of flags indicating block needs add_callback overridden
	
	// search state for use in field callbacks
	private static int lastCBDepth = -1;
	private static String lastCBRegSetPath = "<null>";
	private static String lastCBReg = "<null>";

    //---------------------------- constructor ----------------------------------

    public UVMRegsBuilder(RegModelIntf model) {
	    this.model = model;  // store the model ref
	    setVisitEachReg(false);   // only need to call once for replicated reg groups
	    setVisitEachRegSet(false);   // only need to call once for replicated reg set groups
	    setVisitExternalRegisters(true);  // we will visit externals 
	    setVisitEachExternalRegister(false);	    // handle externals as a group
	    setAllowLocalMapInternals(true);  // cascaded addrmaps will result in local non-ext regions   
	    model.getRoot().generateOutput(null, this);   // generate output structures recursively starting at model root
    }

    /** load systemverilog reserved words to be escaped */
	private static HashSet<String> getReservedWords() {
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
		reservedWords.add("reg");
		reservedWords.add("repeat");
		reservedWords.add("table");
		reservedWords.add("time");
		reservedWords.add("type");
		return reservedWords;
	}
	
    /** escape string if a reserved words  */
	private String escapeReservedString(String word) {
		if (reservedWords.contains(word)) return ("\\" + word + " ");
		return word;
	}

    //---------------------------- OutputBuilder methods to load structures ----------------------------------------

	@Override
	public void addSignal() {
		//System.out.println("Signal: " + signalProperties.getInstancePath());
	}

	@Override
	public void addField() {
		//System.out.println("UVMRegsBuilder: addField id=" + fieldProperties.getId() + ", reset=" + fieldProperties.getReset());
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
		
		// only add the register if it is sw accessible
		if (regProperties.isSwWriteable() || regProperties.isSwReadable()) {
			//if (regProperties.isReplicated()) System.out.println("UVMRegsBuilder finishRegister: replicated reg id=" + regProperties.getId() + ", reps=" + regProperties.getRepCount() + ", thold=" + ExtParameters.uvmregsIsMemThreshold());
			// if a memory, then add info to parent uvm_reg_block
			if (regProperties.isMem() || (regProperties.getRepCount() >= ExtParameters.uvmregsIsMemThreshold())) {   // check is_mem threshold vs reps
				//System.out.println("UVMRegsBuilder finishRegister: replicated MEM reg id=" + regProperties.getId() + ", reps=" + regProperties.getRepCount() + ", thold=" + ExtParameters.uvmregsIsMemThreshold());
                // if debug mode 3 then no wrapper created (old behavior - bad address stride generated in uvm1.1d)
				if (ExtParameters.getDebugMode() == 3) {
					// save info for this memory and virtual regs to be used in parent uvm_reg_block
					saveMemInfo(false);  // no wrapper used
					// build the virtual register class definition
					buildVRegClass();  									
                }
				// otherwise default behavior - create a wrapper block with same create_map width as mem and encapsulate mem/vreg
				else {
					// save wrapper block in parent
					String newBlockName = regProperties.getId();  // use reg name as wrapper block name
					saveRegSetInfo(newBlockName, regProperties.getRelativeBaseAddress()); 
					// save info for this memory and virtual regs in the wrapper
					saveMemInfo(true);  // save info in wrapper block
					// build the virtual register class definition
					buildVRegClass();  									
                	// build the wrapper using reg byte width in map and reg id as suffix
					buildMemWrapperBlockClass(newBlockName, regProperties.getRegByteWidth());
					//System.out.println("UVMRegsBuilder finishRegister wrapper: " + regProperties.getInstancePath() + ", newBlockName=" + newBlockName);
				}
			}
			// otherwise model as a register
			else {
				// save info for this register to be used in parent uvm_reg_block
			    saveRegInfo();
				// build the register class definition
				buildRegClass();  				
			}
		}
	}

	@Override
	public void addRootExternalRegisters() {
		//System.out.println("Root external registers: " + regProperties.getInstancePath() + ", base=" + getExternalBaseAddress());
	}

	@Override
	public void addRegSet() {
		regSetHasCallback.push(false);
	}

	@Override
	public void finishRegSet() {    
		// save info for this register set to be used in parent uvm_reg_block (do in finishRegSet so size is computed) - no overrides
		saveRegSetInfo(null, null);
		// build the block class definition
		Boolean hasCallback = regSetHasCallback.pop();
		buildBlockClass(hasCallback);  
		// parent also needs override if has callack
		//if (regSetProperties.isRootInstance()) System.out.println("UVMRegsBuilder finishRegSet: root instance detected, id=" + regSetProperties.getId() + ", addrmap=" + getAddressMapName());
		if (hasCallback && !regSetHasCallback.isEmpty()) {
			regSetHasCallback.pop();
			regSetHasCallback.push(true);
		}
	}

	/** process root address map */
	@Override
	public void addRegMap() { 
		regSetHasCallback.push(false);
		outputList.add(new OutputLine(indentLvl, "import uvm_pkg::*;"));
		outputList.add(new OutputLine(indentLvl, "import ordt_uvm_reg_pkg::*;"));
	}

	/** finish root address map  */
	@Override
	public  void finishRegMap() {	
		// build the register class definition
		Boolean hasCallback = regSetHasCallback.pop();
		buildBaseBlockClass(hasCallback);  
	}

    //---------------------------- helper methods saving child info for parent build --------------------------------------

	/** save register info for use in parent uvm_reg_block class */
	private void saveRegInfo() {
		// get parent name
		String parentID = this.getParentInstancePath().replace('.', '_');
		// escape id and alias names
		String regId = escapeReservedString(regProperties.getId());
		//String aliasedId = escapeReservedString(regProperties.getAliasedId());
		// save register define statements
		String repStr = (regProperties.isReplicated()) ? "[" + regProperties.getRepCount() + "]" : "";
		subcompDefList.addStatement(parentID, "rand " + getUVMRegID() + " " + regId + repStr + ";");
		
		// save cbs define statements (may not be needed - determine in block finish once alias groups are known)
		regCbsDefineStatements.addStatement(parentID, "rdl_alias_reg_cbs " + getUVMRegCbsID() + ";", getUVMBlockID(), regProperties.getId(), regProperties.getAliasedId());
		// save cbs assign statements
		String cbsName = getUVMRegCbsID();
		regCbsAssignStatements.addStatement(parentID, "GENERATE ALIAS GROUP", getUVMBlockID(), regProperties.getId(), regProperties.getAliasedId()); // special line  
		regCbsAssignStatements.addStatement(parentID, cbsName + " = new(\"" + cbsName + "\");", getUVMBlockID(), regProperties.getId(), regProperties.getAliasedId());  
		regCbsAssignStatements.addStatement(parentID, cbsName + ".set_alias_regs(alias_group);", getUVMBlockID(), regProperties.getId(), regProperties.getAliasedId());  
		regCbsAssignStatements.addStatement(parentID, "uvm_reg_cb::add(" + regProperties.getId() + ", " + cbsName + ");", getUVMBlockID(), regProperties.getId(), regProperties.getAliasedId());  

		// save register coverage statements
		if (ExtParameters.uvmregsIncludeAddressCoverage()) addRegToAddrCoverageList(parentID, regId, false);  // false = not in a uvm_mem wrapper
		// save register build statements
		addRegToBuildList(parentID, regId);
		//if (regProperties.isAlias()) System.out.println("UVMRegsNuilder: alias reg hdl using " + regProperties.getAliasedId() + " not " + regProperties.getId());
		// if an alias register build a cbs class and register callback   
		if (regProperties.isAlias()) {  // also need isAliased()   // just load alias/aliased register structure by block here
			aliasGroups.addGroupReg(getUVMBlockID(), regProperties.getAliasedId(), regProperties.getId());  
		}
		// issue warning if no category defined
		if (!regProperties.hasCategory() && !ExtParameters.uvmregsSuppressNoCategoryWarnings()) 
			Ordt.warnMessage("register " + regProperties.getInstancePath() + " has no category defined");
		//System.out.println("UVMBuild saveRegInfo: " + regProperties.getBaseName() + ", parent=" + parentID + ", rel addr=" + regProperties.getRelativeBaseAddress());
	}
	
	/** create block addr covergroup statements adding current register */
	private void addRegToAddrCoverageList(String parentID, String regId, boolean parentIsWrapper) {
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
	private void addRegToBuildList(String parentID, String regId) {
		// set hdl path component
		String hdlPath = regProperties.isAlias() ? regProperties.getAliasedId() : regProperties.getId();
		String addr = "`UVM_REG_ADDR_WIDTH" + regProperties.getRelativeBaseAddress().toFormat(RegNumber.NumBase.Hex, RegNumber.NumFormat.NoLengthVerilog);
		if (regProperties.isReplicated()) {
			subcompBuildList.addStatement(parentID, "foreach (this." + regId + "[i]) begin");
			subcompBuildList.addStatement(parentID, "  this." + regId + "[i] = new($psprintf(\"" + regProperties.getId() + " [%0d]\",i));");  
			subcompBuildList.addStatement(parentID, "  this." + regId + "[i].configure(this, null, \"\");");  
			subcompBuildList.addStatement(parentID, "  this." + regId + "[i].set_rdl_tag($psprintf(\"" + hdlPath + "_%0d_\",i));");
			if (regProperties.isExternal()) 
				subcompBuildList.addStatement(parentID, "  this." + regId + "[i].set_external(1);");
			subcompBuildList.addStatement(parentID, "  this." + regId + "[i]" + getUvmRegTestModeString());  
			// add any user defined properties
			addUserDefinedPropertyElements(parentID, regProperties, regId + "[i]");
			subcompBuildList.addStatement(parentID, "  this." + regId + "[i].build();");
			subcompBuildList.addStatement(parentID, "  this.default_map.add_reg(this." + regId + "[i], " + addr + "+i*" + getRegAddrIncr() + ", \"" + getRegAccessType() + "\", 0);");						
			subcompBuildList.addStatement(parentID, "end");
		}
		else {
  		   subcompBuildList.addStatement(parentID, "this." + regId + " = new(\"" + regProperties.getId() + "\");");  
		   subcompBuildList.addStatement(parentID, "this." + regId + ".configure(this, null, \"\");"); 
		   subcompBuildList.addStatement(parentID, "this." + regId + ".set_rdl_tag(\"" + hdlPath + "_\");");
		   if (regProperties.isExternal()) 
			   subcompBuildList.addStatement(parentID, "this." + regId + ".set_external(1);");
		   subcompBuildList.addStatement(parentID, "this." + regId + getUvmRegTestModeString());  
			// add any user defined properties
			addUserDefinedPropertyElements(parentID, regProperties, regId);
		   subcompBuildList.addStatement(parentID, "this." + regId + ".build();");
		   subcompBuildList.addStatement(parentID, "this.default_map.add_reg(this." + regId + ", " + addr + ", \"" + getRegAccessType() + "\", 0);");			
		}	
	}

	/* return a string with a call to set_reg_test_info */
	private String getUvmRegTestModeString() {
		String dontTestBit = regProperties.isDontTest()? "1" : "0";
		String dontCompareBit = regProperties.isDontCompare()? "1" : "0";
		return ".set_reg_test_info(" + dontTestBit + ", " + dontCompareBit + ", " + regProperties.getCategory().getValue() + ");";
	}

	/** save memory info for use in parent uvm_reg_block class
	 * @param parentIsWrapper - if true, info will be saved to a wrapper block and reg/mem instance names will be changed
	 */
	private void saveMemInfo(boolean parentIsWrapper) {
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
	private void addMemToBuildList(String parentID, String memId, String escapedRegId) {
		// save mem build statements for uvm_reg_block
		subcompBuildList.addStatement(parentID, "this." + memId + " = new(\"" + memId + "\", " + regProperties.getRepCount() + ", " + regProperties.getRegWidth() + ");");  
		subcompBuildList.addStatement(parentID, "this." + memId + ".configure(this);");  
		
		// compute a reg level reset value from fields (null if no reset fields)
		RegNumber reset = getVRegReset();
		
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

	/** compute a register level reset from the current list of fields */
	private RegNumber getVRegReset() {
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
	 * @param blockNameOverride - if non null, specified name will be used as the block instance rather then regset id
	 *        - regSetProperties (replication, is addrmap) will be ignored if a name override is specified
	 *        (this is used for uvm_mem wrapper block gen)
	 * @param addrOffsetOverride - if non null, specified address offset will be used rather than current regset offset
	 */
	private void saveRegSetInfo(String blockNameOverride, RegNumber addrOffsetOverride) {
		// get parent name
		String parentID = this.getParentInstancePath().replace('.', '_');
		// block id
		boolean hasNameOverride = (blockNameOverride != null);
		String blockId = hasNameOverride? blockNameOverride : regSetProperties.getId();
		// escaped block id 
		String escapedBlockId = escapeReservedString(blockId);
		// save block define statements
		String repStr = (!hasNameOverride && regSetProperties.isReplicated()) ? "[" + regSetProperties.getRepCount() + "]" : "";
		subcompDefList.addStatement(parentID, "rand " + getUVMBlockID(blockNameOverride) + " " + escapedBlockId + repStr + ";");
		// set address offset for new block
		RegNumber addr = (addrOffsetOverride != null)? addrOffsetOverride : regSetProperties.getRelativeBaseAddress();
		String addrStr = "`UVM_REG_ADDR_WIDTH" + addr.toFormat(RegNumber.NumBase.Hex, RegNumber.NumFormat.NoLengthVerilog);
		// save register build statements
		if (!hasNameOverride && regSetProperties.isReplicated()) {  
			subcompBuildList.addStatement(parentID, "foreach (this." + escapedBlockId + "[i]) begin");
			subcompBuildList.addStatement(parentID, "  this." + escapedBlockId + "[i] = " + getUVMBlockID(blockNameOverride) + "::type_id::create($psprintf(\"" + blockId + " [%0d]\",i),, get_full_name());");
			subcompBuildList.addStatement(parentID, "  this." + escapedBlockId + "[i].configure(this, \"\");");  
			subcompBuildList.addStatement(parentID, "  this." + escapedBlockId + "[i].set_rdl_tag($psprintf(\"" + blockId + "_%0d_\",i));");
			if (regSetProperties.isAddressMap()) {
				subcompBuildList.addStatement(parentID, "  this." + escapedBlockId + "[i].set_rdl_address_map(1);");  // tag block as an address map
				subcompBuildList.addStatement(parentID, "  this." + escapedBlockId + "[i].set_rdl_address_map_hdl_path({`" + getParentAddressMapName().toUpperCase() + "_PIO_INSTANCE_PATH, \".pio_logic\"});");  // TODO
			}
			// add any user defined properties
			addUserDefinedPropertyElements(parentID, regSetProperties, escapedBlockId + "[i]");
			subcompBuildList.addStatement(parentID, "  this." + escapedBlockId + "[i].build();");
			subcompBuildList.addStatement(parentID, "  this.default_map.add_submap(this." + escapedBlockId + "[i].default_map, " + addrStr + "+i*" + getRegSetAddrIncrString() + ");");						
			subcompBuildList.addStatement(parentID, "end");
		}
		else {
		   subcompBuildList.addStatement(parentID, "this." + escapedBlockId + " = " + getUVMBlockID(blockNameOverride) + "::type_id::create(\"" + blockId + "\",, get_full_name());");
		   subcompBuildList.addStatement(parentID, "this." + escapedBlockId + ".configure(this, \"\");"); 
		   subcompBuildList.addStatement(parentID, "this." + escapedBlockId + ".set_rdl_tag(\"" + blockId + "_\");");
		   if (!hasNameOverride && regSetProperties.isAddressMap()) {
				subcompBuildList.addStatement(parentID, "this." + escapedBlockId + ".set_rdl_address_map(1);");  // tag block as an address map
				subcompBuildList.addStatement(parentID, "this." + escapedBlockId + ".set_rdl_address_map_hdl_path({`" + getParentAddressMapName().toUpperCase() + "_PIO_INSTANCE_PATH, \".pio_logic\"});");  
		   }
		   // add any user defined properties
		   addUserDefinedPropertyElements(parentID, regSetProperties, escapedBlockId);
		   subcompBuildList.addStatement(parentID, "this." + escapedBlockId + ".build();");
		   subcompBuildList.addStatement(parentID, "this.default_map.add_submap(this." + escapedBlockId + ".default_map, " + addrStr + ");");			
		}		
		//System.out.println("UVMBuild saveRegSetInfo: " + regSetProperties.getBaseName() + ", parent=" + parentID + ", rel addr=" + regSetProperties.getRelativeBaseAddress());
	}

	/** get the increment string for this group of regs */
	private String getRegAddrIncr() {
		RegNumber incr = regProperties.getAddrStride();
		String addr = "`UVM_REG_ADDR_WIDTH" + incr.toFormat(RegNumber.NumBase.Hex, RegNumber.NumFormat.NoLengthVerilog);
		return addr;
	}

	/** get the increment string for this regset */
	private String getRegSetAddrIncrString() {
		RegNumber incr = getRegSetAddressStride(true);
		String addr = "`UVM_REG_ADDR_WIDTH" + incr.toFormat(RegNumber.NumBase.Hex, RegNumber.NumFormat.NoLengthVerilog);
		return addr;
	}
	
	// ----------------------- uvm reg class builder methods -------------------------

	/** build reg class definition for current register instance */   
	private void buildRegClass() {
		// create text name and description if null
		String id = regProperties.getId();
		String fullId = getUVMRegID();
		String textName = regProperties.getTextName();
		if (textName == null) textName = "Register " + id;
		else textName = textName.replace('\n', ' ');
		
		// generate register header 
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl, "// " + textName));
		outputList.add(new OutputLine(indentLvl++, "class " + fullId + " extends uvm_reg_rdl;")); 
		
		// tag used for rdl name generation
		outputList.add(new OutputLine(indentLvl, "string m_rdl_tag;")); 
				
		// create field definitions
		buildRegFieldDefines();
		
		// create new function
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function new(string name = \"" + fullId + "\");"));
		outputList.add(new OutputLine(indentLvl, "super.new(name, " + regProperties.getRegWidth() + ", build_coverage(UVM_NO_COVERAGE));"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: new"));

		// create build function
		buildRegBuildFunction();
		
		// create add_callbacks function
		buildRegAddCallbacksFunction();
		
		// if this register has interrupt fields, add interrupt get methods
		if (regProperties.hasInterruptFields()) buildRegIntrFunctions();
				
		// close out the register definition
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(--indentLvl, "endclass : " + fullId));
	}

	/** add interrupt field get methods */
	private void buildRegIntrFunctions() {
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
	private void buildVRegClass() {   
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
	private void buildBlockClass(Boolean hasCallback) {
		// create text name and description if null
		String id = regSetProperties.getId();
		String fullId = getUVMBlockID();
		String textName = regSetProperties.getTextName();
		if (textName == null) textName = "Block " + id;
		
		// generate register header 
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl, "// " + textName));
		outputList.add(new OutputLine(indentLvl++, "class " + fullId + " extends uvm_reg_block_rdl;")); 

		// create field definitions  
		buildBlockDefines(regSetProperties.getBaseName());
		
		// build any specified coverage defines
		buildBlockCoverageDefines(regSetProperties.getBaseName());
		
		// if this block has alias groups then add a define 
		if (aliasGroups.blockExists(getUVMBlockID())) {
			outputList.add(new OutputLine(indentLvl, "local uvm_reg alias_group[$];"));			
		}
		
		// create new function
		buildBlockNewDefine(fullId);
		
		// if child callbacks, override add_callbacks
		if (hasCallback) buildBlockAddCallbacksMethod();
		
		// create build function  
		Integer mapWidthOverride = (ExtParameters.getDebugMode() == 1)?  regSetProperties.getMaxRegByteWidth()  : null; // if debug mode 1 use regset property width
		buildBlockBuildFunction(regSetProperties.getBaseName(), mapWidthOverride, false);
		
		// close out the class definition
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl, "`uvm_object_utils(" + fullId + ")"));
		outputList.add(new OutputLine(--indentLvl, "endclass : " + fullId));
	}

	/** build uvm_mem/uvm_vreg wrapper block class definition as child of current regset block 
	 *  callbacks, aliases not allowed in this block type 
	 * @param b */   
	private void buildMemWrapperBlockClass(String nameSuffix, Integer mapWidthOverride) {
		// create block name + id with suffix
		String fullId = getUVMBlockID(nameSuffix);  // create block name with suffix
		String refId = ((regSetProperties == null) || regSetProperties.getBaseName().isEmpty()) ? nameSuffix : regSetProperties.getBaseName() + "_" + nameSuffix;  // id used for structure lookup
		String textName = "Uvm_mem wrapper block " + refId;
		
		// generate register header 
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl, "// " + textName));
		outputList.add(new OutputLine(indentLvl++, "class " + fullId + " extends uvm_reg_block_rdl;")); 

		// create field definitions  
		buildBlockDefines(refId);
		
		// build any specified coverage defines
		buildBlockCoverageDefines(refId);
		
		// create new function
		buildBlockNewDefine(fullId);
		
		// create build function ising width of underlying virtual regs/mem
		buildBlockBuildFunction(refId, mapWidthOverride, true);
		
		// close out the class definition
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl, "`uvm_object_utils(" + fullId + ")"));
		outputList.add(new OutputLine(--indentLvl, "endclass : " + fullId));
	}

	/** build block class definition for current regset instance 
	 * @param hasCallback  - indicates block children have callbacks*/ 
	private void buildBaseBlockClass(Boolean hasCallback) {
		String fullId = "block" + getAddrMapPrefix();
		String textName = "Base block";
		//if (getAddressMapName().contains("default")) System.out.println("UVMRegsBuilder buildBaseBlockClass: base block has name=" + getAddressMapName());
		
		// generate register header 
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl, "// " + textName));
		outputList.add(new OutputLine(indentLvl++, "class " + fullId + " extends uvm_reg_block_rdl;")); 

		// create field definitions  
		buildBlockDefines("");
		
		// build any specified coverage defines
		buildBlockCoverageDefines("");
		
		// if this block has alias groups then add a define 
		if (aliasGroups.blockExists(getUVMBlockID())) {
			outputList.add(new OutputLine(indentLvl, "local uvm_reg alias_group[$];"));			
		}
		
		// create new function
		buildBlockNewDefine(fullId);
		
		// if child callbacks, override add_callbacks
		if (hasCallback) buildBlockAddCallbacksMethod();
		
		// create build function (no width override) 
		buildBlockBuildFunction("", null, false);
		
		// close out the class definition
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl, "`uvm_object_utils(" + fullId + ")"));
		outputList.add(new OutputLine(--indentLvl, "endclass : " + fullId));
	}

	// ---------

	/** return address map prefix string  */
	private String getAddrMapPrefix() {
		String map = getAddressMapName();
		if (map.isEmpty()) return "";
		return "_" + map;
	}

	/** return uvm_reg class name */
	private String getUVMRegCbsID() {
		String fullId = "alias_cbs" + getAddrMapPrefix() + "_" + regProperties.getBaseName();
		return fullId;
	}

	/** return uvm_reg class name */
	private String getUVMRegID() {
		String fullId = "reg" + getAddrMapPrefix() + "_" + regProperties.getBaseName();  
		return fullId;
	}

	/** return uvm_vreg class name */
	private String getUVMVRegID() {
		String fullId = "vreg" + getAddrMapPrefix() + "_" + regProperties.getBaseName();  
		return fullId;
	}

	/** return uvm_reg_block class name
	 * @param regSetProps - currently active regset properties
	 * @param nameSuffix - optional block name suffix
	 */
	private String getUVMBlockID(RegSetProperties regSetProps, String nameSuffix) {
		String regSetStr = ((regSetProps == null) || regSetProps.getBaseName().isEmpty()) ? "" : ("_" + regSetProps.getBaseName());
		if (nameSuffix != null) regSetStr += "_" + nameSuffix;
		String fullId = "block" + getAddrMapPrefix() + regSetStr;
		if (fullId.contains("__")) System.out.println("getUVMBlockID: prefix=" + getAddrMapPrefix() + " regSetStr=" + regSetStr);
		return fullId;
	}

	/** return uvm_reg_block class name */
	private String getUVMBlockID() {
		return getUVMBlockID(regSetProperties, null);
	}

	/** return uvm_reg_block class name
	 * @param nameSuffix - optional block name suffix 
	 */
	private String getUVMBlockID(String nameSuffix) {
		return getUVMBlockID(regSetProperties, nameSuffix);
	}
	
	/** create add_callbacks methods for uvm_reg_block derived class   
	 * @param outputList - list of output lines to be updated
	 * @param indentLvl */
	private void buildBlockAddCallbacksMethod() {
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
	private void buildRegFieldDefines() {
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
	private void buildVRegFieldDefines() {
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
	private void buildRegHdlPaths() {
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
	private void buildBlockDefines(String block) {  
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
	private void buildBlockCoverageDefines(String block) {
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

	/** build new function and coverage sample methods */
	private void buildBlockNewDefine(String fullId) {
		outputList.add(new OutputLine(indentLvl, ""));	
		// if address coverage specified, add to new() and create sample
		if (ExtParameters.uvmregsIncludeAddressCoverage()) {
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
	private void buildRegBuildFunction() {
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
					", " + field.getLowIndex() + ", \"" + getFieldAccessType(field) + "\", " + isVolatile + ", " + getFieldReset(field) + 
					", " + isRand + ", " + isOnlyField() + ");"));		
			
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
		if (!regProperties.isExternal()) buildRegHdlPaths();
				
		outputList.add(new OutputLine(--indentLvl, "endfunction: build"));
	}
	
	/** build the add_callbacks function for current register 
	 *  add_callbacks is called recursively after build completes and sets field dynamic assign linkages and associated callbacks */
	private void buildRegAddCallbacksFunction() {
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "virtual function void add_callbacks();")); 
		// find any fields needing callbacks/reference assignment and define callback vars
		boolean intrMaskDetected = false;  // detect intr enable or masks
		boolean intrCascadeDetected = false;   // detect cascaded interrupt fields
		Iterator<FieldProperties> iter = fieldList.iterator();
		while (iter.hasNext()) {
			FieldProperties field = iter.next();
			if (field.isInterrupt() && (field.hasRef(RhsRefType.INTR_ENABLE) || field.hasRef(RhsRefType.INTR_MASK))) {
				intrMaskDetected = true;
				if (field.isMaskIntrBits())  // if maskintrbits then define a callback
					outputList.add(new OutputLine(indentLvl, "rdl_mask_intr_field_cbs " + field.getPrefixedId() + "_mask_cb;")); // define mask field callback
			}
			// intr cascade (callback disabled) 
			if (field.isInterrupt() && (field.hasRef(RhsRefType.NEXT) || (field.hasRef(RhsRefType.INTR) && (field.getIntrType() == FieldProperties.IntrType.LEVEL)))) {
				intrCascadeDetected = true;
				//outputList.add(new OutputLine(indentLvl, "rdl_cascade_intr_field_cbs " + field.getPrefixedId() + "_cascade_cb;")); // define cascade field callback
			}
		} 
		
		// if fields require add_callbacks() to be called,
		boolean fieldCallbackNeeded  = intrMaskDetected || intrCascadeDetected;  // detect intr bit mask using enable or mask value 
		if (fieldCallbackNeeded) {
			// define vars used for extracting reg/fields and setting references in uvm model
			outputList.add(new OutputLine(indentLvl, "uvm_reg_block m_root_cb_block;"));  // found from assign depth
			outputList.add(new OutputLine(indentLvl, "uvm_reg_block m_cb_block;"));  // find from m_root_cb_block using rhs name
			outputList.add(new OutputLine(indentLvl, "uvm_reg m_cb_reg;"));  // find from m_cb_block using reg name
			outputList.add(new OutputLine(indentLvl, "uvm_reg_field m_cb_field;"));  // find from m_cb_reg using field name
			regSetHasCallback.pop();
			regSetHasCallback.push(true);			

			// traverse field list and add any field callbacks
			lastCBDepth = -1;
			lastCBRegSetPath = "<null>";
			lastCBReg = "<null>";
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
						buildFieldSearch(eRef, field);
						
						// set the enable/mask field reference in this field
						String isEnableStr = isEnable ? "1" : "0";  
						outputList.add(new OutputLine(indentLvl, fieldId + ".set_intr_mask_field(m_cb_field, " + isEnableStr + ");"));  
						
						//System.out.println("UVMRegsBuilder buildRegAddCallbacksFunction: enable maskStr=" + maskStr + ", depth=" + eRef.getDepth() );
						// if maskintrbits then also need a callback to change mirror value
						if (field.isMaskIntrBits()) {
							outputList.add(new OutputLine(indentLvl, field.getPrefixedId() + "_mask_cb = new(\"" +
						                        field.getPrefixedId() + "_mask_cb\"," + fieldId + ");"));
							outputList.add(new OutputLine(indentLvl, "uvm_reg_field_cb::add(this." + fieldId + ", " + field.getPrefixedId() + "_mask_cb);")); 						
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
						buildFieldSearch(eRef, field);

						// set the enable/mask field reference in this field
						String isEnableStr = isEnable ? "1" : "0";  
						outputList.add(new OutputLine(indentLvl, fieldId + ".set_halt_mask_field(m_cb_field, " + isEnableStr + ");"));  
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
						buildRegSearch(eRef);

						// set the cascaded reg reference in this field
						String cascadeIsHalt = eRef.hasDeRef("halt")? "1" : "0";
						outputList.add(new OutputLine(indentLvl, fieldId + ".set_cascade_intr_reg(m_cb_reg, " + cascadeIsHalt + ");"));  
						
						/* disable callback
						//System.out.println("UVMRegsBuilder buildRegAddCallbacksFunction: cascade reg=" + eRef.getRegName() + ", depth=" + eRef.getDepth() );
						outputList.add(new OutputLine(indentLvl, field.getPrefixedId() + "_cascade_cb = new(\"" +
					                        field.getPrefixedId() + "_cascade_cb\", " + fieldId + ");")); 
						outputList.add(new OutputLine(indentLvl, "uvm_reg_field_cb::add(this." + fieldId + ", " + field.getPrefixedId() + "_cascade_cb);"));
						*/ 										
					}   
				}
				
			} // while		
		}
		outputList.add(new OutputLine(--indentLvl, "endfunction: add_callbacks"));
	}
	
	private void buildFieldSearch(RhsReference eRef, FieldProperties field) {
		// build register search statements (sets m_cb_reg)
		buildRegSearch(eRef);
		
		// now get the field
		String fieldStr = eRef.getFieldName();
		if (fieldStr.equals("*")) fieldStr = field.getId();
		outputList.add(new OutputLine(indentLvl, "m_cb_field = m_cb_reg.get_field_by_name(\"" + fieldStr +"\");"));
	}

	/** generate statements to search for a rhs reg referenc in uvm model (sets m_cb_block)*/
	private void buildRegSearch(RhsReference eRef) {
		// get root ancestor for assign if depth hasn't changed
		int newCBDepth = eRef.getDepth() - 1;
		boolean cbDepthChanged = newCBDepth != lastCBDepth;
		if (cbDepthChanged) {
			outputList.add(new OutputLine(indentLvl, "m_root_cb_block = this.get_ancestor(" + newCBDepth + ");")); // get root ancestor for assign (if it has changed)
			lastCBDepth = newCBDepth;
		}
		// now use rhs path to find leaf cb block
		String newCBRegSetPath = (eRef.getRegSetPathLength() == 0)? "" : eRef.getRegSetPath();
		boolean cbRegSetPathChanged = !newCBRegSetPath.equals(lastCBRegSetPath);
		if (cbDepthChanged || cbRegSetPathChanged) {
			if (eRef.getRegSetPathLength() == 0) outputList.add(new OutputLine(indentLvl, "m_cb_block = m_root_cb_block;"));
			else outputList.add(new OutputLine(indentLvl, "m_cb_block = uvm_reg_block::find_block(\"" + eRef.getRegSetPath() + "\", m_root_cb_block);"));
			lastCBRegSetPath = newCBRegSetPath;
		}
		// now get reg
		String newCBReg = eRef.getRegName();
		boolean cbRegChanged = (newCBReg != null) && !newCBReg.equals(lastCBReg);
		if (cbRegChanged || cbDepthChanged || cbRegSetPathChanged) {
			outputList.add(new OutputLine(indentLvl, "m_cb_reg = m_cb_block.get_reg_by_name(\"" + eRef.getRegName() +"\");"));
			lastCBReg = newCBReg;							
		}
	}

	/** build the build function for current virtual register */
	private void buildVRegBuildFunction() {
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
	private void addHWAccessInfo(FieldProperties field) {
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
	private void addInterruptInitInfo(FieldProperties field) {
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
	private void addUserDefinedPropertyElements(int indentLvl, FieldProperties instProperties, String instName) {
		if (!instProperties.hasUserDefinedProperties()) return;  // done if no external properties
		PropertyList pList = instProperties.getUserDefinedProperties();
		for (String name : pList.getProperties().keySet()) {
			String value = (pList.getProperty(name) == null)? "" : pList.getProperty(name);
			outputList.add(new OutputLine(indentLvl, "this." + instName +  ".add_def_property(\"" + name + "\", \"" + value + "\");")); 
		}
	}
	
	/** add user defined property assigns (for regs, vregs, regsets) */
	private void addUserDefinedPropertyElements(String parentID, InstanceProperties instProperties, String instName) {
		if (!instProperties.hasUserDefinedProperties()) return;  // done if no external properties
		PropertyList pList = instProperties.getUserDefinedProperties();
		for (String name : pList.getProperties().keySet()) {
			String value = (pList.getProperty(name) == null)? "" : pList.getProperty(name);
			subcompBuildList.addStatement(parentID, "  this." + instName + ".add_def_property(\"" + name + "\", \"" + value + "\");"); 
		}
	}
//				subcompBuildList.addStatement(parentID, "  this." + regId + "[i].set_external(1);");
	
	/** generate add_incr, add_decr calls */
	private void addCounterInitInfo(FieldProperties field) {
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
	 * @param block - block id used for structure lookup
	 * @param mapWidthOverrride - if non-null, the created default map for this block will use specified value
	 * @param isMemWrapper - if true, created block is a wrapper so will avoid regSetProperty references
	 */
	private void buildBlockBuildFunction(String block, Integer mapWidthOverride, boolean isMemWrapper) {
		outputList.add(new OutputLine(indentLvl, "")); 	
		outputList.add(new OutputLine(indentLvl++, "virtual function void build();"));
		// set access width of block to max of full addrmap by default (<MAX_REG_BYTE_WIDTH> will be replaced with final max value)
		String byteWidthString = "<MAX_REG_BYTE_WIDTH>"; 
		if (mapWidthOverride != null) byteWidthString = mapWidthOverride.toString();
		
		// if base block then include in base address offset
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
						HashSet<String> aliasGrp = aliasGroups.getGroup(getUVMBlockID(), line.getBaseRegId());
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
				
		// if an address map, set hdl path base
		if (!isMemWrapper && (isBaseBlock || regSetProperties.isAddressMap()))  // TODO - does this work for child addrmaps? 
			outputList.add(new OutputLine(indentLvl, "set_hdl_path_root({`" + getParentAddressMapName().toUpperCase() + "_PIO_INSTANCE_PATH, \".pio_logic\"});"));
		// now that all subcomponents have been built, add callbacks
		if (!isMemWrapper && isBaseBlock)
			outputList.add(new OutputLine(indentLvl, "this.add_callbacks();"));
		
		outputList.add(new OutputLine(--indentLvl, "endfunction: build"));  // TODO - call getParentAddressMapName catenation above as prefix
	}
	
	/** return "1" if this is the only field so can be accessed individually */
	private String isOnlyField() {
		return (fieldList.size() == 1) ? "1" : "0";
	}

	/** generate field reset string and has_reset indication 
	 * @param field */
	private String getFieldReset(FieldProperties field) {
		String retStr = "0, 0";  // default to no reset
		if (field.getReset() != null) {
			RegNumber resetVal = new RegNumber(field.getReset());
			if (resetVal.isDefined()) {
				retStr = resetVal.toFormat(RegNumber.NumBase.Hex, RegNumber.NumFormat.Verilog) + ", 1";
			}
		}
		return retStr;
	}
	
	/** generate field access type string */
	private String getRegAccessType() {
		String accessMode = "RO";
		if (regProperties.isSwWriteable()) {
			accessMode = regProperties.isSwReadable() ? "RW" : "WO";
		}
		return accessMode;
	}

	/** generate field access type string 
	 * @param field */
	private String getFieldAccessType(FieldProperties field) {
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
    private class subComponentLists {
    	private HashMap<String,List<SpecialLine>> subCompList = new HashMap<String,List<SpecialLine>>();   // hashmap of subcomponent statements
    	
    	private void addStatement(String block, String statement) {
    		// if this hash isnt used then create a new list
    		if (!subCompList.containsKey(block)) {
    			subCompList.put(block, new ArrayList<SpecialLine>());
    		}
    		// add to the list
    		subCompList.get(block).add(new SpecialLine(statement));
    	}
    	
    	private void addStatement(String block, String statement, String cbsId, String regId, String aliasedId) {
    		// if this hash isnt used then create a new list
    		if (!subCompList.containsKey(block)) {
    			subCompList.put(block, new ArrayList<SpecialLine>());
    		}
    		// add to the list
    		subCompList.get(block).add(new SpecialLine(0, statement, cbsId, regId, aliasedId));
    	}
    	
    	
    	private List<SpecialLine> getStatements(String block) {
    		// if this hash isnt used then create a new list
    		if (subCompList.containsKey(block)) {
    			return subCompList.get(block);
    		}
    		return null;
    	}
    }
    
    /** class of alias regs in a group */
    private class AliasGroup {
    	private HashMap<String, HashSet<String>> group = new HashMap<String, HashSet<String>>();
       	
    	// return true if alias group exists for this base reg
    	private boolean baseRegExists(String id) {
    		return group.containsKey(id);
    	}
 
		/** get group
		 *  @return an alias group for the given base register
		 */
    	private HashSet<String> getGroup(String baseReg) {
			if (group.containsKey(baseReg)) return group.get(baseReg);
			return null;
		}

		/** add add a new alias group
		 *  @param baseReg - name of base register being aliased 
		 */
		private void addNewGroup(String baseReg) {
			//System.out.println("UVMRegsBuilder:    added base reg:" + baseReg);
			HashSet<String> newGrp = new HashSet<String>();
			newGrp.add(baseReg);  // also add the base reg to the set
			group.put(baseReg, newGrp);
		}
		
		/** add add a new register to an alias group
		 *  @param baseReg - name of base register being aliased 
		 *  @param aliasReg - name of register being added 
		 */
		private void addGroupReg(String baseReg, String aliasReg) {
			// create the alias group if it doesn't exist
			if (!group.containsKey(baseReg)) addNewGroup(baseReg);
			//System.out.println("   added alias reg:" + aliasReg);
			group.get(baseReg).add(aliasReg);
		}
    }
     
    /** class of all alias groups in a block/reg 
     *    structure is loaded during instance unroll - only valid at block completion */
    private class AliasGroups {
    	HashMap<String, AliasGroup> groups = new HashMap<String, AliasGroup>();
    	
    	// return true if alias group exists for this block
    	private boolean blockExists(String id) {
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
    	private HashSet<String> getGroup(String id, String reg) {
			if (groups.containsKey(id)) {
				AliasGroup grp = groups.get(id);
				if ((reg != null) && grp.baseRegExists(reg)) return grp.getGroup(reg);  
			}
			return new HashSet<String>();
		}
    	
		/** add add a new block alias group
		 *  @param block - name of block  
		 */
		private void addNewGroup(String id) {
			//System.out.println("UVMRegsBuilder:    added block:" + id);
			AliasGroup newGrp = new AliasGroup();
			groups.put(id, newGrp);
		}
		
		/** add add a new register to an alias group
		 *  @param block - name of block  
		 *  @param baseReg - name of base register being aliased 
		 *  @param aliasReg - name of register being added 
		 */
		private void addGroupReg(String id, String baseReg, String aliasReg) {
			//System.out.println("UVMRegsBuilder: alias group add blk=" + id + ", reg=" + baseReg + ", alias=" + aliasReg);
			// create the alias group if it doesn't exist
			if (!groups.containsKey(id)) addNewGroup(id);
			groups.get(id).addGroupReg(baseReg, aliasReg);
		}
    	
    }
    

    /** special line class for post-processing output
     * in addition to line holds info for alias list generation at block finish (once groups are known) */
    private class SpecialLine extends OutputLine {
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
		
		// generate the 
		UVMRdlClasses.buildRdlPackage(pkgOutputList, 0);
		
		// write the output for each output group
		for (OutputLine rLine: pkgOutputList) {
			writeStmt(rLine.getIndent(), rLine.getLine());  
		}
	}
}
