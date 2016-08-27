/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved.
 */
package ordt.extract;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import ordt.annotate.AnnotateCommand;
import ordt.output.OutputBuilder;
import ordt.output.cppmod.CppModBuilder;
import ordt.output.othertypes.JsonBuilder;
import ordt.output.othertypes.JspecBuilder;
import ordt.output.othertypes.RdlBuilder;
import ordt.output.othertypes.RegListBuilder;
import ordt.output.othertypes.XmlBuilder;
import ordt.output.systemverilog.SystemVerilogBuilder;
import ordt.output.systemverilog.SystemVerilogTestBuilder;
import ordt.output.uvmregs.UVMRegsBuilder;
import ordt.output.verilog.VerilogBuilder;
import ordt.output.verilog.VerilogTestBuilder;
import ordt.parameters.ExtParameters;

public class Ordt {

	private static String version = "160826.01"; 
	private static DebugController debug = new MyDebugController(); // override design annotations, input/output files

	public enum InputType { RDL, JSPEC };
	private static InputType inputType;
	private static List<String> inputParmFiles = new ArrayList<String>();
    private static String inputFile = null;

	public enum OutputType { VERILOG, SYSTEMVERILOG, JSPEC, RALF, RDL, REGLIST, SVBENCH, VBENCH, UVMREGS, UVMREGSPKG, XML, CPPMOD, JSON };
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
	    	
			// extract model from rdl or jspec input file depending on name
	    	if (inputFile.endsWith("js")) {
	    		setInputType(InputType.JSPEC);
	    		model = new JSpecModelExtractor(inputFile);
	    	}
	    	else {
	    		setInputType(InputType.RDL);
	    		model = new RdlModelExtractor(inputFile);
	    	}
	    	
	    	// precompute min size of each regset
	    	model.getRoot().setAlignedSize();
	    	
	    	// fix simple address ordering issues 
	    	if (ExtParameters.allowUnorderedAddresses()) model.getRoot().sortRegisters();
	    	
	    	// add any debug annotations
        	if (debug.isActive()) debug.addAnnotations(); 
	    	
	    	// process any model annotate cmds
	    	for (AnnotateCommand cmd: ExtParameters.getAnnotations()) {
	    		model.getRoot().processAnnotation(cmd, 0);
	    		Ordt.infoMessage("Annotate command: " + cmd.getSignature() + " processed " + cmd.getChangeCount() + " elements");
	    	}
	    	
        	// define output names/comment chars by type
        	defineOutputNames();
        	defineCommentChars();
        	
        	// generate output of all types specified on command line
        	for (OutputType tp : OutputType.values()) {
        		if (tp == OutputType.UVMREGS) 
        	    	createUvmRegs(model); // special method for uvm gen
        		else if (tp != OutputType.UVMREGSPKG)
        	    	createOutput(model, tp);  // gen all others
        	}

	    	System.out.println("Ordt complete " + new Date());
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
		outputArgs.put("-json", OutputType.JSON);
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
		outputNames.put(OutputType.JSON, "json");
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
		commentChars.put(OutputType.JSON, null);
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
			   Ordt.warnMessage("Ralf output is disabled");
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
			   return new UVMRegsBuilder(model);
		   case UVMREGSPKG:   
			   return new UVMRegsBuilder(model);  // note: uses UVMRegsBuilder but not std write(), so dont use getBuilder()
		   case XML: 
			   return new XmlBuilder(model);
		   case CPPMOD: 
			   return new CppModBuilder(model);
			   //Ordt.warnMessage("C++ model output is disabled");
			   //return null;
		   case JSON: 
			   return new JsonBuilder(model);
           default:
		}
		return null;
	}
	
	/** return true if model has a root address map */
    private static boolean verifyRootAddressMap(RegModelIntf model, OutputType type) {
		   if (model.getRootInstancedComponent() != null) {
			   if (!model.getRootInstancedComponent().isAddressMap()) {
				   Ordt.errorMessage("A root addrmap is required for " + outputNames.get(type) + " generation");
			       return false;
			   }
		   }
		return true;
	}

	// -------------------------------------------------------------------------------

	/**create output of the specified type if non-null output name is specified
	 */
    public static void createOutput(RegModelIntf model, OutputType type) { 
    	String outName = outputFileNames.get(type);
    	if (outName == null) return;
		System.out.println("Ordt: building " + outputNames.get(type) + "...");
    	OutputBuilder outp = getBuilder(model, type);
    	if (outp != null) outp.write(outName, outputNames.get(type), commentChars.get(type));
    }

    /**create uvm registers output
     */
    public static void createUvmRegs(RegModelIntf model) {
    	String outName = outputFileNames.get(OutputType.UVMREGS);
    	String outPkgName = outputFileNames.get(OutputType.UVMREGSPKG);
    	if (outName == null) return;

		System.out.println("Ordt: building UVM regs...");
		UVMRegsBuilder uvm = new UVMRegsBuilder(model);
    	if (uvm != null) {
    		uvm.write(outName, "UVM regs", "//");
    		
    		// now output the pkg if specified
    		if (outPkgName != null) {
    			System.out.println("Ordt: building UVM regs package...");
    			uvm.writePkg(outPkgName, "UVM regs package");  
    		}
    	}
    }
         
    private static void showUsage() {
    	System.out.println("Open Register Design Tool (version " + getVersion() + ") usage: ordt [options] <input_rdl_or_jspec_file>");
    	System.out.println("Options:");
    	System.out.println("   -parms <input_parms_filename>");
    	System.out.println("       <input_parms_filename> will be used to set ordt control parameters. The -parms");
    	System.out.println("       option may be specified multiple times to include multiple parameter files.");
    	//System.out.println("   -verilog <filename>");
    	//System.out.println("       <filename> will be created containing verilog output");
    	System.out.println("   -systemverilog <output_name>");
    	System.out.println("       if <output name> ends with '/', <output_name> will be a directory where multiple ");
    	System.out.println("       systemverilog output files will be written.  Otherwise <output_name> will be a");
    	System.out.println("       file containing systemverilog output for all generated modules.");
    	//System.out.println("   -json <filename>");
    	//System.out.println("       <filename> will be created containing json output");
    	System.out.println("   -jspec <filename>");
    	System.out.println("       <filename> will be created containing jspec output");
    	System.out.println("   -reglist <filename>");
    	System.out.println("       <filename> will be created containing a text listing of accessible registers");
    	//System.out.println("   -ralf <filename>");
    	//System.out.println("       <filename> will be created containing ralf output");
    	System.out.println("   -rdl <filename>");
    	System.out.println("       <filename> will be created containing rdl output");
    	//System.out.println("   -vbench <filename>");
    	//System.out.println("       <filename> will be created containing a basic verilog testbench"); 
    	//System.out.println("   -svbench <filename>");
    	//System.out.println("       <filename> will be created containing a basic systemverilog testbench"); 
    	System.out.println("   -uvmregs <filename>");
    	System.out.println("       <filename> will be created containing UVM register classes"); 
    	System.out.println("   -uvmregspkg <filename>");
    	System.out.println("       <filename> will be created containing package of ordt extended UVM classes"); 
    	System.out.println("   -xml <filename>");
    	System.out.println("       <filename> will be created containing xml output");
    	//System.out.println("   -cppmod <dirname>");
    	//System.out.println("       <dirname> will be created containing C++ output files");
    	System.exit(0);
    }
    
    // ------------------------ common static methods ---------------------------
    
	/** display error message */
	public static void infoMessage(String msg) {
		System.out.println("*** INFO ***: " + msg);		
	}

	/** display error message */
	public static void warnMessage(String msg) {
		System.err.println("*** WARNING ***: " + msg);		
	}

	/** display error message */
	public static void errorMessage(String msg) {
		System.err.println("*** ERROR ***: " + msg);		
	}

	/** display error message and exit */
	public static void errorExit(String msg) {
		errorMessage(msg);	
    	System.out.println("Ordt exited due to error " + new Date());
		System.exit(8);
	}

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

	/** override the input file name (used by DebugController) */
	public static void setInputFile(String inFile) {
		inputFile = inFile;
	}

	/** add an output of specified name/type (used by DebugController) */
	public static void addOutputFile(OutputType type, String name) {
		outputFileNames.put(type, name);  		
	}

	/** add in input parameter file (used by DebugController) */
	public static void addInputParmFile(String name) {
		inputParmFiles.add(name);  		
	}

	/** return true if an input parms file has been specified */
	public static boolean hasInputParmFile() {
		return !inputParmFiles.isEmpty();
	}

}
