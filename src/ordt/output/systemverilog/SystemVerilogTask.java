package ordt.output.systemverilog;

public class SystemVerilogTask extends SystemVerilogFunction {

	public SystemVerilogTask(String name) {
		super(null, name);
	}

	/** generate signature string for this method (task) */
	@Override
	protected String genSignature() {
		boolean showIODir = true;
		String retStr = isVirtual? "virtual task" : "task";
		String suffix = " " + name + genIODefs(showIODir) + ";";
		retStr = retStr + suffix; 
		return retStr;
	}

	/** generate closing string for this method (task) */
	@Override
	protected String genEnd() {
		return "endtask: " + name;
	}

}
