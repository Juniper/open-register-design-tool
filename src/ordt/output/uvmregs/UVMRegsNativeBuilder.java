package ordt.output.uvmregs;

import java.util.Iterator;
import java.util.List;

import ordt.extract.RegModelIntf;
import ordt.extract.RegNumber;

import ordt.output.FieldProperties;
import ordt.output.common.OutputLine;
import ordt.parameters.ExtParameters;

/** native uvm regs model -
 * 
 * - uses only native uvm ral classes
 * - no field subclasses and associated info (interrupts, counters, etc)
 * - no register interaction callbacks supported ()
 * - no backdoor access/path defines
 * - no user-defined properties/test related reg info
 *
 */
public class UVMRegsNativeBuilder extends UVMRegsBuilder {

	public UVMRegsNativeBuilder(RegModelIntf model) {
		super(model, false);  // extended info not included
	}

	// --------------------------- package setup methods ------------------------
	
	/** generate package import statements - native has no special package */
	@Override
	protected void generatePkgImports() {
		outputList.add(new OutputLine(indentLvl, "import uvm_pkg::*;"));
		outputList.add(new OutputLine(indentLvl,   "`include \"uvm_macros.svh\""));
	}

    //---------------------------- helper methods saving child info for parent build --------------------------------------

	/** save register info for use in parent uvm_reg_block class - no callbacks,coverage */
	@Override
	protected void saveRegInfo(String uvmRegClassName) {
		// get parent name
		String parentID = this.getParentInstancePath().replace('.', '_');
		// escape id and alias names
		String regId = escapeReservedString(regProperties.getId());
		//String aliasedId = escapeReservedString(regProperties.getAliasedId());
		// save register define statements
		String repStr = (regProperties.isReplicated()) ? "[" + regProperties.getRepCount() + "]" : "";
		subcompDefList.addStatement(parentID, "rand " + uvmRegClassName + " " + regId + repStr + ";");
		
		// save register build statements
		addRegToBuildList(uvmRegClassName, parentID, regId);
		//System.out.println("UVMBuild saveRegInfo: " + regProperties.getBaseName() + ", parent=" + parentID + ", rel addr=" + regProperties.getRelativeBaseAddress());
	}

	/** create block build statements adding current register - no hdl_path/rdl_tag/external/defined property setup */
	@Override
	protected void addRegToBuildList(String uvmRegClassName, String parentID, String regId) {
		String addr = "`UVM_REG_ADDR_WIDTH" + regProperties.getRelativeBaseAddress().toFormat(RegNumber.NumBase.Hex, RegNumber.NumFormat.NoLengthVerilog);
		if (regProperties.isReplicated()) {
			subcompBuildList.addStatement(parentID, "foreach (this." + regId + "[i]) begin");
			if (ExtParameters.uvmregsRegsUseFactory()) subcompBuildList.addStatement(parentID, "  this." + regId + "[i] = " + uvmRegClassName + "::type_id::create($psprintf(\"" + regId + " [%0d]\",i));");
			else subcompBuildList.addStatement(parentID, "  this." + regId + "[i] = new($psprintf(\"" + regProperties.getId() + " [%0d]\",i));");  
			subcompBuildList.addStatement(parentID, "  this." + regId + "[i].configure(this, null, \"\");");  
			subcompBuildList.addStatement(parentID, "  this." + regId + "[i].build();");
			subcompBuildList.addStatement(parentID, "  this.default_map.add_reg(this." + regId + "[i], " + addr + "+i*" + getRegAddrIncr() + ", \"" + getRegAccessType() + "\", 0);");						
			subcompBuildList.addStatement(parentID, "end");
		}
		else {
			if (ExtParameters.uvmregsRegsUseFactory()) subcompBuildList.addStatement(parentID, "this." + regId + " = " + uvmRegClassName + "::type_id::create(\"" + regId + "\");");
			else subcompBuildList.addStatement(parentID, "this." + regId + " = new(\"" + regProperties.getId() + "\");");  
			subcompBuildList.addStatement(parentID, "this." + regId + ".configure(this, null, \"\");"); 
			subcompBuildList.addStatement(parentID, "this." + regId + ".build();");
			subcompBuildList.addStatement(parentID, "this.default_map.add_reg(this." + regId + ", " + addr + ", \"" + getRegAccessType() + "\", 0);");			
		}	
	}
	
	/** save register set info for use in parent uvm_reg_block class - no extensions/addrmap/hdl_path/user-defined props  
	 * @param uvmBlockClassName - name of uvm block class
	 * @param blockIdOverride - if non null, specified name will be used as the block instance rather then regset id
	 *        - regSetProperties (replication, is addrmap) will be ignored if a name override is specified
	 *        (this is used for uvm_mem wrapper block gen)
	 * @param addrOffsetOverride - if non null, specified address offset will be used rather than current regset offset
	 */
	@Override
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
			if (ExtParameters.uvmregsRegsUseFactory()) subcompBuildList.addStatement(parentID, "  this." + escapedBlockId + "[i] = " + uvmBlockClassName + "::type_id::create($psprintf(\"" + blockId + " [%0d]\",i),, get_full_name());");
			else subcompBuildList.addStatement(parentID, "  this." + escapedBlockId + "[i] = " + "new($psprintf(\"" + blockId + " [%0d]\",i));");
			subcompBuildList.addStatement(parentID, "  this." + escapedBlockId + "[i].configure(this, \"\");");  
			subcompBuildList.addStatement(parentID, "  this." + escapedBlockId + "[i].build();");
			subcompBuildList.addStatement(parentID, "  this.default_map.add_submap(this." + escapedBlockId + "[i].default_map, " + addrStr + "+i*" + getRegSetAddrIncrString() + ");");						
			subcompBuildList.addStatement(parentID, "end");
		}
		else {
			if (ExtParameters.uvmregsRegsUseFactory()) subcompBuildList.addStatement(parentID, "this." + escapedBlockId + " = " + uvmBlockClassName + "::type_id::create(\"" + blockId + "\",, get_full_name());");
			else subcompBuildList.addStatement(parentID, "this." + escapedBlockId + " = " + "new(\"" + blockId + "\");");
		   subcompBuildList.addStatement(parentID, "this." + escapedBlockId + ".configure(this, \"\");"); 
		   subcompBuildList.addStatement(parentID, "this." + escapedBlockId + ".build();");
		   subcompBuildList.addStatement(parentID, "this.default_map.add_submap(this." + escapedBlockId + ".default_map, " + addrStr + ");");			
		}		
		//System.out.println("UVMBuild saveRegSetInfo: " + regSetProperties.getBaseName() + ", parent=" + parentID + ", rel addr=" + regSetProperties.getRelativeBaseAddress());
	}
	
	// ----------------------- lite1 class builder methods -------------------------

	/** generate package info (overridden by child uvm builder classes) */
	@Override
	protected void buildRdlPackage() {  // no package for native
	}

	/** build reg class definition for current register instance w/ no extended info */
	@Override
	protected void buildRegClass(String uvmRegClassName) {
		// create text name and description if null
		String id = regProperties.getId();
		String textName = regProperties.getTextName();
		if (textName == null) textName = "Register " + id;
		else textName = textName.replace('\n', ' ');
		
		// generate register header 
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl, "// " + textName));
		outputList.add(new OutputLine(indentLvl++, "class " + uvmRegClassName + " extends uvm_reg;")); // use native reg class
		
		// create field definitions
		buildRegFieldDefines();
		
		// create new function
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function new(string name = \"" + uvmRegClassName + "\");"));
		outputList.add(new OutputLine(indentLvl, "super.new(name, " + regProperties.getRegWidth() + ", UVM_NO_COVERAGE);"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: new"));

		// create build function
		buildRegBuildFunction();
						
		// close out the register definition
		outputList.add(new OutputLine(indentLvl, ""));	
		if (ExtParameters.uvmregsRegsUseFactory()) outputList.add(new OutputLine(indentLvl, "`uvm_object_utils(" + uvmRegClassName + ")"));
		outputList.add(new OutputLine(--indentLvl, "endclass : " + uvmRegClassName));
	}

	/** build vreg class definition for current register instance (uses native vreg, not extended version) */   
	@Override
	protected void buildVRegClass(String uvmRegClassName) {   
		// create text name and description if null
		String id = regProperties.getId();
		String vregClassName = uvmRegClassName.replaceFirst("reg_", "vreg_");
		String textName = regProperties.getTextName();
		if (textName == null) textName = "Virtual Register " + id;
		else textName = textName.replace('\n', ' ');
		
		// generate register header 
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl, "// " + textName));
		outputList.add(new OutputLine(indentLvl++, "class " + vregClassName + " extends uvm_vreg;")); // lite model uses native vreg, not extended version
						
		// create field definitions
		buildVRegFieldDefines();   
		
		// create new function
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function new(string name = \"" + vregClassName + "\");"));
		outputList.add(new OutputLine(indentLvl,     "super.new(name, " + regProperties.getRegWidth() + ");"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: new"));

		// create build function
		buildVRegBuildFunction();
				
		// close out the register definition
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(--indentLvl, "endclass : " + vregClassName));
	}

	/** build block class definition for current regset instance 
	 * @param hasCallback - indicates block children have callbacks */   
	@Override
	protected void buildBlockClass(String uvmBlockClassName, Boolean hasCallback) {
		// create text name and description if null
		String id = regSetProperties.getId();
		String refId = regSetProperties.getBaseName();  // ref used for block structure lookup
		
		String textName = regSetProperties.getTextName();
		if (textName == null) textName = "Block " + id;
		
		// generate register header 
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl, "// " + textName));
		outputList.add(new OutputLine(indentLvl++, "class " + uvmBlockClassName + " extends uvm_reg_block;")); 

		// create field definitions  
		buildBlockDefines(refId);
		
		// create new function
		buildBlockNewDefine(uvmBlockClassName, false);
		
		// create build function  
		Integer mapWidthOverride = (ExtParameters.hasDebugMode("uvmregs_maps_use_max_width"))?  regSetProperties.getMaxRegByteWidth()  : null; // if debug mode 1 use regset property width
		buildBlockBuildFunction(refId, mapWidthOverride, false);
		
		// close out the class definition
		outputList.add(new OutputLine(indentLvl, ""));	
		if (ExtParameters.uvmregsRegsUseFactory()) outputList.add(new OutputLine(indentLvl, "`uvm_object_utils(" + uvmBlockClassName + ")"));
		outputList.add(new OutputLine(--indentLvl, "endclass : " + uvmBlockClassName));
	}

	/** build uvm_mem/uvm_vreg wrapper block class definition as child of current regset block 
	 *  callbacks, aliases not allowed in this block type 
	 * @param b */   
	@Override
	protected void buildMemWrapperBlockClass(String uvmBlockClassName, String nameSuffix, Integer mapWidthOverride) {
		// create block name + id with suffix
		String refId = ((regSetProperties == null) || regSetProperties.getBaseName().isEmpty()) ? nameSuffix : regSetProperties.getBaseName() + "_" + nameSuffix;  // id used for structure lookup
		//System.out.println("UVMRegsBuilder buildMemWrapperBlockClass: fullId=" + uvmBlockClassName + ", refId=" + refId);
		
		// generate register header 
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl, "// Uvm_mem wrapper block " + refId));
		outputList.add(new OutputLine(indentLvl++, "class " + uvmBlockClassName + " extends uvm_reg_block;")); 

		// create field definitions  
		buildBlockDefines(refId);
		
		// create new function
		buildBlockNewDefine(uvmBlockClassName, false);
		
		// create build function ising width of underlying virtual regs/mem
		buildBlockBuildFunction(refId, mapWidthOverride, true);
		
		// close out the class definition
		outputList.add(new OutputLine(indentLvl, ""));	
		if (ExtParameters.uvmregsRegsUseFactory()) outputList.add(new OutputLine(indentLvl, "`uvm_object_utils(" + uvmBlockClassName + ")"));
		outputList.add(new OutputLine(--indentLvl, "endclass : " + uvmBlockClassName));
	}

	/** build block class definition for current regset instance 
	 * @param hasCallback  - indicates block children have callbacks*/ 
	@Override
	protected void buildBaseBlockClass(String uvmBlockClassName, Boolean hasCallback) {
		//System.out.println("UVMRegsBuilder buildBaseBlockClass: fullId=" + uvmBlockClassName + ", getUVMBlockID()=" + getUVMBlockID());
		String refId = "";  // ref used for base block structure lookup
		
		// generate register header 
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl, "// Base block"));
		outputList.add(new OutputLine(indentLvl++, "class " + uvmBlockClassName + " extends uvm_reg_block;")); 
		
		//String uvmBlockClassName, 
		// create field definitions  
		buildBlockDefines(refId);
				
		// create new function
		buildBlockNewDefine(uvmBlockClassName, false);
		
		// create build function (no width override) 
		buildBlockBuildFunction(refId, null, false);
		
		// close out the class definition
		outputList.add(new OutputLine(indentLvl, ""));	
		if (ExtParameters.uvmregsRegsUseFactory()) outputList.add(new OutputLine(indentLvl, "`uvm_object_utils(" + uvmBlockClassName + ")"));
		outputList.add(new OutputLine(--indentLvl, "endclass : " + uvmBlockClassName));
	}

	// ---------

	/** build field definitions for current register (use native uvm_reg_field type) */  
	@Override
	protected void buildRegFieldDefines() {
		Iterator<FieldProperties> iter = fieldList.iterator();
		// traverse field list
		while (iter.hasNext()) {
			FieldProperties field = iter.next();
			String fieldId = escapeReservedString(field.getPrefixedId());  
			outputList.add(new OutputLine(indentLvl, "rand uvm_reg_field " + fieldId + ";"));
		}
	}

	/** build the build function for current register */ 
	@Override
	protected void buildRegBuildFunction() {
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "virtual function void build();"));
		
		Iterator<FieldProperties> iter = fieldList.iterator();
		while (iter.hasNext()) {
			FieldProperties field = iter.next();
			String fieldId = escapeReservedString(field.getPrefixedId()); 
			
			// create native field class   
			outputList.add(new OutputLine(indentLvl, "this." + fieldId +  " = new(\"" + field.getPrefixedId() + "\");"));  
			
			// add field configure
			String isVolatile = (field.hwChangesValue() || field.isDontCompare()) ? "1" : "0";
			//if (field.isDontCompare()) System.out.println("UVMRegsNativeBuilder: buildRegBuildFunction volatile set for field=" + field.getInstancePath() + ";");
			String isRand = field.isSwWriteable() ? "1" : "0";  
			outputList.add(new OutputLine(indentLvl, "this." + fieldId + ".configure(this, " + field.getFieldWidth() + 
					", " + field.getLowIndex() + ", \"" + getFieldAccessType(field) + "\", " + isVolatile + ", " + getFieldResetParameters(field) + 
					", " + isRand + ", " + isOnlyField() + ");"));		
			
			// remove reset for uninitialized fields if db update is being skipped
			if (fieldNeedsResetRemoval(field)) outputList.add(new OutputLine(indentLvl, "void'(this." + fieldId + ".has_reset(.delete(1)));"));
							
		} // while
		
		outputList.add(new OutputLine(--indentLvl, "endfunction: build"));
	}
		
	/** build the virtual build function for current block
	 * @param blockId - block id used for structure lookup
	 * @param mapWidthOverrride - if non-null, the created default map for this block will use specified value
	 * @param isMemWrapper - if true, created block is a wrapper so will avoid regSetProperty references
	 */
	@Override
	protected void buildBlockBuildFunction(String blockId, Integer mapWidthOverride, boolean isMemWrapper) {
		outputList.add(new OutputLine(indentLvl, "")); 	
		outputList.add(new OutputLine(indentLvl++, "virtual function void build();"));
		// set access width of block to max of full addrmap by default (<MAX_REG_BYTE_WIDTH> will be replaced with final max value)
		String byteWidthString = "<MAX_REG_BYTE_WIDTH>"; 
		if (mapWidthOverride != null) byteWidthString = mapWidthOverride.toString();
		
		// if base block then include in base address offset
		Boolean isBaseBlock = blockId.equals("");
		String endianness = "UVM_LITTLE_ENDIAN";
		if (isBaseBlock) {
			String addr = "`UVM_REG_ADDR_WIDTH" + ExtParameters.getPrimaryBaseAddress().toFormat(RegNumber.NumBase.Hex, RegNumber.NumFormat.NoLengthVerilog);
			OutputLine oLine = new OutputLine(indentLvl, "this.default_map = create_map(\"\", " + addr + ", " + byteWidthString + ", " + endianness + ", 1);");
			oLine.setHasTextReplacements(true);
			outputList.add(oLine);
		}
		else {
			OutputLine oLine = new OutputLine(indentLvl, "this.default_map = create_map(\"\", 0, " + byteWidthString + ", " + endianness + ", 1);");
			oLine.setHasTextReplacements(true);
			outputList.add(oLine);
		}
		
		// add subcomponent build statements
		List<SpecialLine> buildList = subcompBuildList.getStatements(blockId);
		if (buildList != null) {
			Iterator<SpecialLine> iter = buildList.iterator();
			// traverse subcomponent list
			while (iter.hasNext()) {
				SpecialLine line = iter.next();
				line.setIndent(indentLvl);
				outputList.add(line);
			}  
		}
						
		outputList.add(new OutputLine(--indentLvl, "endfunction: build"));
	}
	
}
