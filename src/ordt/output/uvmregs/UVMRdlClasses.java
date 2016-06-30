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

public class UVMRdlClasses {

	/** build a package with all derived classes  
	 * @param outputList 
	 * @param indentLvl */
	public static void buildRdlPackage(List<OutputLine> outputList, int indentLvl) {
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl, "// uvm_reg_rdl_pkg containing jrdl extended classes"));
		outputList.add(new OutputLine(indentLvl++, "`ifndef UVM_REG_JRDL_PKG_SV"));
		outputList.add(new OutputLine(indentLvl,     "`define UVM_REG_JRDL_PKG_SV"));

		outputList.add(new OutputLine(indentLvl,   "`include \"uvm_macros.svh\""));
		outputList.add(new OutputLine(indentLvl++, "package uvm_reg_jrdl_pkg;")); 
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
		// create cbs class used for fields with next or intr assigned
		//buildCascadeIntrFieldCbsClass(outputList, indentLvl);
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
		
		// create new function
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function new(string name = \"uvm_reg_rdl\", int unsigned n_bits = 0, int has_coverage = UVM_NO_COVERAGE);"));
		outputList.add(new OutputLine(indentLvl, "super.new(name, n_bits, has_coverage);"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: new"));
		
		// add rdl path gen methods
		buildRegRdlPathMethods(outputList, indentLvl);
		
		// set external
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function void set_external(bit is_external);"));
		outputList.add(new OutputLine(indentLvl,     "m_external = is_external;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: set_external"));
		// get external indication
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function bit is_external();"));
		outputList.add(new OutputLine(indentLvl,     "return m_external;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: is_external"));
		// default get_src_intr_fields methods (overridden in intr reg child classes)
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "virtual function void get_intr_fields(ref uvm_reg_field fields[$]); // return all source interrupt fields"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: get_intr_fields"));
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "virtual task get_active_intr_fields(ref uvm_reg_field fields[$], input bit is_halt, input uvm_path_e path = UVM_DEFAULT_PATH); // return all active source intr/halt fields"));
		outputList.add(new OutputLine(--indentLvl, "endtask: get_active_intr_fields"));
		
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

		// create new function
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function new(string name = \"uvm_vreg_rdl\", int unsigned n_bits = 0);"));
		outputList.add(new OutputLine(indentLvl,     "super.new(name, n_bits);"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: new"));
		
		// add reg test info methods
		buildRegTestInfoMethods(outputList, indentLvl);

		// add reset getters/setters
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function bit has_reset_value();"));
		outputList.add(new OutputLine(indentLvl,     "return m_has_reset;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: has_reset_value"));
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function uvm_reg_data_t get_reset_value();"));
		outputList.add(new OutputLine(indentLvl,     "return m_reset_value;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: get_reset_value"));
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function void set_reset_value(uvm_reg_data_t reset_value);"));
		outputList.add(new OutputLine(indentLvl,     "m_has_reset = 1;"));
		outputList.add(new OutputLine(indentLvl,     "m_reset_value = reset_value;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: set_reset_value"));		
		
		// staged data methods
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function uvm_reg_data_t get_staged(longint unsigned stage_idx); // return staged value at specified idx or reset value"));
		outputList.add(new OutputLine(indentLvl,     "if (m_staged.exists(stage_idx)) return m_staged[stage_idx];"));
		outputList.add(new OutputLine(indentLvl,     "else if (has_reset_value()) return m_reset_value;"));
		outputList.add(new OutputLine(indentLvl,     "`uvm_error(\"RegModel\", $sformatf(\"Accessing uninitialized staged value at index %d. (uvm_vreg_rdl::get_staged())\", stage_idx));"));
		outputList.add(new OutputLine(indentLvl,     "return 0;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: get_staged"));
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function uvm_reg_data_t get_staged_field(longint unsigned stage_idx, string name); // return staged field value at specified idx"));
		outputList.add(new OutputLine(indentLvl,     "uvm_reg_data_t rvalue;")); 
		outputList.add(new OutputLine(indentLvl,     "uvm_vreg_field vfld;")); 
		outputList.add(new OutputLine(indentLvl,     "int unsigned lsb;")); 
		outputList.add(new OutputLine(indentLvl,     "int unsigned fsize;")); 
		outputList.add(new OutputLine(indentLvl,     "vfld = this.get_field_by_name(name);")); 
		outputList.add(new OutputLine(indentLvl++,   "if (vfld == null) begin;"));
		outputList.add(new OutputLine(indentLvl,       "`uvm_error(\"RegModel\", $sformatf(\"Unable to find field \\\"%s\\\" specified in uvm_vreg_rdl::get_staged_data()\", name));"));
		outputList.add(new OutputLine(indentLvl,       "return 0;"));
		outputList.add(new OutputLine(--indentLvl,   "end"));
		outputList.add(new OutputLine(indentLvl,     "lsb = vfld.get_lsb_pos_in_register();")); 
		outputList.add(new OutputLine(indentLvl,     "fsize = vfld.get_n_bits();")); 
		outputList.add(new OutputLine(indentLvl,     "rvalue = get_staged(stage_idx);"));
	    outputList.add(new OutputLine(indentLvl,     "return (rvalue & (((1<<fsize)-1) << lsb)) >> lsb;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: get_staged_field"));
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function void set_staged(longint unsigned stage_idx, uvm_reg_data_t staged); // set staged value at specified idx"));
		outputList.add(new OutputLine(indentLvl,     "m_staged[stage_idx] = staged;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: set_staged"));		
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function void stage_field(longint unsigned stage_idx, string name, uvm_reg_data_t value); // set field value in specified staged array idx"));
		outputList.add(new OutputLine(indentLvl,     "uvm_vreg_field vfld;")); 
		outputList.add(new OutputLine(indentLvl,     "int unsigned lsb;")); 
		outputList.add(new OutputLine(indentLvl,     "int unsigned fsize;")); 
		outputList.add(new OutputLine(indentLvl,     "vfld = this.get_field_by_name(name);")); 
		outputList.add(new OutputLine(indentLvl++,   "if (vfld == null) begin;"));
		outputList.add(new OutputLine(indentLvl,       "`uvm_error(\"RegModel\", $sformatf(\"Unable to find field \\\"%s\\\" specified in uvm_vreg_rdl::stage_field()\", name));"));
		outputList.add(new OutputLine(indentLvl,       "return;"));
		outputList.add(new OutputLine(--indentLvl,   "end"));
		outputList.add(new OutputLine(indentLvl,     "lsb = vfld.get_lsb_pos_in_register();")); 
		outputList.add(new OutputLine(indentLvl,     "fsize = vfld.get_n_bits();")); 
		outputList.add(new OutputLine(indentLvl++,   "if (value >> fsize) begin"));
		outputList.add(new OutputLine(indentLvl,       "`uvm_warning(\"RegModel\", $sformatf(\"Staging value 'h%h that is greater than field \\\"%s\\\" size (%0d bits)\", value, name, fsize));"));
		outputList.add(new OutputLine(indentLvl,       "value &= ((1<<fsize)-1);"));
		outputList.add(new OutputLine(--indentLvl,   "end"));
		outputList.add(new OutputLine(indentLvl++,   "if (!m_staged.exists(stage_idx)) begin"));
		outputList.add(new OutputLine(indentLvl,       "if (has_reset_value()) m_staged[stage_idx] = m_reset_value;"));
		outputList.add(new OutputLine(indentLvl,       "else m_staged[stage_idx] = 0;"));
		outputList.add(new OutputLine(--indentLvl,   "end"));
	    outputList.add(new OutputLine(indentLvl,     "m_staged[stage_idx] |= (((1<<fsize)-1) << lsb);"));
	    outputList.add(new OutputLine(indentLvl,     "m_staged[stage_idx] ^= (((1<<fsize)-1) << lsb);"));
	    outputList.add(new OutputLine(indentLvl,     "m_staged[stage_idx] |= (value << lsb);"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: stage_field"));		
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl,   "// write stage value stored at stage_idx to dut memory offset at vreg_idx"));	
		outputList.add(new OutputLine(indentLvl++, "virtual task write_staged(input longint unsigned stage_idx, input longint unsigned vreg_idx, output uvm_status_e status,"));
		outputList.add(new OutputLine(indentLvl,     "    input uvm_path_e path = UVM_DEFAULT_PATH, input uvm_reg_map map = null, input uvm_sequence_base parent = null,"));
		outputList.add(new OutputLine(indentLvl,     "    input uvm_object extension = null, input string fname = \"\", input int lineno = 0);"));
		outputList.add(new OutputLine(indentLvl++,   "if (!m_staged.exists(stage_idx)) begin"));
		outputList.add(new OutputLine(indentLvl,       "`uvm_error(\"RegModel\", $sformatf(\"Attempting write of uninitialized staged value at index %d. (uvm_vreg_rdl::write_staged())\", stage_idx));"));
		outputList.add(new OutputLine(indentLvl,       "return;"));
		outputList.add(new OutputLine(--indentLvl,   "end"));
		outputList.add(new OutputLine(indentLvl,     "this.write(vreg_idx, status, m_staged[stage_idx], path, map, parent, extension, fname, lineno);"));
		outputList.add(new OutputLine(--indentLvl, "endtask: write_staged"));		
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl,   "// write stage value stored at idx to dut memory at same idx offset"));	
		outputList.add(new OutputLine(indentLvl++, "virtual task write_same_staged(input longint unsigned idx, output uvm_status_e status,"));
		outputList.add(new OutputLine(indentLvl,     "    input uvm_path_e path = UVM_DEFAULT_PATH, input uvm_reg_map map = null, input uvm_sequence_base parent = null,"));
		outputList.add(new OutputLine(indentLvl,     "    input uvm_object extension = null, input string fname = \"\", input int lineno = 0);"));
		outputList.add(new OutputLine(indentLvl,     "this.write_staged(idx, idx, status, path, map, parent, extension, fname, lineno);"));
		outputList.add(new OutputLine(--indentLvl, "endtask: write_same_staged"));		
		
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
		
		// uvm reg test mode info
		//outputList.add(new OutputLine(indentLvl, "local bit m_dont_test = 0;")); 
		//outputList.add(new OutputLine(indentLvl, "local bit m_dont_compare = 0;")); 
		//outputList.add(new OutputLine(indentLvl, "local int unsigned m_js_category = 0;")); 
		
		// create new function
		outputList.add(new OutputLine(indentLvl, ""));	

		outputList.add(new OutputLine(indentLvl++, "function new(string name = \"uvm_mem_rdl\", longint unsigned size = 1, int unsigned n_bits = 0, string  access = \"RW\", int has_coverage = UVM_NO_COVERAGE);"));
		outputList.add(new OutputLine(indentLvl, "super.new(name, size, n_bits, access, has_coverage);"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: new"));
		
		// add reg test info methods
		//buildRegTestInfoMethods(outputList, indentLvl);
				
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
		
		// statis defines for counter/interrupt field lists  
		//outputList.add(new OutputLine(indentLvl, "static uvm_reg_field_rdl_counter  m_counter_fields[$];"));
		//outputList.add(new OutputLine(indentLvl, "static uvm_reg_field_rdl_interrupt  m_interrupt_fields[$];"));

		// create new function
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function new(string name = \"uvm_reg_block_rdl\", int has_coverage = UVM_NO_COVERAGE);"));
		outputList.add(new OutputLine(indentLvl, "super.new(name, has_coverage);"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: new"));

		
		// set_rdl_address_map
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function void set_rdl_address_map(bit val = 0);"));
		outputList.add(new OutputLine(indentLvl, "m_rdl_address_map = val;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: set_rdl_address_map"));
		
		// set_rdl_address_map
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function void set_rdl_address_map_hdl_path(string path = \"\");"));
		outputList.add(new OutputLine(indentLvl, "m_rdl_address_map_hdl_path = path;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: set_rdl_address_map_hdl_path"));
		
		// add rdl path gen methods
		buildBlockRdlPathMethods(outputList, indentLvl);
		
		// add_callbacks method
		buildAddCallbacksMethod(outputList, indentLvl);
		
		// add get ancestor method
		buildRdlAncestorMethod(outputList, indentLvl);
		
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
		
		// create new function
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function new(string name = \"uvm_reg_field_rdl\");"));
		outputList.add(new OutputLine(indentLvl, "super.new(name);"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: new"));  
		   
		// get_rdl_register - get the rdl reg parent for this field
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function uvm_reg_rdl get_rdl_register();"));
		outputList.add(new OutputLine(indentLvl, "uvm_reg_rdl rdl_reg;"));
		outputList.add(new OutputLine(indentLvl, "$cast(rdl_reg, get_register());"));
		outputList.add(new OutputLine(indentLvl, "return rdl_reg;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: get_rdl_register"));  

		// set_hw_access function
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function void set_rdl_access_info(bit is_sw_readable, bit is_sw_writeable, bit is_hw_readable, bit is_hw_writeable, bit has_hw_we, bit has_hw_wel);"));
		outputList.add(new OutputLine(indentLvl, "m_is_sw_readable = is_sw_readable;"));
		outputList.add(new OutputLine(indentLvl, "m_is_sw_writeable = is_sw_writeable;"));
		outputList.add(new OutputLine(indentLvl, "m_is_hw_readable = is_hw_readable;"));
		outputList.add(new OutputLine(indentLvl, "m_is_hw_writeable = is_hw_writeable;"));
		outputList.add(new OutputLine(indentLvl, "m_has_hw_we = has_hw_we;"));
		outputList.add(new OutputLine(indentLvl, "m_has_hw_wel = has_hw_wel;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: set_rdl_access_info"));
		
		// read data output
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function string get_hw_read_signal();  // read data"));
		outputList.add(new OutputLine(indentLvl,     "uvm_reg_rdl rdl_reg;")); 
		outputList.add(new OutputLine(indentLvl,     "rdl_reg = this.get_rdl_register();")); 
		outputList.add(new OutputLine(indentLvl,     "return {rdl_reg.get_rdl_name(\"l2h_\", 1), this.get_name(), \"_r\"};")); 
		outputList.add(new OutputLine(--indentLvl, "endfunction: get_hw_read_signal"));
		// write data input
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function string get_hw_write_signal();  // write data"));
		outputList.add(new OutputLine(indentLvl,     "uvm_reg_rdl rdl_reg;")); 
		outputList.add(new OutputLine(indentLvl,     "rdl_reg = this.get_rdl_register();")); 
		outputList.add(new OutputLine(indentLvl,     "return {rdl_reg.get_rdl_name(\"h2l_\", 1), this.get_name(), \"_w\"};")); 
		outputList.add(new OutputLine(--indentLvl, "endfunction: get_hw_write_signal"));
		// write data enable input
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function string get_hw_we_signal();  // write data enable"));
		outputList.add(new OutputLine(indentLvl,     "uvm_reg_rdl rdl_reg;")); 
		outputList.add(new OutputLine(indentLvl,     "rdl_reg = this.get_rdl_register();")); 
		outputList.add(new OutputLine(indentLvl,     "return {rdl_reg.get_rdl_name(\"h2l_\", 1), this.get_name(), \"_we\"};")); 
		outputList.add(new OutputLine(--indentLvl, "endfunction: get_hw_we_signal"));
		// write data enable input
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function string get_hw_wel_signal();  // write data enable low"));
		outputList.add(new OutputLine(indentLvl,     "uvm_reg_rdl rdl_reg;")); 
		outputList.add(new OutputLine(indentLvl,     "rdl_reg = this.get_rdl_register();")); 
		outputList.add(new OutputLine(indentLvl,     "return {rdl_reg.get_rdl_name(\"h2l_\", 1), this.get_name(), \"_wel\"};")); 
		outputList.add(new OutputLine(--indentLvl, "endfunction: get_hw_wel_signal"));
		// is_sw_readable
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function bit is_sw_readable();"));
		outputList.add(new OutputLine(indentLvl,     "return m_is_sw_readable;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: is_sw_readable"));
		// is_sw_writeable
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function bit is_sw_writeable();"));
		outputList.add(new OutputLine(indentLvl,     "return m_is_sw_writeable;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: is_sw_writeable"));
		// is_hw_readable
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function bit is_hw_readable();"));
		outputList.add(new OutputLine(indentLvl,     "return m_is_hw_readable;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: is_hw_readable"));
		// is_hw_writeable
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function bit is_hw_writeable();"));
		outputList.add(new OutputLine(indentLvl,     "return m_is_hw_writeable;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: is_hw_writeable"));
		// has_hw_we
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function bit has_hw_we();"));
		outputList.add(new OutputLine(indentLvl,     "return m_has_hw_we;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: has_hw_we"));
		// has_hw_wel
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function bit has_hw_wel();"));
		outputList.add(new OutputLine(indentLvl,     "return m_has_hw_wel;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: has_hw_wel"));
		// is_counter
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function bit is_counter();"));
		outputList.add(new OutputLine(indentLvl,     "return m_is_counter;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: is_counter"));
		// is_interrupt
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function bit is_interrupt();"));
		outputList.add(new OutputLine(indentLvl,     "return m_is_interrupt;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: is_interrupt"));
		// is_unsupported
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function void set_unsupported();"));
		outputList.add(new OutputLine(indentLvl,     "m_is_unsupported = 1;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: set_unsupported"));
		outputList.add(new OutputLine(indentLvl++, "function bit is_unsupported();"));
		outputList.add(new OutputLine(indentLvl,     "return m_is_unsupported;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: is_unsupported"));
		// is_dontcompare
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function void set_dontcompare();"));
		outputList.add(new OutputLine(indentLvl,     "m_is_dontcompare = 1;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: set_dontcompare"));
		outputList.add(new OutputLine(indentLvl++, "function bit is_dontcompare();"));
		outputList.add(new OutputLine(indentLvl,     "return m_is_dontcompare;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: is_dontcompare"));
		
		// has_js_subcategory
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function bit has_a_js_subcategory();"));
		outputList.add(new OutputLine(indentLvl,     "return (m_js_subcategory > 0);"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: has_a_js_subcategory"));
		// has_js_subcategory
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function bit has_js_subcategory(js_subcategory_e cat);"));
		outputList.add(new OutputLine(indentLvl,     "return ((cat & m_js_subcategory) > 0);"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: has_js_subcategory"));
		// add_js_subcategory
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function void add_js_subcategory(js_subcategory_e cat);"));  
		outputList.add(new OutputLine(indentLvl,     "m_js_subcategory = m_js_subcategory | cat;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: add_js_subcategory"));
		// remove_js_subcategory
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function void remove_js_subcategory(js_subcategory_e cat);")); 
		outputList.add(new OutputLine(indentLvl,     "m_js_subcategory = m_js_subcategory & ~cat;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: remove_js_subcategory"));
		// set_js_subcategory
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function void set_js_subcategory(int js_subcategory);"));
		outputList.add(new OutputLine(indentLvl,     "m_js_subcategory = js_subcategory;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: set_js_subcategory"));

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
		
		// create new function
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function new(string name = \"uvm_reg_field_rdl_interrupt\");"));
		outputList.add(new OutputLine(indentLvl, "super.new(name);"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: new"));

		// add_intr function
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl, "function void add_intr(int intr_level_type = 0, int intr_sticky_type = 0,"));
		outputList.add(new OutputLine(indentLvl++, "            string intr_sig = \"\", bit mask_intr_bits = 0);"));
		outputList.add(new OutputLine(indentLvl, "m_is_interrupt = 1;")); 
		outputList.add(new OutputLine(indentLvl, "if (intr_level_type > 0) m_intr_level_type = intr_level_type;"));
		outputList.add(new OutputLine(indentLvl, "if (intr_sticky_type > 0) m_intr_sticky_type = intr_sticky_type;"));
		outputList.add(new OutputLine(indentLvl, "if (intr_sig.len() > 0) m_intr_sig = intr_sig;"));
		outputList.add(new OutputLine(indentLvl, "m_mask_intr_bits = mask_intr_bits;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: add_intr"));
		
		// get intr signal
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function string get_intr_signal();  // interrupt input"));
		outputList.add(new OutputLine(indentLvl,     "uvm_reg_rdl rdl_reg;")); 
		outputList.add(new OutputLine(indentLvl,     "string intr_signal;"));  
		outputList.add(new OutputLine(indentLvl,     "rdl_reg = this.get_rdl_register();")); 
		outputList.add(new OutputLine(indentLvl,     "if (m_intr_sig.len() > 0) intr_signal = {rdl_reg.get_rdl_name(\"l2h_\", 1), m_intr_sig};"));  // field intr signal
		outputList.add(new OutputLine(indentLvl,     "else intr_signal = {rdl_reg.get_rdl_name(\"h2l_\", 1), this.get_name(), \"_intr\"};"));  // input intr signal
		outputList.add(new OutputLine(indentLvl,     "//$display(\"---  getting intrement sigmal: %s\", intr_signal);"));
		outputList.add(new OutputLine(indentLvl,     "return intr_signal;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: get_intr_signal"));
		// get intr output signal for parent reg
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function string get_intr_out_signal();  // interrupt output"));
		outputList.add(new OutputLine(indentLvl,     "uvm_reg_rdl rdl_reg;")); 
		outputList.add(new OutputLine(indentLvl,     "rdl_reg = this.get_rdl_register();")); 
		outputList.add(new OutputLine(indentLvl,     "return {rdl_reg.get_rdl_name(\"l2h_\", 1), \"_intr_o\"};"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: get_intr_out_signal"));
		// intr_level_type value
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function int get_intr_level_type();  // LEVEL(0), POSEDGE(1), NEGEDGE(2), BOTHEDGE(3)"));
		outputList.add(new OutputLine(indentLvl,     "return m_intr_level_type;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: get_intr_level_type"));
		// intr_sticky_type value
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function int get_intr_sticky_type();  // STICKYBIT(0), STICKY(1), NONSTICKY(2)"));
		outputList.add(new OutputLine(indentLvl,     "return m_intr_sticky_type;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: get_intr_sticky_type"));
        // get mask intr bits indicator
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function bit get_mask_intr_bits();"));
		outputList.add(new OutputLine(indentLvl,     "return m_mask_intr_bits;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: get_mask_intr_bits"));

		// add_halt function
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function void add_halt();"));
		outputList.add(new OutputLine(indentLvl,     "m_is_halt = 1;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: add_halt"));
		
		// get halt indication
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function bit is_halt();"));
		outputList.add(new OutputLine(indentLvl,     "return m_is_halt;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: is_halt"));
		// get halt output signal for parent reg
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function string get_halt_out_signal();  // halt output"));
		outputList.add(new OutputLine(indentLvl,     "uvm_reg_rdl rdl_reg;")); 
		outputList.add(new OutputLine(indentLvl,     "rdl_reg = this.get_rdl_register();")); 
		outputList.add(new OutputLine(indentLvl,     "return {rdl_reg.get_rdl_name(\"l2h_\", 1), \"_halt_o\"};"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: get_halt_out_signal"));
		
		// set field that will be used as mask or enable of intr output from this field
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function void set_intr_mask_field(uvm_reg_field intr_mask_fld, bit intr_mask_fld_is_enable);"));
		outputList.add(new OutputLine(indentLvl,     "$cast(m_intr_mask_fld, intr_mask_fld);"));
		outputList.add(new OutputLine(indentLvl,     "m_intr_mask_fld_is_enable = intr_mask_fld_is_enable;")); 
		outputList.add(new OutputLine(--indentLvl, "endfunction: set_intr_mask_field"));
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function uvm_reg_field_rdl get_intr_mask_field();"));
		outputList.add(new OutputLine(indentLvl,     "return m_intr_mask_fld;")); 
		outputList.add(new OutputLine(--indentLvl, "endfunction: get_intr_mask_field"));
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function bit has_intr_mask_field();"));
		outputList.add(new OutputLine(indentLvl,     "return (m_intr_mask_fld != null);")); 
		outputList.add(new OutputLine(--indentLvl, "endfunction: has_intr_mask_field"));
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function bit intr_mask_field_is_enable();"));  // is mask field an enable else a mask
		outputList.add(new OutputLine(indentLvl,     "return m_intr_mask_fld_is_enable;")); 
		outputList.add(new OutputLine(--indentLvl, "endfunction: intr_mask_field_is_enable"));

		// get field value after applying the intr mask/enable
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function uvm_reg_data_t get_intr_masked();")); 
		outputList.add(new OutputLine(indentLvl,     "uvm_reg_field_rdl mask_fld;")); 
		outputList.add(new OutputLine(indentLvl++,   "if (has_intr_mask_field()) begin")); // if a mask/enable then apply
		outputList.add(new OutputLine(indentLvl,       "if (intr_mask_field_is_enable()) return get() & m_intr_mask_fld.get();")); // enable field
		outputList.add(new OutputLine(indentLvl,       "else return get() & ~m_intr_mask_fld.get();")); // mask field
		outputList.add(new OutputLine(--indentLvl,   "end"));
		outputList.add(new OutputLine(indentLvl,     "return get();"));  // otherwise use std get
		outputList.add(new OutputLine(--indentLvl, "endfunction: get_intr_masked"));
		
		// set field that will be used as mask or enable of halt output from this field
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function void set_halt_mask_field(uvm_reg_field halt_mask_fld, bit halt_mask_fld_is_enable);"));
		outputList.add(new OutputLine(indentLvl,     "$cast(m_halt_mask_fld, halt_mask_fld);"));
		outputList.add(new OutputLine(indentLvl,     "m_halt_mask_fld_is_enable = halt_mask_fld_is_enable;")); 
		outputList.add(new OutputLine(--indentLvl, "endfunction: set_halt_mask_field"));
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function uvm_reg_field_rdl get_halt_mask_field();"));
		outputList.add(new OutputLine(indentLvl,     "return m_halt_mask_fld;")); 
		outputList.add(new OutputLine(--indentLvl, "endfunction: get_halt_mask_field"));
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function bit has_halt_mask_field();"));
		outputList.add(new OutputLine(indentLvl,     "return (m_halt_mask_fld != null);")); 
		outputList.add(new OutputLine(--indentLvl, "endfunction: has_halt_mask_field"));
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function bit halt_mask_field_is_enable();"));  // is mask field an enable else a mask
		outputList.add(new OutputLine(indentLvl,     "return m_halt_mask_fld_is_enable;")); 
		outputList.add(new OutputLine(--indentLvl, "endfunction: halt_mask_field_is_enable"));

		// get field value after applying the halt mask/enable
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function uvm_reg_data_t get_halt_masked();")); 
		outputList.add(new OutputLine(indentLvl,     "uvm_reg_field_rdl mask_fld;")); 
		outputList.add(new OutputLine(indentLvl++,   "if (has_halt_mask_field()) begin")); // if a mask/enable then apply
		outputList.add(new OutputLine(indentLvl,       "if (halt_mask_field_is_enable()) return get() & m_halt_mask_fld.get();")); // enable field
		outputList.add(new OutputLine(indentLvl,       "else return get() & ~m_halt_mask_fld.get();")); // mask field
		outputList.add(new OutputLine(--indentLvl,   "end"));
		outputList.add(new OutputLine(indentLvl,     "return get();"));  // otherwise use std get
		outputList.add(new OutputLine(--indentLvl, "endfunction: get_halt_masked"));
				
		// set cascaded intr reg that will set value of this field
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function void set_cascade_intr_reg(uvm_reg cascade_intr_reg, bit cascade_reg_is_halt);"));
		outputList.add(new OutputLine(indentLvl,     "$cast(m_cascade_intr_reg, cascade_intr_reg);"));
		outputList.add(new OutputLine(indentLvl,     "m_cascade_reg_is_halt = cascade_reg_is_halt;")); 
		outputList.add(new OutputLine(--indentLvl, "endfunction: set_cascade_intr_reg"));
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function uvm_reg_rdl get_cascade_intr_reg();"));
		outputList.add(new OutputLine(indentLvl,     "return m_cascade_intr_reg;")); 
		outputList.add(new OutputLine(--indentLvl, "endfunction: get_cascade_intr_reg"));
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function bit has_cascade_intr_reg();"));
		outputList.add(new OutputLine(indentLvl,     "return (m_cascade_intr_reg != null);")); 
		outputList.add(new OutputLine(--indentLvl, "endfunction: has_cascade_intr_reg"));
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function bit cascade_reg_is_halt();"));  // is cascaded input the halt signal?
		outputList.add(new OutputLine(indentLvl,     "return m_cascade_reg_is_halt;")); 
		outputList.add(new OutputLine(--indentLvl, "endfunction: cascade_reg_is_halt"));

		// default get_src_intr_fields methods (overridden in intr reg child classes)
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function void get_intr_fields(ref uvm_reg_field fields[$]); // return all source interrupt fields")); 
		outputList.add(new OutputLine(indentLvl,     "if (has_cascade_intr_reg()) m_cascade_intr_reg.get_intr_fields(fields);"));
		outputList.add(new OutputLine(indentLvl,     "else fields.push_back(this);"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: get_intr_fields"));
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "task get_active_intr_fields(ref uvm_reg_field fields[$], input bit is_halt, input uvm_path_e path = UVM_DEFAULT_PATH); // return all active source intr/halt fields"));
		outputList.add(new OutputLine(indentLvl,     "if (has_cascade_intr_reg()) m_cascade_intr_reg.get_active_intr_fields(fields, m_cascade_reg_is_halt, path);"));
		outputList.add(new OutputLine(indentLvl,     "else if (is_halt && (|get_halt_masked())) fields.push_back(this);"));
		outputList.add(new OutputLine(indentLvl,     "else if (!is_halt && (|get_intr_masked())) fields.push_back(this);"));
		outputList.add(new OutputLine(--indentLvl, "endtask: get_active_intr_fields"));
		
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
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function new(string name = \"uvm_reg_field_rdl_counter\");"));
		outputList.add(new OutputLine(indentLvl, "super.new(name);"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: new"));
		
		// get accum value
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function uvm_reg_data_t get_accum_value();"));
		outputList.add(new OutputLine(indentLvl,     "return m_accum_value;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: get_accum_value"));
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function void set_accum_value(uvm_reg_data_t accum_value);"));
		outputList.add(new OutputLine(indentLvl,     "m_accum_value = accum_value;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: set_accum_value"));

		// add_incr function
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function void add_incr(uvm_reg_data_t incr_value, string incr_sig = \"\", string incr_value_sig = \"\", int unsigned incr_value_sig_width = 0);"));
		outputList.add(new OutputLine(indentLvl, "m_is_counter = 1;")); 
		outputList.add(new OutputLine(indentLvl, "m_incr_value = incr_value;")); 
		outputList.add(new OutputLine(indentLvl, "if (incr_sig.len() > 0) m_incr_sig = incr_sig;"));
		outputList.add(new OutputLine(indentLvl, "if (incr_value_sig.len() > 0) m_incr_value_sig = incr_value_sig;"));
		outputList.add(new OutputLine(indentLvl, "m_incr_value_sig_width = incr_value_sig_width;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: add_incr"));
		
		// add_incr sat function
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function void add_incr_sat(uvm_reg_data_t incr_sat_value, string incr_sat_value_sig = \"\");"));
		outputList.add(new OutputLine(indentLvl, "m_has_incr_sat = 1;")); 
		outputList.add(new OutputLine(indentLvl, "m_incr_sat_value = incr_sat_value;"));
		outputList.add(new OutputLine(indentLvl, "if (incr_sat_value_sig.len() > 0) m_incr_sat_value_sig = incr_sat_value_sig;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: add_incr_sat"));
		
		// add_incr thold function
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function void add_incr_thold(uvm_reg_data_t incr_thold_value, string incr_thold_value_sig = \"\");"));
		outputList.add(new OutputLine(indentLvl, "m_has_incr_thold = 1;")); 
		outputList.add(new OutputLine(indentLvl, "m_incr_thold_value = incr_thold_value;"));
		outputList.add(new OutputLine(indentLvl, "if (incr_thold_value_sig.len() > 0) m_incr_thold_value_sig = incr_thold_value_sig;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: add_incr_thold"));
		
		// get incr signal
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function string get_incr_signal();  // increment input"));
		outputList.add(new OutputLine(indentLvl,     "string incr_signal;"));  
		outputList.add(new OutputLine(indentLvl,     "uvm_reg_rdl rdl_reg;")); 
		outputList.add(new OutputLine(indentLvl,     "rdl_reg = this.get_rdl_register();")); 
		outputList.add(new OutputLine(indentLvl,     "if (m_incr_sig.len() > 0) incr_signal = {rdl_reg.get_rdl_name(\"rg_\", 1), m_incr_sig};"));  // field incr signal
		outputList.add(new OutputLine(indentLvl,     "else incr_signal = {rdl_reg.get_rdl_name(\"h2l_\", 1), this.get_name(), \"_incr\"};"));  // input incr signal
		outputList.add(new OutputLine(indentLvl,     "//$display(\"---  getting increment sigmal: %s\", incr_signal);"));
		outputList.add(new OutputLine(indentLvl,     "return incr_signal;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: get_incr_signal"));
		// overflow output
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function string get_overflow_signal();  // overflow output"));
		outputList.add(new OutputLine(indentLvl,     "uvm_reg_rdl rdl_reg;")); 
		outputList.add(new OutputLine(indentLvl,     "rdl_reg = this.get_rdl_register();")); 
		outputList.add(new OutputLine(indentLvl,     "return {rdl_reg.get_rdl_name(\"l2h_\", 1), this.get_name(), \"_overflow\"};")); 
		outputList.add(new OutputLine(--indentLvl, "endfunction: get_overflow_signal"));
		// incrsat output
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function string get_incr_sat_signal();  // increment saturation output"));
		outputList.add(new OutputLine(indentLvl,     "uvm_reg_rdl rdl_reg;")); 
		outputList.add(new OutputLine(indentLvl,     "rdl_reg = this.get_rdl_register();")); 
		outputList.add(new OutputLine(indentLvl,     "return {rdl_reg.get_rdl_name(\"l2h_\", 1), this.get_name(), \"_incrsat_o\"};")); 
		outputList.add(new OutputLine(--indentLvl, "endfunction: get_incr_sat_signal"));
		// incrthold output
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function string get_incr_thold_signal();  // increment threshold output"));
		outputList.add(new OutputLine(indentLvl,     "uvm_reg_rdl rdl_reg;")); 
		outputList.add(new OutputLine(indentLvl,     "rdl_reg = this.get_rdl_register();")); 
		outputList.add(new OutputLine(indentLvl,     "return {rdl_reg.get_rdl_name(\"l2h_\", 1), this.get_name(), \"_incrthold_o\"};")); 
		outputList.add(new OutputLine(--indentLvl, "endfunction: get_incr_thold_signal"));
		// incr value
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function uvm_reg_data_t get_incr_value();"));
		outputList.add(new OutputLine(indentLvl,     "return m_incr_value;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: get_incr_value"));
		// get incr value signal
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function string get_incr_value_signal();  // incr_value signal")); 
		outputList.add(new OutputLine(indentLvl,     "uvm_reg_rdl rdl_reg;")); 
		outputList.add(new OutputLine(indentLvl,     "rdl_reg = this.get_rdl_register();")); 
		outputList.add(new OutputLine(indentLvl,     "if (m_incr_value_sig.len() > 0) return rdl_reg.get_rdl_name(\"rg_\", 1, m_incr_value_sig);")); // assumes peer field w/ no property
		outputList.add(new OutputLine(indentLvl,     "else return {rdl_reg.get_rdl_name(\"h2l_\", 1), this.get_name(), \"_incrvalue\"};"));  // input incr value signal
		outputList.add(new OutputLine(--indentLvl, "endfunction: get_incr_value_signal"));
		// get incr value signal width
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function int unsigned get_incr_value_signal_width();"));
		outputList.add(new OutputLine(indentLvl,     "return m_incr_value_sig_width;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: get_incr_value_signal_width"));
		// incr sat value
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function bit has_incr_sat();"));
		outputList.add(new OutputLine(indentLvl,     "return m_has_incr_sat;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: has_incr_sat"));
		outputList.add(new OutputLine(indentLvl++, "function uvm_reg_data_t get_incr_sat_value();"));
		outputList.add(new OutputLine(indentLvl,     "return m_incr_sat_value;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: get_incr_sat_value"));
		// get incr sat value signal
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function string get_incr_sat_value_signal();  // incr_sat_value signal")); 
		outputList.add(new OutputLine(indentLvl,     "uvm_reg_rdl rdl_reg;")); 
		outputList.add(new OutputLine(indentLvl,     "rdl_reg = this.get_rdl_register();")); 
		outputList.add(new OutputLine(indentLvl,     "if (m_incr_sat_value_sig.len() < 1) return \"\";")); // no default input
		outputList.add(new OutputLine(indentLvl,     "else return rdl_reg.get_rdl_name(\"rg_\", 1, m_incr_sat_value_sig);")); // assumes peer field w/ no property
		outputList.add(new OutputLine(--indentLvl, "endfunction: get_incr_sat_value_signal"));
		// incr thold value
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function bit has_incr_thold();"));
		outputList.add(new OutputLine(indentLvl,     "return m_has_incr_thold;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: has_incr_thold"));
		outputList.add(new OutputLine(indentLvl++, "function uvm_reg_data_t get_incr_thold_value();"));
		outputList.add(new OutputLine(indentLvl,     "return m_incr_thold_value;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: get_incr_thold_value"));
		// get incr thold value signal
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function string get_incr_thold_value_signal();  // incr_sat_value signal")); 
		outputList.add(new OutputLine(indentLvl,     "uvm_reg_rdl rdl_reg;")); 
		outputList.add(new OutputLine(indentLvl,     "rdl_reg = this.get_rdl_register();")); 
		outputList.add(new OutputLine(indentLvl,     "if (m_incr_thold_value_sig.len() < 1) return \"\";")); // no default input 
		outputList.add(new OutputLine(indentLvl,     "else return rdl_reg.get_rdl_name(\"rg_\", 1, m_incr_thold_value_sig);")); // assumes peer field w/ no property
		outputList.add(new OutputLine(--indentLvl, "endfunction: get_incr_thold_value_signal"));

		// add_decr function
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function void add_decr(uvm_reg_data_t decr_value, string decr_sig = \"\", string decr_value_sig = \"\", int unsigned decr_value_sig_width = 0);"));
		outputList.add(new OutputLine(indentLvl, "m_is_counter = 1;")); 
		outputList.add(new OutputLine(indentLvl, "m_decr_value = decr_value;")); 
		outputList.add(new OutputLine(indentLvl, "if (decr_sig.len() > 0) m_decr_sig = decr_sig;"));
		outputList.add(new OutputLine(indentLvl, "if (decr_value_sig.len() > 0) m_decr_value_sig = decr_value_sig;"));
		outputList.add(new OutputLine(indentLvl, "m_decr_value_sig_width = decr_value_sig_width;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: add_decr"));
		
		// add_decr sat function
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function void add_decr_sat(uvm_reg_data_t decr_sat_value, string decr_sat_value_sig = \"\");"));
		outputList.add(new OutputLine(indentLvl, "m_has_decr_sat = 1;")); 
		outputList.add(new OutputLine(indentLvl, "m_decr_sat_value = decr_sat_value;"));
		outputList.add(new OutputLine(indentLvl, "if (decr_sat_value_sig.len() > 0) m_decr_sat_value_sig = decr_sat_value_sig;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: add_decr_sat"));
		
		// add_decr thold function
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function void add_decr_thold(uvm_reg_data_t decr_thold_value, string decr_thold_value_sig = \"\");"));
		outputList.add(new OutputLine(indentLvl, "m_has_decr_thold = 1;")); 
		outputList.add(new OutputLine(indentLvl, "m_decr_thold_value = decr_thold_value;"));
		outputList.add(new OutputLine(indentLvl, "if (decr_thold_value_sig.len() > 0) m_decr_thold_value_sig = decr_thold_value_sig;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: add_decr_thold"));
		
		// get decr signal
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function string get_decr_signal();  // decrement input"));
		outputList.add(new OutputLine(indentLvl,     "string decr_signal;"));  
		outputList.add(new OutputLine(indentLvl,     "uvm_reg_rdl rdl_reg;")); 
		outputList.add(new OutputLine(indentLvl,     "rdl_reg = this.get_rdl_register();")); 
		outputList.add(new OutputLine(indentLvl,     "if (m_decr_sig.len() > 0) decr_signal = {rdl_reg.get_rdl_name(\"rg_\", 1), m_decr_sig};"));  // field decr signal
		outputList.add(new OutputLine(indentLvl,     "else decr_signal = {rdl_reg.get_rdl_name(\"h2l_\", 1), this.get_name(), \"_decr\"};"));  // input decr signal
		outputList.add(new OutputLine(indentLvl,     "//$display(\"---  getting decrement sigmal: %s\", decr_signal);"));
		outputList.add(new OutputLine(indentLvl,     "return decr_signal;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: get_decr_signal"));
		// underflow output
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function string get_underflow_signal();  // underflow output"));
		outputList.add(new OutputLine(indentLvl,     "uvm_reg_rdl rdl_reg;")); 
		outputList.add(new OutputLine(indentLvl,     "rdl_reg = this.get_rdl_register();")); 
		outputList.add(new OutputLine(indentLvl,     "return {rdl_reg.get_rdl_name(\"l2h_\", 1), this.get_name(), \"_underflow\"};")); 
		outputList.add(new OutputLine(--indentLvl, "endfunction: get_underflow_signal"));
		// decrsat output
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function string get_decr_sat_signal();  // decrement saturation output"));
		outputList.add(new OutputLine(indentLvl,     "uvm_reg_rdl rdl_reg;")); 
		outputList.add(new OutputLine(indentLvl,     "rdl_reg = this.get_rdl_register();")); 
		outputList.add(new OutputLine(indentLvl,     "return {rdl_reg.get_rdl_name(\"l2h_\", 1), this.get_name(), \"_decrsat_o\"};")); 
		outputList.add(new OutputLine(--indentLvl, "endfunction: get_decr_sat_signal"));
		// decrthold output
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function string get_decr_thold_signal();  // decrement threshold output"));
		outputList.add(new OutputLine(indentLvl,     "uvm_reg_rdl rdl_reg;")); 
		outputList.add(new OutputLine(indentLvl,     "rdl_reg = this.get_rdl_register();")); 
		outputList.add(new OutputLine(indentLvl,     "return {rdl_reg.get_rdl_name(\"l2h_\", 1), this.get_name(), \"_decrthold_o\"};")); 
		outputList.add(new OutputLine(--indentLvl, "endfunction: get_decr_thold_signal"));
		// decr value
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function uvm_reg_data_t get_decr_value();"));
		outputList.add(new OutputLine(indentLvl,     "return m_decr_value;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: get_decr_value"));
		// get decr value signal
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function string get_decr_value_signal();  // decr_value signal")); 
		outputList.add(new OutputLine(indentLvl,     "uvm_reg_rdl rdl_reg;")); 
		outputList.add(new OutputLine(indentLvl,     "rdl_reg = this.get_rdl_register();")); 
		outputList.add(new OutputLine(indentLvl,     "if (m_decr_value_sig.len() > 0) return rdl_reg.get_rdl_name(\"rg_\", 1, m_decr_value_sig);")); // assumes peer field w/ no property
		outputList.add(new OutputLine(indentLvl,     "else return {rdl_reg.get_rdl_name(\"h2l_\", 1), this.get_name(), \"_decrvalue\"};"));  // input decr value signal
		outputList.add(new OutputLine(--indentLvl, "endfunction: get_decr_value_signal"));
		// get decr value signal width
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function int unsigned get_decr_value_signal_width();"));
		outputList.add(new OutputLine(indentLvl,     "return m_decr_value_sig_width;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: get_decr_value_signal_width"));
		// decr sat value
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function bit has_decr_sat();"));
		outputList.add(new OutputLine(indentLvl,     "return m_has_decr_sat;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: has_decr_sat"));
		outputList.add(new OutputLine(indentLvl++, "function uvm_reg_data_t get_decr_sat_value();"));
		outputList.add(new OutputLine(indentLvl,     "return m_decr_sat_value;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: get_decr_sat_value"));
		// get decr sat value signal
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function string get_decr_sat_value_signal();  // decr_sat_value signal")); 
		outputList.add(new OutputLine(indentLvl,     "uvm_reg_rdl rdl_reg;")); 
		outputList.add(new OutputLine(indentLvl,     "rdl_reg = this.get_rdl_register();")); 
		outputList.add(new OutputLine(indentLvl,     "if (m_decr_sat_value_sig.len() < 1) return \"\";"));  // no default input
		outputList.add(new OutputLine(indentLvl,     "else return rdl_reg.get_rdl_name(\"rg_\", 1, m_decr_sat_value_sig);")); // assumes peer field w/ no property
		outputList.add(new OutputLine(--indentLvl, "endfunction: get_decr_sat_value_signal"));
		// decr thold value
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function bit has_decr_thold();"));
		outputList.add(new OutputLine(indentLvl,     "return m_has_decr_thold;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: has_decr_thold"));
		outputList.add(new OutputLine(indentLvl++, "function uvm_reg_data_t get_decr_thold_value();"));
		outputList.add(new OutputLine(indentLvl,     "return m_decr_thold_value;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: get_decr_thold_value"));
		// get decr thold value signal
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function string get_decr_thold_value_signal();  // decr_sat_value signal")); 
		outputList.add(new OutputLine(indentLvl,     "uvm_reg_rdl rdl_reg;")); 
		outputList.add(new OutputLine(indentLvl,     "rdl_reg = this.get_rdl_register();")); 
		outputList.add(new OutputLine(indentLvl,     "if (m_decr_thold_value_sig.len() < 1) return \"\";"));  // no default input
		outputList.add(new OutputLine(indentLvl,     "else return rdl_reg.get_rdl_name(\"rg_\", 1, m_decr_thold_value_sig);")); // assumes peer field w/ no property
		outputList.add(new OutputLine(--indentLvl, "endfunction: get_decr_thold_value_signal"));

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
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function new(string name = \"rdl_alias_reg_cbs\");"));
		outputList.add(new OutputLine(indentLvl, "super.new(name);"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: new"));
		
		// create set_alias_regs function
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl, "// set alias register group for this cbs"));	
		outputList.add(new OutputLine(indentLvl++, "function void set_alias_regs(uvm_reg alias_regs[$]);"));
		outputList.add(new OutputLine(indentLvl, "m_alias_regs = alias_regs;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: set_alias_regs"));
		  		
		// task to set all regs in an alias group to same value post r/w
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl, "// set all regs in an alias group to same value post r/w"));	
		outputList.add(new OutputLine(indentLvl++, "task alias_group_predict(uvm_reg_item rw);")); 
		outputList.add(new OutputLine(indentLvl, " uvm_reg_data_t 	updated_value;"));
		outputList.add(new OutputLine(indentLvl++, "if (rw.status != UVM_IS_OK)"));
		outputList.add(new OutputLine(indentLvl--, "return;"));
		outputList.add(new OutputLine(indentLvl++, "if (rw.element_kind == UVM_REG) begin"));
		outputList.add(new OutputLine(indentLvl, "uvm_reg rg;"));
		outputList.add(new OutputLine(indentLvl, "$cast(rg, rw.element);"));
		outputList.add(new OutputLine(indentLvl++, "if (m_alias_regs[0] != null) begin"));
		outputList.add(new OutputLine(indentLvl, "updated_value = rg.get();"));
		outputList.add(new OutputLine(indentLvl++, "foreach (m_alias_regs[i]) begin"));
		outputList.add(new OutputLine(indentLvl, "void'(m_alias_regs[i].predict(updated_value));"));
		outputList.add(new OutputLine(indentLvl, "//$display(\"  new value for %s is %h\", m_alias_regs[i].get_full_name(), m_alias_regs[i].get());"));
		outputList.add(new OutputLine(--indentLvl, "end"));
		outputList.add(new OutputLine(--indentLvl, "end"));
		outputList.add(new OutputLine(--indentLvl, "end"));
		outputList.add(new OutputLine(--indentLvl, "endtask"));

		// post_read
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl, "// update all regs in group after read"));	
		outputList.add(new OutputLine(indentLvl++, "virtual task post_read(uvm_reg_item rw);")); 
		outputList.add(new OutputLine(indentLvl, " //$display(\"*** post_read ***\");"));
		outputList.add(new OutputLine(indentLvl, " alias_group_predict(rw);"));
		outputList.add(new OutputLine(--indentLvl, "endtask"));
		
		// post_write
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl, "// update all regs in group after write"));	
		outputList.add(new OutputLine(indentLvl++, "virtual task post_write(uvm_reg_item rw);")); 
		outputList.add(new OutputLine(indentLvl, " //$display(\"*** post_write ***\");"));
		outputList.add(new OutputLine(indentLvl, " alias_group_predict(rw);"));
		outputList.add(new OutputLine(--indentLvl, "endtask"));

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
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function new(string name = \"\", uvm_reg_field masked_field = null);"));
		outputList.add(new OutputLine(indentLvl,     "super.new(name);"));
		outputList.add(new OutputLine(indentLvl,     "$cast(m_masked_field, masked_field);"));  // cast to rdl interrupt
		outputList.add(new OutputLine(--indentLvl, "endfunction: new"));
		
		// override post_predict function
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "virtual function void post_predict(input uvm_reg_field fld, input uvm_reg_data_t previous, inout uvm_reg_data_t value, input uvm_predict_e kind, input uvm_path_e path, input uvm_reg_map map);"));
		outputList.add(new OutputLine(indentLvl++, "if (kind == UVM_PREDICT_READ) begin"));
		outputList.add(new OutputLine(indentLvl,     "value = m_masked_field.get_intr_masked();"));  // use the masked value
		outputList.add(new OutputLine(--indentLvl, "end"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: post_predict"));

		// close out the alias cbs definition
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl, "`uvm_object_utils(rdl_mask_intr_field_cbs)"));
		outputList.add(new OutputLine(--indentLvl, "endclass : rdl_mask_intr_field_cbs"));
	}

	/** build cbs class to be used for fields with next or intr assigned to an intr_o from another reg
	 * @param outputList - list of output lines to be updated
	 * @param indentLvl */
	public static void buildCascadeIntrFieldCbsClass(List<OutputLine> outputList, int indentLvl) {
		// generate class header 
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl, "// cbs class for fields with next or intr assigned as cascaded intr_o value"));
		outputList.add(new OutputLine(indentLvl++, "class rdl_cascade_intr_field_cbs extends uvm_reg_cbs;"));   
				
		// define
		outputList.add(new OutputLine(indentLvl, "local uvm_reg_field_rdl_interrupt m_cascade_field;"));
		
		// create new function, intr_o_reg=next/intr reg whose intr_o value will be used
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function new(string name = \"\", uvm_reg_field cascade_field = null);"));
		outputList.add(new OutputLine(indentLvl,     "super.new(name);"));
		outputList.add(new OutputLine(indentLvl,     "$cast(m_cascade_field, cascade_field);"));  // cast to rdl interrupt
		outputList.add(new OutputLine(--indentLvl, "endfunction: new"));
		
		// override post_predict function / loop thru field values and set value if any intr bits defined  
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "virtual function void post_predict(input uvm_reg_field fld, input uvm_reg_data_t previous, inout uvm_reg_data_t value, input uvm_predict_e kind, input uvm_path_e path, input uvm_reg_map map);"));
		outputList.add(new OutputLine(indentLvl,     "uvm_reg_field f[$];"));
		outputList.add(new OutputLine(indentLvl,     "uvm_reg_field_rdl rdl_f;"));
		outputList.add(new OutputLine(indentLvl,     "uvm_reg_field_rdl_interrupt rdl_intr_f;"));
		outputList.add(new OutputLine(indentLvl,     "uvm_reg m_intr_o_reg;")); 
		outputList.add(new OutputLine(indentLvl++,   "if (kind == UVM_PREDICT_READ) begin"));
		outputList.add(new OutputLine(indentLvl,       "m_intr_o_reg = m_cascade_field.get_cascade_intr_reg();")); // get referenced intr_o reg
		outputList.add(new OutputLine(indentLvl,       "m_intr_o_reg.get_fields(f);")); 
		outputList.add(new OutputLine(indentLvl,       "value = 0;"));
		outputList.add(new OutputLine(indentLvl++,     "foreach(f[i]) begin"));
		outputList.add(new OutputLine(indentLvl,         "$cast(rdl_f, f[i]);"));  // cast to rdl filed so intr methods are visible
		outputList.add(new OutputLine(indentLvl++,       "if (rdl_f.is_interrupt()) begin"));
		outputList.add(new OutputLine(indentLvl,           "$cast(rdl_intr_f, rdl_f);"));  // cast to rdl filed so intr methods are visible
		outputList.add(new OutputLine(indentLvl,           "if (rdl_intr_f.cascade_reg_is_halt) value = value | rdl_intr_f.get_halt_masked();"));  // use halt masked/enabled values here
		outputList.add(new OutputLine(indentLvl,           "else value = value | rdl_intr_f.get_intr_masked();"));  // use intr masked/enabled values here
		outputList.add(new OutputLine(--indentLvl,       "end"));
		outputList.add(new OutputLine(--indentLvl,     "end"));
		outputList.add(new OutputLine(--indentLvl,   "end"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: post_predict"));

		// close out the alias cbs definition
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl, "`uvm_object_utils(rdl_cascade_intr_field_cbs)"));
		outputList.add(new OutputLine(--indentLvl, "endclass : rdl_cascade_intr_field_cbs"));
	}
	
	// ----------- common method gen methods
	
	/** build reg test info methods */
	private static void buildRegTestInfoMethods(List<OutputLine> outputList, int indentLvl) {
		// set_reg_test_info function
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function void set_reg_test_info(bit dont_test, bit dont_compare, int js_category);"));
		outputList.add(new OutputLine(indentLvl,     "m_dont_test = dont_test;"));
		outputList.add(new OutputLine(indentLvl,     "m_dont_compare = dont_compare;"));
		outputList.add(new OutputLine(indentLvl,     "m_js_category = js_category;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: set_reg_test_info"));
		// is_dont_test
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function bit is_dont_test();"));
		outputList.add(new OutputLine(indentLvl,     "return m_dont_test;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: is_dont_test"));
		// is_dont_compare
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function bit is_dont_compare();"));
		outputList.add(new OutputLine(indentLvl,     "return m_dont_compare;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: is_dont_compare"));
		// has_js_category
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function bit has_a_js_category();"));
		outputList.add(new OutputLine(indentLvl,     "return (m_js_category > 0);"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: has_a_js_category"));
		// has_js_category
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function bit has_js_category(js_category_e cat);"));
		outputList.add(new OutputLine(indentLvl,     "return ((cat & m_js_category) > 0);"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: has_js_category"));
		// add_js_category
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function void add_js_category(js_category_e cat);"));  
		outputList.add(new OutputLine(indentLvl,     "m_js_category = m_js_category | cat;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: add_js_category"));
		// remove_js_category
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function void remove_js_category(js_category_e cat);")); 
		outputList.add(new OutputLine(indentLvl,     "m_js_category = m_js_category & ~cat;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: remove_js_category"));
	}

	/** create rdl path generation methods for uvm_reg derived class   
	 * @param outputList - list of output lines to be updated
	 * @param indentLvl */
	private static void buildRegRdlPathMethods(List<OutputLine> outputList, int indentLvl) {
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function void set_rdl_tag(string rdl_tag = \"rdl_tag\");"));
		outputList.add(new OutputLine(indentLvl, "m_rdl_tag = rdl_tag;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: set_rdl_tag"));
		
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function string get_rdl_name(string prefix, bit add_hdl_prefix = 0, string override_tag = \"\");"));
		outputList.add(new OutputLine(indentLvl,     "uvm_reg_block_rdl rdl_parent;"));
		outputList.add(new OutputLine(indentLvl,     "string rdl_tag;"));
		outputList.add(new OutputLine(indentLvl,     "if (override_tag.len() > 0)"));
		outputList.add(new OutputLine(indentLvl,     "  rdl_tag = override_tag;"));
		outputList.add(new OutputLine(indentLvl,     "else"));
		outputList.add(new OutputLine(indentLvl,     "  rdl_tag = m_rdl_tag;"));
		outputList.add(new OutputLine(indentLvl,     "if (get_parent() != null) begin"));
		outputList.add(new OutputLine(indentLvl,     "  $cast(rdl_parent, get_parent());"));
		outputList.add(new OutputLine(indentLvl,     "  return {rdl_parent.get_rdl_name(prefix, add_hdl_prefix, override_tag), rdl_tag};"));
		outputList.add(new OutputLine(indentLvl,     "end"));
		outputList.add(new OutputLine(indentLvl, "return rdl_tag;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: get_rdl_name"));
	}
	
	
	/** create rdl path generation methods for uvm_reg_block derived class   
	 * @param outputList - list of output lines to be updated
	 * @param indentLvl */
	private static void buildBlockRdlPathMethods(List<OutputLine> outputList, int indentLvl) {
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function void set_rdl_tag(string rdl_tag = \"rdl_tag\");"));
		outputList.add(new OutputLine(indentLvl, "m_rdl_tag = rdl_tag;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: set_rdl_tag"));
		
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "function string get_rdl_name(string prefix, bit add_hdl_prefix = 0, string override_tag = \"\");"));
		outputList.add(new OutputLine(indentLvl,     "uvm_reg_block_rdl rdl_parent;"));
		outputList.add(new OutputLine(indentLvl,     "string rdl_tag;"));
		outputList.add(new OutputLine(indentLvl,     "if (override_tag.len() > 0)"));
		outputList.add(new OutputLine(indentLvl,     "  rdl_tag = override_tag;"));
		outputList.add(new OutputLine(indentLvl,     "else"));
		outputList.add(new OutputLine(indentLvl,     "  rdl_tag = m_rdl_tag;"));
		outputList.add(new OutputLine(indentLvl,     "if (m_rdl_address_map) begin")); // if an address map, recursion ends
		outputList.add(new OutputLine(indentLvl,     "  if (add_hdl_prefix)"));
		outputList.add(new OutputLine(indentLvl,     "    return {m_rdl_address_map_hdl_path, \".\", prefix, rdl_tag};"));
		outputList.add(new OutputLine(indentLvl,     "  else"));
		outputList.add(new OutputLine(indentLvl,     "    return {prefix, rdl_tag};"));
		outputList.add(new OutputLine(indentLvl,     "end"));
		outputList.add(new OutputLine(indentLvl,     "if (get_parent() != null) begin"));
		outputList.add(new OutputLine(indentLvl,     "  $cast(rdl_parent, get_parent());"));
		outputList.add(new OutputLine(indentLvl,     "  return {rdl_parent.get_rdl_name(prefix, add_hdl_prefix, override_tag), rdl_tag};"));
		outputList.add(new OutputLine(indentLvl,     "end"));
		outputList.add(new OutputLine(indentLvl, "return rdl_tag;"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: get_rdl_name"));
	}
	
	/** create empty add_callbacks methods for regs/blocks (overridden by child classes if necessary)   
	 * @param outputList - list of output lines to be updated
	 * @param indentLvl */
	private static void buildAddCallbacksMethod(List<OutputLine> outputList, int indentLvl) {
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "virtual function void add_callbacks();"));
		outputList.add(new OutputLine(--indentLvl, "endfunction: add_callbacks"));		
	}
	
	/** create get_ancestor method for regs/blocks   
	 * @param outputList - list of output lines to be updated
	 * @param indentLvl */
	private static void buildRdlAncestorMethod(List<OutputLine> outputList, int indentLvl) {
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl++, "virtual function uvm_reg_block_rdl get_ancestor(int depth);"));
		outputList.add(new OutputLine(indentLvl,     "uvm_reg_block_rdl rdl_parent;"));
		outputList.add(new OutputLine(indentLvl,     "$cast(rdl_parent, get_parent());")); // cast to rdl block
		outputList.add(new OutputLine(indentLvl,     "if (depth < 2) return rdl_parent;")); // if 1 or less just return parent
		outputList.add(new OutputLine(indentLvl,     "else return rdl_parent.get_ancestor(depth-1);")); // else call recursively
		outputList.add(new OutputLine(--indentLvl, "endfunction: get_ancestor"));		
	}
}
