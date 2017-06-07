package ordt.output.uvmregs;

import java.util.List;

import ordt.output.OutputLine;
import ordt.output.systemverilog.common.SystemVerilogFunction;
import ordt.output.systemverilog.common.SystemVerilogTask;
import ordt.parameters.ExtParameters;

public class UVMRdlTranslate1Classes extends UVMRegsCommon {
	
	// alt model additional read/write parameter
	public static boolean hasAddlParameter = true;  // alt model read/write has addl input 
	public static String addlParameter = "facade";  // alt model parameter name 
	private static String firstChar = addlParameter.substring(0, 1);
	public static String addlParameterType = addlParameter.replaceFirst(firstChar, firstChar.toUpperCase());  // alt model parameter type  
	public static String addlParameterLocal = "m_" + addlParameter;  
	
	// alt model package import
	public static String altModelPackageFile = ExtParameters.getUvmModelParameter("altModelPackageFile", "unknown package file");  
	public static String altModelPackage = ExtParameters.getUvmModelParameter("altModelPackage", "unknown package");   

	/** build a package with all derived classes for lite model 
	 * @param outputList 
	 * @param indentLvl */
	public static void buildRdlPackage(List<OutputLine> outputList, int indentLvl) {
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl, "// ordt_uvm_reg_translate1_pkg"));
		outputList.add(new OutputLine(indentLvl++, "`ifndef ORDT_UVM_REG_TRANSLATE1_PKG_SV"));
		outputList.add(new OutputLine(indentLvl,     "`define ORDT_UVM_REG_TRANSLATE1_PKG_SV"));

		outputList.add(new OutputLine(indentLvl,   "`include \"uvm_macros.svh\""));
		if (altModelPackageFile != null) outputList.add(new OutputLine(indentLvl,   "`include \"" + altModelPackageFile + "\""));
		outputList.add(new OutputLine(indentLvl++, "package ordt_uvm_reg_translate1_pkg;")); 
		outputList.add(new OutputLine(indentLvl,   "import uvm_pkg::*;"));
		if (altModelPackage != null) outputList.add(new OutputLine(indentLvl, "import " + altModelPackage + ";"));  // add alt model pkg
		outputList.add(new OutputLine(indentLvl,   "import jspec::*;"));
		// create derived classes
		buildRdlClasses(outputList, indentLvl);
		// close out the package definition
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(--indentLvl,   "endpackage"));
		outputList.add(new OutputLine(--indentLvl, "`endif"));
	}

	/** build all base translation classes  
	 * @param outputList 
	 * @param indentLvl */
	public static void buildRdlClasses(List<OutputLine> outputList, int indentLvl) {
		// create base level translation class containing static elements
		buildUvmBaseTranslateClass(outputList, indentLvl);
		// create block mapping class
		buildUvmBlockTranslateClass(outputList, indentLvl);
		// create reg mapping class
		buildUvmRegTranslateClass(outputList, indentLvl);
		// create reg mapping class
		buildUvmVRegTranslateClass(outputList, indentLvl);
		// create field mapping class
		buildUvmRegFieldTranslateClass(outputList, indentLvl);
	}

	/** create reg base level translation class containing static elements */
	private static void buildUvmBaseTranslateClass(List<OutputLine> outputList, int indentLvl) {
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl, "// uvm_base_translate class"));
		outputList.add(new OutputLine(indentLvl++, "class uvm_base_translate;")); 
		
		// static class parameters
		if (hasAddlParameter)
		   outputList.add(new OutputLine(indentLvl, "static " + addlParameterType + " " + addlParameterLocal + ";"));  
		
		outputList.add(new OutputLine(indentLvl, "static uvm_reg_data_t m_data_store;")); 
		outputList.add(new OutputLine(indentLvl, "static int m_data_store_id = 0;")); // reg id for current stored data
		outputList.add(new OutputLine(indentLvl, "static int m_inst_count = 0;")); 
		outputList.add(new OutputLine(indentLvl, "protected int m_id = 0;")); // unique reg id
		
		// get data_store slice  
		SystemVerilogFunction func = new SystemVerilogFunction("int", "get_id");
		func.addStatement("return m_id;");
		outputList.addAll(func.genOutputLines(indentLvl));	


		if (hasAddlParameter) {
			// set additional static read/write parameter
			func = new SystemVerilogFunction("void", "set_" + addlParameter);
			func.addComment("Set root alt model " + addlParameter + " for reads/writes");
			func.addIO(addlParameterType, addlParameter);
			func.addStatement(addlParameterLocal + " = " + addlParameter + ";");
			outputList.addAll(func.genOutputLines(indentLvl));	
		}
		
		// get data_stare
		func = new SystemVerilogFunction("uvm_reg_data_t", "get_data_store");
		func.addComment("Get temp data store");
		func.addIO("int", "id");  // unique register id
		func.addIO("input", "int", "loidx", "0");
		func.addIO("input", "int", "width", "0");
		func.addStatement("if (id != m_data_store_id) $display(\"ERROR in UVM reg translate: attempting get() to a register not in data store\");");  
		func.addStatement("if (width == 0) return m_data_store;");  // no width specified, so get full data_store
		func.addStatement("else return get_data_store_slice(loidx, width);");  // width specified, so get slice
		outputList.addAll(func.genOutputLines(indentLvl));
		
		// set data_stare
		func = new SystemVerilogFunction("void", "set_data_store");
		func.addComment("Set temp data store");
		func.addIO("int", "id");  // unique register id
		func.addIO("uvm_reg_data_t", "value");
		func.addIO("input", "int", "loidx", "0");
		func.addIO("input", "int", "width", "0");
		func.addStatement("if (id != m_data_store_id) begin");  // clear store and update store_id if a new reg is being set
		func.addStatement("  m_data_store = 0;");
		func.addStatement("  m_data_store_id = id;");
		func.addStatement("end"); 
		func.addStatement("if (width == 0) m_data_store = value;");  // no width specified, so set full data_store
		func.addStatement("else set_data_store_slice(value, loidx, width);");  // width specified, so set slice    
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// get data_store slice  
		func = new SystemVerilogFunction("uvm_reg_data_t", "get_data_store_slice");
		func.addIO("input", "int", "loidx", "0");
		func.addIO("input", "int", "width", "0");
		addGetSliceStatements(func, "m_data_store", "loidx", "width");
		outputList.addAll(func.genOutputLines(indentLvl));	

		// set data_store slice  
		func = new SystemVerilogFunction("void", "set_data_store_slice");
		func.addIO("uvm_reg_data_t", "value");
		func.addIO("input", "int", "loidx", "0");
		func.addIO("input", "int", "width", "0");
		func.addStatement("if (value >> width) begin");
		func.addStatement("  $display(\"ERROR in UVM reg translate: atempting to set a slice value greater than field width\");");
		func.addStatement("  value &= ((1<<width)-1);");
		func.addStatement("end");
		addSetSliceStatements(func, "m_data_store", "value", "loidx", "width");
		outputList.addAll(func.genOutputLines(indentLvl));	

		// close out the class definition
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(--indentLvl, "endclass : uvm_base_translate"));
	}

	/** create block mapping class */
	private static void buildUvmBlockTranslateClass(List<OutputLine> outputList, int indentLvl) {
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl, "// uvm_block_translate class"));
		outputList.add(new OutputLine(indentLvl++, "class uvm_block_translate;")); 
		
		outputList.add(new OutputLine(indentLvl, "protected uvm_block_translate m_parent;")); // parent block
		outputList.add(new OutputLine(indentLvl, "protected int m_rep;"));			
		
		// new function
		SystemVerilogFunction func = new SystemVerilogFunction(null, "new");
		func.addIO("input", "uvm_block_translate", "parent", null);
		func.addStatement("m_parent = parent;");
		outputList.addAll(func.genOutputLines(indentLvl));	

		// set_rep - set rep number of this instance
		func = new SystemVerilogFunction(null, "set_rep");
		func.addIO("input", "int", "rep", null);
		func.addStatement("m_rep = rep;");
		outputList.addAll(func.genOutputLines(indentLvl));	

		// close out the class definition
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(--indentLvl, "endclass : uvm_block_translate"));
	}

	/** create reg mapping class */
	private static void buildUvmRegTranslateClass(List<OutputLine> outputList, int indentLvl) {
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl, "// uvm_reg_translate class"));
		outputList.add(new OutputLine(indentLvl++, "class uvm_reg_translate #(type REGDATA_T = int, int REGWIDTH = 32) extends uvm_base_translate;")); 
		
		outputList.add(new OutputLine(indentLvl, "protected uvm_block_translate m_parent;")); // parent block
		outputList.add(new OutputLine(indentLvl, "protected int m_rep;"));			
		
		// new function
		SystemVerilogFunction func = new SystemVerilogFunction(null, "new");
		func.addIO("input", "uvm_block_translate", "parent", null);
		func.addStatement("m_parent = parent;");
		func.addStatement("m_id = ++m_inst_count;");
		outputList.addAll(func.genOutputLines(indentLvl));	

		// set_rep - set rep number of this instance
		func = new SystemVerilogFunction(null, "set_rep");
		func.addIO("input", "int", "rep", null);
		func.addStatement("m_rep = rep;");
		outputList.addAll(func.genOutputLines(indentLvl));	
				
		// add read/write translation methods
		SystemVerilogTask task = new SystemVerilogTask("read");
		task.setVirtual();
		task.addComment("Register read translation class");
		task.addIO("output", "uvm_status_e", "status", null);
		task.addIO("output", "uvm_reg_data_t", "value", null);
		task.addIO("input", "uvm_path_e", "path", "UVM_DEFAULT_PATH");
		task.addStatement("REGDATA_T read_data;");
		task.addStatement("value = 0;");
		task.addStatement("call_alt_read(read_data" + (hasAddlParameter? ", " + addlParameterLocal : "") + ");");
		task.addStatement("value = read_data;");
		outputList.addAll(task.genOutputLines(indentLvl));	
		
		task = new SystemVerilogTask("call_alt_read");
		task.addComment("Alt model register read (overloaded by child classes)");
		task.setVirtual();
		task.addIO("output", "REGDATA_T", "read_data", null);
		if (hasAddlParameter)
			task.addIO("input", addlParameterType, addlParameterLocal, null);
		outputList.addAll(task.genOutputLines(indentLvl));	

		task = new SystemVerilogTask("write");
		task.setVirtual();
		task.addComment("Register write translation class");
		task.addIO("output", "uvm_status_e", "status", null);
		task.addIO("input", "uvm_reg_data_t", "value", null);
		task.addIO("input", "uvm_path_e", "path", "UVM_DEFAULT_PATH");
		task.addStatement("REGDATA_T write_data;");
		task.addStatement("write_data = value[REGWIDTH-1:0];");
		task.addStatement("call_alt_write(write_data" + (hasAddlParameter? ", " + addlParameterLocal : "") + ");");
		outputList.addAll(task.genOutputLines(indentLvl));	
		
		task = new SystemVerilogTask("call_alt_write");
		task.addComment("Alt model register write (overloaded by child classes)");
		task.setVirtual();
		task.addIO("input", "REGDATA_T", "write_data", null);
		if (hasAddlParameter)
			task.addIO("input", addlParameterType, addlParameterLocal, null);
		outputList.addAll(task.genOutputLines(indentLvl));	
		
		// add get/set translation methods
		func = new SystemVerilogFunction("uvm_reg_data_t", "get");
		func.setVirtual();
		func.addStatement("return get_data_store(m_id);");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		func = new SystemVerilogFunction(null, "set");
		func.setVirtual();
		func.addIO("input", "uvm_reg_data_t", "value", null);
		task.addStatement("set_data_store(m_id, value);");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// add slice get/set slice translation methods (called by field get/set)
		func = new SystemVerilogFunction("uvm_reg_data_t", "get_slice");
		func.addIO("input", "int", "loidx", null);
		func.addIO("input", "int", "width", null);
		func.addStatement("return get_data_store(m_id, loidx, width);");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		func = new SystemVerilogFunction(null, "set_slice");
		func.addIO("input", "uvm_reg_data_t", "value", null);
		func.addIO("input", "int", "loidx", null);
		func.addIO("input", "int", "width", null);
		func.addStatement("set_data_store(m_id, value, loidx, width);");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// add update/mirror translation methods   
		task = new SystemVerilogTask("mirror");
		task.setVirtual();
		task.addComment("Register read into data store");
		task.addIO("output", "uvm_status_e", "status", null);
		task.addIO("input", "uvm_check_e", "check", "UVM_NO_CHECK");
		task.addIO("input", "uvm_path_e", "path", "UVM_DEFAULT_PATH");
		task.addStatement("uvm_reg_data_t mirror_data;");
		task.addStatement("read(status, mirror_data, path);");
		task.addStatement("set_data_store(m_id, mirror_data);");
		outputList.addAll(task.genOutputLines(indentLvl));	
		
		// 
		task = new SystemVerilogTask("update");
		task.setVirtual();
		task.addComment("Register write from data_store");
		task.addIO("output", "uvm_status_e", "status", null);
		task.addIO("input", "uvm_path_e", "path", "UVM_DEFAULT_PATH");
		task.addStatement("uvm_reg_data_t update_data = get_data_store(m_id);");
		task.addStatement("write(status, update_data, path);");
		outputList.addAll(task.genOutputLines(indentLvl));
				
		// close out the class definition
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(--indentLvl, "endclass : uvm_reg_translate"));
	}

	/** create reg mapping class */
	private static void buildUvmVRegTranslateClass(List<OutputLine> outputList, int indentLvl) {
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl, "// uvm_vreg_translate class"));
		outputList.add(new OutputLine(indentLvl++, "class uvm_vreg_translate #(type REGDATA_T = int, int REGWIDTH = 32) extends uvm_base_translate;")); 
		
		outputList.add(new OutputLine(indentLvl, "protected uvm_block_translate m_parent;")); // parent block
		
		// new function
		SystemVerilogFunction func = new SystemVerilogFunction(null, "new");
		func.addIO("input", "uvm_block_translate", "parent", null);
		func.addStatement("m_parent = parent;");
		func.addStatement("m_id = ++m_inst_count;");
		outputList.addAll(func.genOutputLines(indentLvl));	
				
		// add read/write translation methods
		SystemVerilogTask task = new SystemVerilogTask("read");
		task.addComment("Virtual register read translation class");
		task.addIO("input", "longint unsigned", "idx", null);
		task.addIO("output", "uvm_status_e", "status", null);
		task.addIO("output", "uvm_reg_data_t", "value", null);
		task.addIO("input", "uvm_path_e", "path", "UVM_DEFAULT_PATH");
		task.addStatement("REGDATA_T read_data;");
		task.addStatement("value = 0;");
		task.addStatement("call_alt_read(idx, read_data" + (hasAddlParameter? ", " + addlParameterLocal : "") + ");");
		task.addStatement("value = read_data;");
		outputList.addAll(task.genOutputLines(indentLvl));	
		
		task = new SystemVerilogTask("call_alt_read");
		task.addComment("Alt model virtual register read (overloaded by child classes)");
		task.setVirtual();
		task.addIO("input", "longint unsigned", "idx", null);
		task.addIO("output", "REGDATA_T", "read_data", null);
		if (hasAddlParameter)
			task.addIO("input", addlParameterType, addlParameterLocal, null);
		outputList.addAll(task.genOutputLines(indentLvl));	

		task = new SystemVerilogTask("write");
		task.addComment("Virtual register write translation class");
		task.addIO("input", "longint unsigned", "idx", null);
		task.addIO("output", "uvm_status_e", "status", null);
		task.addIO("input", "uvm_reg_data_t", "value", null);
		task.addIO("input", "uvm_path_e", "path", "UVM_DEFAULT_PATH");
		task.addStatement("REGDATA_T write_data;");
		task.addStatement("write_data = value[REGWIDTH-1:0];");
		task.addStatement("call_alt_write(idx, write_data" + (hasAddlParameter? ", " + addlParameterLocal : "") + ");");
		outputList.addAll(task.genOutputLines(indentLvl));	
		
		task = new SystemVerilogTask("call_alt_write");
		task.addComment("Alt model virtual register write (overloaded by child classes)");
		task.setVirtual();
		task.addIO("input", "longint unsigned", "idx", null);
		task.addIO("input", "REGDATA_T", "write_data", null);
		if (hasAddlParameter)
			task.addIO("input", addlParameterType, addlParameterLocal, null);
		outputList.addAll(task.genOutputLines(indentLvl));	
		
		// add poor mans mirror translation methods
		// TODO
		
		// close out the class definition
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(--indentLvl, "endclass : uvm_vreg_translate"));
	}

	/** create non-uvmreg child field class with no data store and lightweight api (get/set)
	 * 
	 * @param outputList - list where generated output lines will be added
	 * @param indentLvl - incoming indent level
	 */
	protected static void buildUvmRegFieldTranslateClass(List<OutputLine> outputList, int indentLvl) {
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(indentLvl, "// uvm_field_translate class"));
		outputList.add(new OutputLine(indentLvl++, "class uvm_field_translate;")); 
		
		outputList.add(new OutputLine(indentLvl, "protected uvm_base_translate m_parent;")); // parent reg
		outputList.add(new OutputLine(indentLvl, "protected int m_loidx;")); // field low index
		outputList.add(new OutputLine(indentLvl, "protected int m_width;")); // field width
		
		// new function
		SystemVerilogFunction func = new SystemVerilogFunction(null, "new");
		func.addIO("input", "uvm_base_translate", "parent", null);
		func.addIO("input", "int", "loidx", null);
		func.addIO("input", "int", "width", null);
		func.addStatement("m_parent = parent;");
		func.addStatement("m_loidx = loidx;");
		func.addStatement("m_width = width;");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// add get/set methods here matching uvm_regs
		func = new SystemVerilogFunction("uvm_reg_data_t", "get");
		func.setVirtual();
		func.addStatement("int p_id;");
		func.addStatement("p_id = m_parent.get_id();");
		func.addStatement("return m_parent.get_data_store(p_id, m_loidx, m_width);");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		func = new SystemVerilogFunction(null, "set");
		func.setVirtual();
		func.addIO("input", "uvm_reg_data_t", "value", null);
		func.addStatement("int p_id;");
		func.addStatement("p_id = m_parent.get_id();");
		func.addStatement("m_parent.set_data_store(p_id, value, m_loidx, m_width);");
		outputList.addAll(func.genOutputLines(indentLvl));	
		
		// close out the class definition
		outputList.add(new OutputLine(indentLvl, ""));	
		outputList.add(new OutputLine(--indentLvl, "endclass : uvm_field_translate"));
	}

}