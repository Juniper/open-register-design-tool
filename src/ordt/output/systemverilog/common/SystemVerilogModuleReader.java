package ordt.output.systemverilog.common;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import ordt.output.common.MsgUtils;
import ordt.output.systemverilog.common.SystemVerilogModule.SystemVerilogParameter;
import ordt.output.systemverilog.common.io.SystemVerilogIOSignalList;
import ordt.output.systemverilog.common.parse.simplesv.SimpleSVBaseListener;
import ordt.output.systemverilog.common.parse.simplesv.SimpleSVLexer;
import ordt.output.systemverilog.common.parse.simplesv.SimpleSVParser;
import ordt.output.systemverilog.common.parse.simplesv.SimpleSVParser.Module_identifierContext;

public class SystemVerilogModuleReader extends SimpleSVBaseListener {
	protected Set<String> portListSet = new HashSet<String>();  // set of ports in a legacy format header list
	private String moduleName;
	private boolean moduleFound = false;
	private SystemVerilogModule readModule = null;
	private int insideLocs = SystemVerilogLocationMap.getInternalId();  // default internal locs
	private int outsideLocs = SystemVerilogLocationMap.getExternalId();  // default external locs

	/** extract a SystemVerilogModule from an input file
	 * 
	 * @param svInputFile - input file name
	 * @param moduleName - module name to be extracted from file
	 * @param insideLocs - optional location encoding to be used for module internals
	 * @param outsideLocs - optional location encoding to be used for module externals
	 */
	public SystemVerilogModuleReader(String svInputFile, String moduleName, boolean headersOnly, Integer insideLocs, Integer outsideLocs) {
		this.moduleName = moduleName;
    	if (insideLocs != null) this.insideLocs = insideLocs;
    	if (outsideLocs != null) this.outsideLocs = outsideLocs;
		readModule(svInputFile, headersOnly);
    	if (readModule == null) MsgUtils.errorExit("Unable to read module " + moduleName + " in file " + svInputFile + ".");
	}
	
	/** extract a SystemVerilogModule from an input file using default locations
	 * 
	 * @param svInputFile - input file name
	 * @param moduleName - module name to be extracted from file
	 * @param headersOnly - if true, only module header will be extracted
	 */
	public SystemVerilogModuleReader(String svInputFile, String moduleName, boolean headersOnly) {
		this(svInputFile, moduleName, headersOnly, null, null);
	}
	
	/**
	 * read parameters from specified file  
	 */
	public void readModule(String svInputFile, boolean headersOnly) {		
    	System.out.println("Ordt: extracting " + (headersOnly? "header " : "") + "info for module " + moduleName + " from " + svInputFile + "...");
    	if (!headersOnly) MsgUtils.errorMessage("Only header read of systemverilog module is supported currently");  // FIXME
        try {
        	
        	InputStream is = System.in;
        	if ( svInputFile!=null ) is = new FileInputStream(svInputFile);
        
        	ANTLRInputStream input = new ANTLRInputStream(is);
        	SimpleSVLexer lexer = new SimpleSVLexer(input);

        	CommonTokenStream tokens = new CommonTokenStream(lexer);

        	SimpleSVParser parser; 
        	parser = new SimpleSVParser(tokens);

        	ParseTree tree = parser.root(); 
        	//System.out.println(tree.toStringTree());
        	ParseTreeWalker walker = new ParseTreeWalker(); // create standard
        	walker.walk(this, tree); // initiate walk of tree with listener
        	if (parser.getNumberOfSyntaxErrors() > 0) {
        		MsgUtils.errorExit("File parser errors detected.");  
        	}
        	
        	//root.display(true);

        } catch (FileNotFoundException e) {
        	MsgUtils.errorExit("file not found. "  + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }		
	}

	// ------- parser override methods
	
	/**
	 */
	@Override 
	public void enterModule_identifier(Module_identifierContext ctx) { 
		String parsedModuleName = ctx.getText();
		if (parsedModuleName.equals(moduleName)) {
			moduleFound = true;
			readModule = new SystemVerilogModule(null, parsedModuleName, insideLocs, "clk", "reset", false); // create a new module - use generic names
			readModule.useDefaultIOList();
			SystemVerilogModule.showDefaultIOListWarnings(false);
		}
		//System.out.println("SystemVerilogModuleReader enterModuleIdentifier: " + ctx.getText() + (moduleFound? " *" : ""));
	}
	
	/**
	 */
	@Override 
	public void enterEnd_module(SimpleSVParser.End_moduleContext ctx) { 
		//System.out.println("SystemVerilogModuleReader enterEnd_module: " + ctx.getText() + (moduleFound? " *, ports=" + portsFound + ", port defs=" + portDefsFound : ""));
		moduleFound = false;
	}
	
	/**
     parameter_def
       : parameter_identifier (array)? (EQ NUM)?
	 */
	@Override 
	public void enterParameter_def(SimpleSVParser.Parameter_defContext ctx)	{	
		if (!moduleFound) return;
		//System.out.println("SystemVerilogModuleReader enterParameter_def:    " + ctx.getText());
		String parmName = ctx.getChild(0).getText();
		boolean hasArrayDef = ctx.getText().contains("[");
		boolean hasDefault = ctx.getText().contains("=");
		String defaultValue = hasDefault? (hasArrayDef? ctx.getChild(3).getText() : ctx.getChild(2).getText()) : null;
		readModule.addParameter(parmName, defaultValue);
	}
	
	/** simple list of ports w/o type
	 */
	@Override 
	public void enterPort(SimpleSVParser.PortContext ctx) { 
		if (!moduleFound) return;
		//System.out.println("SystemVerilogModuleReader enterPort:    " + ctx.getText());
		// add an empty SystemVerilogSignal to the port list, info must be loaded in port define (non-ansi header)
		String portName = ctx.getText();
		if (portName.contains("[")) {
			MsgUtils.errorExit("Unsupported port name (" + portName + ") found in parse of module " + moduleName);
			return;
		}
		portListSet.add(portName);
	}
	
	/**
	 */
	@Override 
	public void exitPort(SimpleSVParser.PortContext ctx) { }	
	
	/**
	 port_def
      : ID (array)? ID
	 */
	@Override 
	public void enterPort_def(SimpleSVParser.Port_defContext ctx) { 
		if (!moduleFound) return;
		//System.out.println("SystemVerilogModuleReader enterPort_def:      " + ctx.getText());
		// extract range of this port and store in the portList
		String portType = ctx.getChild(0).getText();
		boolean hasRange = ctx.getChildCount() > 2;
		String portName = hasRange? ctx.getChild(2).getText() : ctx.getChild(1).getText();
        // extract range
		String sliceStr = "0:0";  // default is a scalar range
		if (hasRange) {
			Pattern pat = Pattern.compile("^\\[(\\S+\\:\\S+)\\]$");  // extract slice string to handle parameterized widths
			Matcher m = pat.matcher(ctx.getChild(1).getText());
			if (m.matches()) 
				sliceStr = m.group(1);  // extract range
			else
				MsgUtils.errorExit("SystemVerilogModuleReader is unable to extract slice of form=" + ctx.getChild(1).getText());
		}
		// store  // TODO - handle interfaces
		if ("input".equals(portType)) { 
			//System.out.println("SystemVerilogModuleReader enterPort_def: found input " + portName + ", idx=" + lowIdx + ", width=" + width + ", outsideLocs=" + outsideLocs);
			//readModule.addSimpleVectorFrom(outsideLocs, portName, lowIdx, width);
			readModule.addSimpleVectorFrom(outsideLocs, portName, sliceStr);
		}
		else if ("output".equals(portType)) {
			//System.out.println("SystemVerilogModuleReader enterPort_def: found output " + portName + ", idx=" + lowIdx + ", width=" + width);
			//readModule.addSimpleVectorTo(outsideLocs, portName, lowIdx, width);
			readModule.addSimpleVectorTo(outsideLocs, portName, sliceStr);
		}
		else if ((portListSet.isEmpty()) || ((portListSet.contains(portName) && !"wire reg logic".contains(portType))) ) {   // check for unsupported port types 
			//System.out.println("SystemVerilogModuleReader enterPort_def: portList empty=" + portListSet.isEmpty() + ", portListSet.contains(portName)=" + portListSet.contains(portName) + ", portType=" + portType);
			MsgUtils.errorExit("Port definition (" + portType + " " + portName + ") is not currently supported in parse of module " + moduleName);
		}
	}
	
	/**
	 */
	@Override 
	public void exitPort_def(SimpleSVParser.Port_defContext ctx) { 
	}

	// -------

	/** return the read module */
	public SystemVerilogModule getModule() {
		return readModule;
	}

	// -------
	
	public static void main(String[] args) {
		SystemVerilogModuleReader rdr = new SystemVerilogModuleReader("/Users/snellenbach/Documents/jrdl_work/output2.v", "simple1_jrdl_logic", true, 7, 8);
		//SystemVerilogModuleReader rdr = new SystemVerilogModuleReader("/Users/snellenbach/Documents/jrdl_work/output2.v", "foo_jrdl_logic");  // bad module
		SystemVerilogModule readMod = rdr.getModule();
		System.out.println("------- ports:");
		SystemVerilogIOSignalList newList = readMod.getFullIOSignalList();
		newList.display();
		System.out.println("------- parameters:");
		for (SystemVerilogParameter parm: readMod.getParameterList()) System.out.println(parm.toString());
	}

}