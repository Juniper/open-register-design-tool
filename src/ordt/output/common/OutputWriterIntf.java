package ordt.output.common;

import java.util.List;

public interface OutputWriterIntf {

	void writeStmt(int indentLevel, String string);

	void writeStmts(int indentLevel, List<String> stringList);

	/** return the name of this writer */
	String getWriterName();

	/** return true if open was successful */
	boolean isOpen();

	/** close this writer */
	void close();
}
