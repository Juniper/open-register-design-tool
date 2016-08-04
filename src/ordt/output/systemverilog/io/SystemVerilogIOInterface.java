package ordt.output.systemverilog.io;

public class SystemVerilogIOInterface extends SystemVerilogIOSignalSet {

	public SystemVerilogIOInterface(Integer from, Integer to, String name, int reps, String extType) { // TODO - need to set prefix
		// TODO Auto-generated constructor stub
		super(from, to, name, reps);
	}

	/** returns true if this element is virtual (ie not an actually group in systemverilog output).
	 *  This method is overridden in child classes */
    @Override
	protected boolean isVirtual() { return false; }

}
