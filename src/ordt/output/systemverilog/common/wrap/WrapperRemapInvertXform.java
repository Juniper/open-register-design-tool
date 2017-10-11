package ordt.output.systemverilog.common.wrap;

import java.util.HashMap;

public class WrapperRemapInvertXform extends WrapperRemapXform {

	public WrapperRemapInvertXform() {
		this.type = WrapperRemapType.INVERT;
	}
	
	/** return sv xform string for this src/dest signal name pair */
	@Override
	public String getXformString(String srcName, String dstName, int width, HashMap<String, String> optionalParms) {
		return dstName + " = ~" + srcName + ";";  // bitwise invert
	}

}
