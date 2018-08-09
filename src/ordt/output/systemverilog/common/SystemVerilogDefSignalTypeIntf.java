package ordt.output.systemverilog.common;

/** interface used for selection of signal info from the SystemVerilogDefinedSignals map.
 *  typically this would be implemented on an application-specific enum containing signal types */
public interface SystemVerilogDefSignalTypeIntf {

	/** return true if this signal type represents a systemverilog interface */
	public boolean isInterface();

	/** return true if this signal type represents a systemverilog struct */
	public boolean isStruct();
}
