package ordt.output.uvmregs;

import java.util.Iterator;

import ordt.output.common.MsgUtils;
import ordt.extract.RegModelIntf;
import ordt.extract.RegNumber;

import ordt.output.FieldProperties;
import ordt.output.common.OutputLine;
import ordt.parameters.ExtParameters;

/** lightweight uvm regs model -
 * 
 * - uses only a single uvm_reg_field for expect data storage (width of register)
 * - assumes all fields in a reg have a single access mode
 * - no field subclasses and associated info (interrupts, counters, etc)
 * - no register interaction callbacks supported ()
 * - no backdoor access/path defines
 * - no user-defined properties/test related reg info
 *
 */
public class UVMRegsLite1Builder extends UVMRegsNativeBuilder {

	public UVMRegsLite1Builder(RegModelIntf model) {
		super(model);
	}

	// --------------------------- package setup methods ------------------------
	
	/** generate package import statements */
	@Override
	protected void generatePkgImports() {
		outputList.add(new OutputLine(indentLvl, "import uvm_pkg::*;"));
		outputList.add(new OutputLine(indentLvl, "import ordt_uvm_reg_lite1_pkg::*;"));
	}
	
	// ----------------------- lite1 class builder methods -------------------------

	/** generate package info (overridden by child uvm builder classes) */
	@Override
	protected void buildRdlPackage() {
		UVMRdlLite1Classes.buildRdlPackage(pkgOutputList, 0);		
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
		outputList.add(new OutputLine(indentLvl++, "class " + uvmRegClassName + " extends uvm_reg_lite;"));  // use lightweight reg class
		
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

	// ---------

	/** build field definitions for current register  (use uvm_field_lite type)*/
	@Override
	protected void buildRegFieldDefines() {
		Iterator<FieldProperties> iter = fieldList.iterator();
		// traverse field list
		while (iter.hasNext()) {
			FieldProperties field = iter.next();
			String fieldId = escapeReservedString(field.getPrefixedId());  
			outputList.add(new OutputLine(indentLvl, "uvm_field_lite " + fieldId + " = new(this, " + field.getLowIndex() + ", " + field.getFieldWidth() + ");"));
		}
	}

	/** build the build function for current register */
	@Override
	protected void buildRegBuildFunction() {
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "virtual function void build();"));
		
		// traverse field list and create single field configure statement
		String accessType = null;
		String isVolatile = "0";
		String isRand = "0";  
		Iterator<FieldProperties> iter = fieldList.iterator();
		while (iter.hasNext()) {
			FieldProperties field = iter.next();
			// warn if field access differences
			if (accessType == null) accessType = getFieldAccessType(field); // save first field access type
			else if (!accessType.equals(getFieldAccessType(field))) {
				if (getFieldAccessType(field).contains("W")) accessType = getFieldAccessType(field);
				MsgUtils.warnMessage("Multiple field access types found in register=" + regProperties.getInstancePath() + ", lite model assumes a single type per register.");
			}
			if  (field.hwChangesValue() || field.isDontCompare()) isVolatile = "1";
			if (field.isSwWriteable()) isRand = "1";				
		} // while
		
		// add single field configure
		outputList.add(new OutputLine(indentLvl, "this.m_REG_FIELD.configure(this, " + regProperties.getRegWidth() + 
				", 0, \"" + accessType + "\", " + isVolatile + ", " + getFieldResetParameters() + 
				", " + isRand + ", 1);"));		
		
		// remove reset for uninitialized field if db update is being skipped
		if (fieldNeedsResetRemoval()) outputList.add(new OutputLine(indentLvl, "void'(this.m_REG_FIELD.has_reset(.delete(1)));"));
						
		outputList.add(new OutputLine(--indentLvl, "endfunction: build"));
	}
	
	/** generate field reset string and has_reset indication 
	 * @param field */
	protected String getFieldResetParameters() {
		String retStr = ExtParameters.uvmregsSkipNoResetDbUpdate()? "0, 1" : "0, 0";  // if skip db update then config with a reset and remove after, else default to no reset
			RegNumber resetVal = getFullRegReset();
			if ((resetVal != null) && resetVal.isDefined()) {
				retStr = resetVal.toFormat(RegNumber.NumBase.Hex, RegNumber.NumFormat.Verilog) + ", 1";
			}
		return retStr;
	}

	/** return true if field has no reset and skip db update is specified  
	 * @param field */
	protected boolean fieldNeedsResetRemoval() {
		if (!ExtParameters.uvmregsSkipNoResetDbUpdate()) return false;
		RegNumber resetVal = getFullRegReset();
		if ((resetVal != null) && resetVal.isDefined()) return false;
		return true;
	}
	
}
