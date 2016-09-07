package ordt.output;

import java.util.List;

public interface OutputWriterIntf {

	void writeStmt(int indentLevel, String string);

	void writeStmts(int indentLevel, List<String> stringList);

	/** return the name of this writer */
	String getWriterName();

}
