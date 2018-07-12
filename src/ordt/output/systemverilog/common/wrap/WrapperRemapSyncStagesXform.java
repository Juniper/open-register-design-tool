package ordt.output.systemverilog.common.wrap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class WrapperRemapSyncStagesXform extends WrapperRemapXform {

	protected int delayStages = 1;
	protected String clkName;
	protected String moduleOverride;
	static int instanceCount = 0;
	static String moduleName = "ordt_wrap_sync_stages";
    private static boolean hasBeenWritten = false;
	
	public WrapperRemapSyncStagesXform(int delayStages, String clkName, String moduleOverride) {
		super();
		this.type = WrapperRemapType.SYNC_STAGES;
		this.delayStages = delayStages;
		this.clkName = clkName;
		this.moduleOverride = moduleOverride;
	}

	public String getClkName() {
		return clkName;
	}
	
	/* return sv xform string for this src/dest signal name pair */
	@Override
	public String getXformString(String srcName, String dstName, int width, HashMap<String, String> optionalParms) { // optionalParms: defaultClkName
		String instancedClk = (clkName!=null)? clkName : optionalParms.get("defaultClkName");
		String instancedModule = (moduleOverride!=null)? moduleOverride : moduleName;
		return instancedModule + " #(.STAGES(" + delayStages + "), .WIDTH(" + width + ")) sync_delay_" + instanceCount++ + 
				 " (" + instancedClk + ", " + srcName + ", " + dstName + ");";  // instance sync delay module
	}
	
	/** Return List of String statements defining this xform module */
	@Override
	public List<String> getXformModuleDef() {
		hasBeenWritten = true;  // mark transform as written once its module define is retrieved
		List<String> outList = new ArrayList<String>();
		outList.add("//---------- module " + moduleName);
		outList.add("module " + moduleName);
		outList.add("( clk, in_sig, out_sig );");
		outList.add("  parameter STAGES = 1;");
		outList.add("  parameter WIDTH = 1;");
		outList.add("  input    clk;");
		outList.add("  input     [WIDTH-1:0] in_sig;");
		outList.add("  output    [WIDTH-1:0] out_sig;");
		outList.add("  reg   [WIDTH-1:0] dly [STAGES:0];");
		outList.add("  integer idx;");
		outList.add("  always @ (*)");
		outList.add("    dly[0] = in_sig;");
		outList.add("  always @ (posedge clk) begin");
		outList.add("    for (idx = 1; idx <= STAGES; idx++)");
		outList.add("        dly[idx] <= #1 dly[idx-1];");
		outList.add("  end");
		outList.add("  assign out_sig = dly[STAGES];");
		outList.add("endmodule");
		return outList;  
	}

	@Override
	public boolean hasBeenWritten() {
		return hasBeenWritten;
	}

	/** return module name for this xform */
	@Override
	public String getModuleName() {
		return moduleName;
	}

}
