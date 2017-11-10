/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved.
 */
package ordt.parameters;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import ordt.annotate.AnnotateCommand;
import ordt.annotate.AnnotateSetCommand;
import ordt.extract.Ordt;
import ordt.extract.RegNumber;
import ordt.extract.model.ModComponent.CompType;
import ordt.output.systemverilog.common.wrap.WrapperRemapInvertXform;
import ordt.output.systemverilog.common.wrap.WrapperRemapSyncStagesXform;
import ordt.output.systemverilog.common.wrap.WrapperRemapXform;
import ordt.extract.model.ModRegister;
import ordt.parse.parameters.ExtParmsBaseListener;
import ordt.parse.parameters.ExtParmsLexer;
import ordt.parse.parameters.ExtParmsParser;

/**
 *  @author snellenbach      
 *  Jul 12, 2014
 *
 */
public class ExtParameters extends ExtParmsBaseListener  {
	private static List<String> parmFiles;

	// standard typed parameter set
	private static HashMap<String, ExtParameter<?>> params = new HashMap<String, ExtParameter<?>>();
	
	// enums for non-standard parameter types
	public enum SVBlockSelectModes { INTERNAL, EXTERNAL, ALWAYS } 
	public enum SVDecodeInterfaceTypes { NONE, LEAF, SERIAL8, RING8, RING16, RING32, PARALLEL, ENGINE1} 
	public enum SVChildInfoModes { PERL, MODULE } 
	public enum UVMModelModes { HEAVY, LITE1, TRANSLATE1 } 
	
	// non-standard typed parameters
	private static SVDecodeInterfaceTypes sysVerRootDecoderInterface;
	private static SVDecodeInterfaceTypes sysVerSecondaryDecoderInterface;
	private static SVBlockSelectModes systemverilogBlockSelectMode;  
	private static SVChildInfoModes sysVerChildInfoMode;  
	private static UVMModelModes uvmModelMode;  

	private static int defaultMaxInternalRegReps = 4096;  // max internal reg reps allowed
	
	// list of model annotation commands
	private static List<AnnotateCommand> annotations = new ArrayList<AnnotateCommand>();
	
	// set of systemverilog wrapper signal mapping rules
	private static LinkedHashMap<String, WrapperRemapXform> xformMap = new LinkedHashMap<String, WrapperRemapXform>();
			
	public ExtParameters() {
	}
	
	/** initialize all parameters */
	public static void init() {
		
		// ---- global defaults
		params.put("min_data_size", new ExtIntegerParameter("min_data_size", 32) {  // special handling for min_data_size
			@Override
			public void set(String valStr) {
				Integer intval = Utils.strToInteger(valStr);
				if (intval != null) {
					if (!Utils.isPowerOf2(intval) || !Utils.isInRange(intval, 8, 128))  
						Ordt.errorMessage("Invalid minimum data size (" + intval + ").  Must be power of 2 between 8 and 128.");
					else {
						value = intval;
						if (value > 32) ModRegister.setDefaultWidth(value);  // if min size exceeds 32, change default
					}
				} 
				else Ordt.errorMessage("Invalid minimum data size specified (" + value + ").");
			}
		});
		initRegNumberParameter("base_address", new RegNumber(0)); 
		initRegNumberParameter("secondary_base_address", null); 
		initRegNumberParameter("secondary_low_address", null); 
		initRegNumberParameter("secondary_high_address", null); 
		initBooleanParameter("secondary_on_child_addrmaps", false); 
		initBooleanParameter("use_js_address_alignment", true); 
		initBooleanParameter("suppress_alignment_warnings", false); 
		initStringParameter("default_base_map_name", "");  
		initBooleanParameter("allow_unordered_addresses", false); 
		// special handling for debug_mode. currently defined:
		//     uvmregs_no_mem_wrap
		//     uvmregs_maps_use_max_width
		params.put("debug_mode", new ExtStringParameter("debug_mode", "") {  
			@Override
			public void set(String valStr) {
				if ((valStr != null) && !valStr.isEmpty()) {
					value = valStr;
					Ordt.warnMessage("debug_mode parameter is set (value = " + value + ").  Non-standard ordt behavior can occur.");
				} 
			}
		});
		
		// ---- rdl input defaults
		initStringListParameter("process_component", new ArrayList<String>());
		initBooleanParameter("resolve_reg_category", false); 
		initBooleanParameter("restrict_defined_property_names", true); 
		initBooleanParameter("default_rw_hw_access", false); 
		
		// ---- jspec input defaults
		initStringListParameter("process_typedef", new ArrayList<String>());
		initBooleanParameter("root_regset_is_addrmap", false); 
		initBooleanParameter("root_is_external_decode", true); 
		initIntegerParameter("external_replication_threshold", defaultMaxInternalRegReps); 	
		
		// ---- systemverilog output defaults
		initIntegerParameter("leaf_address_size", 40); 	
		sysVerRootDecoderInterface = SVDecodeInterfaceTypes.LEAF;
		sysVerSecondaryDecoderInterface = SVDecodeInterfaceTypes.NONE;
		initBooleanParameter("base_addr_is_parameter", false); 
		initStringParameter("module_tag", "");
		initBooleanParameter("use_gated_logic_clock", false);
		initIntegerParameter("gated_logic_access_delay", 6); 	
		systemverilogBlockSelectMode = SVBlockSelectModes.EXTERNAL;  
		initBooleanParameter("export_start_end", false);
		initBooleanParameter("always_generate_iwrap", false);
		initBooleanParameter("suppress_no_reset_warnings", false); 
		initBooleanParameter("generate_child_addrmaps", false); 
		initIntegerParameter("ring_inter_node_delay", 0); 	
		initBooleanParameter("bbv5_timeout_input", false); 
		initBooleanParameter("include_default_coverage", false);
		sysVerChildInfoMode = SVChildInfoModes.PERL;  
		initBooleanParameter("pulse_intr_on_clear", false);
		initBooleanParameter("reuse_iwrap_structures", false); 
		initBooleanParameter("optimize_parallel_externals", false); 
		initBooleanParameter("use_async_resets", false); 
		initBooleanParameter("nack_partial_writes", false); 
		initIntegerParameter("write_enable_size", 0); 	
		initIntegerParameter("max_internal_reg_reps", defaultMaxInternalRegReps); 	
		
		// ---- rdl output defaults
		initBooleanParameter("root_component_is_instanced", true); 
		initBooleanParameter("output_jspec_attributes", false);

		// ---- jspec output defaults
		initBooleanParameter("root_regset_is_instanced", true); 
		initStringListParameter("add_js_include", new ArrayList<String>());
		initBooleanParameter("no_root_enum_defs", false); 
		initStringParameter("root_typedef_name", null);
		initStringParameter("root_instance_name", null);
		initIntegerParameter("root_instance_repeat", 1);
		initBooleanParameter("add_user_param_defines", false); 
		
		// ---- reglist output defaults
		initBooleanParameter("display_external_regs", true); 
		initBooleanParameter("show_reg_type", false);
		initStringParameter("match_instance", null);
		initBooleanParameter("show_fields", false);
		
		// ---- uvmregs output defaults
		initBooleanParameter("suppress_no_category_warnings", false); 
		initIntegerParameter("is_mem_threshold", 1000);
		initBooleanParameter("include_address_coverage", false); 
		initIntegerParameter("max_reg_coverage_bins", 128);
		initBooleanParameter("reuse_uvm_classes", false); 
		initBooleanParameter("skip_no_reset_db_update", true); 
		uvmModelMode = UVMModelModes.HEAVY; 
		initStringMapParameter("uvm_model_parameters", new HashMap<String, String>()); 
		initBooleanParameter("regs_use_factory", false); 
		
		// ---- bench output defaults
		initStringListParameter("add_test_command", new ArrayList<String>());
		initBooleanParameter("generate_external_regs", false); 
		initBooleanParameter("only_output_dut_instances", false); 
		initIntegerParameter("total_test_time", 5000);
		
		// ---- xml output defaults
		initBooleanParameter("include_field_hw_info", true);
	}
	
	static void initBooleanParameter(String name, Boolean value) {
		params.put(name, new ExtBooleanParameter(name, value));
	}
	
	static Boolean getBooleanParameter(String name) {
		return (Boolean) params.get(name).get();
	}
	
	static void initIntegerParameter(String name, Integer value) {
		params.put(name, new ExtIntegerParameter(name, value));
	}
	
	static Integer getIntegerParameter(String name) {
		return (Integer) params.get(name).get();
	}
	
	static void initRegNumberParameter(String name, RegNumber value) {
		params.put(name, new ExtRegNumberParameter(name, value));
	}
	
	static RegNumber getRegNumberParameter(String name) {
		return (RegNumber) params.get(name).get();
	}
	
	static void initStringParameter(String name, String value) {
		params.put(name, new ExtStringParameter(name, value));
	}
	
	static String getStringParameter(String name) {
		return (String) params.get(name).get();
	}

	// StringList
	static void initStringListParameter(String name, List<String> value) {
		params.put(name, new ExtStringListParameter(name, value));
	}
	
	@SuppressWarnings("unchecked")
	static List<String> getStringListParameter(String name) {
		return (List<String>) params.get(name).get();
	}
	
	@SuppressWarnings("unchecked")
	static Boolean hasStringListParameter(String name) {
		return !((List<String>) params.get(name).get()).isEmpty();
	}
	
	// StringMap
	static void initStringMapParameter(String name, HashMap<String, String> value) {
		params.put(name, new ExtStringMapParameter(name, value));
	}
	
	@SuppressWarnings("unchecked")
	static HashMap<String, String> getStringMapParameter(String name) {
		return (HashMap<String, String>) params.get(name).get();
	}
	
	@SuppressWarnings("unchecked")
	static Boolean hasStringMapParameter(String name) {
		return !((HashMap<String, String>) params.get(name).get()).isEmpty();
	}
	
	/**
	 * read parameters from specified parms files
	 */
	public static void loadParameters(List<String> inputParmFiles) {
		// save the parameter input file names
		parmFiles = inputParmFiles;
		
    	if (parmFiles.isEmpty()) Ordt.warnMessage("No parameters file specified.  Default or inline defined parameters will be used.");

		// read parameters from each file in list
		for (String inputFile: parmFiles) {
			ReadExtParameters(inputFile);
		}
	}
	
	/**
	 * read parameters from specified file  
	 */
	public static void ReadExtParameters(String inputParmFile) {		
    	System.out.println("Ordt: reading parameters from " + inputParmFile + "...");
        try {
        	// need to create an instance to be used as parse listener
        	ExtParameters inParms = new ExtParameters();
        	
        	InputStream is = System.in;
        	if ( inputParmFile!=null ) is = new FileInputStream(inputParmFile);
        
        	ANTLRInputStream input = new ANTLRInputStream(is);
        	ExtParmsLexer lexer = new ExtParmsLexer(input);

        	CommonTokenStream tokens = new CommonTokenStream(lexer);

        	ExtParmsParser parser; 
        	parser = new ExtParmsParser(tokens);

        	ParseTree tree = parser.ext_parms_root(); 
        	//System.out.println(tree.toStringTree());
        	ParseTreeWalker walker = new ParseTreeWalker(); // create standard
        	walker.walk(inParms, tree); // initiate walk of tree with listener
        	if (parser.getNumberOfSyntaxErrors() > 0) {
        		Ordt.errorExit("Parameter file parser errors detected.");  
        	}
        	
        	//root.display(true);

        } catch (FileNotFoundException e) {
        	Ordt.errorExit("parameter file not found. "  + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }		
	}

	// ------------------- ExtParmsBaseListener override methods
	
	/**
	 * Assign global parameters
	 */
	@Override public void enterGlobal_parm_assign(@NotNull ExtParmsParser.Global_parm_assignContext ctx) {
		assignParameter(ctx.getChild(0).getText(), ctx.getChild(2).getText());
	
	}
	
	/**
	 * Assign rdl input parameters
	 */
	@Override public void enterRdl_in_parm_assign(@NotNull ExtParmsParser.Rdl_in_parm_assignContext ctx) { 
		assignParameter(ctx.getChild(0).getText(), ctx.getChild(2).getText());			
	}
	
	/**
	 * Assign jspec input parameters
	 */
	@Override public void enterJspec_in_parm_assign(@NotNull ExtParmsParser.Jspec_in_parm_assignContext ctx) { 
		assignParameter(ctx.getChild(0).getText(), ctx.getChild(2).getText());			
	}
	
	/**
	 * Assign systemverilog output parameters
	 */
	@Override public void enterSystemverilog_out_parm_assign(@NotNull ExtParmsParser.Systemverilog_out_parm_assignContext ctx) {
		assignParameter(ctx.getChild(0).getText(), ctx.getChild(2).getText());		
	}
	
	/**
	 * Assign rdl output parameters
	 */
	@Override public void enterRdl_out_parm_assign(@NotNull ExtParmsParser.Rdl_out_parm_assignContext ctx) { 
		assignParameter(ctx.getChild(0).getText(), ctx.getChild(2).getText());				
	}
	
	/**
	 * Assign jspec output parameters
	 */
	@Override public void enterJspec_out_parm_assign(@NotNull ExtParmsParser.Jspec_out_parm_assignContext ctx) {
		assignParameter(ctx.getChild(0).getText(), ctx.getChild(2).getText());		
	}
	
	/**
	 * Assign reglist output parameters
	 */
	@Override public void enterReglist_out_parm_assign(@NotNull ExtParmsParser.Reglist_out_parm_assignContext ctx) {
		assignParameter(ctx.getChild(0).getText(), ctx.getChild(2).getText());		
	}
	
	/** 
	 * Assign SystemVerilog wrapper module remap commands
	 */
	@Override public void enterSystemverilog_wrapper_remap_command(ExtParmsParser.Systemverilog_wrapper_remap_commandContext ctx) {
		String cmdName = ctx.getChild(0).getText();
		String signalPattern = ctx.getChild(1).getText();
		WrapperRemapXform xf;
		switch(cmdName) {
		case ("set_passthru"): // 'set_passthru' STR
			xf = new WrapperRemapXform();  // default is assign 
		    xformMap.put(signalPattern, xf);
			//System.out.println("ExtParameters enterSystemverilog_wrapper_remap_command: adding pattern=" + signalPattern + ", " + xf.getType());
			break;
		case ("set_invert"): // 'set_invert' STR
			xf = new WrapperRemapInvertXform(); 
		    xformMap.put(signalPattern, xf);
			//System.out.println("ExtParameters enterSystemverilog_wrapper_remap_command: adding pattern=" + signalPattern + ", " + xf.getType()); 
			break;
		case ("add_sync_stages"): // 'add_sync_stages' STR NUM ID? ID?
			int delayStages = Integer.valueOf(ctx.getChild(2).getText());
		    String clkName = (ctx.getChildCount()>3)? ctx.getChild(3).getText() : null;
		    String moduleOverride = (ctx.getChildCount()>4)? ctx.getChild(4).getText() : null;
			xf = new WrapperRemapSyncStagesXform(delayStages, clkName, moduleOverride);  
	        xformMap.put(signalPattern, xf);
			//System.out.println("ExtParameters enterSystemverilog_wrapper_remap_command: adding pattern=" + signalPattern + ", " + xf.getType());
		    break;
		default:
			Ordt.errorExit("Unsupported RTL wrapper remap command (" + ctx.getText() + ") specified in parameters.");
			break;
		}
	}
	
	/**
	 * Assign uvmregs output parameters
	 */
	@Override public void enterUvmregs_out_parm_assign(@NotNull ExtParmsParser.Uvmregs_out_parm_assignContext ctx) {
		assignParameter(ctx.getChild(0).getText(), ctx.getChild(2).getText());		
	}
	
	/**
	 * Assign bench output parameters
	 */
	@Override public void enterBench_out_parm_assign(@NotNull ExtParmsParser.Bench_out_parm_assignContext ctx) {
		assignParameter(ctx.getChild(0).getText(), ctx.getChild(2).getText());		
	}
	
	/**
	 * Assign xml output parameters
	 */
	@Override public void enterXml_out_parm_assign(ExtParmsParser.Xml_out_parm_assignContext ctx) {
		assignParameter(ctx.getChild(0).getText(), ctx.getChild(2).getText());		
	}

	/**
	 * Capture annotation command  
		 annotation_command
		   : ('set_reg_property' | 'set_field_property')
		     ID EQ STR
		     ('instances' | 'components')
		     STR
	 */
	@Override public void enterAnnotation_command(ExtParmsParser.Annotation_commandContext ctx) {
		//System.out.println("ExtParameters enterAnnotation_command: " + ctx.getText());
		processAnnotationCommand(ctx);
	}

	// -------------------

	public static void processAnnotationCommand(ParserRuleContext ctx) {
		// extract command info
		String cmdName = ctx.getChild(0).getText();
		// if a set command, then extract info and save command
		if ("set_reg_property".equals(cmdName) || "set_field_property".equals(cmdName) || "set_fieldset_property".equals(cmdName) || "set_regset_property".equals(cmdName)) {
			String nextTag = ctx.getChild(1).getText().replaceAll("\"", "");
			boolean isDefault = "default".equals(nextTag);
			int offset = isDefault? 1 : 0;
			String propertyName = ctx.getChild(1 + offset).getText().replaceAll("\"", "");
			String propertyValue = ctx.getChild(3 + offset).getText().replaceAll("\"", "");
			boolean pathUsesComps = "components".equals(ctx.getChild(4 + offset).getText());
			String pathStr = ctx.getChild(5 + offset).getText().replaceAll("\"", "");
			CompType target = "set_field_property".equals(cmdName)? CompType.FIELD : 
							  "set_fieldset_property".equals(cmdName)? CompType.FIELDSET : 
							  "set_reg_property".equals(cmdName)? CompType.REG : CompType.REGSET;
			AnnotateSetCommand cmd = new AnnotateSetCommand(target, pathUsesComps, pathStr, isDefault, propertyName, propertyValue);
			if (cmd != null) annotations.add(cmd);
		}
	}

	/** 
	 * Assign valid parameters
	 * @param parameter name
	 * @param parameter value
	 */
	public static void assignParameter(String name, String value) {
		//System.out.println("ExtParameters assignParameter: " + name + " = " + value);

        // first check list of std-typed parameters
		if (params.containsKey(name)) params.get(name).set(value);
		//else System.out.println("----- cant find param " + name);
		
		// ---- not a match for std types, so check others		
		else if (name.equals("root_has_leaf_interface")) {  // DEPRECATED 
			sysVerRootDecoderInterface = value.equals("true") ? SVDecodeInterfaceTypes.LEAF : SVDecodeInterfaceTypes.PARALLEL;
			Ordt.warnMessage("Use of control parameter 'root_has_leaf_interface' is deprecated. Use 'root_decoder_interface = leaf' instead.");
		}
		else if (name.equals("root_decoder_interface")) {  
			if (value.equals("leaf")) sysVerRootDecoderInterface = SVDecodeInterfaceTypes.LEAF;
			else if (value.equals("serial8")) sysVerRootDecoderInterface = SVDecodeInterfaceTypes.SERIAL8;
			else if (value.equals("ring8")) sysVerRootDecoderInterface = SVDecodeInterfaceTypes.RING8;
			else if (value.equals("ring16")) sysVerRootDecoderInterface = SVDecodeInterfaceTypes.RING16;
			else if (value.equals("ring32")) sysVerRootDecoderInterface = SVDecodeInterfaceTypes.RING32;
			else sysVerRootDecoderInterface = SVDecodeInterfaceTypes.PARALLEL;  // parallel interface is default
		}
		else if (name.equals("secondary_decoder_interface")) {  
			if (value.equals("leaf")) sysVerSecondaryDecoderInterface = SVDecodeInterfaceTypes.LEAF;
			else if (value.equals("serial8")) sysVerSecondaryDecoderInterface = SVDecodeInterfaceTypes.SERIAL8;
			else if (value.equals("ring8")) sysVerSecondaryDecoderInterface = SVDecodeInterfaceTypes.RING8;
			else if (value.equals("ring16")) sysVerSecondaryDecoderInterface = SVDecodeInterfaceTypes.RING16;
			else if (value.equals("ring32")) sysVerSecondaryDecoderInterface = SVDecodeInterfaceTypes.RING32;
			else if (value.equals("parallel")) sysVerSecondaryDecoderInterface = SVDecodeInterfaceTypes.PARALLEL;
			else if (value.equals("engine1")) sysVerSecondaryDecoderInterface = SVDecodeInterfaceTypes.ENGINE1;
			else sysVerSecondaryDecoderInterface = SVDecodeInterfaceTypes.NONE;  // no interface is default
		}
		else if (name.equals("use_external_select")) {  // DEPRECATED 
			systemverilogBlockSelectMode = value.equals("true") ? SVBlockSelectModes.EXTERNAL : SVBlockSelectModes.INTERNAL;
			Ordt.warnMessage("Use of control parameter 'use_external_select' is deprecated. Use 'block_select_mode' instead.");
		}
		else if (name.equals("block_select_mode")) {  
			if (value.equals("internal")) systemverilogBlockSelectMode = SVBlockSelectModes.INTERNAL;
			else if (value.equals("external")) systemverilogBlockSelectMode = SVBlockSelectModes.EXTERNAL;
			else systemverilogBlockSelectMode = SVBlockSelectModes.ALWAYS;
		}

		else if (name.equals("child_info_mode")) {  
			if (value.equals("module")) sysVerChildInfoMode = SVChildInfoModes.MODULE;
			else sysVerChildInfoMode = SVChildInfoModes.PERL;
		}

		else if (name.equals("external_decode_is_root")) {   // DEPRECATED 
			Ordt.warnMessage("Use of control parameter 'external_decode_is_root' is deprecated.");
		}

		else if (name.equals("uvm_model_mode")) {  
			if (value.equals("lite1")) uvmModelMode = UVMModelModes.LITE1;
			else if (value.equals("translate1")) uvmModelMode = UVMModelModes.TRANSLATE1;
			else uvmModelMode = UVMModelModes.HEAVY;
		}
		
		//else
		//	Ordt.errorMessage("invalid parameter detected (" + name + ").");
	}

	/** get parmFile
	 *  @return the parmFile
	 */
	public static List<String> getParmFiles() {
		return parmFiles;
	}

	// ----------------------------------- getters ------------------------------------------
	
	public static List<AnnotateCommand> getAnnotations() {
		return annotations;
	}

	/** get leafAddressSize
	 *  @return the leafAddressSize
	 */
	public static int getLeafAddressSize() {
		return getIntegerParameter("leaf_address_size");
	}

	/** get leafMinDataSize
	 *  @return the leafMinDataSize (bits)
	 */
	public static Integer getMinDataSize() {
		return getIntegerParameter("min_data_size");
	}
	
	/** get root decoder baseAddress */
	public static RegNumber getPrimaryBaseAddress() {
		return getRegNumberParameter("base_address");
	}
	
	/** true if decoder has non-null secondary decoder intf baseAddress */
	public static boolean hasSecondaryBaseAddress() {
		return getSecondaryBaseAddress() != null;
	}
	
	/** true if decoder has non-null secondary decoder intf min allowed address */
	public static boolean hasSecondaryLowAddress() {
		return getSecondaryLowAddress() != null;
	}
	
	/** true if decoder has non-null secondary decoder intf max allowed address */
	public static boolean hasSecondaryHighAddress() {
		return getSecondaryHighAddress() != null;
	}
	
	/** get secondary decoder intf baseAddress */
	public static RegNumber getSecondaryBaseAddress() {
		return getRegNumberParameter("secondary_base_address");
	}
	
	/** get secondary decoder intf min allowed address */
	public static RegNumber getSecondaryLowAddress() {
		return getRegNumberParameter("secondary_low_address");
	}
	
	/** get secondary decoder intf max allowed address */
	public static RegNumber getSecondaryHighAddress() {
		return getRegNumberParameter("secondary_high_address");
	}
	
	/** true if secondary interfaces should be created on child address map decoders */
	public static Boolean secondaryOnChildAddrmaps() {
		return getBooleanParameter("secondary_on_child_addrmaps");
	}

	/** get useJsAddressAlignment
	 */
	public static Boolean useJsAddressAlignment() {
		return getBooleanParameter("use_js_address_alignment");  
	}

	/** get suppressAlignmentWarnings  
	 */
	public static Boolean suppressAlignmentWarnings() {
		return getBooleanParameter("suppress_alignment_warnings");
	}
	
	/** get allowUnorderedAddresses  
	 */
	public static Boolean allowUnorderedAddresses() {
		return getBooleanParameter("allow_unordered_addresses");
	}

	/** get defaultBaseMapName
	 *  @return the defaultBaseMapName
	 */
	public static String defaultBaseMapName() {
		return getStringParameter("default_base_map_name");
	}

	/** return true if specified debug mode is set */
	public static boolean hasDebugMode(String targetMode) {
		String debugMode = getStringParameter("debug_mode");
		return (debugMode != null) && debugMode.contains(targetMode);
	}

	/** returns true if comp names have been specified for processing */
	public static boolean hasRdlProcessComponents() {
		return hasStringListParameter("process_component");
	}
	
	/** get list of comp to be processed
	 *  @return the jspecProcessTypedef
	 */
	public static List<String> getRdlProcessComponents() {
		return getStringListParameter("process_component");
	}

	/** returns true if reg categories should be deduced from rdl settings */
	public static boolean rdlResolveRegCategory() {
		return getBooleanParameter("resolve_reg_category");
	}
	
	/** returns true if user defined rdl properties should always start with 'p_' */
	public static boolean rdlRestrictDefinedPropertyNames() {
		return getBooleanParameter("restrict_defined_property_names");
	}
	
	public static boolean rdlDefaultRwHwAccess() {
		return getBooleanParameter("default_rw_hw_access");
	}
		
	// -------- js input getters

	/** returns true if typedef names have been specified for processing */
	public static boolean hasJspecProcessTypedefs() {
		return hasStringListParameter("process_typedef");
	}
	
	/** get list of typedefs to be processed
	 *  @return the jspecProcessTypedef
	 */
	public static List<String> getJspecProcessTypedefs() {
		return getStringListParameter("process_typedef");
	}

	/** get jspecRootRegsetIsAddrmap
	 *  @return the jspecRootRegsetIsAddrmap
	 */
	public static Boolean jspecRootRegsetIsAddrmap() {
		return getBooleanParameter("root_regset_is_addrmap");
	}

	/** get jspecRootIsExternalDecode
	 *  @return the jspecRootIsExternalDecode
	 */
	public static Boolean jspecRootIsExternalDecode() {
		return getBooleanParameter("root_is_external_decode");
	}
	
	/** get jspecExternalReplicationThreshold
	 *  @return the jspecExternalReplicationThreshold
	 */
	public static int jspecExternalReplicationThreshold() {
		return getIntegerParameter("external_replication_threshold");
	}

	// -------- systemverilog getters
	
	public static SVDecodeInterfaceTypes getSysVerRootDecoderInterface() {
		return sysVerRootDecoderInterface;
	}
	
	public static SVDecodeInterfaceTypes getSysVerSecondaryDecoderInterface() {
		return sysVerSecondaryDecoderInterface;
	}

	/** get baseAddrIsParameter
	 */
	public static Boolean systemverilogBaseAddrIsParameter() {
		return getBooleanParameter("base_addr_is_parameter");
	}

	/** get getSystemverilogModuleTag
	 */
	public static String getSystemverilogModuleTag() {
		return getStringParameter("module_tag");
	}

	/** get systemverilogUseGatedLogicClk
	 */
	public static Boolean systemverilogUseGatedLogicClk() {
		return getBooleanParameter("use_gated_logic_clock");
	}

	/** get systemverilogGatedLogicAccessDelay
	 */
	public static Integer systemverilogGatedLogicAccessDelay() {
		return getIntegerParameter("gated_logic_access_delay");
	}

	/** get systemverilogExportStartEnd
	 */
	public static Boolean systemverilogExportStartEnd() {
		return getBooleanParameter("export_start_end");
	}

	/** get systemverilogBlockSelectMode */
	public static SVBlockSelectModes getSystemverilogBlockSelectMode() {
		return systemverilogBlockSelectMode;
	}

	/** get sysVerChildInfoMode */
	public static SVChildInfoModes getSysVerChildInfoMode() {
		return sysVerChildInfoMode;
	}
	
	public static Boolean sysVerSuppressNoResetWarnings() {
		return getBooleanParameter("suppress_no_reset_warnings");
	}
	
	public static Boolean sysVerGenerateChildAddrmaps() {
		return getBooleanParameter("generate_child_addrmaps");
	}
	
	public static int sysVerRingInterNodeDelay() {
		return getIntegerParameter("ring_inter_node_delay");
	}
	
	public static Boolean sysVerBBV5TimeoutInput() {
		return getBooleanParameter("bbv5_timeout_input");
	}
	
	public static Boolean sysVerIncludeDefaultCoverage() {
		return getBooleanParameter("include_default_coverage");
	}
	
	public static Boolean sysVerPulseIntrOnClear() {
		return getBooleanParameter("pulse_intr_on_clear");
	}
	
	public static Boolean sysVerOptimizeParallelExternals() {
		return getBooleanParameter("optimize_parallel_externals");
	}
		
	public static Boolean sysVerUseAsyncResets() {
		return getBooleanParameter("use_async_resets");
	}
	
	public static Boolean sysVerNackPartialWrites() {
		return getBooleanParameter("nack_partial_writes");
	}
	
	public static int sysVerWriteEnableSize() {
		return getIntegerParameter("write_enable_size");
	}

	/* return max allowed internal register replications */
	public static int sysVerMaxInternalRegReps() {
		return getIntegerParameter("max_internal_reg_reps");
	}

	/** returns true if always_generate_iwrap is true or wrapper xforms are specified */
	public static Boolean sysVerGenerateWrapperModule() {
		return getBooleanParameter("always_generate_iwrap") || !xformMap.isEmpty();
	}
		
	public static Boolean sysVerReuseIwrapStructures() {
		return getBooleanParameter("reuse_iwrap_structures");
	}
	
	/** return rtl wrapper signal transforms */
	public static LinkedHashMap<String, WrapperRemapXform> sysVerWrapperXformMap() {
		return xformMap;
	}
	
	// bench parameter getters

	public static Boolean sysVerGenerateExternalRegs() {
		return getBooleanParameter("generate_external_regs");
	}

	// rdl parameter getters

	/** get rdlRootComonentIsInstanced
	 *  @return the rdlRootComonentIsInstanced
	 */
	public static Boolean rdlRootComponentIsInstanced() {
		return getBooleanParameter("root_component_is_instanced");
	}

	/** get rdlOutputJspecAttributes
	 *  @return the rdlOutputJspecAttributes
	 */
	public static Boolean rdlOutputJspecAttributes() {
		return getBooleanParameter("output_jspec_attributes");
	}

	/** get rdlNoRootEnumDefs
	 */
	public static Boolean rdlNoRootEnumDefs() {
		return getBooleanParameter("no_root_enum_defs");
	}
	
	// js out getters/setters

	public static Boolean jspecRootRegsetIsInstanced() {
		return getBooleanParameter("root_regset_is_instanced");
	}

	/** get jspecIncludeFiles list */
	public static List<String> getJspecIncludeFiles() {
		return getStringListParameter("add_js_include");
	}
	
	public static String getJspecRootTypedefName() {
		return getStringParameter("root_typedef_name");
	}
	
	public static String getJspecRootInstanceName() {
		return getStringParameter("root_instance_name");
	}
	
	public static Integer getJspecRootInstanceRepeat() {
		return getIntegerParameter("root_instance_repeat");
	}
	
	public static Boolean jspecAddUserParamDefines() {
		return getBooleanParameter("add_user_param_defines");
	}
	
	/** get reglistDisplayExternalRegs
	 *  @return the reglistDisplayExternalRegs
	 */
	public static Boolean reglistDisplayExternalRegs() {
		return getBooleanParameter("display_external_regs");
	}
	   
	// reglist getters/setters

	/** get reglistShowRegType
	 *  @return the reglistShowRegType
	 */
	public static Boolean reglistShowRegType() {
		return getBooleanParameter("show_reg_type");
	}

	public static String getReglistMatchInstance() {
		return getStringParameter("match_instance");
	}

	public static Boolean reglistShowFields() {
		return getBooleanParameter("show_fields");
	}
	
	// uvm getters/setters
	
	public static Boolean uvmregsSuppressNoCategoryWarnings() {
		return getBooleanParameter("suppress_no_category_warnings");
	}

	/** get uvmregsIsMemThreshold
	 *  @return the uvmregsIsMemThreshold
	 */
	public static Integer uvmregsIsMemThreshold() {
		return getIntegerParameter("is_mem_threshold");
	}

	public static Boolean uvmregsIncludeAddressCoverage() {
		return getBooleanParameter("include_address_coverage");
	}

	public static Boolean uvmregsReuseUvmClasses() {
		return getBooleanParameter("reuse_uvm_classes");
	}

	public static Boolean uvmregsSkipNoResetDbUpdate() {
		return getBooleanParameter("skip_no_reset_db_update");
	}
	
	public static Boolean uvmregsRegsUseFactory() {
		return getBooleanParameter("regs_use_factory");
	}
	
	public static int uvmregsMaxRegCoverageBins() {
		return getIntegerParameter("max_reg_coverage_bins");
	}

	public static UVMModelModes uvmregsModelMode() {
		return uvmModelMode;
	}
	
	/** get specified uvm model parameter */
	public static String getUvmModelParameter(String parm, String dflt) {
		String retVal = getStringMapParameter("uvm_model_parameters").get(parm);
		if (retVal==null) return dflt;  // return default value if parameter not found
		return retVal;
	}
	
	// --------
	
	/** returns true if test commands have been specified  */
	public static boolean hasTestCommands() {
		return hasStringListParameter("add_test_command");
	}
	
	/** get list of test commands to be processed
	 */
	public static List<String> getTestCommands() {
		return getStringListParameter("add_test_command");
	}
	
	public static Boolean benchOnlyOutputDutInstances() {
		return getBooleanParameter("only_output_dut_instances");
	}
	
	public static int benchTotalTestTime() {
		return getIntegerParameter("total_test_time");
	}
	
	public static Boolean xmlIncludeFieldHwInfo() {
		return getBooleanParameter("include_field_hw_info");
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		List<String> inFiles = new ArrayList<String>();
		ExtParameters.loadParameters(inFiles);
		//System.out.println("leafAddressSize=" + getLeafAddressSize());
		//System.out.println("leafMinDataSize=" + getMinDataSize());
		//System.out.println("leafMaxDataSize=" + getLeafMaxDataSize());
		//System.out.println("leafBaseAddress=" + getLeafBaseAddress());
		for (String key: params.keySet()) {
			System.out.println("key=" + key + ", value=" + params.get(key));
		}
		System.out.println("has rdl proc comps=" + hasRdlProcessComponents());

	}

}
