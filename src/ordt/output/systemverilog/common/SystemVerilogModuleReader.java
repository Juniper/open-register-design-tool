package ordt.output.systemverilog.common;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import ordt.output.common.MsgUtils;
import ordt.output.systemverilog.common.io.SystemVerilogIOSignal;
import ordt.output.systemverilog.common.io.SystemVerilogIOSignalList;
import ordt.output.systemverilog.common.parse.simplesv.SimpleSVBaseListener;
import ordt.output.systemverilog.common.parse.simplesv.SimpleSVLexer;
import ordt.output.systemverilog.common.parse.simplesv.SimpleSVParser;
import ordt.output.systemverilog.common.parse.simplesv.SimpleSVParser.Module_identifierContext;

public class SystemVerilogModuleReader extends SimpleSVBaseListener {
	protected Map<String, SystemVerilogIOSignal> portList = new LinkedHashMap<String, SystemVerilogIOSignal>();
	private String moduleName;
	private boolean moduleFound = false;
	@SuppressWarnings("unused")
	private int portsFound = 0;
	@SuppressWarnings("unused")
	private int portDefsFound = 0;

	public SystemVerilogModuleReader(String svInputFile, String moduleName) {
		this.moduleName = moduleName;
		readModuleHeaders(svInputFile);
	}
	
	/**
	 * read parameters from specified file  
	 */
	public void readModuleHeaders(String svInputFile) {		
    	System.out.println("Ordt: extracting header info for module " + moduleName + " from " + svInputFile + "...");
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
        	MsgUtils.errorExit("parameter file not found. "  + e.getMessage());
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
		if (parsedModuleName.equals(moduleName)) moduleFound = true;
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
	 */
	@Override 
	public void enterPort(SimpleSVParser.PortContext ctx) { 
		if (!moduleFound) return;
		//System.out.println("SystemVerilogModuleReader enterPort:          " + ctx.getText());
		// add an empty SystemVerilogSignal to the port list, info must be loaded in port define (non-ansi header)
		String portName = ctx.getText();
		if (portName.contains("[")) {
			MsgUtils.errorExit("Unsupported port name (" + portName + ") found in parse of module " + moduleName);
			return;
		}
		portsFound ++;
		portList.put(portName, null);
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
		int lowIdx = 0;
		int width = 1;
		if (hasRange) {
			Pattern pat = Pattern.compile("^\\[(\\d+)\\:(\\d+)\\]$");  // TODO hanle parameterized widths
			Matcher m = pat.matcher(ctx.getChild(1).getText());
			if (m.matches()) {
				int highIdx = Integer.valueOf(m.group(1));  // extract id root
				lowIdx = Integer.valueOf(m.group(2));  // extract low index
				width = highIdx - lowIdx + 1;
			}
		}
		// store  // TODO - handle interfaces
		if ("input".equals(portType)) { 
			portDefsFound ++;
			portList.put(portName, new SystemVerilogIOSignal(SystemVerilogLocationMap.getExternalId(), SystemVerilogLocationMap.getInternalId(), null, portName, lowIdx, width));
		}
		else if ("output".equals(portType)) {
			portDefsFound ++;
			portList.put(portName, new SystemVerilogIOSignal(SystemVerilogLocationMap.getInternalId(), SystemVerilogLocationMap.getExternalId(), null, portName, lowIdx, width));
		}
	}
	
	/**
	 */
	@Override 
	public void exitPort_def(SimpleSVParser.Port_defContext ctx) { 
	}

	// -------

	/** return a SystemVerilogIOSignalList for the parsed module header */  // TODO - move to SystemVerilogIOSignalList as constructor
	private SystemVerilogIOSignalList getIOSignalList(String listName) {
		SystemVerilogIOSignalList newList = new SystemVerilogIOSignalList(listName);
		for (String name : portList.keySet()) {
			SystemVerilogIOSignal sig = portList.get(name);
			if (sig == null)
				MsgUtils.errorExit("Definition of port " + name + " not able to be resolved in parse of module " + moduleName);
            newList.addSimpleVector(sig.getFrom(), sig.getTo(), sig.getName(), sig.getLowIndex(), sig.getSize());
		}
		return newList;
	}

	// -------
	
	public static void main(String[] args) {
		SystemVerilogModuleReader rdr = new SystemVerilogModuleReader("/Users/snellenbach/Documents/jrdl_work/output1.v", "alt_sipfo0_jrdl_logic");
		SystemVerilogIOSignalList newList = rdr.getIOSignalList("mylist");
		newList.display();
	}

}
