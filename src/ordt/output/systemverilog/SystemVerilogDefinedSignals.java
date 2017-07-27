package ordt.output.systemverilog;

import java.util.HashMap;

import ordt.output.systemverilog.SystemVerilogDefinedSignal.DefSignalAssoc;

/** class of static members to manage generated systemverilog signal names - these have hierarchy dependent names */
public class SystemVerilogDefinedSignals {
	
	public enum DefSignalType {
		FIELD, FIELD_NEXT, 
		L2H_DATA, L2H_SWACC, L2H_SWMOD, L2H_OVERFLOW, L2H_UNDERFLOW, L2H_INCRSAT, L2H_DECRSAT, L2H_INCRTHOLD, L2H_DECRTHOLD, L2H_ANDED, L2H_ORED, L2H_XORED, 
		H2L_WEL, H2L_WE, H2L_HWSET, H2L_HWCLR, H2L_SWWEL, H2L_SWWE, H2L_INTR, H2L_DATA, H2L_INCR, H2L_DECR, H2L_INCRVALUE, H2L_DECRVALUE, 
		PREVINTR, CNTR_NEXT, INTR_CLEAR, INTR_DLY,
		D2L_DATA, L2D_DATA, D2L_WE, D2L_RE, D2L_ENABLE,
		D2H_ADDR, D2H_DATA, D2H_SIZE, D2H_WE, D2H_RE, D2H_ENABLE,
		H2D_RETSIZE, H2D_DATA, H2D_ACK, H2D_NACK, 
		L2H_INTR, L2H_HALT, 
		USR_SIGNAL,
		LH_INTERFACE, DH_INTERFACE, LH_STRUCT, DH_STRUCT, SIGSET;
		
		public boolean isInterface() {
			return ((this == LH_INTERFACE) || (this == DH_INTERFACE));
		}
		public boolean isStruct() {
			return ((this == LH_STRUCT) || (this == DH_STRUCT));
		}
	};
	
	// define io locations
	public static final Integer ANY = null;
	public static final Integer NONE = 0;
	public static final Integer HW = 1;
	public static final Integer LOGIC = 2;
	public static final Integer DECODE = 4;
	public static final Integer PIO = 8;

	private static HashMap<DefSignalType, SystemVerilogDefinedSignal> sigSet = initDefinedSignals();  // set of defined signals by type
	private static HashMap<String, DefSignalType> rhsSigSet = initRhsSignals();  // set of signal types allowed in rhs assign by deref
	
	// ---------
	
	/** initialize the list of defined signals */
	private static HashMap<DefSignalType, SystemVerilogDefinedSignal> initDefinedSignals() {
		HashMap<DefSignalType, SystemVerilogDefinedSignal> newList = new HashMap<DefSignalType, SystemVerilogDefinedSignal>();
		
		newList.put(DefSignalType.FIELD, new SystemVerilogDefinedSignal(DefSignalAssoc.FIELD, NONE, NONE, "rg_", null));
		newList.put(DefSignalType.FIELD_NEXT, new SystemVerilogDefinedSignal(DefSignalAssoc.FIELD, NONE, NONE, "reg_", "next"));
		
		newList.put(DefSignalType.L2H_DATA, new SystemVerilogDefinedSignal(DefSignalAssoc.FIELD, LOGIC, HW, "l2h_", "r"));
		newList.put(DefSignalType.L2H_SWACC, new SystemVerilogDefinedSignal(DefSignalAssoc.FIELD, LOGIC, HW, "l2h_", "swacc_o"));
		newList.put(DefSignalType.L2H_SWMOD, new SystemVerilogDefinedSignal(DefSignalAssoc.FIELD, LOGIC, HW, "l2h_", "swmod_o"));
		newList.put(DefSignalType.L2H_OVERFLOW, new SystemVerilogDefinedSignal(DefSignalAssoc.FIELD, LOGIC, HW, "l2h_", "overflow_o"));
		newList.put(DefSignalType.L2H_UNDERFLOW, new SystemVerilogDefinedSignal(DefSignalAssoc.FIELD, LOGIC, HW, "l2h_", "underflow_o"));
		newList.put(DefSignalType.L2H_INCRSAT, new SystemVerilogDefinedSignal(DefSignalAssoc.FIELD, LOGIC, HW, "l2h_", "incrsat_o"));
		newList.put(DefSignalType.L2H_DECRSAT, new SystemVerilogDefinedSignal(DefSignalAssoc.FIELD, LOGIC, HW, "l2h_", "decrsat_o"));
		newList.put(DefSignalType.L2H_INCRTHOLD, new SystemVerilogDefinedSignal(DefSignalAssoc.FIELD, LOGIC, HW, "l2h_", "incrthold_o"));
		newList.put(DefSignalType.L2H_DECRTHOLD, new SystemVerilogDefinedSignal(DefSignalAssoc.FIELD, LOGIC, HW, "l2h_", "decrthold_o"));
		newList.put(DefSignalType.L2H_ANDED, new SystemVerilogDefinedSignal(DefSignalAssoc.FIELD, LOGIC, HW, "l2h_", "anded_o"));
		newList.put(DefSignalType.L2H_ORED, new SystemVerilogDefinedSignal(DefSignalAssoc.FIELD, LOGIC, HW, "l2h_", "ored_o"));
		newList.put(DefSignalType.L2H_XORED, new SystemVerilogDefinedSignal(DefSignalAssoc.FIELD, LOGIC, HW, "l2h_", "xored_o"));
		
		newList.put(DefSignalType.H2L_WEL, new SystemVerilogDefinedSignal(DefSignalAssoc.FIELD, HW, LOGIC, "h2l_", "wel"));
		newList.put(DefSignalType.H2L_WE, new SystemVerilogDefinedSignal(DefSignalAssoc.FIELD, HW, LOGIC, "h2l_", "we"));
		newList.put(DefSignalType.H2L_HWSET, new SystemVerilogDefinedSignal(DefSignalAssoc.FIELD, HW, LOGIC, "h2l_", "hwset"));
		newList.put(DefSignalType.H2L_HWCLR, new SystemVerilogDefinedSignal(DefSignalAssoc.FIELD, HW, LOGIC, "h2l_", "hwclr"));
		newList.put(DefSignalType.H2L_SWWEL, new SystemVerilogDefinedSignal(DefSignalAssoc.FIELD, HW, LOGIC, "h2l_", "swwel"));
		newList.put(DefSignalType.H2L_SWWE, new SystemVerilogDefinedSignal(DefSignalAssoc.FIELD, HW, LOGIC, "h2l_", "swwe"));
		newList.put(DefSignalType.H2L_INTR, new SystemVerilogDefinedSignal(DefSignalAssoc.FIELD, HW, LOGIC, "h2l_", "intr"));
		newList.put(DefSignalType.H2L_DATA, new SystemVerilogDefinedSignal(DefSignalAssoc.FIELD, HW, LOGIC, "h2l_", "w"));
		newList.put(DefSignalType.H2L_INCR, new SystemVerilogDefinedSignal(DefSignalAssoc.FIELD, HW, LOGIC, "h2l_", "incr"));
		newList.put(DefSignalType.H2L_DECR, new SystemVerilogDefinedSignal(DefSignalAssoc.FIELD, HW, LOGIC, "h2l_", "decr"));
		newList.put(DefSignalType.H2L_INCRVALUE, new SystemVerilogDefinedSignal(DefSignalAssoc.FIELD, HW, LOGIC, "h2l_", "incrvalue"));
		newList.put(DefSignalType.H2L_DECRVALUE, new SystemVerilogDefinedSignal(DefSignalAssoc.FIELD, HW, LOGIC, "h2l_", "decrvalue"));
		
		newList.put(DefSignalType.PREVINTR, new SystemVerilogDefinedSignal(DefSignalAssoc.FIELD, NONE, NONE, "intr_", "previntr"));
		newList.put(DefSignalType.CNTR_NEXT, new SystemVerilogDefinedSignal(DefSignalAssoc.FIELD, NONE, NONE, "cntr_", "next"));
		newList.put(DefSignalType.INTR_DLY, new SystemVerilogDefinedSignal(DefSignalAssoc.FIELD, NONE, NONE, "intr_", "delay"));
		
		newList.put(DefSignalType.D2L_DATA, new SystemVerilogDefinedSignal(DefSignalAssoc.REG, DECODE, LOGIC, "d2l_", "w"));
		newList.put(DefSignalType.D2L_ENABLE, new SystemVerilogDefinedSignal(DefSignalAssoc.REG, DECODE, LOGIC, "d2l_", "w_enable"));
		newList.put(DefSignalType.L2D_DATA, new SystemVerilogDefinedSignal(DefSignalAssoc.REG, LOGIC, DECODE, "l2d_", "r"));
		newList.put(DefSignalType.D2L_WE, new SystemVerilogDefinedSignal(DefSignalAssoc.REG, DECODE, LOGIC, "d2l_", "we"));
		newList.put(DefSignalType.D2L_RE, new SystemVerilogDefinedSignal(DefSignalAssoc.REG, DECODE, LOGIC, "d2l_", "re"));
		
		newList.put(DefSignalType.D2H_ADDR, new SystemVerilogDefinedSignal(DefSignalAssoc.REG, DECODE, HW, "d2h_", "addr"));
		newList.put(DefSignalType.D2H_DATA, new SystemVerilogDefinedSignal(DefSignalAssoc.REG, DECODE, HW, "d2h_", "w"));
		newList.put(DefSignalType.D2H_ENABLE, new SystemVerilogDefinedSignal(DefSignalAssoc.REG, DECODE, HW, "d2h_", "w_enable"));
		newList.put(DefSignalType.D2H_SIZE, new SystemVerilogDefinedSignal(DefSignalAssoc.REG, DECODE, HW, "d2h_", "size"));
		newList.put(DefSignalType.D2H_WE, new SystemVerilogDefinedSignal(DefSignalAssoc.REG, DECODE, HW, "d2h_", "we"));
		newList.put(DefSignalType.D2H_RE, new SystemVerilogDefinedSignal(DefSignalAssoc.REG, DECODE, HW, "d2h_", "re"));
		
		newList.put(DefSignalType.H2D_RETSIZE, new SystemVerilogDefinedSignal(DefSignalAssoc.REG, HW, DECODE, "h2d_", "retsize"));
		newList.put(DefSignalType.H2D_DATA, new SystemVerilogDefinedSignal(DefSignalAssoc.REG, HW, DECODE, "h2d_", "r"));
		newList.put(DefSignalType.H2D_ACK, new SystemVerilogDefinedSignal(DefSignalAssoc.REG, HW, DECODE, "h2d_", "ack"));
		newList.put(DefSignalType.H2D_NACK, new SystemVerilogDefinedSignal(DefSignalAssoc.REG, HW, DECODE, "h2d_", "nack"));
		
		newList.put(DefSignalType.L2H_INTR, new SystemVerilogDefinedSignal(DefSignalAssoc.REG, LOGIC, HW, "l2h_", "intr_o"));
		newList.put(DefSignalType.L2H_HALT, new SystemVerilogDefinedSignal(DefSignalAssoc.REG, LOGIC, HW, "l2h_", "halt_o"));
		newList.put(DefSignalType.INTR_CLEAR, new SystemVerilogDefinedSignal(DefSignalAssoc.REG, NONE, NONE, "intr_", "clear"));
		
		newList.put(DefSignalType.USR_SIGNAL, new SystemVerilogDefinedSignal(DefSignalAssoc.SIGNAL, NONE, NONE, "sig_", null));  // default to no from/to
		newList.put(DefSignalType.SIGSET, new SystemVerilogDefinedSignal(DefSignalAssoc.ANY, NONE, NONE, null, null));
		newList.put(DefSignalType.LH_INTERFACE, new SystemVerilogDefinedSignal(DefSignalAssoc.ANY, LOGIC, HW, "lh_", null)); 
		newList.put(DefSignalType.DH_INTERFACE, new SystemVerilogDefinedSignal(DefSignalAssoc.ANY, DECODE, HW, "dh_", null)); 
		newList.put(DefSignalType.LH_STRUCT, new SystemVerilogDefinedSignal(DefSignalAssoc.ANY, LOGIC, HW, "lh_", null)); 
		newList.put(DefSignalType.DH_STRUCT, new SystemVerilogDefinedSignal(DefSignalAssoc.ANY, DECODE, HW, "dh_", null)); 
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
	
	/** return the from location for specified defined signal type */
	public static Integer getFrom(DefSignalType sigType) {
		if (!sigSet.containsKey(sigType)) return null;
		return sigSet.get(sigType).getFrom();
	}
	
	/** return the to location for specified defined signal type */
	public static Integer getTo(DefSignalType sigType) {
		if (!sigSet.containsKey(sigType)) return null;
		return sigSet.get(sigType).getTo();
	}
	
	/** return the name for specified defined signal type 
	 *  This is a catenation of prefix (if addPrefix is true), input pathStr, and suffix */
	public static String getFullName(DefSignalType sigType, String pathStr, boolean addPrefix) {
		if (!sigSet.containsKey(sigType)) return null;
		return sigSet.get(sigType).getFullName(pathStr, addPrefix);
	}

	/** return true is specified signal deref corresponds to a valid rhs signal */
	public static boolean isValidRhsDeRef(String deRef) {
		return rhsSigSet.containsKey(deRef);
	}

	/** return the verilog expression for the deRef/path used in an rhs assign */
	public static String getResolvedRhsSignalExpression(String deRef, String instancePath, boolean addPrefix) {
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
