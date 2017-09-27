package ordt.output;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import ordt.parameters.Utils;

public class SimpleOutputWriter implements OutputWriterIntf {
	protected BufferedWriter bw;
	
	public SimpleOutputWriter(String fileName, String description) {
    	bw = OutputBuilder.openBufferedWriter(fileName, description);
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
		OutputBuilder.closeBufferedWriter(bw);
	}

	@Override
	public String getWriterName() {
		return null;
	}

	@Override
	public void writeStmt(int indentLevel, String stmt) {
		   //System.out.println("OutputBuilder: bufnull=" + (bufferedWriter == null) + ", indent=" + ",Stmt=" + stmt);
		   try {
			bw.write(Utils.repeat(' ', indentLevel*2) + stmt +"\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void writeStmts(int indentLevel, List<String> outputLines) {
		Iterator<String> iter = outputLines.iterator();
		while (iter.hasNext()) writeStmt(indentLevel, iter.next());	
	}	
}
