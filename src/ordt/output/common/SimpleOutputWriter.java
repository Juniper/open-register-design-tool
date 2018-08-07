package ordt.output.common;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class SimpleOutputWriter implements OutputWriterIntf {
	protected BufferedWriter bw;
	
	public SimpleOutputWriter(String fileName, String description) {
    	bw = openBufferedWriter(fileName, description);
	}

	/** return BufferedWriter */
	public BufferedWriter getBufferedWriter() {
		return bw;
	}
	
	// -------- OutputWriterIntf methods

	/** return true if open was successful */
	@Override
	public boolean isOpen() {
		return (bw != null);
	}

	/** close this writer */
	@Override
	public void close() {
		closeBufferedWriter(bw);
	}

	@Override
	public String getWriterName() {
		return null;
	}

	@Override
	public void writeStmt(int indentLevel, String stmt) {
		   //System.out.println("OutputBuilder: bufnull=" + (bufferedWriter == null) + ", indent=" + ",Stmt=" + stmt);
		   try {
			bw.write(MsgUtils.repeat(' ', indentLevel*2) + stmt +"\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void writeStmts(int indentLevel, List<String> outputLines) {
		Iterator<String> iter = outputLines.iterator();
		while (iter.hasNext()) writeStmt(indentLevel, iter.next());	
	}	
	
	/** validate output file and create buffered writer
     */
    public static BufferedWriter openBufferedWriter(String outName, String description) {
    	File outFile = null;
    	try {	  			
    		outFile = new File(outName); 

    		System.out.println(MsgUtils.getProgName() + ": writing " + description + " file " + outFile + "...");

    		// if file doesnt exists, then create it
    		if (!outFile.exists()) {
    			outFile.createNewFile();
    		}

    		FileWriter fw = new FileWriter(outFile.getAbsoluteFile());
    		BufferedWriter bw = new BufferedWriter(fw);
    		return bw;

    	} catch (IOException e) {
    		MsgUtils.errorMessage("Create of " + description + " file " + outFile.getAbsoluteFile() + " failed.");
    		return null;
    	}
    }
    
    /** validate output file and create buffered writer
     */
    public static void closeBufferedWriter(BufferedWriter bw) {
    	try {	  
    		bw.close();

    	} catch (IOException e) {
    		MsgUtils.errorMessage("File close failed.");
    	}
    }

}
