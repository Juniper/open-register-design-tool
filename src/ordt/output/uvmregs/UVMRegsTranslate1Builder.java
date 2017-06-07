package ordt.output.uvmregs;


import java.util.Iterator;
import java.util.List;

import ordt.extract.RegModelIntf;
import ordt.extract.RegNumber;
import ordt.output.FieldProperties;  
import ordt.output.OutputLine;
import ordt.output.systemverilog.common.SystemVerilogFunction;
import ordt.output.systemverilog.common.SystemVerilogTask;
import ordt.parameters.ExtParameters;

/** builder class to gen uvm model that translates simple subset of uvm_regs api into read/write calls to an alternate register model */
public class UVMRegsTranslate1Builder extends UVMRegsBuilder {

	public UVMRegsTranslate1Builder(RegModelIntf model) {
		super(model);
	}
	
	// --------------------------- alternate model conversion methods ------------------------
	// alt model root type
	public static String altModelRootType = ExtParameters.getUvmModelParameter("altModelRootType", "unknown root type");;  

	/** get alt reg data type by width */
	private String getAltRegDataType() {
		return "logic[PIO_MAX_TR_WIDTH-1:0]";   // TODO - make parameterizable,  "u_int" + regProperties.getRegWidth() + "_t"
	}

	/** get alt reg type */
	private String getAltRegType() {  // TODO - fix Register reference hardcoded in reg defines parm default
		String firstChar = regProperties.getId().substring(0, 1);
		String regTypeParam = regProperties.getId().replaceFirst(firstChar, firstChar.toUpperCase());  // alt model parameter type 
		String regBaseType = regProperties.isReplicated()? "RegisterArray" : "Register";
		return regBaseType + " #(" + getAltBlockType() + "::" + regTypeParam + ")";  // TODO - make parameterizable, getAddressMapName() + "_" + regProperties.getBaseName() + "_t"   
	}

	/** get alt block type */
	private String getAltBlockType() { 
		String blockBase = (regSetProperties.getBaseName().isEmpty())? "" : "_" + regSetProperties.getBaseName();
		return altModelRootType + blockBase;    // TODO - make parameterizable, getAddressMapName() + "_" + regSetProperties.getBaseName() + "_t"
	}

	// --------------------------- package setup methods ------------------------
	
	/** generate package import statements */
	@Override
	protected void generatePkgImports() {
		outputList.add(new OutputLine(indentLvl, "import uvm_pkg::*;"));
		outputList.add(new OutputLine(indentLvl, "import ordt_uvm_reg_translate1_pkg::*;"));
		if (UVMRdlTranslate1Classes.altModelPackage != null) outputList.add(new OutputLine(indentLvl, "import " + UVMRdlTranslate1Classes.altModelPackage + ";"));  // add alt model pkg
	}

    //---------------------------- helper methods saving child info for parent build --------------------------------------

	/** save register info for use in parent uvm_reg_block class */
	@Override
	protected void saveRegInfo(String uvmRegClassName, String uvmBlockClassName) {
		// get parent name
		String parentID = this.getParentInstancePath().replace('.', '_');
		// escape id and alias names
		String regId = escapeReservedString(regProperties.getId());
		//String aliasedId = escapeReservedString(regProperties.getAliasedId());
		// save register define statements
		String repStr = (regProperties.isReplicated()) ? "[" + regProperties.getRepCount() + "]" : "";
		String regParameterStr = " #(" + getAltRegType() + ", " + getAltRegDataType() + ", " + regProperties.getRegWidth() + ") "; 
		subcompDefList.addStatement(parentID, uvmRegClassName + regParameterStr + " " + regId + repStr + " = new(this);");		
		// save register build statements
		if (regProperties.isReplicated()) {
			subcompBuildList.addStatement(parentID, "foreach (this." + regId + "[i]) begin");
			subcompBuildList.addStatement(parentID, "  this." + regId + "[i].set_rep(i);");
			subcompBuildList.addStatement(parentID, "end");
		}
	}
		
	/** save memory info for use in parent uvm_reg_block class  
	 * @param parentIsWrapper - if true, info will be saved to a wrapper block and reg/mem instance names will be changed
	 */
	@Override
	protected void saveMemInfo(boolean parentIsWrapper) {
		// get parent block name (use reg name if a wrapper is being used)
		String parentID = parentIsWrapper? regProperties.getInstancePath().replace('.', '_') : this.getParentInstancePath().replace('.', '_'); 
		// set vreg instance id
		String regId = parentIsWrapper? "vregs" : regProperties.getId();
		// escape id name
		String escapedRegId = escapeReservedString(regId);		
		// save vreg define statement
		subcompDefList.addStatement(parentID, getUVMVRegID() + " " + escapedRegId + ";");   // virtual regs (name = vregs)
		//System.out.println("UVMRegsTranslate1Builder saveMemInfo: saving statements into wrapper parentID=" + parentID);
	}	
	
	/** save register set info for use in parent uvm_reg_block class   
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
		String blockParameterStr = " #(" + getAltBlockType() + ") "; 
		subcompDefList.addStatement(parentID, uvmBlockClassName + blockParameterStr + " " + escapedBlockId + repStr + " = new(this);");
		//System.out.println("UVMBuild saveRegSetInfo: " + regSetProperties.getBaseName() + ", parent=" + parentID + ", rel addr=" + regSetProperties.getRelativeBaseAddress());
		// save register build statements
		if (!hasInstanceNameOverride && regSetProperties.isReplicated()) {  
			subcompBuildList.addStatement(parentID, "foreach (this." + escapedBlockId + "[i]) begin");
			subcompBuildList.addStatement(parentID, "  this." + escapedBlockId + "[i].set_rep(i);");
			subcompBuildList.addStatement(parentID, "  this." + escapedBlockId + "[i].build();");
			subcompBuildList.addStatement(parentID, "end");
		}
		else
			subcompBuildList.addStatement(parentID, "this." + escapedBlockId + ".build();");
	}
	
	// ----------------------- translate class builder methods -------------------------

	/** generate package info (overridden by child uvm builder classes) */
	@Override
	protected void buildRdlPackage() {
		UVMRdlTranslate1Classes.buildRdlPackage(pkgOutputList, 0);		
	}

	/** build reg class definition for current register instance */   
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
		outputList.add(new OutputLine(indentLvl++, "class " + uvmRegClassName + " #(type ALTREG_T = Register #(int), type REGDATA_T = int, int REGWIDTH = 32) extends uvm_reg_translate #(REGDATA_T, REGWIDTH);"));  
		
		// create field definitions
		buildRegFieldDefines();  

		// override of read/write call tasks 
		SystemVerilogTask task = new SystemVerilogTask("call_alt_read");
		task.addComment("Alt model register read (overload of base)");
		task.setVirtual();
		task.addIO("output", "REGDATA_T", "read_data", null);
		if (UVMRdlTranslate1Classes.hasAddlParameter)
			task.addIO("input", UVMRdlTranslate1Classes.addlParameterType, UVMRdlTranslate1Classes.addlParameterLocal, null);
		task.addStatement("ALTREG_T alt_reg = get_alt_reg();");
		task.addStatement("alt_reg.read(read_data" + (UVMRdlTranslate1Classes.hasAddlParameter? ", " + UVMRdlTranslate1Classes.addlParameterLocal : "") + ");");
		outputList.addAll(task.genOutputLines(indentLvl));	
		
		task = new SystemVerilogTask("call_alt_write");
		task.addComment("Alt model register write (overload of base)");
		task.setVirtual();
		task.addIO("input", "REGDATA_T", "write_data", null);
		if (UVMRdlTranslate1Classes.hasAddlParameter)
			task.addIO("input", UVMRdlTranslate1Classes.addlParameterType, UVMRdlTranslate1Classes.addlParameterLocal, null);
		task.addStatement("ALTREG_T alt_reg = get_alt_reg();");
		task.addStatement("alt_reg.write(write_data" + (UVMRdlTranslate1Classes.hasAddlParameter? ", " + UVMRdlTranslate1Classes.addlParameterLocal : "") + ");");
		outputList.addAll(task.genOutputLines(indentLvl));
		
		// get_alt_reg - return alternate model register struct corresponding to this reg
		SystemVerilogFunction func = new SystemVerilogFunction("ALTREG_T", "get_alt_reg");
		String escRegId = escapeReservedString(regProperties.getId());
		if (regProperties.isReplicated())
		  func.addStatement("return m_parent.get_alt_block()." + escRegId + "[m_rep];");
		else 
			  func.addStatement("m_parent.get_alt_block()." + escRegId + ";"); 
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// close out the register definition
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(--indentLvl, "endclass : " + uvmRegClassName));
	}

	/** build field definitions for current register */
	@Override
	protected void buildRegFieldDefines() {
		Iterator<FieldProperties> iter = fieldList.iterator();
		// traverse field list
		while (iter.hasNext()) {
			FieldProperties field = iter.next();
			String fieldId = escapeReservedString(field.getPrefixedId());  
			String fieldClass = "uvm_field_translate";
			outputList.add(new OutputLine(indentLvl, fieldClass + " " + fieldId + " = new(this, " + field.getLowIndex() + ", " + field.getFieldWidth() + ");"));
		}
	}

	/** build vreg class definition for current register instance */ 
	@Override
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
		outputList.add(new OutputLine(indentLvl++, "class " + fullId + " #(type ALTREG_T = Register #(int), type REGDATA_T = int, int REGWIDTH = 32) extends uvm_vreg_translate #(REGDATA_T, REGWIDTH);"));  
						
		// override of read/write call tasks 
		SystemVerilogTask task = new SystemVerilogTask("call_alt_read");
		task.addComment("Alt model register read (overload of base)");
		task.setVirtual();
		task.addIO("input", "longint unsigned", "idx", null);
		task.addIO("output", "REGDATA_T", "read_data", null);
		if (UVMRdlTranslate1Classes.hasAddlParameter)
			task.addIO("input", UVMRdlTranslate1Classes.addlParameterType, UVMRdlTranslate1Classes.addlParameterLocal, null);
		task.addStatement("ALTREG_T alt_reg = get_alt_reg(idx);");
		task.addStatement("alt_reg.read(read_data" + (UVMRdlTranslate1Classes.hasAddlParameter? ", " + UVMRdlTranslate1Classes.addlParameterLocal : "") + ");");
		outputList.addAll(task.genOutputLines(indentLvl));	
		
		task = new SystemVerilogTask("call_alt_write");
		task.addComment("Alt model register write (overload of base)");
		task.setVirtual();
		task.addIO("input", "longint unsigned", "idx", null);
		task.addIO("input", "REGDATA_T", "write_data", null);
		if (UVMRdlTranslate1Classes.hasAddlParameter)
			task.addIO("input", UVMRdlTranslate1Classes.addlParameterType, UVMRdlTranslate1Classes.addlParameterLocal, null);
		task.addStatement("ALTREG_T alt_reg = get_alt_reg(idx);");
		task.addStatement("alt_reg.write(write_data" + (UVMRdlTranslate1Classes.hasAddlParameter? ", " + UVMRdlTranslate1Classes.addlParameterLocal : "") + ");");
		outputList.addAll(task.genOutputLines(indentLvl));
		
		// get_alt_reg - return alternate model register struct corresponding to this reg
		SystemVerilogFunction func = new SystemVerilogFunction("ALTREG_T", "get_alt_reg");
		task.addIO("input", "longint unsigned", "idx", null);
		String escRegId = escapeReservedString(regProperties.getId());
		if (regProperties.isReplicated())
		  func.addStatement("return m_parent.get_alt_block()." + escRegId + "[idx];");
		else 
			  func.addStatement("m_parent.get_alt_block()." + escRegId + ";"); 
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// close out the register definition
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(--indentLvl, "endclass : " + fullId));
		
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
		outputList.add(new OutputLine(indentLvl++, "class " + uvmBlockClassName + "#(type ALTBLOCK_T = " + altModelRootType + ") extends uvm_block_translate;"));  

		// create field definitions  
		buildBlockDefines(refId);
		
		// create new function
		//buildBlockNewDefine(uvmBlockClassName); 
		
		// create build function  
		buildBlockBuildFunction(refId);
		
		// get_alt_block - return alternate model regset struct corresponding to this block
		String escRegSetId = escapeReservedString(regSetProperties.getId());
		SystemVerilogFunction func = new SystemVerilogFunction("ALTBLOCK_T", "get_alt_block"); 
		if (regSetProperties.isRootInstance())
			  func.addStatement("return " + escRegSetId + ";");
		if (regSetProperties.isReplicated())
		  func.addStatement("return m_parent.get_alt_block()." + escRegSetId + "[m_rep];");
		else 
			  func.addStatement("return m_parent.get_alt_block()." + escRegSetId + ";"); 
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// close out the class definition
		outputList.add(new OutputLine(indentLvl, ""));	
		//outputList.add(new OutputLine(indentLvl, "`uvm_object_utils(" + uvmBlockClassName + ")"));  
		outputList.add(new OutputLine(--indentLvl, "endclass : " + uvmBlockClassName));
	}

	/** build uvm_mem/uvm_vreg wrapper block class definition as child of current regset block 
	 *  callbacks, aliases not allowed in this block type 
	 */   
	@Override
	protected void buildMemWrapperBlockClass(String uvmBlockClassName, String nameSuffix, Integer mapWidthOverride) {
		// create block name + id with suffix
		String refId = ((regSetProperties == null) || regSetProperties.getBaseName().isEmpty()) ? nameSuffix : regSetProperties.getBaseName() + "_" + nameSuffix;  // id used for structure lookup
		//System.out.println("UVMRegsTranslate1Builder buildMemWrapperBlockClass: fullId=" + uvmBlockClassName + ", refId=" + refId);
		
		// generate register header 
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl, "// Uvm_mem wrapper block " + refId));
		outputList.add(new OutputLine(indentLvl++, "class " + uvmBlockClassName + "#(type ALTBLOCK_T = " + altModelRootType + ") extends uvm_block_translate;"));  

		// create field definitions  
		buildBlockDefines(refId);
		
		// create new function
		//buildBlockNewDefine(uvmBlockClassName);
		
		// create build function ising width of underlying virtual regs/mem
		buildBlockBuildFunction(refId);
		
		// get_alt_block - return alternate model regset struct corresponding to this block
		SystemVerilogFunction func = new SystemVerilogFunction("ALTBLOCK_T", "get_alt_block");  
	    func.addStatement("return m_parent.get_alt_block();"); // wrapper block, so just return parent alt block
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		
		// close out the class definition
		outputList.add(new OutputLine(indentLvl, ""));	
		//outputList.add(new OutputLine(indentLvl, "`uvm_object_utils(" + uvmBlockClassName + ")"));
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
		outputList.add(new OutputLine(indentLvl++, "class " + uvmBlockClassName + "#(type ALTBLOCK_T = " + altModelRootType + ") extends uvm_block_translate;"));
		
		outputList.add(new OutputLine(indentLvl, "local ALTBLOCK_T m_alt_root_instance;")); 
		
		//String uvmBlockClassName, 
		// create field definitions  
		buildBlockDefines(refId);
		
		// create new function
		//buildBlockNewDefine(uvmBlockClassName);
		
		// create build function 
		buildBlockBuildFunction(refId);
		
		// get_alt_block - return alternate model regset struct corresponding to this block (special for base block)
		SystemVerilogFunction func = new SystemVerilogFunction("ALTBLOCK_T", "get_alt_block");  
	    func.addStatement("return m_alt_root_instance;");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// set_alt_root_instance - return alternate model regset struct corresponding to this block (special for base block)
		func = new SystemVerilogFunction(null, "set_alt_root_instance");  
		func.addIO("input", "ALTBLOCK_T", "alt_root_instance", null);
	    func.addStatement("m_alt_root_instance = alt_root_instance;");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// close out the class definition
		outputList.add(new OutputLine(indentLvl, ""));	
		//outputList.add(new OutputLine(indentLvl, "`uvm_object_utils(" + uvmBlockClassName + ")"));
		outputList.add(new OutputLine(--indentLvl, "endclass : " + uvmBlockClassName));
	}
	
	/** build the virtual build function for current block
	 * @param block - block id used for structure lookup
	 */
	protected void buildBlockBuildFunction(String block) {
		outputList.add(new OutputLine(indentLvl, "")); 	
		outputList.add(new OutputLine(indentLvl++, "virtual function void build();"));
		
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
		outputList.add(new OutputLine(--indentLvl, "endfunction: build"));
	}

}
