package ordt.output.systemverilog.common;

public class SystemVerilogTask extends SystemVerilogFunction {

	public SystemVerilogTask(String name) {
		super(null, name);
	}

	/** generate header string for this method (task) */
	protected String genHeader() {
		String retStr = isVirtual? "virtual task" : "task";
		return retStr;
	}
	  
	/** generate signature string for this method (task) */
	@Override
	protected String genSignature() {
		boolean showIODir = true;
		String suffix = " " + name + genIODefs(showIODir) + ";";
		return genHeader() + suffix;
	}

	/** generate closing string for this method (task) */
	@Override
	protected String genEnd() {
		return "endtask: " + name;
	}

}
