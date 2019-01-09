package ordt.output.systemverilog.common;

/** abstract class describing a systemverilog signal's bit range */
public abstract class SystemVerilogRange {

	/** return true if this range has been initialized with valid values */
	abstract public boolean isValid();

	/** get low index for this io signal range */
	abstract public int getLowIndex();

	/** get high index for this io signal range */
	abstract public int getHighIndex();

	/** get left index for this io signal range */
	abstract public int getLeftIndex();

	/** get right index for this io signal range */
	abstract public int getRightIndex();

	/** get size in bits of this io signal range */
	abstract public int getSize();
	
	/** return true if this io signal range is greater than 1 */
	abstract public boolean isVector();
	
	/** return the array string used for definitions (includes) prefixed array string */
	abstract public String getDefArray();

}
