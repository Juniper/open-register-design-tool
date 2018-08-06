package ordt.output.uvmregs;

import java.util.List;

import ordt.output.common.OutputLine;
import ordt.output.systemverilog.common.SystemVerilogFunction;

public class UVMRdlLite1Classes extends UVMRegsCommon {
	
	/** build a package with all derived classes for lite model 
	 * @param outputList 
	 * @param indentLvl */
	public static void buildRdlPackage(List<OutputLine> outputList, int indentLvl) {
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl, "// ordt_uvm_reg_lite1_pkg"));
		outputList.add(new OutputLine(indentLvl++, "`ifndef ORDT_UVM_REG_LITE1_PKG_SV"));
		outputList.add(new OutputLine(indentLvl,     "`define ORDT_UVM_REG_LITE1_PKG_SV"));

		outputList.add(new OutputLine(indentLvl,   "`include \"uvm_macros.svh\""));
		outputList.add(new OutputLine(indentLvl++, "package ordt_uvm_reg_lite1_pkg;")); 
		outputList.add(new OutputLine(indentLvl,     "import uvm_pkg::*;"));
		// create derived classes
		buildRdlClasses(outputList, indentLvl);
		// close out the package definition
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(--indentLvl,   "endpackage"));
		outputList.add(new OutputLine(--indentLvl, "`endif"));
	}

	/** build all derived classes  
	 * @param outputList 
	 * @param indentLvl */
	public static void buildRdlClasses(List<OutputLine> outputList, int indentLvl) {
		// create child reg class that stores a list of uvm_reg_field_lite
		buildUvmRegLiteClass(outputList, indentLvl);
		// create child field info class - similar to uvm_vreg_field, but w/ uvm_reg_lite parent
		buildUvmRegFieldLiteClass(outputList, indentLvl);
	}

	// create reg w/ base field and slice read/write methods, then field that calls these methods, then child reg w/ list of fields
	
	// use native vregs, not derived class

	/** create child uvm_reg_lite_base child class that stores a single uvm field and has non-uvm field_lite children */
	private static void buildUvmRegLiteClass(List<OutputLine> outputList, int indentLvl) {
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl, "// uvm_reg_lite class"));
		outputList.add(new OutputLine(indentLvl++, "class uvm_reg_lite extends uvm_reg;")); 
		
		// single field for this register
		outputList.add(new OutputLine(indentLvl, "rand uvm_reg_field m_REG_FIELD = new();")); 
		
		// create new function
		SystemVerilogFunction func = new SystemVerilogFunction(null, "new");
		func.addIO("input", "string", "name", "\"uvm_reg_lite\"");
		func.addIO("input", "int unsigned", "n_bits", "0");
		func.addIO("input", "int", "has_coverage", "UVM_NO_COVERAGE");
		func.addStatement("super.new(name, n_bits, has_coverage);");
		outputList.addAll(func.genOutputLines(indentLvl));	
				
		// get data slice from common field  
		func = new SystemVerilogFunction("uvm_reg_data_t", "get_slice");
		func.addIO("input", "int", "loidx", "0");
		func.addIO("input", "int", "width", "0");
		func.addStatement("uvm_reg_data_t gvalue;");
		func.addStatement("gvalue = m_REG_FIELD.get();");
		addGetSliceStatements(func, "gvalue", "loidx", "width");
		outputList.addAll(func.genOutputLines(indentLvl));	

		// set data slice from common field 
		func = new SystemVerilogFunction("void", "set_slice");
		func.addIO("uvm_reg_data_t", "value");
		func.addIO("input", "int", "loidx", "0");
		func.addIO("input", "int", "width", "0");
		func.addStatement("uvm_reg_data_t svalue;");
		func.addStatement("if (value >> width) begin");
		func.addStatement("  $display(\"ERROR in UVM reg lite1 model: atempting to set a slice value greater than field width\");");
		func.addStatement("  value &= ((1<<width)-1);");
		func.addStatement("end");
		func.addStatement("svalue = m_REG_FIELD.get();");
		addSetSliceStatements(func, "svalue", "value", "loidx", "width");
		func.addStatement("m_REG_FIELD.set(svalue);");
		outputList.addAll(func.genOutputLines(indentLvl));	
	
		// close out the class definition
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(--indentLvl, "endclass : uvm_reg_lite"));
	}

	/** create non-uvmreg child field class with no data store and lightweight api (get/set)
	 * 
	 * @param outputList - list where generated output lines will be added
	 * @param indentLvl - incoming indent level
	 */
	protected static void buildUvmRegFieldLiteClass(List<OutputLine> outputList, int indentLvl) {
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl, "// uvm_field_lite class"));
		outputList.add(new OutputLine(indentLvl++, "class uvm_field_lite;")); 
		
		outputList.add(new OutputLine(indentLvl, "protected uvm_reg_lite m_parent;")); // parent reg
		outputList.add(new OutputLine(indentLvl, "protected int m_loidx;")); // field low index
		outputList.add(new OutputLine(indentLvl, "protected int m_width;")); // field width
		
		// new function
		SystemVerilogFunction func = new SystemVerilogFunction(null, "new");
		func.addIO("input", "uvm_reg_lite", "parent", null);
		func.addIO("input", "int", "loidx", null);
		func.addIO("input", "int", "width", null);
		func.addStatement("m_parent = parent;");
		func.addStatement("m_loidx = loidx;");
		func.addStatement("m_width = width;");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// add get/set methods here matching uvm_regs
		func = new SystemVerilogFunction("uvm_reg_data_t", "get");
		func.setVirtual();
		func.addStatement("return m_parent.get_slice(m_loidx, m_width);");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		func = new SystemVerilogFunction(null, "set");
		func.setVirtual();
		func.addIO("input", "uvm_reg_data_t", "value", null);
		func.addStatement("m_parent.set_slice(value, m_loidx, m_width);");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// close out the class definition
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(--indentLvl, "endclass : uvm_field_lite"));
	}

}
