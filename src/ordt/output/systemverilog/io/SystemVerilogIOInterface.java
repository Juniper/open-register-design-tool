package ordt.output.systemverilog.io;

public class SystemVerilogIOInterface extends SystemVerilogIOSignalSet {

	public SystemVerilogIOInterface(Integer from, Integer to, String namePrefix, String name, int reps, String extType) { 
		super(namePrefix, name, reps);
		this.from = from;
		this.to = to;
		this.type = extType;
	}

	/** returns true if this element is virtual (ie not an actually group in systemverilog output).
	 *  This method is overridden in child classes */
    @Override
	protected boolean isVirtual() { return false; }

}
