package ordt.output.systemverilog.common.wrap;

import java.util.HashMap;
import java.util.List;

/** remap transform base class (simple assign) */
public class WrapperRemapXform {
	public enum WrapperRemapType { PASSTHRU, INVERT, SYNC_STAGES }
	
	WrapperRemapType type = WrapperRemapType.PASSTHRU;
	
	public WrapperRemapType getType() {
		return type;
	}
	
	/** return sv xform string for this src/dest signal name pair */
	public String getXformString(String srcName, String dstName, int width, HashMap<String, String> optionalParms) {
		return dstName + " = " + srcName + ";";  // default is an assign
	}

	/** Return List of String statements defining this xform module */
	public List<String> getXformModuleDef() {
		return null;
	}

	/** return module name for this xform */
	public String getModuleName() {
		return null;
	}

	public boolean hasBeenWritten() {
		return true;  // default xform has no module, so mark as written
	}
}
