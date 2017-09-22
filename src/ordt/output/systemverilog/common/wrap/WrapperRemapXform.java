package ordt.output.systemverilog.common.wrap;

import java.util.HashMap;

/** remap transform base class (simple assign) */
public class WrapperRemapXform {
	public enum WrapperRemapType { ASSIGN, SYNC_DELAY, ASYNC_LEVEL, ASYNC_DATA }

	WrapperRemapType type = WrapperRemapType.ASSIGN;
	
	public WrapperRemapType getType() {
		return type;
	}
	
	/* return sv xform string for this src/dest signal name pair */
	public String getXformString(String srcName, String dstName, HashMap<String, String> optionalParms) {
		return dstName + " = " + srcName + ";";  // default is an assign
	}
}
