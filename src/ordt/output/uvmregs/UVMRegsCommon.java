package ordt.output.uvmregs;

import ordt.output.systemverilog.common.SystemVerilogFunction;

/** common methods used in various uvmreg builder output types */
public class UVMRegsCommon {
	
	/** add return statement to a sv function that extracts a bit slice from specified uvm_data var
	 * 
	 * @param func - SystemVerilog function to be modified
	 * @param dataName - uvm data variable name
	 * @param loIdxName - low index variable name
	 * @param widthName - width name
	 */
	protected static void addGetSliceStatements(SystemVerilogFunction func, String dataName, String loIdxName, String widthName) {
		func.addStatement("return (" + dataName + " & (((1<<" + widthName + ")-1) << " + loIdxName + ")) >> " + loIdxName + ";");		
	}
	
	/** add return statements to a sv function that set a bit slice in specified uvm_data var
	 * 
	 * @param func - SystemVerilog function to be modified
	 * @param dataName - name of uvm data that will be modified
	 * @param sliceName - name of slice uvm data that will set the slice value
	 * @param loIdxName - low index variable name
	 * @param widthName - width name
	 */
	protected static void addSetSliceStatements(SystemVerilogFunction func, String dataName, String sliceName, String loIdxName, String widthName) {
		func.addStatement(dataName + " |= (((1<<" + widthName + ")-1) << " + loIdxName + ");");
		func.addStatement(dataName + " ^= (((1<<" + widthName + ")-1) << " + loIdxName + ");");
		func.addStatement(dataName + " |= (" + sliceName + " << " + loIdxName + ");");
	}

}
