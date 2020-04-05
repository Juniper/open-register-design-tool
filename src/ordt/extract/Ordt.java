/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved.
 */
package ordt.extract;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import ordt.annotate.AnnotateCommand;
import ordt.extract.model.ModComponent;
import ordt.extract.model.ModRegister;
import ordt.output.OutputBuilder;
import ordt.output.common.MsgUtils;
import ordt.output.cppmod.CppModBuilder;
import ordt.output.drvmod.cpp.CppDrvModBuilder;
import ordt.output.drvmod.py.PyDrvModBuilder;
import ordt.output.othertypes.JsonBuilder;
import ordt.output.othertypes.JspecBuilder;
import ordt.output.othertypes.RdlBuilder;
import ordt.output.othertypes.RegListBuilder;
import ordt.output.othertypes.XmlBuilder;
import ordt.output.systemverilog.SystemVerilogBuilder;
import ordt.output.systemverilog.SystemVerilogChildInfoBuilder;
import ordt.output.systemverilog.SystemVerilogDefinedOrdtSignals;
import ordt.output.systemverilog.SystemVerilogTestBuilder;
import ordt.output.uvmregs.UVMRegsBuilder;
import ordt.output.uvmregs.UVMRegsLite1Builder;
import ordt.output.uvmregs.UVMRegsNativeBuilder;
import ordt.output.verilog.VerilogBuilder;
import ordt.output.verilog.VerilogTestBuilder;
import ordt.parameters.ExtParameters;
import ordt.parameters.ExtParameters.SVChildInfoModes;
import ordt.parameters.ExtParameters.UVMModelModes;

public class Ordt {

	private static String version = "200404.01";
	private static DebugController debug = new MyDebugController(); // override design annotations, input/output files

	public enum InputType { RDL, JSPEC };
	private static InputType inputType;   // primary input file type
	private static String inputFile = null;   // primary input file
    private static List<OverlayFileInfo> overlayFiles = new ArrayList<OverlayFileInfo>();   // overlay input files
	
	private static List<String> inputParmFiles = new ArrayList<String>();

	public enum OutputType { VERILOG, SYSTEMVERILOG, JSPEC, RALF, RDL, REGLIST, SVBENCH, VBENCH, 
		                     UVMREGS, UVMREGSPKG, XML, CPPMOD, CPPDRVMOD, PYDRVMOD, JSON, SVCHILDINFO };
	private static HashMap<OutputType, String> outputNames = new HashMap<OutputType, String>();
	private static HashMap<OutputType, String> commentChars = new HashMap<OutputType, String>();
	private static HashMap<OutputType, String> outputFileNames = new HashMap<OutputType, String>();
	private static HashMap<String, OutputType> outputArgs = new HashMap<String, OutputType>();
	
    private static RegModelIntf model;
    
    /**
	 * @param args
	 */
    public static void main(String[] args) throws Exception {
        
        // extract command line args
        defineOutputArgs();
        if ( args.length>0 ) {
       	    int remainingArgs = args.length;
        	// input file is last arg
        	inputFile = args[--remainingArgs];
        	// now get any options
        	while (remainingArgs>0) {   
        		String arg = args[args.length - remainingArgs - 1];  // next arg
        		// output file args
        		if (outputArgs.containsKey(arg) && (remainingArgs>1)) {
        			outputFileNames.put(outputArgs.get(arg), args[args.length - remainingArgs]);
            		remainingArgs -= 2;
        		}
        		// parameter file
        		else if (arg.equals("-parms") && (remainingArgs>1)) {
        			inputParmFiles.add(args[args.length - remainingArgs]);
            		remainingArgs -= 2;
        		}
        		// overlay file / form: -overlay tag file
        		else if (arg.equals("-overlay") && (remainingArgs>2)) {
        			String olayTag = args[args.length - remainingArgs];
        			String olayName = args[args.length - remainingArgs + 1];
        			overlayFiles.add(new OverlayFileInfo(olayName, olayTag));
            		remainingArgs -= 3;
        		}
        		else showUsage();
        	}
        	//System.out.println("input=" + inputFile + ", verilog=" + vlogOutName + ", jspec=" + jspecOutName); System.exit(0);
        }
        // no args specified
        else {
        	if (debug.isActive()) debug.setRunInfo(); // override info if debug
        	else showUsage();	
        }
        
		try {	  
	    	System.out.println("Open Register Design Tool, version=" + getVersion() + ", input=" + inputFile);
	    	// get control parameters from parms files
	    	ExtParameters.init();
	    	ExtParameters.loadParameters(inputParmFiles);
	    	
			// extract model from input file and prep as needed
	    	model = extractModel(inputFile);

        	// define output names/comment chars by type
        	defineOutputNames();
        	defineCommentChars();
        	
    		SystemVerilogDefinedOrdtSignals.initDefinedSignalMap();  // load the mapping of pre-defined systemverilog signals since used in multiple outputs, eg uvmregs
        	
        	// generate output of all types specified on command line
        	for (OutputType tp : OutputType.values()) {
        		if (tp == OutputType.UVMREGS) 
        	    	createUvmRegs(model); // special method for uvm gen
        		else if (tp != OutputType.UVMREGSPKG)
        	    	createOutput(model, tp);  // gen all others
        	}

	    	System.out.println("Ordt complete " + new Date());
	    	System.exit(MsgUtils.getReturnCode());
		} catch (Exception e) {
			//errorMessage("Read of rdl file " + inputFile + " failed");
			e.printStackTrace();
		}
    }

    // ----------------------- output definition methods ----------------------------
    

	/** assign an output type for each command line parm */
    private static void defineOutputArgs() {
		outputArgs.put("-verilog", OutputType.VERILOG);
		outputArgs.put("-systemverilog", OutputType.SYSTEMVERILOG);
		outputArgs.put("-jspec", OutputType.JSPEC);
		outputArgs.put("-ralf", OutputType.RALF);
		outputArgs.put("-rdl", OutputType.RDL);
		outputArgs.put("-reglist", OutputType.REGLIST);
		outputArgs.put("-vbench", OutputType.VBENCH);
		outputArgs.put("-svbench", OutputType.SVBENCH);
		outputArgs.put("-uvmregs", OutputType.UVMREGS);
		outputArgs.put("-uvmregspkg", OutputType.UVMREGSPKG);
		outputArgs.put("-xml", OutputType.XML);
		outputArgs.put("-cppmod", OutputType.CPPMOD);
		outputArgs.put("-cppdrvmod", OutputType.CPPDRVMOD);
		outputArgs.put("-pydrvmod", OutputType.PYDRVMOD);
		outputArgs.put("-json", OutputType.JSON);
		outputArgs.put("-svchildinfo", OutputType.SVCHILDINFO);
	}

	/** assign a name string for each output type */
	private static void defineOutputNames() {
		outputNames.put(OutputType.VERILOG, "verilog");
		outputNames.put(OutputType.SYSTEMVERILOG, "systemverilog");
		outputNames.put(OutputType.JSPEC, "jspec");
		outputNames.put(OutputType.RALF, "ralf");
		outputNames.put(OutputType.RDL, "rdl");
		outputNames.put(OutputType.REGLIST, "register list");
		outputNames.put(OutputType.VBENCH, "verilog testbench");
		outputNames.put(OutputType.SVBENCH, "systemverilog testbench");
		outputNames.put(OutputType.UVMREGS, "UVM regs");
		outputNames.put(OutputType.UVMREGSPKG, "UVM regs package");
		outputNames.put(OutputType.XML, "xml");
		outputNames.put(OutputType.CPPMOD, "C++ model");
		outputNames.put(OutputType.CPPDRVMOD, "C++ driver model");
		outputNames.put(OutputType.PYDRVMOD, "python driver model");
		outputNames.put(OutputType.JSON, "json");
		outputNames.put(OutputType.SVCHILDINFO, "systemverilog child map info");
	}

	/** assign a comment string for each output type */
	private static void defineCommentChars() {
		commentChars.put(OutputType.VERILOG, "//");
		commentChars.put(OutputType.SYSTEMVERILOG, "//");
		commentChars.put(OutputType.JSPEC, "/*");
		commentChars.put(OutputType.RALF, "##");
		commentChars.put(OutputType.RDL, "/*");
		commentChars.put(OutputType.REGLIST, "//");
		commentChars.put(OutputType.VBENCH, "//");
		commentChars.put(OutputType.SVBENCH, "//");
		commentChars.put(OutputType.UVMREGS, "//");
		commentChars.put(OutputType.UVMREGSPKG, "//");
		commentChars.put(OutputType.XML, "<!--");
		commentChars.put(OutputType.CPPMOD, "//");
		commentChars.put(OutputType.CPPDRVMOD, "//");
		commentChars.put(OutputType.PYDRVMOD, "#");
		commentChars.put(OutputType.JSON, null);
		commentChars.put(OutputType.SVCHILDINFO, (ExtParameters.getSysVerChildInfoMode() == SVChildInfoModes.MODULE)? "//" : "#");
	}

    /** return an OutputBuilder of specified type */
	private static OutputBuilder getBuilder(RegModelIntf model, OutputType type) {
		switch (type) {
		   case VERILOG: 
			   if (!verifyRootAddressMap(model, type)) return null;
			   return new VerilogBuilder(model);
		   case SYSTEMVERILOG: 
			   if (!verifyRootAddressMap(model, type)) return null;
			   return new SystemVerilogBuilder(model);
		   case JSPEC: 
			   return new JspecBuilder(model); 
		   case RALF: 
			   //return new RalfBuilder(model);
			   MsgUtils.warnMessage("Ralf output is disabled");
			   return null;
		   case RDL: 
			   return new RdlBuilder(model);
		   case REGLIST: 
			   return new RegListBuilder(model);
		   case VBENCH: 
			   if (!verifyRootAddressMap(model, type)) return null;
			   return new VerilogTestBuilder(model); 
		   case SVBENCH: 
			   if (!verifyRootAddressMap(model, type)) return null;
			   return new SystemVerilogTestBuilder(model);
		   case UVMREGS: 
			   return new UVMRegsBuilder(model, true);
		   case UVMREGSPKG:   
			   return new UVMRegsBuilder(model, true);  // note: uses UVMRegsBuilder but not std write(), so dont use getBuilder()
		   case XML: 
			   return new XmlBuilder(model);
		   case CPPMOD: 
			   return new CppModBuilder(model);
		   case CPPDRVMOD: 
			   return new CppDrvModBuilder(model);
		   case PYDRVMOD: 
			   return new PyDrvModBuilder(model);
		   case JSON: 
			   return new JsonBuilder(model);
		   case SVCHILDINFO: 
			   return new SystemVerilogChildInfoBuilder(model);
           default:
		}
		return null;
	}

	// -------------------------------------------------------------------------------
	
	/** return true if model has a root address map */
    private static boolean verifyRootAddressMap(RegModelIntf model, OutputType type) {
    	   ModComponent rootComp = model.getRootInstancedComponent();
		   if (rootComp != null) {
			   if (!rootComp.isAddressMap()) {
				   String instId = model.getRootInstance().getId();
				   if (hasInputType(InputType.JSPEC))
					   MsgUtils.errorMessage("Root instance " + instId + " is not a register_set.  A single root register_set instance is required for " + outputNames.get(type) + " generation unless the process_typedef parameter is specified.");
				   else  
					   MsgUtils.errorMessage("Root instance " + instId + " is not an addrmap.  A single root addrmap instance is required for " + outputNames.get(type) + " generation unless the process_component parameter is specified.");
			       return false;
			   }
		   }
		return true;
	}
    
	/** extract model from input file and prep as needed */
	private static RegModelIntf extractModel(String inputFile) {
		RegModelIntf newModel;
		
		// extract model from rdl or jspec input file depending on name
    	if (inputFile.endsWith("js")) {
    		setInputType(InputType.JSPEC);
    		newModel = new JSpecModelExtractor(inputFile);
    	}
    	else {
    		setInputType(InputType.RDL);
    		newModel = new RdlModelExtractor(inputFile);
    	}
    	
    	// precompute min size of each register and regset
    	newModel.getRoot().setAlignedSize(ModRegister.defaultWidth);
    	
    	// fix simple address ordering issues 
    	if (ExtParameters.allowUnorderedAddresses()) newModel.getRoot().sortRegisters();
    	
    	// add any debug annotation commands to the active list
    	if (debug.isActive()) debug.addAnnotations(); 
    	
    	// process any model annotate cmds
    	for (AnnotateCommand cmd: ExtParameters.getAnnotations()) {
    		newModel.getRoot().processAnnotation(cmd, 0);
    		MsgUtils.infoMessage("Annotate command: " + cmd.getSignature() + " processed " + cmd.getChangeCount() + " elements");
    	}
    	return newModel;
	}

	/**create output of the specified type if non-null output name is specified
	 */
    public static void createOutput(RegModelIntf model, OutputType type) { 
    	String outFileName = outputFileNames.get(type);
    	if (outFileName == null) return;
    	// create builder and generate structures from model
		System.out.println("Ordt: building " + outputNames.get(type) + "...");
    	OutputBuilder outBuilder = getBuilder(model, type);  
    	if (outBuilder != null) {
        	// process overlay files if builder type supports
        	if (outBuilder.supportsOverlays()) {
        		for (OverlayFileInfo ofile: overlayFiles) {
        			String fname=ofile.getName();
        			System.out.println("Ordt: processing overlay file " + fname + "...");
        			// extract model from overlay file and prep as needed
        	    	model = extractModel(fname);
        	    	outBuilder.processOverlay(model);
        		}
        	}
        	// generate output
        	outBuilder.write(outFileName, outputNames.get(type), commentChars.get(type));
    	}
    }

    /**create uvm registers output
     */
    public static void createUvmRegs(RegModelIntf model) {
    	String outFileName = outputFileNames.get(OutputType.UVMREGS);
    	String outPkgFileName = outputFileNames.get(OutputType.UVMREGSPKG);
    	// override description info w/ model type
		String uvmModeStr = (ExtParameters.uvmregsModelMode() != UVMModelModes.HEAVY)? " (mode = " + ExtParameters.uvmregsModelMode() + ")" : "";
    	String outName = outputNames.get(OutputType.UVMREGS) + uvmModeStr;
    	String outPkgName = outputNames.get(OutputType.UVMREGSPKG) + uvmModeStr;

    	if (outFileName == null) return;

		System.out.println("Ordt: building " + outName + "...");
		UVMRegsBuilder uvm = (ExtParameters.uvmregsModelMode() == UVMModelModes.LITE1)? new UVMRegsLite1Builder(model) :
			(ExtParameters.uvmregsModelMode() == UVMModelModes.NATIVE)? new UVMRegsNativeBuilder(model) :
			new UVMRegsBuilder(model, true);
    	if (uvm != null) {
    		uvm.write(outFileName, outName, "//");
    		
    		// now output the pkg if specified
    		if (outPkgFileName != null) {
    			System.out.println("Ordt: building " + outPkgName + "...");
    			uvm.writePkg(outPkgFileName, outPkgName);  
    		}
    	}
    }
         
    private static void showUsage() {
    	System.out.println("Open Register Design Tool (version " + getVersion() + ") usage: ordt [options] <input_rdl_or_jspec_file>");
    	System.out.println("Options:");
    	System.out.println("   -parms <input_parms_filename>");
    	System.out.println("       <input_parms_filename> will be used to set ordt control parameters. The -parms");
    	System.out.println("       option may be specified multiple times to include multiple parameter files.");
    	System.out.println("   -cppmod <dirname>");
    	System.out.println("       <dirname> will be created containing C++ model output files");
    	System.out.println("   -cppdrvmod <dirname>");
    	System.out.println("       <dirname> will be created containing C++ driver model output files");
    	//System.out.println("   -json <filename>");
    	//System.out.println("       <filename> will be created containing json output");
    	System.out.println("   -jspec <filename>");
    	System.out.println("       <filename> will be created containing jspec output");
    	System.out.println("   -overlay <tag> <input_filename>");
    	System.out.println("       <input_filename> will be processed as an overlay input with specified tag");
    	System.out.println("   -pydrvmod <filename>");
    	System.out.println("       <filename> will be created containing python driver model");
    	System.out.println("   -reglist <filename>");
    	System.out.println("       <filename> will be created containing a text listing of accessible registers");
    	//System.out.println("   -ralf <filename>");
    	//System.out.println("       <filename> will be created containing ralf output");
    	System.out.println("   -rdl <filename>");
    	System.out.println("       <filename> will be created containing rdl output");
    	//System.out.println("   -svbench <filename>");
    	//System.out.println("       <filename> will be created containing a basic systemverilog testbench"); 
    	System.out.println("   -systemverilog <output_name>");
    	System.out.println("       if <output name> ends with '/', <output_name> will be a directory where multiple ");
    	System.out.println("       systemverilog output files will be written.  Otherwise <output_name> will be a");
    	System.out.println("       file containing systemverilog output for all generated modules.");
    	System.out.println("   -svchildinfo <filename>");
    	System.out.println("       <filename> will be created containing child decoder address information");
    	System.out.println("   -uvmregs <filename>");
    	System.out.println("       <filename> will be created containing UVM register classes"); 
    	System.out.println("   -uvmregspkg <filename>");
    	System.out.println("       <filename> will be created containing package of ordt extended UVM classes"); 
    	System.out.println("   -vbench <filename>");
    	System.out.println("       <filename> will be created containing a basic verilog testbench"); 
    	System.out.println("   -verilog <output_name>");
    	System.out.println("       if <output name> ends with '/', <output_name> will be a directory where multiple ");
    	System.out.println("       verilog output files will be written.  Otherwise <output_name> will be a");
    	System.out.println("       file containing verilog output for all generated modules.");
    	System.out.println("   -xml <filename>");
    	System.out.println("       <filename> will be created containing xml output");
    	System.exit(0);
    }
    
    // ------------------------ common static methods ---------------------------
    
	/** return ordt version */
	public static String getVersion() {
		return version;
	}

	/** return the extracted model */
	public static RegModelIntf getModel() {
		return model;
	}

	/** return true if input is of specified type */
	public static boolean hasInputType(InputType type) {
		return inputType == type;
	}

	private static void setInputType(InputType inputType) {
		Ordt.inputType = inputType;
	}

	/** return true if an input parms file has been specified */
	public static boolean hasInputParmFile() {
		return !inputParmFiles.isEmpty();
	}

    // ------------------------ static methods for DebugController overrides ---------------------------
	
	/** override the input file name (used by DebugController) */
	public static void setInputFile(String inFile) {
		inputFile = inFile;
	}

	/** add an output of specified name/type (used by DebugController) */
	public static void addOutputFile(OutputType type, String name) {
		outputFileNames.put(type, name);  		
	}

	/** add an input parameter file (used by DebugController) */
	public static void addInputParmFile(String name) {
		inputParmFiles.add(name);  		
	}

	/** add an input overlay file (used by DebugController) */
	public static void addOverlayFile(String olayName, String olayTag) {   // TODO - add checks on overlay file type, etc
		overlayFiles.add(new OverlayFileInfo(olayName, olayTag));
	}

    /** return the list of overlay file tags */
	public static List<String> getOverlayFileTags() {
		List<String> retlist = new ArrayList<String>();
		for (OverlayFileInfo finfo: overlayFiles) retlist.add(finfo.getTag());
		return retlist;
	}

}
