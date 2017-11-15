/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved. 
 */
package ordt.output.uvmregs;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import ordt.output.JspecCategory;
import ordt.output.JspecSubCategory;
import ordt.output.OutputLine;
import ordt.output.systemverilog.common.SystemVerilogFunction;
import ordt.output.systemverilog.common.SystemVerilogTask;

public class UVMRdlClasses extends UVMRegsCommon {

	/** build a package with all derived classes  
	 * @param outputList 
	 * @param indentLvl */
	public static void buildRdlPackage(List<OutputLine> outputList, int indentLvl) {
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl, "// ordt_uvm_reg_pkg containing ordt extended classes"));
		outputList.add(new OutputLine(indentLvl++, "`ifndef ORDT_UVM_REG_PKG_SV"));
		outputList.add(new OutputLine(indentLvl,     "`define ORDT_UVM_REG_PKG_SV"));

		outputList.add(new OutputLine(indentLvl,   "`include \"uvm_macros.svh\""));
		outputList.add(new OutputLine(indentLvl++, "package ordt_uvm_reg_pkg;")); 
		outputList.add(new OutputLine(indentLvl,     "import uvm_pkg::*;"));
		// create enum defines
		buildRdlEnums(outputList, indentLvl);
		// create derived classes
		buildRdlClasses(outputList, indentLvl);
		// close out the package definition
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(--indentLvl,   "endpackage"));
		outputList.add(new OutputLine(--indentLvl, "`endif"));
	}
	
	/** build enumes defined for this package */
	private static void buildRdlEnums(List<OutputLine> outputList, int indentLvl) {
		// js_category_e enum define
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "typedef enum int unsigned {"));
		// use defined category map values
		HashMap<String, Integer> map = JspecCategory.getMap();
		Iterator<String> iter = map.keySet().iterator();
		while (iter.hasNext()) {
			String cat = iter.next();
			String suffix = iter.hasNext()? "," : "";
			outputList.add(new OutputLine(indentLvl,  cat + " = " + map.get(cat) + suffix));
		}
		outputList.add(new OutputLine(--indentLvl, "} js_category_e;"));	
		
		// js_subcategory_e enum define
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "typedef enum int unsigned {"));
		// use defined category map values
		map = JspecSubCategory.getMap();
		iter = map.keySet().iterator();
		while (iter.hasNext()) {
			String cat = iter.next();
			String suffix = iter.hasNext()? "," : "";
			outputList.add(new OutputLine(indentLvl,  cat + " = " + map.get(cat) + suffix));
		}
		outputList.add(new OutputLine(--indentLvl, "} js_subcategory_e;"));		
	}

	/** build all derived classes  
	 * @param outputList 
	 * @param indentLvl */
	public static void buildRdlClasses(List<OutputLine> outputList, int indentLvl) {
		// create derived block class
		buildUvmRegBlockRdlClass(outputList, indentLvl);
		// create derived reg class
		buildUvmRegRdlClass(outputList, indentLvl);
		// create derived reg class
		buildUvmVRegRdlClass(outputList, indentLvl);
		// create derived mem class
		buildUvmMemRdlClass(outputList, indentLvl);
		// create derived field class  
		buildUvmRegFieldRdlClass(outputList, indentLvl);
		// create derived field class for counters  
		buildUvmRegFieldRdlCounterClass(outputList, indentLvl);
		// create derived reg class  
		buildUvmRegFieldRdlInterruptClass(outputList, indentLvl);
		// create cbs class used for alias registers
		buildAliasRegCbsClass(outputList, indentLvl);
		// create cbs class used for masked/enabled fields
		buildMaskIntrFieldCbsClass(outputList, indentLvl);
	}

	/** build uvm_reg_rdl class extension of uvm_reg 
	 * @param outputList - list of output lines to be updated
	 * @param indentLvl */   
	private static void buildUvmRegRdlClass(List<OutputLine> outputList, int indentLvl) {
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl, "// uvm_reg_rdl class"));
		outputList.add(new OutputLine(indentLvl++, "class uvm_reg_rdl extends uvm_reg;")); 
		
		// tag used for rdl name generation
		outputList.add(new OutputLine(indentLvl, "local string m_rdl_tag;")); 
		
		// external register indicator
		outputList.add(new OutputLine(indentLvl, "local bit m_external = 0;")); 

		// uvm reg test mode info
		outputList.add(new OutputLine(indentLvl, "local bit m_dont_test = 0;")); 
		outputList.add(new OutputLine(indentLvl, "local bit m_dont_compare = 0;")); 
		outputList.add(new OutputLine(indentLvl, "local int unsigned m_js_category = 0;")); 
		
		// user defined property array
		outputList.add(new OutputLine(indentLvl, "local string m_def_properties[ string ];")); 
		
		// create new function
		SystemVerilogFunction func = new SystemVerilogFunction(null, "new");
		func.addIO("input", "string", "name", "\"uvm_reg_rdl\"");
		func.addIO("input", "int unsigned", "n_bits", "0");
		func.addIO("input", "int", "has_coverage", "UVM_NO_COVERAGE");
		func.addStatement("super.new(name, n_bits, has_coverage);");
		outputList.addAll(func.genOutputLines(indentLvl));	
				
		// add rdl path gen methods
		buildRegRdlPathMethods(outputList, indentLvl);
		
		// add user defined property methods
		buildUserDefinedPropertyMethods(outputList, indentLvl);
		
		// set external
		func = new SystemVerilogFunction("void", "set_external");
		func.addComment("Set indication that this register is external to autogenerated logic.");
		func.addIO("bit", "is_external");
		func.addStatement("m_external = is_external;");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// get external indication
		func = new SystemVerilogFunction("bit", "is_external");
		func.addComment("Return 1 if this register is external to autogenerated logic.");
		func.addStatement("return m_external;");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// default get_src_intr_fields methods (overridden in intr reg child classes)
		func = new SystemVerilogFunction("void", "get_intr_fields"); 
		func.addComment("Return a list of all leaf fields in this register's interrupt hierarchy.");
		func.addComment("This null function is overridden by child interrupt register classes.");
		func.setVirtual();
		func.addIOArray("ref uvm_reg_field", "fields");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		SystemVerilogTask task = new SystemVerilogTask("get_active_intr_fields");
		task.addComment("Return a list of all fields in this register's interrupt hierarchy that are causing");
		task.addComment("halt or interrupt to be asserted.  This null task is overridden by child interrupt register classes.");
		task.setVirtual();
		task.addIOArray("ref", "uvm_reg_field", "fields", null);
		task.addIO("input", "bit", "is_halt", null);
		task.addIO("input", "uvm_path_e", "path", "UVM_DEFAULT_PATH");
		outputList.addAll(task.genOutputLines(indentLvl));	
		
		// add reg test info methods
		buildRegTestInfoMethods(outputList, indentLvl);
				
		// add_callbacks method
		buildAddCallbacksMethod(outputList, indentLvl);
		
		// add get ancestor method
		buildRdlAncestorMethod(outputList, indentLvl);

		// close out the class definition
		outputList.add(new OutputLine(indentLvl, ""));	
		//outputList.add(new OutputLine(indentLvl, "`uvm_object_utils(uvm_reg_rdl)")); //NOFACTORY
		outputList.add(new OutputLine(--indentLvl, "endclass : uvm_reg_rdl"));
	}

	/** build uvm_vreg_rdl class extension of uvm_vreg 
	 * @param outputList - list of output lines to be updated
	 * @param indentLvl */   
	private static void buildUvmVRegRdlClass(List<OutputLine> outputList, int indentLvl) {
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl, "// uvm_vreg_rdl class"));
		outputList.add(new OutputLine(indentLvl++, "class uvm_vreg_rdl extends uvm_vreg;")); 
		
		// uvm reg test mode info
		outputList.add(new OutputLine(indentLvl, "local bit m_dont_test = 0;")); 
		outputList.add(new OutputLine(indentLvl, "local bit m_dont_compare = 0;")); 
		outputList.add(new OutputLine(indentLvl, "local int unsigned m_js_category = 0;")); 
		
		outputList.add(new OutputLine(indentLvl, "local bit m_has_reset = 0;")); 
		outputList.add(new OutputLine(indentLvl, "local uvm_reg_data_t m_reset_value;"));  // reset value for vreg group (null by default)
		
		outputList.add(new OutputLine(indentLvl, "local uvm_reg_data_t m_staged[longint unsigned];"));  // assoc array of values for accumulating/caching vreg data

		// user defined property array
		outputList.add(new OutputLine(indentLvl, "local string m_def_properties[ string ];")); 

		// create new function
		SystemVerilogFunction func = new SystemVerilogFunction(null, "new");
		func.addIO("input", "string", "name", "\"uvm_vreg_rdl\"");
		func.addIO("input", "int unsigned", "n_bits", "0");
		func.addStatement("super.new(name, n_bits);");
		outputList.addAll(func.genOutputLines(indentLvl));	
				
		// add reg test info methods
		buildRegTestInfoMethods(outputList, indentLvl);
		
		// add user defined property methods
		buildUserDefinedPropertyMethods(outputList, indentLvl);

		// add reset getters/setters
		func = new SystemVerilogFunction("bit", "has_reset_value");
		func.addComment("Return 1 if a reset value exists for this vreg set.");
		func.addStatement("return m_has_reset;");
		outputList.addAll(func.genOutputLines(indentLvl));	
				
		func = new SystemVerilogFunction("uvm_reg_data_t", "get_reset_value");
		func.addComment("Return the reset value for this vreg set.");
		func.addStatement("return m_reset_value;");
		outputList.addAll(func.genOutputLines(indentLvl));	
				
		func = new SystemVerilogFunction("void", "set_reset_value");
		func.addComment("Set the reset value for each register in this vreg set.");
		func.addIO("uvm_reg_data_t", "reset_value");
		func.addStatement("m_has_reset = 1;");
		func.addStatement("m_reset_value = reset_value;");
		outputList.addAll(func.genOutputLines(indentLvl));	
				
		// staged data methods  
		func = new SystemVerilogFunction("uvm_reg_data_t", "get_staged");
		func.addComment("Return the value at the specified stage array idx. If no valid data exists at specified index,");
		func.addComment("the reset value associated with this vreg will be returned.  If no reset value exists, a uvm_error");
		func.addComment("will be thrown.");
		func.addIO("longint unsigned", "stage_idx");
		func.addStatement("if (m_staged.exists(stage_idx)) return m_staged[stage_idx];");
		func.addStatement("else if (has_reset_value()) return m_reset_value;");
		func.addStatement("`uvm_error(\"RegModel\", $sformatf(\"Accessing uninitialized staged value at index %d. (uvm_vreg_rdl::get_staged())\", stage_idx));");
		func.addStatement("return 0;");
		outputList.addAll(func.genOutputLines(indentLvl));	
				
		func = new SystemVerilogFunction("uvm_reg_data_t", "get_staged_field");
		func.addComment("Return the value of the field with specified name at a given stage array idx.");
		func.addIO("longint unsigned", "stage_idx");
		func.addIO("string", "name");
		func.addStatement("uvm_reg_data_t rvalue;");
		func.addStatement("uvm_vreg_field vfld;");
		func.addStatement("int unsigned lsb;");
		func.addStatement("int unsigned fsize;");
		func.addStatement("vfld = this.get_field_by_name(name);");
		func.addStatement("if (vfld == null) begin;");
		func.addStatement("  `uvm_error(\"RegModel\", $sformatf(\"Unable to find field \\\"%s\\\" specified in uvm_vreg_rdl::get_staged_data()\", name));");
		func.addStatement("  return 0;");
		func.addStatement("end");
		func.addStatement("lsb = vfld.get_lsb_pos_in_register();");
		func.addStatement("fsize = vfld.get_n_bits();");
		func.addStatement("rvalue = get_staged(stage_idx);");
		addGetSliceStatements(func, "rvalue", "lsb", "fsize");
		outputList.addAll(func.genOutputLines(indentLvl));	
				
		func = new SystemVerilogFunction("void", "set_staged");  // set staged value at specified idx
		func.addComment("Set the value stored at the specified stage array idx.");
		func.addIO("longint unsigned", "stage_idx");
		func.addIO("uvm_reg_data_t", "staged");
		func.addStatement("m_staged[stage_idx] = staged;");
		outputList.addAll(func.genOutputLines(indentLvl));	
				
		func = new SystemVerilogFunction("void", "stage_field");  // set field value in specified staged array idx
		func.addComment("Set the value of the field with specified name stored at a given stage array idx.");
		func.addComment("Other bit locations in the target stage array location are not modified.");
		func.addIO("longint unsigned", "stage_idx");
		func.addIO("string", "name");
		func.addIO("uvm_reg_data_t", "value");
		func.addStatement("uvm_vreg_field vfld;");
		func.addStatement("int unsigned lsb;");
		func.addStatement("int unsigned fsize;");
		func.addStatement("vfld = this.get_field_by_name(name);");
		func.addStatement("if (vfld == null) begin;");
		func.addStatement("  `uvm_error(\"RegModel\", $sformatf(\"Unable to find field \\\"%s\\\" specified in uvm_vreg_rdl::stage_field()\", name));");
		func.addStatement("  return;");
		func.addStatement("end");
		func.addStatement("lsb = vfld.get_lsb_pos_in_register();");
		func.addStatement("fsize = vfld.get_n_bits();");
		func.addStatement("if (value >> fsize) begin");
		func.addStatement("  `uvm_warning(\"RegModel\", $sformatf(\"Staging value 'h%h that is greater than field \\\"%s\\\" size (%0d bits)\", value, name, fsize));");
		func.addStatement("  value &= ((1<<fsize)-1);");
		func.addStatement("end");
		func.addStatement("if (!m_staged.exists(stage_idx)) begin");
		func.addStatement("  if (has_reset_value()) m_staged[stage_idx] = m_reset_value;");
		func.addStatement("  else m_staged[stage_idx] = 0;");
		func.addStatement("end");
		addSetSliceStatements(func, "m_staged[stage_idx]", "value", "lsb", "fsize");
		outputList.addAll(func.genOutputLines(indentLvl));	
				
		SystemVerilogTask task = new SystemVerilogTask("write_staged");
		task.setVirtual();
		task.addComment("Write the stage array value stored at stage_idx to dut memory at offset vreg_idx");	
		task.addIO("input", "longint unsigned", "stage_idx", null);
		task.addIO("input", "longint unsigned", "vreg_idx", null);
		task.addIO("output", "uvm_status_e", "status", null);
		task.addIO("input", "uvm_path_e", "path", "UVM_DEFAULT_PATH");
		task.addIO("input", "uvm_reg_map", "map", "null");
		task.addIO("input", "uvm_sequence_base", "parent", "null");
		task.addIO("input", "uvm_object", "extension", "null");
		task.addIO("input", "string", "fname", "\"\"");
		task.addIO("input", "int", "lineno", "0");
		task.addStatement("if (!m_staged.exists(stage_idx)) begin");
		task.addStatement("  `uvm_error(\"RegModel\", $sformatf(\"Attempting write of uninitialized staged value at index %d. (uvm_vreg_rdl::write_staged())\", stage_idx));");
		task.addStatement("  return;");
		task.addStatement("end");
		task.addStatement("this.write(vreg_idx, status, m_staged[stage_idx], path, map, parent, extension, fname, lineno);");
		outputList.addAll(task.genOutputLines(indentLvl));	
		
		task = new SystemVerilogTask("write_same_staged");
		task.setVirtual();
		task.addComment("Write the stage array value stored at idx to dut memory at same idx offset");	
		task.addIO("input", "longint unsigned", "idx", null);
		task.addIO("output", "uvm_status_e", "status", null);
		task.addIO("input", "uvm_path_e", "path", "UVM_DEFAULT_PATH");
		task.addIO("input", "uvm_reg_map", "map", "null");
		task.addIO("input", "uvm_sequence_base", "parent", "null");
		task.addIO("input", "uvm_object", "extension", "null");
		task.addIO("input", "string", "fname", "\"\"");
		task.addIO("input", "int", "lineno", "0");
		task.addStatement("this.write_staged(idx, idx, status, path, map, parent, extension, fname, lineno);");
		outputList.addAll(task.genOutputLines(indentLvl));	
		
		// close out the class definition
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(--indentLvl, "endclass : uvm_vreg_rdl"));
	}

	/** build uvm_mem_rdl class extension of uvm_mem  
	 * @param outputList - list of output lines to be updated
	 * @param indentLvl */   
	private static void buildUvmMemRdlClass(List<OutputLine> outputList, int indentLvl) {
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl, "// uvm_mem_rdl class"));
		outputList.add(new OutputLine(indentLvl++, "class uvm_mem_rdl extends uvm_mem;")); 
		
		// create new function
		SystemVerilogFunction func = new SystemVerilogFunction(null, "new");
		func.addIO("input", "string", "name", "\"uvm_mem_rdl\"");
		func.addIO("input", "longint unsigned", "size", "1");
		func.addIO("input", "int unsigned", "n_bits", "0");
		func.addIO("input", "string", "access", "\"RW\"");
		func.addIO("input", "int", "has_coverage", "UVM_NO_COVERAGE");
		func.addStatement("super.new(name, size, n_bits, access, has_coverage);");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// close out the class definition
		outputList.add(new OutputLine(indentLvl, ""));	
		//outputList.add(new OutputLine(indentLvl, "`uvm_object_utils(uvm_reg_rdl)")); //NOFACTORY
		outputList.add(new OutputLine(--indentLvl, "endclass : uvm_mem_rdl"));
	}

	/** build uvm_reg_block_rdl class extension of uvm_reg_block  
	 * @param outputList - list of output lines to be updated
	 * @param indentLvl */   
	private static void buildUvmRegBlockRdlClass(List<OutputLine> outputList, int indentLvl) {
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl, "// uvm_reg_block_rdl class"));
		outputList.add(new OutputLine(indentLvl++, "class uvm_reg_block_rdl extends uvm_reg_block;")); 
		
		// tag used for rdl name generation
		outputList.add(new OutputLine(indentLvl, "string m_rdl_tag;")); 
		outputList.add(new OutputLine(indentLvl, "bit m_rdl_address_map = 0;")); 
		outputList.add(new OutputLine(indentLvl, "string m_rdl_address_map_hdl_path = \"\";")); 
		
		// user defined property array
		outputList.add(new OutputLine(indentLvl, "local string m_def_properties[ string ];")); 
		
		// create new function
		SystemVerilogFunction func = new SystemVerilogFunction(null, "new");
		func.addIO("input", "string", "name", "\"uvm_reg_block_rdl\"");
		func.addIO("input", "int", "has_coverage", "UVM_NO_COVERAGE");
		func.addStatement("super.new(name, has_coverage);");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// set_rdl_address_map
		func = new SystemVerilogFunction("void", "set_rdl_address_map");
		func.addComment("Set indication that this block represents an rdl addrmap.");	
		func.addIO("input", "bit", "val", "0");
		func.addStatement("m_rdl_address_map = val;");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// set_rdl_address_map
		func = new SystemVerilogFunction("void", "set_rdl_address_map_hdl_path");
		func.addComment("Set the root hdl path to the address map module that this block represents.");	
		func.addIO("input", "string", "path", "\"\"");
		func.addStatement("m_rdl_address_map_hdl_path = path;");
		func.addStatement("this.set_hdl_path_root(path);");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// add rdl path gen methods
		buildBlockRdlPathMethods(outputList, indentLvl);
		
		// add_callbacks method
		buildAddCallbacksMethod(outputList, indentLvl);
		
		// add get ancestor method
		buildRdlAncestorMethod(outputList, indentLvl);
		
		// add user defined property methods
		buildUserDefinedPropertyMethods(outputList, indentLvl);
		
		// close out the class definition
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl, "`uvm_object_utils(uvm_reg_block_rdl)"));
		outputList.add(new OutputLine(--indentLvl, "endclass : uvm_reg_block_rdl"));
	}

	/** create class definition for rdl fields  
	 * @param outputList - list of output lines to be updated
	 * @param indentLvl */   
	private static void buildUvmRegFieldRdlClass(List<OutputLine> outputList, int indentLvl) {
		// generate class header 
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl, "// derived rdl field class "));
		outputList.add(new OutputLine(indentLvl++, "class uvm_reg_field_rdl extends uvm_reg_field;"));   
				
		// defines   
		outputList.add(new OutputLine(indentLvl, "protected bit m_is_counter = 0;")); 
		outputList.add(new OutputLine(indentLvl, "protected bit m_is_interrupt = 0;")); 
		outputList.add(new OutputLine(indentLvl, "protected bit m_is_dontcompare = 0;")); 
		outputList.add(new OutputLine(indentLvl, "local bit m_is_sw_readable = 1;")); 
		outputList.add(new OutputLine(indentLvl, "local bit m_is_sw_writeable = 1;")); 
		outputList.add(new OutputLine(indentLvl, "local bit m_is_hw_readable = 1;")); 
		outputList.add(new OutputLine(indentLvl, "local bit m_is_hw_writeable = 0;")); 
		outputList.add(new OutputLine(indentLvl, "local bit m_has_hw_we = 0;")); 
		outputList.add(new OutputLine(indentLvl, "local bit m_has_hw_wel = 0;")); 
		outputList.add(new OutputLine(indentLvl, "local bit m_is_unsupported = 0;")); 
		outputList.add(new OutputLine(indentLvl, "local int unsigned m_js_subcategory = 0;")); 
		
		// user defined property array
		outputList.add(new OutputLine(indentLvl, "local string m_def_properties[ string ];")); 
		
		// create new function
		SystemVerilogFunction func = new SystemVerilogFunction(null, "new");
		func.addIO("input", "string", "name", "\"uvm_reg_field_rdl\"");
		func.addStatement("super.new(name);");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// add user defined property methods
		buildUserDefinedPropertyMethods(outputList, indentLvl);
		
		// get_rdl_register - get the rdl reg parent for this field
		func = new SystemVerilogFunction("uvm_reg_rdl", "get_rdl_register");
		func.addComment("Return the uvm_reg_rdl parent of this field.");	
		func.addStatement("uvm_reg_rdl rdl_reg;");
		func.addStatement("$cast(rdl_reg, get_register());");
		func.addStatement("return rdl_reg;");
		outputList.addAll(func.genOutputLines(indentLvl));	

		// set_hw_access function
		func = new SystemVerilogFunction("void", "set_rdl_access_info");
		func.addComment("Set rdl hw/sw access info for this field.");	
		func.addIO("bit", "is_sw_readable");
		func.addIO("bit", "is_sw_writeable");
		func.addIO("bit", "is_hw_readable");
		func.addIO("bit", "is_hw_writeable");
		func.addIO("bit", "has_hw_we");
		func.addIO("bit", "has_hw_wel");
		func.addStatement("m_is_sw_readable = is_sw_readable;");
		func.addStatement("m_is_sw_writeable = is_sw_writeable;");
		func.addStatement("m_is_hw_readable = is_hw_readable;");
		func.addStatement("m_is_hw_writeable = is_hw_writeable;");
		func.addStatement("m_has_hw_we = has_hw_we;");
		func.addStatement("m_has_hw_wel = has_hw_wel;");
		outputList.addAll(func.genOutputLines(indentLvl));	

		// read data output
		func = new SystemVerilogFunction("string", "get_hw_read_signal");
		func.addComment("Return the name of the read data output signal for this field.");	
		func.addComment("Note that a signal having this name may not exist in the RTL depending on field options.");	
		func.addStatement("uvm_reg_rdl rdl_reg;");
		func.addStatement("rdl_reg = this.get_rdl_register();");
		func.addStatement("return {rdl_reg.get_rdl_name(\"l2h_\", 1), this.get_name(), \"_r\"};");
		outputList.addAll(func.genOutputLines(indentLvl));	

		// write data input
		func = new SystemVerilogFunction("string", "get_hw_write_signal");
		func.addComment("Return the name of the write data input signal for this field.");	
		func.addComment("Note that a signal having this name may not exist in the RTL depending on field options.");	
		func.addStatement("uvm_reg_rdl rdl_reg;");
		func.addStatement("rdl_reg = this.get_rdl_register();");
		func.addStatement("return {rdl_reg.get_rdl_name(\"h2l_\", 1), this.get_name(), \"_w\"};");
		outputList.addAll(func.genOutputLines(indentLvl));	

		// write data enable input
		func = new SystemVerilogFunction("string", "get_hw_we_signal");
		func.addComment("Return the name of the active high write enable input signal for this field.");	
		func.addComment("Note that a signal having this name may not exist in the RTL depending on field options.");	
		func.addStatement("uvm_reg_rdl rdl_reg;");
		func.addStatement("rdl_reg = this.get_rdl_register();");
		func.addStatement("return {rdl_reg.get_rdl_name(\"h2l_\", 1), this.get_name(), \"_we\"};");
		outputList.addAll(func.genOutputLines(indentLvl));	

		// write data enable input
		func = new SystemVerilogFunction("string", "get_hw_wel_signal");
		func.addComment("Return the name of the active low write enable input signal for this field.");	
		func.addComment("Note that a signal having this name may not exist in the RTL depending on field options.");	
		func.addStatement("uvm_reg_rdl rdl_reg;");
		func.addStatement("rdl_reg = this.get_rdl_register();");
		func.addStatement("return {rdl_reg.get_rdl_name(\"h2l_\", 1), this.get_name(), \"_wel\"};");
		outputList.addAll(func.genOutputLines(indentLvl));	

		// is_sw_readable
		func = new SystemVerilogFunction("bit", "is_sw_readable");
		func.addComment("Returns 1 if this field can be read by sw.");	
		func.addStatement("return m_is_sw_readable;");
		outputList.addAll(func.genOutputLines(indentLvl));	

		// is_sw_writeable
		func = new SystemVerilogFunction("bit", "is_sw_writeable");
		func.addComment("Returns 1 if this field can be written by sw.");	
		func.addStatement("return m_is_sw_writeable;");
		outputList.addAll(func.genOutputLines(indentLvl));	

		// is_hw_readable
		func = new SystemVerilogFunction("bit", "is_hw_readable");
		func.addComment("Returns 1 if this field can be read by hw.");	
		func.addStatement("return m_is_hw_readable;");
		outputList.addAll(func.genOutputLines(indentLvl));	

		// is_hw_writeable
		func = new SystemVerilogFunction("bit", "is_hw_writeable");
		func.addComment("Returns 1 if this field can be written by hw.");	
		func.addStatement("return m_is_hw_writeable;");
		outputList.addAll(func.genOutputLines(indentLvl));	

		// has_hw_we
		func = new SystemVerilogFunction("bit", "has_hw_we");
		func.addComment("Returns 1 if this field has an active high hw write enable.");	
		func.addStatement("return m_has_hw_we;");
		outputList.addAll(func.genOutputLines(indentLvl));	

		// has_hw_wel
		func = new SystemVerilogFunction("bit", "has_hw_wel");
		func.addComment("Returns 1 if this field has an active low hw write enable.");	
		func.addStatement("return m_has_hw_wel;");
		outputList.addAll(func.genOutputLines(indentLvl));	

		// is_counter
		func = new SystemVerilogFunction("bit", "is_counter");
		func.addComment("Returns 1 if this field is a counter.  It is assumed this class can be cast to");	
		func.addComment("uvm_reg_field_rdl_counter if this value is set.");	
		func.addStatement("return m_is_counter;");
		outputList.addAll(func.genOutputLines(indentLvl));	

		// is_interrupt
		func = new SystemVerilogFunction("bit", "is_interrupt");
		func.addComment("Returns 1 if this field is an interrupt.  It is assumed this class can be cast to");	
		func.addComment("uvm_reg_field_rdl_interrupt if this value is set.");	
		func.addStatement("return m_is_interrupt;");
		outputList.addAll(func.genOutputLines(indentLvl));	

		// is_unsupported
		func = new SystemVerilogFunction("void", "set_unsupported");
		func.addComment("Sets indication that this field has functionality that is unsupported in the uvm model (use tbd).");	
		func.addStatement("m_is_unsupported = 1;");
		outputList.addAll(func.genOutputLines(indentLvl));	

		func = new SystemVerilogFunction("bit", "is_unsupported");
		func.addComment("Returns 1 if this field has functionality that is unsupported in the uvm model (use tbd).");	
		func.addStatement("return m_is_unsupported;");
		outputList.addAll(func.genOutputLines(indentLvl));	

		// is_dontcompare
		func = new SystemVerilogFunction("void", "set_dontcompare");
		func.addComment("Sets the dontcompare indicator on this field.");	
		func.addStatement("m_is_dontcompare = 1;");
		outputList.addAll(func.genOutputLines(indentLvl));	

		func = new SystemVerilogFunction("bit", "is_dontcompare");
		func.addComment("Returns 1 if this field has dontcompare set.");	
		func.addStatement("return m_is_dontcompare;");
		outputList.addAll(func.genOutputLines(indentLvl));	

		// has_js_subcategory
		func = new SystemVerilogFunction("bit", "has_a_js_subcategory");
		func.addComment("Returns 1 if this field has a jspec subcategory.");	
		func.addStatement("return (m_js_subcategory > 0);");
		outputList.addAll(func.genOutputLines(indentLvl));	

		// has_js_subcategory
		func = new SystemVerilogFunction("bit", "has_js_subcategory");
		func.addComment("Returns 1 if this field has the specified jspec subcategory.");	
		func.addIO("js_subcategory_e", "cat");
		func.addStatement("return ((cat & m_js_subcategory) > 0);");
		outputList.addAll(func.genOutputLines(indentLvl));	

		// add_js_subcategory
		func = new SystemVerilogFunction("void", "add_js_subcategory");
		func.addComment("Sets the specified jspec subcategory for this field.");	
		func.addIO("js_subcategory_e", "cat");
		func.addStatement("m_js_subcategory = m_js_subcategory | cat;");
		outputList.addAll(func.genOutputLines(indentLvl));	

		// remove_js_subcategory
		func = new SystemVerilogFunction("void", "remove_js_subcategory");
		func.addComment("Removes the specified jspec subcategory for this field.");	
		func.addIO("js_subcategory_e", "cat");
		func.addStatement("m_js_subcategory = m_js_subcategory & ~cat;");
		outputList.addAll(func.genOutputLines(indentLvl));	

		// set_js_subcategory
		func = new SystemVerilogFunction("void", "set_js_subcategory");
		func.addComment("Sets the specified set of encoded jspec subcategories for this field.");	
		func.addIO("int", "js_subcategory");
		func.addStatement("m_js_subcategory = js_subcategory;");
		outputList.addAll(func.genOutputLines(indentLvl));	

		outputList.add(new OutputLine(indentLvl, ""));	
		//outputList.add(new OutputLine(indentLvl, "`uvm_object_utils(uvm_reg_field_rdl)"));  //NOFACTORY
		outputList.add(new OutputLine(--indentLvl, "endclass : uvm_reg_field_rdl"));
	}

	/** create class definition for interrupt fields
	 * @param outputList - list of output lines to be updated
	 * @param indentLvl */
	private static void buildUvmRegFieldRdlInterruptClass(List<OutputLine> outputList, int indentLvl) {
		// generate class header 
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl, "// interrupt field class "));
		outputList.add(new OutputLine(indentLvl++, "class uvm_reg_field_rdl_interrupt extends uvm_reg_field_rdl;"));   
				
		// defines
		outputList.add(new OutputLine(indentLvl, "local string m_intr_sig;"));  
		outputList.add(new OutputLine(indentLvl, "local int m_intr_level_type = 0;"));  // {LEVEL, POSEDGE, NEGEDGE, BOTHEDGE}
		outputList.add(new OutputLine(indentLvl, "local int m_intr_sticky_type = 0;"));  // {STICKYBIT, STICKY, NONSTICKY}
		outputList.add(new OutputLine(indentLvl, "local bit m_is_halt = 0;"));
		outputList.add(new OutputLine(indentLvl, "local bit m_mask_intr_bits = 0;"));
		
		outputList.add(new OutputLine(indentLvl, "local uvm_reg_field_rdl m_intr_mask_fld;"));  // field value used as intr mask/enable
		outputList.add(new OutputLine(indentLvl, "local bit m_intr_mask_fld_is_enable;"));  // 1 if enable, 0 if mask type
		outputList.add(new OutputLine(indentLvl, "local uvm_reg_field_rdl m_halt_mask_fld;"));  // field value used as halt mask/enable
		outputList.add(new OutputLine(indentLvl, "local bit m_halt_mask_fld_is_enable;"));  // 1 if enable, 0 if mask type
		outputList.add(new OutputLine(indentLvl, "local uvm_reg_rdl m_cascade_intr_reg;"));  // cascaded intr reg value used
		outputList.add(new OutputLine(indentLvl, "local bit m_cascade_reg_is_halt;"));  // 1 if halt output from reg is used to set value
		
		outputList.add(new OutputLine(indentLvl, "local uvm_vreg_rdl m_associated_vreg;"));  // virtual register/memory association for this intr
		
		// create new function
		SystemVerilogFunction func = new SystemVerilogFunction(null, "new");
		func.addIO("input", "string", "name", "\"uvm_reg_field_rdl_interrupt\"");
		func.addStatement("super.new(name);");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// add_intr function
		func = new SystemVerilogFunction("void", "add_intr");
		func.addComment("Set interrupt info for this field.");	
		func.addIO("input", "int", "intr_level_type", "0");
		func.addIO("input", "int", "intr_sticky_type", "0");
		func.addIO("input", "string", "intr_sig", "\"\"");
		func.addIO("input", "bit", "mask_intr_bits", "0");
		func.addStatement("m_is_interrupt = 1;");
		func.addStatement("if (intr_level_type > 0) m_intr_level_type = intr_level_type;");
		func.addStatement("if (intr_sticky_type > 0) m_intr_sticky_type = intr_sticky_type;");
		func.addStatement("if (intr_sig.len() > 0) m_intr_sig = intr_sig;");
		func.addStatement("m_mask_intr_bits = mask_intr_bits;");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// get intr input signal
		func = new SystemVerilogFunction("string", "get_intr_signal");
		func.addComment("Return the name of the interrupt input signal for this field.");	
		func.addStatement("uvm_reg_rdl rdl_reg;");
		func.addStatement("string intr_signal;");
		func.addStatement("rdl_reg = this.get_rdl_register();");
		func.addStatement("if (m_intr_sig.len() > 0) intr_signal = {rdl_reg.get_rdl_name(\"l2h_\", 1), m_intr_sig};");  //FIXME
		func.addStatement("else intr_signal = {rdl_reg.get_rdl_name(\"h2l_\", 1), this.get_name(), \"_intr\"};");
		func.addStatement("//$display(\"---  getting interrupt signal: %s\", intr_signal);");
		func.addStatement("return intr_signal;");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// get intr output signal for parent reg
		func = new SystemVerilogFunction("string", "get_intr_out_signal");
		func.addComment("Return the name of the interrupt output signal for this field's parent register.");	
		func.addStatement("uvm_reg_rdl rdl_reg;");
		func.addStatement("rdl_reg = this.get_rdl_register();");
		func.addStatement("return {rdl_reg.get_rdl_name(\"l2h_\", 1), \"_intr_o\"};");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// intr_level_type value // LEVEL(0), POSEDGE(1), NEGEDGE(2), BOTHEDGE(3)
		func = new SystemVerilogFunction("int", "get_intr_level_type");
		func.addComment("Return the interrupt level type for this field.");	
		func.addComment("   Values: LEVEL(0), POSEDGE(1), NEGEDGE(2), BOTHEDGE(3)");	
		func.addStatement("return m_intr_level_type;");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// intr_sticky_type value // STICKYBIT(0), STICKY(1), NONSTICKY(2)
		func = new SystemVerilogFunction("int", "get_intr_sticky_type");
		func.addComment("Return the interrupt sticky type for this field.");	
		func.addComment("   Values: STICKYBIT(0), STICKY(1), NONSTICKY(2)");	
		func.addStatement("return m_intr_sticky_type;");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
        // get mask intr bits indicator
		func = new SystemVerilogFunction("bit", "get_mask_intr_bits");
		func.addComment("Return 1 if the value of this field will be modified by enables/masks.");	
		func.addStatement("return m_mask_intr_bits;");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// add_halt function
		func = new SystemVerilogFunction("void", "add_halt");
		func.addComment("Set indication that this field will contribute to halt.");	
		func.addStatement("m_is_halt = 1;");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// get halt indication
		func = new SystemVerilogFunction("bit", "is_halt");
		func.addComment("Return 1 if halt is set for this interrupt field.");	
		func.addStatement("return m_is_halt;");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// get halt output signal for parent reg
		func = new SystemVerilogFunction("string", "get_halt_out_signal");
		func.addComment("Return the name of the halt output signal for this field's parent register.");	
		func.addStatement("uvm_reg_rdl rdl_reg;");
		func.addStatement("rdl_reg = this.get_rdl_register();");
		func.addStatement("return {rdl_reg.get_rdl_name(\"l2h_\", 1), \"_halt_o\"};");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// set field that will be used as mask or enable of intr output from this field
		func = new SystemVerilogFunction("void", "set_intr_mask_field");
		func.addComment("Set the uvm_reg_field whose value is used as an intr mask or enable for this interrupt field.");	
		func.addIO("uvm_reg_field", "intr_mask_fld");
		func.addIO("bit", "intr_mask_fld_is_enable");
		func.addStatement("$cast(m_intr_mask_fld, intr_mask_fld);");
		func.addStatement("m_intr_mask_fld_is_enable = intr_mask_fld_is_enable;");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		func = new SystemVerilogFunction("uvm_reg_field_rdl", "get_intr_mask_field");
		func.addComment("Return the uvm_reg_field_rdl whose value is used as an intr mask or enable for this interrupt field.");	
		func.addStatement("return m_intr_mask_fld;");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		func = new SystemVerilogFunction("bit", "has_intr_mask_field");
		func.addComment("Return 1 if an intr mask or enable field is assigned for this interrupt field.");	
		func.addStatement("return (m_intr_mask_fld != null);");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		func = new SystemVerilogFunction("bit", "intr_mask_field_is_enable");
		func.addComment("Return 1 if an intr enable field is assigned for this interrupt field, otherwise mask is assumed.");	
		func.addStatement("return m_intr_mask_fld_is_enable;");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// get field value after applying the intr mask/enable
		func = new SystemVerilogFunction("uvm_reg_data_t", "get_intr_masked");
		func.addComment("Return the field value after applying any intr masks/enables.");	
		func.addStatement("uvm_reg_field_rdl mask_fld;");
		func.addStatement("if (has_intr_mask_field()) begin");
		func.addStatement("  if (intr_mask_field_is_enable()) return get() & m_intr_mask_fld.get();");
		func.addStatement("  else return get() & ~m_intr_mask_fld.get();");
		func.addStatement("end");
		func.addStatement("return get();");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// set field that will be used as mask or enable of halt output from this field
		func = new SystemVerilogFunction("void", "set_halt_mask_field");
		func.addComment("Set the uvm_reg_field whose value is used as an halt mask or enable for this interrupt field.");	
		func.addIO("uvm_reg_field", "halt_mask_fld");
		func.addIO("bit", "halt_mask_fld_is_enable");
		func.addStatement("$cast(m_halt_mask_fld, halt_mask_fld);");
		func.addStatement("m_halt_mask_fld_is_enable = halt_mask_fld_is_enable;");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		func = new SystemVerilogFunction("uvm_reg_field_rdl", "get_halt_mask_field");
		func.addComment("Return the uvm_reg_field_rdl whose value is used as an halt mask or enable for this interrupt field.");	
		func.addStatement("return m_halt_mask_fld;");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		func = new SystemVerilogFunction("bit", "has_halt_mask_field");
		func.addComment("Return 1 if a halt mask or enable field is assigned for this interrupt field.");	
		func.addStatement("return (m_halt_mask_fld != null);");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		func = new SystemVerilogFunction("bit", "halt_mask_field_is_enable"); 
		func.addComment("Return 1 if an halt enable field is assigned for this interrupt field, otherwise mask is assumed.");	
		func.addStatement("return m_halt_mask_fld_is_enable;");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// get field value after applying the halt mask/enable
		func = new SystemVerilogFunction("uvm_reg_data_t", "get_halt_masked");
		func.addComment("Return the field value after applying any halt masks/enables.");	
		func.addStatement("uvm_reg_field_rdl mask_fld;");
		func.addStatement("if (has_halt_mask_field()) begin"); // if a mask/enable then apply
		func.addStatement("  if (halt_mask_field_is_enable()) return get() & m_halt_mask_fld.get();"); // enable field
		func.addStatement("  else return get() & ~m_halt_mask_fld.get();"); // mask field
		func.addStatement("end");
		func.addStatement("return get();"); // otherwise use std get
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// set cascaded intr reg that will set value of this field
		func = new SystemVerilogFunction("void", "set_cascade_intr_reg");
		func.addComment("Set a register whose intr or halt output drives this interrupt field.  This creates an interrupt");	
		func.addComment("hierarchy in the model where fields in the specified register are child interrupts of the current field.");	
		func.addIO("uvm_reg", "cascade_intr_reg");
		func.addIO("bit", "cascade_reg_is_halt");
		func.addStatement("$cast(m_cascade_intr_reg, cascade_intr_reg);");
		func.addStatement("m_cascade_reg_is_halt = cascade_reg_is_halt;");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		func = new SystemVerilogFunction("uvm_reg_rdl", "get_cascade_intr_reg");
		func.addComment("Return the uvm_reg_rdl whose intr or halt value drives this interrupt field.");	
		func.addStatement("return m_cascade_intr_reg;");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		func = new SystemVerilogFunction("bit", "has_cascade_intr_reg");
		func.addComment("Return 1 if this interrupt field is driven by a child interrupt register.");	
		func.addStatement("return (m_cascade_intr_reg != null);");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		func = new SystemVerilogFunction("bit", "cascade_reg_is_halt");
		func.addComment("Return 1 if this interrupt field is driven by a child register's halt (else intr).");	
		func.addStatement("return m_cascade_reg_is_halt;");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// default get_src_intr_fields methods (overridden in intr reg child classes)
		func = new SystemVerilogFunction("void", "get_intr_fields");  
		func.addComment("Return all leaf child interrupt fields in this field's interrupt hierarchy.");	
		func.addIOArray("ref uvm_reg_field", "fields");
		func.addStatement("if (has_cascade_intr_reg()) m_cascade_intr_reg.get_intr_fields(fields);");
		func.addStatement("else fields.push_back(this);");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// return all active source intr/halt fields
		SystemVerilogTask task = new SystemVerilogTask("get_active_intr_fields");
		task.addComment("Return all leaf child interrupt fields in this field's interrupt hierarchy that.");	
		task.addComment("are contributing to an active intr or halt.");	
		task.addIOArray("ref", "uvm_reg_field", "fields", null);
		task.addIO("input", "bit", "is_halt", null);
		task.addIO("input", "uvm_path_e", "path", "UVM_DEFAULT_PATH");
		task.addStatement("if (has_cascade_intr_reg()) m_cascade_intr_reg.get_active_intr_fields(fields, m_cascade_reg_is_halt, path);");
		task.addStatement("else if (is_halt && (|get_halt_masked())) fields.push_back(this);");
		task.addStatement("else if (!is_halt && (|get_intr_masked())) fields.push_back(this);");
		outputList.addAll(task.genOutputLines(indentLvl));	
		
		// set uvm_vreg_rdl that is associated with this interrupt field
		func = new SystemVerilogFunction("void", "set_associated_vreg");
		func.addComment("Set the uvm_vreg_rdl that is associated with this interrupt field.");	
		func.addIO("uvm_vreg_rdl", "associated_vreg");
		func.addStatement("m_associated_vreg = associated_vreg;");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		func = new SystemVerilogFunction("uvm_vreg_rdl", "get_associated_vreg");
		func.addComment("Return the uvm_vreg_rdl that is associated with this interrupt field.");	
		func.addStatement("return m_associated_vreg;");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		func = new SystemVerilogFunction("bit", "has_associated_vreg");
		func.addComment("Return 1 if a uvm_vreg_rdl is associated with this interrupt field.");	
		func.addStatement("return (m_associated_vreg != null);");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		outputList.add(new OutputLine(indentLvl, ""));	
		//outputList.add(new OutputLine(indentLvl, "`uvm_object_utils(uvm_reg_field_rdl_interrupt)")); //NOFACTORY
		outputList.add(new OutputLine(--indentLvl, "endclass : uvm_reg_field_rdl_interrupt"));
	}

	/** create class definition for counter fields  
	 * @param outputList - list of output lines to be updated
	 * @param indentLvl */
	private static void buildUvmRegFieldRdlCounterClass(List<OutputLine> outputList, int indentLvl) {
		// generate class header 
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl, "// counter field class "));
		outputList.add(new OutputLine(indentLvl++, "class uvm_reg_field_rdl_counter extends uvm_reg_field_rdl;"));   
				
		// defines   
		outputList.add(new OutputLine(indentLvl, "local uvm_reg_data_t m_accum_value = 0;"));
		
		outputList.add(new OutputLine(indentLvl, "local string m_incr_sig;"));  
		outputList.add(new OutputLine(indentLvl, "local uvm_reg_data_t m_incr_value = 1;"));  
		outputList.add(new OutputLine(indentLvl, "local string m_incr_value_sig;"));
		outputList.add(new OutputLine(indentLvl, "local int unsigned m_incr_value_sig_width = 0;")); 
		outputList.add(new OutputLine(indentLvl, "local bit m_has_incr_sat = 0;"));  
		outputList.add(new OutputLine(indentLvl, "local uvm_reg_data_t m_incr_sat_value = 0;"));  
		outputList.add(new OutputLine(indentLvl, "local string m_incr_sat_value_sig;"));
		outputList.add(new OutputLine(indentLvl, "local bit m_has_incr_thold = 0;"));  
		outputList.add(new OutputLine(indentLvl, "local uvm_reg_data_t m_incr_thold_value = 0;"));  
		outputList.add(new OutputLine(indentLvl, "local string m_incr_thold_value_sig;"));
		
		outputList.add(new OutputLine(indentLvl, "local string m_decr_sig;"));
		outputList.add(new OutputLine(indentLvl, "local uvm_reg_data_t m_decr_value = 1;"));  
		outputList.add(new OutputLine(indentLvl, "local string m_decr_value_sig;"));
		outputList.add(new OutputLine(indentLvl, "local int unsigned m_decr_value_sig_width = 0;")); 
		outputList.add(new OutputLine(indentLvl, "local bit m_has_decr_sat = 0;"));  
		outputList.add(new OutputLine(indentLvl, "local uvm_reg_data_t m_decr_sat_value = 0;"));  
		outputList.add(new OutputLine(indentLvl, "local string m_decr_sat_value_sig;"));
		outputList.add(new OutputLine(indentLvl, "local bit m_has_decr_thold = 0;"));  
		outputList.add(new OutputLine(indentLvl, "local uvm_reg_data_t m_decr_thold_value = 0;"));  
		outputList.add(new OutputLine(indentLvl, "local string m_decr_thold_value_sig;"));

		// create new function
		SystemVerilogFunction func = new SystemVerilogFunction(null, "new");
		func.addIO("input", "string", "name", "\"uvm_reg_field_rdl_counter\"");
		func.addStatement("super.new(name);");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// get accum value
		func = new SystemVerilogFunction("uvm_reg_data_t", "get_accum_value");
		func.addComment("Return the scratchpad data stored for this counter field.");	
		func.addStatement("return m_accum_value;");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		func = new SystemVerilogFunction("void", "set_accum_value");
		func.addComment("Set the scratchpad data value for this counter field.");	
		func.addIO("uvm_reg_data_t", "accum_value");
		func.addStatement("m_accum_value = accum_value;");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// add_incr function
		func = new SystemVerilogFunction("void", "add_incr");  
		func.addComment("Set incrementing counter info for this field.");	
		func.addIO("uvm_reg_data_t", "incr_value");
		func.addIO("input", "string", "incr_sig", "\"\"");
		func.addIO("input", "string", "incr_value_sig", "\"\"");
		func.addIO("input", "int unsigned", "incr_value_sig_width", "0");
		func.addStatement("m_is_counter = 1;");
		func.addStatement("m_incr_value = incr_value;");
		func.addStatement("if (incr_sig.len() > 0) m_incr_sig = incr_sig;");
		func.addStatement("if (incr_value_sig.len() > 0) m_incr_value_sig = incr_value_sig;");
		func.addStatement("m_incr_value_sig_width = incr_value_sig_width;");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// add_incr sat function
		func = new SystemVerilogFunction("void", "add_incr_sat"); 
		func.addComment("Set incrementing counter saturation value info for this field.");	
		func.addIO("uvm_reg_data_t", "incr_sat_value");
		func.addIO("input", "string", "incr_sat_value_sig", "\"\"");
		func.addStatement("m_has_incr_sat = 1;");
		func.addStatement("m_incr_sat_value = incr_sat_value;");
		func.addStatement("if (incr_sat_value_sig.len() > 0) m_incr_sat_value_sig = incr_sat_value_sig;");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// add_incr thold function
		func = new SystemVerilogFunction("void", "add_incr_thold");
		func.addComment("Set incrementing counter threshold value info for this field.");	
		func.addIO("uvm_reg_data_t", "incr_thold_value");
		func.addIO("input", "string", "incr_thold_value_sig", "\"\"");
		func.addStatement("m_has_incr_thold = 1;");
		func.addStatement("m_incr_thold_value = incr_thold_value;");
		func.addStatement("if (incr_thold_value_sig.len() > 0) m_incr_thold_value_sig = incr_thold_value_sig;");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// get incr input signal
		func = new SystemVerilogFunction("string", "get_incr_signal");
		func.addComment("Return the name of the increment input signal for this field.");	
		func.addComment("Note that a signal having this name may not exist in the RTL depending on field options.");	
		func.addStatement("string incr_signal;");
		func.addStatement("uvm_reg_rdl rdl_reg;");
		func.addStatement("rdl_reg = this.get_rdl_register();");
		func.addStatement("if (m_incr_sig.len() > 0) incr_signal = {rdl_reg.get_rdl_name(\"rg_\", 1), m_incr_sig};"); // FIXME
		func.addStatement("else incr_signal = {rdl_reg.get_rdl_name(\"h2l_\", 1), this.get_name(), \"_incr\"};");
		func.addStatement("//$display(\"---  getting increment signal: %s\", incr_signal);");
		func.addStatement("return incr_signal;");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// overflow output
		func = new SystemVerilogFunction("string", "get_overflow_signal");
		func.addComment("Return the name of the overflow output signal for this field.");	
		func.addComment("Note that a signal having this name may not exist in the RTL depending on field options.");	
		func.addStatement("uvm_reg_rdl rdl_reg;");
		func.addStatement("rdl_reg = this.get_rdl_register();");
		func.addStatement("return {rdl_reg.get_rdl_name(\"l2h_\", 1), this.get_name(), \"_overflow\"};");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// incrsat output
		func = new SystemVerilogFunction("string", "get_incr_sat_signal");
		func.addComment("Return the name of the increment saturation output signal for this field.");	
		func.addComment("Note that a signal having this name may not exist in the RTL depending on field options.");	
		func.addStatement("uvm_reg_rdl rdl_reg;");
		func.addStatement("rdl_reg = this.get_rdl_register();");
		func.addStatement("return {rdl_reg.get_rdl_name(\"l2h_\", 1), this.get_name(), \"_incrsat_o\"};");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// incrthold output
		func = new SystemVerilogFunction("string", "get_incr_thold_signal");
		func.addComment("Return the name of the increment threshold output signal for this field.");	
		func.addComment("Note that a signal having this name may not exist in the RTL depending on field options.");	
		func.addStatement("uvm_reg_rdl rdl_reg;");
		func.addStatement("rdl_reg = this.get_rdl_register();");
		func.addStatement("return {rdl_reg.get_rdl_name(\"l2h_\", 1), this.get_name(), \"_incrthold_o\"};");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// incr value
		func = new SystemVerilogFunction("uvm_reg_data_t", "get_incr_value");
		func.addComment("Return the increment value for this counter field.");	
		func.addStatement("return m_incr_value;");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// get incr value signal
		func = new SystemVerilogFunction("string", "get_incr_value_signal");
		func.addComment("Return the name of the increment value input signal for this field.");	
		func.addComment("Note that a signal having this name may not exist in the RTL depending on field options.");	
		func.addStatement("uvm_reg_rdl rdl_reg;");
		func.addStatement("rdl_reg = this.get_rdl_register();");
		func.addStatement("if (m_incr_value_sig.len() > 0) return rdl_reg.get_rdl_name(\"rg_\", 1, m_incr_value_sig);"); // FIXME
		func.addStatement("else return {rdl_reg.get_rdl_name(\"h2l_\", 1), this.get_name(), \"_incrvalue\"};");  
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// get incr value signal width
		func = new SystemVerilogFunction("int unsigned", "get_incr_value_signal_width");
		func.addComment("Return the increment input signal width for this counter field.");	
		func.addStatement("return m_incr_value_sig_width;");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// incr sat value
		func = new SystemVerilogFunction("bit", "has_incr_sat");
		func.addComment("Return 1 if an increment saturation value/signal is defined for this counter field.");	
		func.addStatement("return m_has_incr_sat;");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		func = new SystemVerilogFunction("uvm_reg_data_t", "get_incr_sat_value");
		func.addComment("Return the increment saturation value for this counter field.");	
		func.addStatement("return m_incr_sat_value;");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// get incr sat value signal
		func = new SystemVerilogFunction("string", "get_incr_sat_value_signal");
		func.addComment("Return the name of the increment saturation value input signal for this field.");	
		func.addComment("Note that a signal having this name may not exist in the RTL depending on field options.");	
		func.addStatement("uvm_reg_rdl rdl_reg;");
		func.addStatement("rdl_reg = this.get_rdl_register();");
		func.addStatement("if (m_incr_sat_value_sig.len() < 1) return \"\";"); // no default input
		func.addStatement("else return rdl_reg.get_rdl_name(\"rg_\", 1, m_incr_sat_value_sig);");  // FIXME assumes peer field w/ no property
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// incr thold value
		func = new SystemVerilogFunction("bit", "has_incr_thold");
		func.addComment("Return 1 if an increment threshold value/signal is defined for this counter field.");	
		func.addStatement("return m_has_incr_thold;");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		func = new SystemVerilogFunction("uvm_reg_data_t", "get_incr_thold_value");
		func.addComment("Return the increment threshold value for this counter field.");	
		func.addStatement("return m_incr_thold_value;");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// get incr thold value signal
		func = new SystemVerilogFunction("string", "get_incr_thold_value_signal");
		func.addComment("Return the name of the increment threshold value input signal for this field.");	
		func.addComment("Note that a signal having this name may not exist in the RTL depending on field options.");	
		func.addStatement("uvm_reg_rdl rdl_reg;");
		func.addStatement("rdl_reg = this.get_rdl_register();");
		func.addStatement("if (m_incr_thold_value_sig.len() < 1) return \"\";"); // no default input
		func.addStatement("else return rdl_reg.get_rdl_name(\"rg_\", 1, m_incr_thold_value_sig);"); // FIXME assumes peer field w/ no property
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// add_decr function
		func = new SystemVerilogFunction("void", "add_decr");  
		func.addComment("Set decrementing counter info for this field.");	
		func.addIO("uvm_reg_data_t", "decr_value");
		func.addIO("input", "string", "decr_sig", "\"\"");
		func.addIO("input", "string", "decr_value_sig", "\"\"");
		func.addIO("input", "int unsigned", "decr_value_sig_width", "0");
		func.addStatement("m_is_counter = 1;");
		func.addStatement("m_decr_value = decr_value;");
		func.addStatement("if (decr_sig.len() > 0) m_decr_sig = decr_sig;");
		func.addStatement("if (decr_value_sig.len() > 0) m_decr_value_sig = decr_value_sig;");
		func.addStatement("m_decr_value_sig_width = decr_value_sig_width;");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// add_decr sat function
		func = new SystemVerilogFunction("void", "add_decr_sat");
		func.addComment("Set decrementing counter saturation value info for this field.");	
		func.addIO("uvm_reg_data_t", "decr_sat_value");
		func.addIO("input", "string", "decr_sat_value_sig", "\"\"");
		func.addStatement("m_has_decr_sat = 1;");
		func.addStatement("m_decr_sat_value = decr_sat_value;");
		func.addStatement("if (decr_sat_value_sig.len() > 0) m_decr_sat_value_sig = decr_sat_value_sig;");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// add_decr thold function
		func = new SystemVerilogFunction("void", "add_decr_thold");
		func.addComment("Set decrementing counter threshold value info for this field.");	
		func.addIO("uvm_reg_data_t", "decr_thold_value");
		func.addIO("input", "string", "decr_thold_value_sig", "\"\"");
		func.addStatement("m_has_decr_thold = 1;");
		func.addStatement("m_decr_thold_value = decr_thold_value;");
		func.addStatement("if (decr_thold_value_sig.len() > 0) m_decr_thold_value_sig = decr_thold_value_sig;");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// get decr input signal
		func = new SystemVerilogFunction("string", "get_decr_signal");
		func.addComment("Return the name of the decrement input signal for this field.");	
		func.addComment("Note that a signal having this name may not exist in the RTL depending on field options.");	
		func.addStatement("string decr_signal;");
		func.addStatement("uvm_reg_rdl rdl_reg;");
		func.addStatement("rdl_reg = this.get_rdl_register();");
		func.addStatement("if (m_decr_sig.len() > 0) decr_signal = {rdl_reg.get_rdl_name(\"rg_\", 1), m_decr_sig};"); // FIXME
		func.addStatement("else decr_signal = {rdl_reg.get_rdl_name(\"h2l_\", 1), this.get_name(), \"_decr\"};");
		func.addStatement("//$display(\"---  getting decrement sigmal: %s\", decr_signal);");
		func.addStatement("return decr_signal;");
		outputList.addAll(func.genOutputLines(indentLvl));	

		// underflow output
		func = new SystemVerilogFunction("string", "get_underflow_signal");
		func.addComment("Return the name of the underflow output signal for this field.");	
		func.addComment("Note that a signal having this name may not exist in the RTL depending on field options.");	
		func.addStatement("uvm_reg_rdl rdl_reg;");
		func.addStatement("rdl_reg = this.get_rdl_register();");
		func.addStatement("return {rdl_reg.get_rdl_name(\"l2h_\", 1), this.get_name(), \"_underflow\"};");
		outputList.addAll(func.genOutputLines(indentLvl));	

		// decrsat output
		func = new SystemVerilogFunction("string", "get_decr_sat_signal");
		func.addComment("Return the name of the decrement saturation output signal for this field.");	
		func.addComment("Note that a signal having this name may not exist in the RTL depending on field options.");	
		func.addStatement("uvm_reg_rdl rdl_reg;");
		func.addStatement("rdl_reg = this.get_rdl_register();");
		func.addStatement("return {rdl_reg.get_rdl_name(\"l2h_\", 1), this.get_name(), \"_decrsat_o\"};");
		outputList.addAll(func.genOutputLines(indentLvl));	

		// decrthold output
		func = new SystemVerilogFunction("string", "get_decr_thold_signal");
		func.addComment("Return the name of the decrement threshold output signal for this field.");	
		func.addComment("Note that a signal having this name may not exist in the RTL depending on field options.");	
		func.addStatement("uvm_reg_rdl rdl_reg;");
		func.addStatement("rdl_reg = this.get_rdl_register();");
		func.addStatement("return {rdl_reg.get_rdl_name(\"l2h_\", 1), this.get_name(), \"_decrthold_o\"};");
		outputList.addAll(func.genOutputLines(indentLvl));	

		// decr value
		func = new SystemVerilogFunction("uvm_reg_data_t", "get_decr_value");
		func.addComment("Return the decrement value for this counter field.");	
		func.addStatement("return m_decr_value;");
		outputList.addAll(func.genOutputLines(indentLvl));	

		// get decr value signal
		func = new SystemVerilogFunction("string", "get_decr_value_signal");
		func.addComment("Return the name of the decrement value input signal for this field.");	
		func.addComment("Note that a signal having this name may not exist in the RTL depending on field options.");	
		func.addStatement("uvm_reg_rdl rdl_reg;");
		func.addStatement("rdl_reg = this.get_rdl_register();");
		func.addStatement("if (m_decr_value_sig.len() > 0) return rdl_reg.get_rdl_name(\"rg_\", 1, m_decr_value_sig);");  // FIXME
		func.addStatement("else return {rdl_reg.get_rdl_name(\"h2l_\", 1), this.get_name(), \"_decrvalue\"};"); 
		outputList.addAll(func.genOutputLines(indentLvl));	

		// get decr value signal width
		func = new SystemVerilogFunction("int unsigned", "get_decr_value_signal_width");
		func.addComment("Return the decrement input signal width for this counter field.");	
		func.addStatement("return m_decr_value_sig_width;");
		outputList.addAll(func.genOutputLines(indentLvl));	

		// decr sat value
		func = new SystemVerilogFunction("bit", "has_decr_sat"); 
		func.addComment("Return 1 if a decrement saturation value/signal is defined for this counter field.");	
		func.addStatement("return m_has_decr_sat;");
		outputList.addAll(func.genOutputLines(indentLvl));	

		func = new SystemVerilogFunction("uvm_reg_data_t", "get_decr_sat_value");
		func.addComment("Return the decrement saturation value for this counter field.");	
		func.addStatement("return m_decr_sat_value;");
		outputList.addAll(func.genOutputLines(indentLvl));	

		// get decr sat value signal
		func = new SystemVerilogFunction("string", "get_decr_sat_value_signal");
		func.addComment("Return the name of the decrement saturation value input signal for this field.");	
		func.addComment("Note that a signal having this name may not exist in the RTL depending on field options.");	
		func.addStatement("uvm_reg_rdl rdl_reg;");
		func.addStatement("rdl_reg = this.get_rdl_register();");
		func.addStatement("if (m_decr_sat_value_sig.len() < 1) return \"\";");  // no default input
		func.addStatement("else return rdl_reg.get_rdl_name(\"rg_\", 1, m_decr_sat_value_sig);"); // FIXME assumes peer field w/ no property
		outputList.addAll(func.genOutputLines(indentLvl));	

		// decr thold value
		func = new SystemVerilogFunction("bit", "has_decr_thold");
		func.addComment("Return 1 if a decrement threshold value/signal is defined for this counter field.");	
		func.addStatement("return m_has_decr_thold;");
		outputList.addAll(func.genOutputLines(indentLvl));	

		func = new SystemVerilogFunction("uvm_reg_data_t", "get_decr_thold_value");
		func.addComment("Return the decrement threshold value for this counter field.");	
		func.addStatement("return m_decr_thold_value;");
		outputList.addAll(func.genOutputLines(indentLvl));	

		// get decr thold value signal
		func = new SystemVerilogFunction("string", "get_decr_thold_value_signal");
		func.addComment("Return the name of the decrement threshold value input signal for this field.");	
		func.addComment("Note that a signal having this name may not exist in the RTL depending on field options.");	
		func.addStatement("uvm_reg_rdl rdl_reg;");
		func.addStatement("rdl_reg = this.get_rdl_register();");
		func.addStatement("if (m_decr_thold_value_sig.len() < 1) return \"\";");  // no default input
		func.addStatement("else return rdl_reg.get_rdl_name(\"rg_\", 1, m_decr_thold_value_sig);"); // FIXME assumes peer field w/ no property
		outputList.addAll(func.genOutputLines(indentLvl));	

		outputList.add(new OutputLine(indentLvl, ""));	
		//outputList.add(new OutputLine(indentLvl, "`uvm_object_utils(uvm_reg_field_rdl_counter)")); //NOFACTORY
		outputList.add(new OutputLine(--indentLvl, "endclass : uvm_reg_field_rdl_counter"));
		
	}

	/** build cbs class to be used for alias registers 
	 * @param outputList - list of output lines to be updated
	 * @param indentLvl */
	public static void buildAliasRegCbsClass(List<OutputLine> outputList, int indentLvl) {
		// generate class header 
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl, "// cbs class for alias register "));
		outputList.add(new OutputLine(indentLvl++, "class rdl_alias_reg_cbs extends uvm_reg_cbs;"));   
				
		// define
		outputList.add(new OutputLine(indentLvl, "uvm_reg  m_alias_regs[$];"));
		
		// create new function
		SystemVerilogFunction func = new SystemVerilogFunction(null, "new");
		func.addIO("input", "string", "name", "\"rdl_alias_reg_cbs\"");
		func.addStatement("super.new(name);");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// create set_alias_regs function
		func = new SystemVerilogFunction("void", "set_alias_regs");
		func.addComment("Set alias register group for this callback.");
		func.addIOArray("uvm_reg", "alias_regs");
		func.addStatement("m_alias_regs = alias_regs;");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// task to set all regs in an alias group to same value post r/w
		SystemVerilogTask task = new SystemVerilogTask("alias_group_predict");
		task.addComment("Set all regs in an alias group to same value post r/w");
		task.addIO("uvm_reg_item", "rw");
		task.addStatement(" uvm_reg_data_t 	updated_value;");
		task.addStatement("if (rw.status != UVM_IS_OK)");
		task.addStatement("  return;");
		task.addStatement("if (rw.element_kind == UVM_REG) begin");
		task.addStatement("  uvm_reg rg;");
		task.addStatement("  $cast(rg, rw.element);");
		task.addStatement("  if (m_alias_regs[0] != null) begin");
		task.addStatement("    updated_value = rg.get();");
		task.addStatement("    foreach (m_alias_regs[i]) begin");
		task.addStatement("      void'(m_alias_regs[i].predict(updated_value));");
		task.addStatement("      //$display(\"  new value for %s is %h\", m_alias_regs[i].get_full_name(), m_alias_regs[i].get());");
		task.addStatement("    end");
		task.addStatement("  end");
		task.addStatement("end");
		outputList.addAll(task.genOutputLines(indentLvl));	
		
		// post_read
		task = new SystemVerilogTask("post_read");
		task.setVirtual();
		task.addComment("Update all regs in an alias group after a read");
		task.addIO("uvm_reg_item", "rw");
		task.addStatement(" //$display(\"*** post_read ***\");");
		task.addStatement(" alias_group_predict(rw);");
		outputList.addAll(task.genOutputLines(indentLvl));	
		
		// post_write
		task = new SystemVerilogTask("post_write");
		task.setVirtual();
		task.addComment("Update all regs in an alias group after a write");
		task.addIO("uvm_reg_item", "rw");
		task.addStatement(" //$display(\"*** post_write ***\");");
		task.addStatement(" alias_group_predict(rw);");
		outputList.addAll(task.genOutputLines(indentLvl));	
		
		// close out the alias cbs definition
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl, "`uvm_object_utils(rdl_alias_reg_cbs)"));
		outputList.add(new OutputLine(--indentLvl, "endclass : rdl_alias_reg_cbs"));
	}

	/** build cbs class to be used for enabled/masked fields 
	 * @param outputList - list of output lines to be updated
	 * @param indentLvl */
	public static void buildMaskIntrFieldCbsClass(List<OutputLine> outputList, int indentLvl) {
		// generate class header 
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl, "// cbs class for enabled/masked intr fields "));
		outputList.add(new OutputLine(indentLvl++, "class rdl_mask_intr_field_cbs extends uvm_reg_cbs;"));   
				
		// define
		outputList.add(new OutputLine(indentLvl, "local uvm_reg_field_rdl_interrupt m_masked_field;"));
		
		// create new function, mask_field=enable/mask field that will affect this field, is_enable=mask or enable boolean
		SystemVerilogFunction func = new SystemVerilogFunction(null, "new");
		func.addIO("input", "string", "name", "\"\"");
		func.addIO("input", "uvm_reg_field", "masked_field", "null");
		func.addStatement("super.new(name);");
		func.addStatement("$cast(m_masked_field, masked_field);");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// override post_predict function
		func = new SystemVerilogFunction("void", "post_predict");
		func.addComment("Override post_predict to update value of field according to mask/enable state.");
		func.setVirtual();
		func.addIO("input", "uvm_reg_field", "fld", null);
		func.addIO("input", "uvm_reg_data_t", "previous", null);
		func.addIO("inout", "uvm_reg_data_t", "value", null);
		func.addIO("input", "uvm_predict_e", "kind", null);
		func.addIO("input", "uvm_path_e", "path", null);
		func.addIO("input", "uvm_reg_map", "map", null);
		func.addStatement("if (kind == UVM_PREDICT_READ) begin");
		func.addStatement("  value = m_masked_field.get_intr_masked();");
		func.addStatement("end");
		outputList.addAll(func.genOutputLines(indentLvl));
		
		// close out the alias cbs definition
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl, "`uvm_object_utils(rdl_mask_intr_field_cbs)"));
		outputList.add(new OutputLine(--indentLvl, "endclass : rdl_mask_intr_field_cbs"));
	}
	
	// ----------- common method gen methods
	
	/** build reg test info methods */
	private static void buildRegTestInfoMethods(List<OutputLine> outputList, int indentLvl) {
		// set_reg_test_info function
		SystemVerilogFunction func = new SystemVerilogFunction("void", "set_reg_test_info");
		func.addComment("Set test related info for this register (donttest, dontcompare, js category).");
		func.addIO("bit", "dont_test");
		func.addIO("bit", "dont_compare");
		func.addIO("int", "js_category");
		func.addStatement("m_dont_test = dont_test;");
		func.addStatement("m_dont_compare = dont_compare;");
		func.addStatement("m_js_category = js_category;");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// is_dont_test
		func = new SystemVerilogFunction("bit", "is_dont_test");
		func.addComment("Return 1 if register is marked don't test.");
		func.addStatement("return m_dont_test;");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// is_dont_compare
		func = new SystemVerilogFunction("bit", "is_dont_compare");
		func.addComment("Return 1 if register is marked don't compare.");
		func.addStatement("return m_dont_compare;");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// has_js_category
		func = new SystemVerilogFunction("bit", "has_a_js_category");
		func.addComment("Return 1 if register has a jspec category.");
		func.addStatement("return (m_js_category > 0);");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// has_js_category
		func = new SystemVerilogFunction("bit", "has_js_category");
		func.addComment("Return 1 if register has the specified jspec category.");
		func.addIO("js_category_e", "cat");
		func.addStatement("return ((cat & m_js_category) > 0);");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// add_js_category
		func = new SystemVerilogFunction("void", "add_js_category");
		func.addComment("Add the specified jspec category to this register.");
		func.addIO("js_category_e", "cat");
		func.addStatement("m_js_category = m_js_category | cat;");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// remove_js_category
		func = new SystemVerilogFunction("void", "remove_js_category");
		func.addComment("Remove the specified jspec category from this register.");
		func.addIO("js_category_e", "cat");
		func.addStatement("m_js_category = m_js_category & ~cat;");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
	}

	/** create rdl path generation methods for uvm_reg derived class   
	 * @param outputList - list of output lines to be updated
	 * @param indentLvl */
	private static void buildRegRdlPathMethods(List<OutputLine> outputList, int indentLvl) {
		// set rdl tag for this hierarchy level
		SystemVerilogFunction func = new SystemVerilogFunction("void", "set_rdl_tag");
		func.addComment("Set the rdl tag string for this register.  The tag will be used to build");
		func.addComment("RTL signal names using the model hierarchy.");
		func.addIO("input", "string", "rdl_tag", "\"rdl_tag\"");
		func.addStatement("m_rdl_tag = rdl_tag;");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// recursively build hierarchical name
		func = new SystemVerilogFunction("string", "get_rdl_name");
		func.addComment("Return a string corresponding to this model element using recursive call to");
		func.addComment("ancestors.  Used to generate RTL signal names.");
		func.addComment("RTL signal names using the model hierarchy.");
		func.addIO("input", "string", "prefix", null);
		func.addIO("input", "bit", "add_hdl_prefix", "0");
		func.addIO("input", "string", "override_tag", "\"\"");
		func.addStatement("uvm_reg_block_rdl rdl_parent;");
		func.addStatement("string rdl_tag;");
		func.addStatement("if (override_tag.len() > 0)");
		func.addStatement("  rdl_tag = override_tag;");
		func.addStatement("else");
		func.addStatement("  rdl_tag = m_rdl_tag;");
		func.addStatement("if (get_parent() != null) begin");
		func.addStatement("  $cast(rdl_parent, get_parent());");
		func.addStatement("  return {rdl_parent.get_rdl_name(prefix, add_hdl_prefix, override_tag), rdl_tag};");
		func.addStatement("end");
		func.addStatement("return rdl_tag;");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
	}
	

	/** create user defined property add/access methods for derived rdl classes   
	 * @param outputList - list of output lines to be updated
	 * @param indentLvl */
	private static void buildUserDefinedPropertyMethods(List<OutputLine> outputList, int indentLvl) {
		// add a user defined property value
		SystemVerilogFunction func = new SystemVerilogFunction("void", "add_def_property");
		func.addComment("Add a user-defined property to this rdl class instance");
		func.addIO("string", "name");
		func.addIO("string", "value");
		func.addStatement("m_def_properties[name] = value;");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// return 1 if a user defined property exists
		func = new SystemVerilogFunction("bit", "has_def_property");
		func.addComment("Returns 1 if a user-defined property of specified name exists for this rdl class instance");
		func.addIO("string", "name");
		func.addStatement("return m_def_properties.exists(name);");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// get a user defined property of specified name
		func = new SystemVerilogFunction("string", "get_def_property");
		func.addComment("Returns a user-defined property of specified name for this rdl class instance");
		func.addIO("string", "name");
		func.addStatement("return m_def_properties[name];");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// remove a user defined property of specified name
		func = new SystemVerilogFunction("void", "remove_def_property");
		func.addComment("Removes the user-defined property of specified name for this rdl class instance");
		func.addIO("string", "name");
		func.addStatement("m_def_properties.delete(name);");
		outputList.addAll(func.genOutputLines(indentLvl));	
	}
	
	/** create rdl path generation methods for uvm_reg_block derived class   
	 * @param outputList - list of output lines to be updated
	 * @param indentLvl */
	private static void buildBlockRdlPathMethods(List<OutputLine> outputList, int indentLvl) {
		// set rdl tag for this hierarchy level
		SystemVerilogFunction func = new SystemVerilogFunction("void", "set_rdl_tag");
		func.addComment("Set the rdl tag string for this block.  The tag will be used to build");
		func.addComment("RTL signal names using the model hierarchy.");
		func.addIO("input", "string", "rdl_tag", "\"rdl_tag\"");
		func.addStatement("m_rdl_tag = rdl_tag;");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// recursively build hierarchical name
		func = new SystemVerilogFunction("string", "get_rdl_name");
		func.addComment("Return a string corresponding to this model element using recursive call to");
		func.addComment("ancestors (stopping at an address map).  Used to generate RTL signal names.");
		func.addIO("input", "string", "prefix", null);
		func.addIO("input", "bit", "add_hdl_prefix", "0");
		func.addIO("input", "string", "override_tag", "\"\"");
		func.addStatement("uvm_reg_block_rdl rdl_parent;");
		func.addStatement("string rdl_tag;");
		func.addStatement("if (override_tag.len() > 0)");
		func.addStatement("  rdl_tag = override_tag;");
		func.addStatement("else");
		func.addStatement("  rdl_tag = m_rdl_tag;");
		func.addStatement("if (m_rdl_address_map) begin"); // if an address map, recursion ends
		func.addStatement("  if (add_hdl_prefix)");
		func.addStatement("    return {m_rdl_address_map_hdl_path, \".\", prefix, rdl_tag};");
		func.addStatement("  else");
		func.addStatement("    return {prefix, rdl_tag};");
		func.addStatement("end");
		func.addStatement("if (get_parent() != null) begin");
		func.addStatement("  $cast(rdl_parent, get_parent());");
		func.addStatement("  return {rdl_parent.get_rdl_name(prefix, add_hdl_prefix, override_tag), rdl_tag};");
		func.addStatement("end");
		func.addStatement("return rdl_tag;");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
	}
	
	/** create empty add_callbacks methods for regs/blocks (overridden by child classes if necessary)   
	 * @param outputList - list of output lines to be updated
	 * @param indentLvl */
	private static void buildAddCallbacksMethod(List<OutputLine> outputList, int indentLvl) {
		SystemVerilogFunction func = new SystemVerilogFunction("void", "add_callbacks");
		func.addComment("Null function that is overridden by block/reg classes requiring callback setup.");
		func.setVirtual();
		outputList.addAll(func.genOutputLines(indentLvl));	
	}
	
	/** create get_ancestor method for regs/blocks   
	 * @param outputList - list of output lines to be updated
	 * @param indentLvl */
	private static void buildRdlAncestorMethod(List<OutputLine> outputList, int indentLvl) {
		SystemVerilogFunction func = new SystemVerilogFunction("uvm_reg_block_rdl", "get_ancestor");
		func.addComment("Return the uvm_reg_block_rdl element that is the specified number of ancestor");
		func.addComment("levels from the current block/register.");
		func.setVirtual();
		func.addIO("int", "depth");
		func.addStatement("uvm_reg_block_rdl rdl_parent;");
		func.addStatement("$cast(rdl_parent, get_parent());");
		func.addStatement("if (depth < 2) return rdl_parent;"); // if 1 or less just return parent
		func.addStatement("else return rdl_parent.get_ancestor(depth-1);"); // else call recursively
		outputList.addAll(func.genOutputLines(indentLvl));	
	}
}
