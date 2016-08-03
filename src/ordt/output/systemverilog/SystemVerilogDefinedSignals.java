package ordt.output.systemverilog;

import java.util.HashMap;

import ordt.output.systemverilog.SystemVerilogDefinedSignal.DefSignalAssoc;

/** class of static members to manage generated system verilog signal names */
public class SystemVerilogDefinedSignals {
	
	public enum DefSignalType {
		FIELD, FIELD_NEXT, 
		L2H_DATA, L2H_SWACC, L2H_SWMOD, L2H_OVERFLOW, L2H_UNDERFLOW, L2H_INCRSAT, L2H_DECRSAT, L2H_INCRTHOLD, L2H_DECRTHOLD, L2H_ANDED, L2H_ORED, L2H_XORED, 
		H2L_WEL, H2L_WE, H2L_HWSET, H2L_HWCLR, H2L_SWWEL, H2L_SWWE, H2L_INTR, H2L_DATA, H2L_INCR, H2L_DECR, H2L_INCRVALUE, H2L_DECRVALUE, 
		PREVINTR, CNTR_NEXT,
		D2L_DATA, L2D_DATA, D2L_WE, D2L_RE, 
		D2H_ADDR, D2H_DATA, D2H_SIZE, D2H_WE, D2H_RE, 
		H2D_RETSIZE, H2D_DATA, H2D_ACK, H2D_NACK, 
		L2H_INTR, L2H_HALT
	};

	private static HashMap<DefSignalType, SystemVerilogDefinedSignal> sigSet = initDefinedSignals();  // set of defined signals by type
	private static HashMap<String, DefSignalType> rhsSigSet = initRhsSignals();  // set of signal types allowed in rhs assign by deref

	// ---------
	
	/** initialize the list of defined signals */
	private static HashMap<DefSignalType, SystemVerilogDefinedSignal> initDefinedSignals() {
		HashMap<DefSignalType, SystemVerilogDefinedSignal> newList = new HashMap<DefSignalType, SystemVerilogDefinedSignal>();
		
		// add field signals
/*

	public static String getFieldRegisterName(String fieldPath, boolean addPrefix) {
	*/
		newList.put(DefSignalType.FIELD, new SystemVerilogDefinedSignal(DefSignalAssoc.FIELD, "rg", null));
	/*
	 * 
	public static String getFieldRegisterNextName(String fieldPath, boolean addPrefix) {
	*/
		newList.put(DefSignalType.FIELD_NEXT, new SystemVerilogDefinedSignal(DefSignalAssoc.FIELD, "reg", "next"));
    /*
     * 
	public static String getLogicToHwDataName(String fieldPath, boolean addPrefix) {
	*/
		newList.put(DefSignalType.L2H_DATA, new SystemVerilogDefinedSignal(DefSignalAssoc.FIELD, "l2h", "r"));
    /*
	
	public static String getLogicToHwSwAccName(String fieldPath, boolean addPrefix) {
	*/
		newList.put(DefSignalType.L2H_SWACC, new SystemVerilogDefinedSignal(DefSignalAssoc.FIELD, "l2h", "swacc_o"));
    /*

	public static String getLogicToHwSwModName(String fieldPath, boolean addPrefix) {
	*/
		newList.put(DefSignalType.L2H_SWMOD, new SystemVerilogDefinedSignal(DefSignalAssoc.FIELD, "l2h", "swmod_o"));
    /*

	public static String getLogicToHwOverflowName(String fieldPath, boolean addPrefix) {  // overflow output
	*/
		newList.put(DefSignalType.L2H_OVERFLOW, new SystemVerilogDefinedSignal(DefSignalAssoc.FIELD, "l2h", "overflow_o"));
    /*

	public static String getLogicToHwUnderflowName(String fieldPath, boolean addPrefix) {  // underflow output
	*/
		newList.put(DefSignalType.L2H_UNDERFLOW, new SystemVerilogDefinedSignal(DefSignalAssoc.FIELD, "l2h", "underflow_o"));
    /*

	public static String getLogicToHwIncrSatName(String fieldPath, boolean addPrefix) {  // incr saturate output
	*/
		newList.put(DefSignalType.L2H_INCRSAT, new SystemVerilogDefinedSignal(DefSignalAssoc.FIELD, "l2h", "incrsat_o"));
    /*

	public static String getLogicToHwDecrSatName(String fieldPath, boolean addPrefix) {  // decr saturate output
	*/
		newList.put(DefSignalType.L2H_DECRSAT, new SystemVerilogDefinedSignal(DefSignalAssoc.FIELD, "l2h", "decrsat_o"));
    /*
	
	public static String getLogicToHwIncrTholdName(String fieldPath, boolean addPrefix) {  // incr threshold output
	*/
		newList.put(DefSignalType.L2H_INCRTHOLD, new SystemVerilogDefinedSignal(DefSignalAssoc.FIELD, "l2h", "incrthold_o"));
    /*

	public static String getLogicToHwDecrTholdName(String fieldPath, boolean addPrefix) {  // decr threshold output
	*/
		newList.put(DefSignalType.L2H_DECRTHOLD, new SystemVerilogDefinedSignal(DefSignalAssoc.FIELD, "l2h", "decrthold_o"));
    /*

	public static String getLogicToHwAndedName(String fieldPath, boolean addPrefix) {  // anded output
	*/
		newList.put(DefSignalType.L2H_ANDED, new SystemVerilogDefinedSignal(DefSignalAssoc.FIELD, "l2h", "anded_o"));
    /*

	public static String getLogicToHwOredName(String fieldPath, boolean addPrefix) {  // ored output
	*/
		newList.put(DefSignalType.L2H_ORED, new SystemVerilogDefinedSignal(DefSignalAssoc.FIELD, "l2h", "ored_o"));
    /*

	public static String getLogicToHwXoredName(String fieldPath, boolean addPrefix) {  // xored output
	*/
		newList.put(DefSignalType.L2H_XORED, new SystemVerilogDefinedSignal(DefSignalAssoc.FIELD, "l2h", "xored_o"));
    /*



	public static String getHwToLogicWelName(String fieldPath, boolean addPrefix) {
	*/
		newList.put(DefSignalType.H2L_WEL, new SystemVerilogDefinedSignal(DefSignalAssoc.FIELD, "h2l", "wel"));
    /*

	public static String getHwToLogicWeName(String fieldPath, boolean addPrefix) {
	*/
		newList.put(DefSignalType.H2L_WE, new SystemVerilogDefinedSignal(DefSignalAssoc.FIELD, "h2l", "we"));
    /*

	public static String getHwToLogicHwSetName(String fieldPath, boolean addPrefix) {
	*/
		newList.put(DefSignalType.H2L_HWSET, new SystemVerilogDefinedSignal(DefSignalAssoc.FIELD, "h2l", "hwset"));
    /*

	public static String getHwToLogicHwClrName(String fieldPath, boolean addPrefix) {
	*/
		newList.put(DefSignalType.H2L_HWCLR, new SystemVerilogDefinedSignal(DefSignalAssoc.FIELD, "h2l", "hwclr"));
    /*

	public static String getHwToLogicSwWelName(String fieldPath, boolean addPrefix) {
	*/
		newList.put(DefSignalType.H2L_SWWEL, new SystemVerilogDefinedSignal(DefSignalAssoc.FIELD, "h2l", "swwel"));
    /*

	public static String getHwToLogicSwWeName(String fieldPath, boolean addPrefix) {
	*/
		newList.put(DefSignalType.H2L_SWWE, new SystemVerilogDefinedSignal(DefSignalAssoc.FIELD, "h2l", "swwe"));
    /*

	public static String getHwToLogicIntrName(String fieldPath, boolean addPrefix) {
	*/
		newList.put(DefSignalType.H2L_INTR, new SystemVerilogDefinedSignal(DefSignalAssoc.FIELD, "h2l", "intr"));
    /*

	public static String getHwToLogicDataName(String fieldPath, boolean addPrefix) {
	*/
		newList.put(DefSignalType.H2L_DATA, new SystemVerilogDefinedSignal(DefSignalAssoc.FIELD, "h2l", "w"));
    /*

	// counter signals
	public static String getHwToLogicIncrName(String fieldPath, boolean addPrefix) {  // increment input
	*/
		newList.put(DefSignalType.H2L_INCR, new SystemVerilogDefinedSignal(DefSignalAssoc.FIELD, "h2l", "incr"));
    /*

	public static String getHwToLogicDecrName(String fieldPath, boolean addPrefix) {  // decrement input
	*/
		newList.put(DefSignalType.H2L_DECR, new SystemVerilogDefinedSignal(DefSignalAssoc.FIELD, "h2l", "decr"));
    /*
	
	public static String getHwToLogicIncrValueName(String fieldPath, boolean addPrefix) {  // increment value input 
	*/
		newList.put(DefSignalType.H2L_INCRVALUE, new SystemVerilogDefinedSignal(DefSignalAssoc.FIELD, "h2l", "incrvalue"));
    /*

	public static String getHwToLogicDecrValueName(String fieldPath, boolean addPrefix) {  // decrement value input 
	*/
		newList.put(DefSignalType.H2L_DECRVALUE, new SystemVerilogDefinedSignal(DefSignalAssoc.FIELD, "h2l", "decrvalue"));
    /*
	
	
	public static String getPrevIntrName(String fieldPath, boolean addPrefix) {
	*/
		newList.put(DefSignalType.PREVINTR, new SystemVerilogDefinedSignal(DefSignalAssoc.FIELD, "intr", "previntr"));
    /*
	
	public static String getNextCountName(String fieldPath, boolean addPrefix) {  // next value of counter 
	*/
		newList.put(DefSignalType.CNTR_NEXT, new SystemVerilogDefinedSignal(DefSignalAssoc.FIELD, "cntr", "next"));
    /*
		
		// add reg signals
/*

	/** return the logic module input name for register *
	public String getDecodeToLogicName() {
	*/
		newList.put(DefSignalType.D2L_DATA, new SystemVerilogDefinedSignal(DefSignalAssoc.REG, "d2l", "w"));
    /*

	/** return the logic module input name for register *
	public String getLogicToDecodeName() {
	*/
		newList.put(DefSignalType.L2D_DATA, new SystemVerilogDefinedSignal(DefSignalAssoc.REG, "l2d", "r"));
    /*

	/** return the logic module write enable name for register *
	public String getDecodeToLogicWeName() {
	*/
		newList.put(DefSignalType.D2L_WE, new SystemVerilogDefinedSignal(DefSignalAssoc.REG, "d2l", "we"));
    /*

	/** return the logic module read enable name for register *
	public String getDecodeToLogicReName() {
	*/
		newList.put(DefSignalType.D2L_RE, new SystemVerilogDefinedSignal(DefSignalAssoc.REG, "d2l", "re"));
    /*
	
	/** return the external reg write data name *
	public String getDecodeToHwAddrName() {
	*/
		newList.put(DefSignalType.D2H_ADDR, new SystemVerilogDefinedSignal(DefSignalAssoc.REG, "d2h", "addr"));
    /*
	
	/** return the external reg write data name *
	public String getDecodeToHwName() {
	*/
		newList.put(DefSignalType.D2H_DATA, new SystemVerilogDefinedSignal(DefSignalAssoc.REG, "d2h", "w"));
    /*
	
	/** return the external reg transaction size name *
	public String getDecodeToHwTransSizeName() {
	*/
		newList.put(DefSignalType.D2H_SIZE, new SystemVerilogDefinedSignal(DefSignalAssoc.REG, "d2h", "size"));
    /*

	/** return the external reg we name *
	public String getDecodeToHwWeName() {
	*/
		newList.put(DefSignalType.D2H_WE, new SystemVerilogDefinedSignal(DefSignalAssoc.REG, "d2h", "we"));
    /*
	
	/** return the external reg re name *
	public String getDecodeToHwReName() {
	*/
		newList.put(DefSignalType.D2H_RE, new SystemVerilogDefinedSignal(DefSignalAssoc.REG, "d2h", "re"));
    /*
	
	/** return the external reg return transaction size name *
	public String getHwToDecodeTransSizeName() {
	*/
		newList.put(DefSignalType.H2D_RETSIZE, new SystemVerilogDefinedSignal(DefSignalAssoc.REG, "h2d", "retsize"));
    /*

	/** return the external reg read data name *
	public String getHwToDecodeName() {
	*/
		newList.put(DefSignalType.H2D_DATA, new SystemVerilogDefinedSignal(DefSignalAssoc.REG, "h2d", "r"));
    /*

	/** return the external reg ack name *
	public String getHwToDecodeAckName() {
	*/
		newList.put(DefSignalType.H2D_ACK, new SystemVerilogDefinedSignal(DefSignalAssoc.REG, "h2d", "ack"));
    /*

	/** return the external reg nack name *
	public String getHwToDecodeNackName() {
	*/
		newList.put(DefSignalType.H2D_NACK, new SystemVerilogDefinedSignal(DefSignalAssoc.REG, "h2d", "nack"));
    /*

	/** return the external reg interrupt output name 
	public static String getLogicToHwIntrName(String regPath, boolean addPrefix) {
	*/
		newList.put(DefSignalType.L2H_INTR, new SystemVerilogDefinedSignal(DefSignalAssoc.REG, "l2h", "intr_o"));
    /*

	/** return the external reg halt output name *
	public static String getLogicToHwHaltName(String regPath, boolean addPrefix) {
	*/
		newList.put(DefSignalType.L2H_HALT, new SystemVerilogDefinedSignal(DefSignalAssoc.REG, "l2h", "halt_o"));
    /*
		
 */
		return newList;
	}
	
	/** initialize the set of defined signal types by deref string 
	 * if a key has null signal type, a complex assign is indicated so default signame can't be used */
	private static HashMap<String, DefSignalType> initRhsSignals() {
		HashMap<String, DefSignalType> newList = new HashMap<String, DefSignalType>();
		newList.put(null, DefSignalType.FIELD);  
		newList.put("next", DefSignalType.FIELD_NEXT);  
		newList.put("nextposedge", null);  // special case
		newList.put("nextposedge", null);  // special case
		newList.put("saturate", DefSignalType.L2H_INCRSAT);
		newList.put("incrsaturate", DefSignalType.L2H_INCRSAT);
		newList.put("threshold", DefSignalType.L2H_INCRTHOLD);
		newList.put("incrthreshold", DefSignalType.L2H_INCRTHOLD);
		newList.put("decrsaturate", DefSignalType.L2H_DECRSAT);
		newList.put("decrthreshold", DefSignalType.L2H_DECRTHOLD);
		newList.put("underflow", DefSignalType.L2H_UNDERFLOW);
		newList.put("overflow", DefSignalType.L2H_OVERFLOW);
		newList.put("intr", DefSignalType.L2H_INTR);
		newList.put("halt", DefSignalType.L2H_HALT);
		newList.put("anded", DefSignalType.L2H_ANDED);
		newList.put("ored", DefSignalType.L2H_ORED);
		newList.put("xored", DefSignalType.L2H_XORED);
		newList.put("swacc", DefSignalType.L2H_SWACC);
		newList.put("swmod", DefSignalType.L2H_SWMOD);

		return newList;
	}
	
	
	/** return the prefix for specified defined signal type */
	public static String getPrefix(DefSignalType sigType) {
		if (!sigSet.containsKey(sigType)) return null;
		return sigSet.get(sigType).getPrefix();
	}
	
	/** return the suffix for specified defined signal type */
	public static String getSuffix(DefSignalType sigType) {
		if (!sigSet.containsKey(sigType)) return null;
		return sigSet.get(sigType).getSuffix();
	}
	
	/** return the name for specified defined signal type 
	 *  This is a catenation of prefix (if addPrefix is true), input pathStr, and suffix */
	public static String getFullName(DefSignalType sigType, String pathStr, boolean addPrefix) {
		if (!sigSet.containsKey(sigType)) return null;
		return sigSet.get(sigType).getFullName(pathStr, addPrefix);
	}

	/** return true is specified signal deref corresponds to a valid rhs signal */
	public boolean isValidRhsDeRef(String deRef) {
		return rhsSigSet.containsKey(deRef);
	}

	/** return the verilog expression for the deRef/path used in an rhs assign */
	public String getResolvedRhsSignalExpression(String deRef, String instancePath, boolean addPrefix) {
		// if deRef is invalid, return null
		if (!isValidRhsDeRef(deRef)) return null;
		// if a special assign is required
		if (rhsSigSet.get(deRef) == null) {
			// special next edge properties
			if (deRef.equals("nextposedge"))
				return "(" + getFullName(DefSignalType.FIELD_NEXT, instancePath, addPrefix) + 
						" & ~" + getFullName(DefSignalType.FIELD, instancePath, addPrefix) + ")"; 
			if (deRef.equals("nextnegedge"))
				return "(" + getFullName(DefSignalType.FIELD, instancePath, addPrefix) + 
						" & ~" + getFullName(DefSignalType.FIELD_NEXT, instancePath, addPrefix) + ")"; 
		}
		// otherwise just use signal for this type
		return getFullName(rhsSigSet.get(deRef), instancePath, addPrefix);
	}
	
    public static void main(String[] args) throws Exception {
       //System.out.println("prefix =" + SystemVerilogDefinedSignals.getPrefix(DefSignalType.FIELD_NEXT));
       //for (DefSignalType st : sigSet.keySet()) 
       //    System.out.println(st + ": " + SystemVerilogDefinedSignals.getFullName(st, "foo.bar", true));
       for (String st : rhsSigSet.keySet()) 
           System.out.println(st + ": " + SystemVerilogDefinedSignals.getFullName(rhsSigSet.get(st), "foo.bar", true));
    }

}
