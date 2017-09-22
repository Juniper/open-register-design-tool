package ordt.output.systemverilog.common.wrap;

import java.util.HashMap;

public class WrapperRemapSyncDelayXform extends WrapperRemapXform {

	protected int delayStages = 1;
	static int instanceCount = 1;
	
	public WrapperRemapSyncDelayXform(int delayStages) {
		super();
		this.type = WrapperRemapType.SYNC_DELAY;
		this.delayStages = delayStages;
	}
	
	/* return sv xform string for this src/dest signal name pair */
	public String getXformString(String srcName, String dstName, HashMap<String, String> optionalParms) {
		return "ordt_wrap_sync_delay " + "sync_delay_" + instanceCount++ + " #(" + delayStages + ") (" + srcName + ", " + dstName + ");";  // instance he sync delay module
	}

}
