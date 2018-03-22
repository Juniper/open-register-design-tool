package ordt.output.systemverilog.common;

public class SystemVerilogTask extends SystemVerilogFunction {

	public SystemVerilogTask(String name, String parentClass) {
		super(null, name, parentClass);
	}

	public SystemVerilogTask(String name) {
		super(null, name);
	}

	/** generate header type string for this method (task) */
	@Override
	protected String genHeaderType(boolean isExtern, boolean includeParent) {
		   String retStr = isExtern? "extern " : "";
		   if (isVirtual && !includeParent) retStr += "virtual ";
		   if (isStatic && !includeParent) retStr += "static ";
		   retStr += "task";
		return retStr;
	}
	  
	/** generate signature string for this method (task) */
	@Override
	public String genSignature(boolean isExtern, boolean includeParent) {
		boolean showIODir = true;
		String suffix = " " + genHeaderName(includeParent) + genIODefs(showIODir) + ";";
		return genHeaderType(isExtern, includeParent) + suffix;
	}

	/** generate closing string for this method (task) */
	@Override
	protected String genEnd() {
		return "endtask: " + name;
	}

}
