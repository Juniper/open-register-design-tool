package ordt.output.systemverilog.common;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import ordt.output.common.MsgUtils;
import ordt.parse.simplesv.SimpleSVBaseListener;
import ordt.parse.simplesv.SimpleSVLexer;
import ordt.parse.simplesv.SimpleSVParser;
import ordt.parse.simplesv.SimpleSVParser.Module_identifierContext;

public class SystemVerilogModuleReader extends SimpleSVBaseListener {

	public SystemVerilogModuleReader(String svInputFile) {
		readModuleHeaders(svInputFile);
	}
	
	/**
	 * read parameters from specified file  
	 */
	public void readModuleHeaders(String svInputFile) {		
    	System.out.println("Ordt: extracting module header info from " + svInputFile + "...");
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
		System.out.println("SystemVerilogModuleReader enterModuleIdentifier: " + ctx.getText());
	}
	
	/**
	 */
	@Override 
	public void exitModule_identifier(Module_identifierContext ctx) { 
		//System.out.println("SystemVerilogModuleReader exitModuleIdentifier: " + ctx.getText());
	}

	// -------
	
	public static void main(String[] args) {
		SystemVerilogModuleReader rdr = new SystemVerilogModuleReader("/Users/snellenbach/Documents/jrdl_work/output.v");
	}

}
