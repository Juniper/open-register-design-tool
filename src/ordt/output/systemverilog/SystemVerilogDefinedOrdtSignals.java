package ordt.output.systemverilog;

import java.util.HashMap;

import ordt.output.systemverilog.common.SystemVerilogDefSignalTypeIntf;
import ordt.output.systemverilog.common.SystemVerilogDefinedSignal;
import ordt.output.systemverilog.common.SystemVerilogDefinedSignalMap;
import ordt.output.systemverilog.common.SystemVerilogLocationMap;

/** class of static members to manage generated systemverilog signal names - these have hierarchy dependent names */
public class SystemVerilogDefinedOrdtSignals extends SystemVerilogDefinedSignalMap {
	
	public enum DefSignalType implements SystemVerilogDefSignalTypeIntf {
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
		
		@Override
		public boolean isInterface() {
			return ((this == LH_INTERFACE) || (this == DH_INTERFACE));
		}
		
		@Override
		public boolean isStruct() {
			return ((this == LH_STRUCT) || (this == DH_STRUCT));
		}
	};
	
	// define io locations
	public static final Integer NONE = SystemVerilogLocationMap.getId("NONE");
	public static final Integer HW = SystemVerilogLocationMap.add("HW");
	public static final Integer LOGIC = SystemVerilogLocationMap.add("LOGIC");
	public static final Integer DECODE = SystemVerilogLocationMap.add("DECODE");
	public static final Integer PIO = SystemVerilogLocationMap.add("PIO");
	public static final Integer WRAPPER = SystemVerilogLocationMap.add("WRAPPER");

	private static HashMap<String, DefSignalType> rhsSigSet = initRhsSignals();  // set of signal types allowed in rhs assign by deref
		
	/** load the defined signal map */
	public static void initDefinedSignalMap() {
        clearMap();
		
		addDefinedSignal(DefSignalType.FIELD, new SystemVerilogDefinedSignal(LOGIC, LOGIC, "rg_", null, "comp_type", "FIELD"));
		addDefinedSignal(DefSignalType.FIELD_NEXT, new SystemVerilogDefinedSignal(LOGIC, LOGIC, "reg_", "next", "comp_type", "FIELD"));
		
		addDefinedSignal(DefSignalType.L2H_DATA, new SystemVerilogDefinedSignal(LOGIC, HW, "l2h_", "r", "comp_type", "FIELD"));
		addDefinedSignal(DefSignalType.L2H_SWACC, new SystemVerilogDefinedSignal(LOGIC, HW, "l2h_", "swacc_o", "comp_type", "FIELD"));
		addDefinedSignal(DefSignalType.L2H_SWMOD, new SystemVerilogDefinedSignal(LOGIC, HW, "l2h_", "swmod_o", "comp_type", "FIELD"));
		addDefinedSignal(DefSignalType.L2H_OVERFLOW, new SystemVerilogDefinedSignal(LOGIC, HW, "l2h_", "overflow_o", "comp_type", "FIELD"));
		addDefinedSignal(DefSignalType.L2H_UNDERFLOW, new SystemVerilogDefinedSignal(LOGIC, HW, "l2h_", "underflow_o", "comp_type", "FIELD"));
		addDefinedSignal(DefSignalType.L2H_INCRSAT, new SystemVerilogDefinedSignal(LOGIC, HW, "l2h_", "incrsat_o", "comp_type", "FIELD"));
		addDefinedSignal(DefSignalType.L2H_DECRSAT, new SystemVerilogDefinedSignal(LOGIC, HW, "l2h_", "decrsat_o", "comp_type", "FIELD"));
		addDefinedSignal(DefSignalType.L2H_INCRTHOLD, new SystemVerilogDefinedSignal(LOGIC, HW, "l2h_", "incrthold_o", "comp_type", "FIELD"));
		addDefinedSignal(DefSignalType.L2H_DECRTHOLD, new SystemVerilogDefinedSignal(LOGIC, HW, "l2h_", "decrthold_o", "comp_type", "FIELD"));
		addDefinedSignal(DefSignalType.L2H_ANDED, new SystemVerilogDefinedSignal(LOGIC, HW, "l2h_", "anded_o", "comp_type", "FIELD"));
		addDefinedSignal(DefSignalType.L2H_ORED, new SystemVerilogDefinedSignal(LOGIC, HW, "l2h_", "ored_o", "comp_type", "FIELD"));
		addDefinedSignal(DefSignalType.L2H_XORED, new SystemVerilogDefinedSignal(LOGIC, HW, "l2h_", "xored_o", "comp_type", "FIELD"));
		
		addDefinedSignal(DefSignalType.H2L_WEL, new SystemVerilogDefinedSignal(HW, LOGIC, "h2l_", "wel", "comp_type", "FIELD"));
		addDefinedSignal(DefSignalType.H2L_WE, new SystemVerilogDefinedSignal(HW, LOGIC, "h2l_", "we", "comp_type", "FIELD"));
		addDefinedSignal(DefSignalType.H2L_HWSET, new SystemVerilogDefinedSignal(HW, LOGIC, "h2l_", "hwset", "comp_type", "FIELD"));
		addDefinedSignal(DefSignalType.H2L_HWCLR, new SystemVerilogDefinedSignal(HW, LOGIC, "h2l_", "hwclr", "comp_type", "FIELD"));
		addDefinedSignal(DefSignalType.H2L_SWWEL, new SystemVerilogDefinedSignal(HW, LOGIC, "h2l_", "swwel", "comp_type", "FIELD"));
		addDefinedSignal(DefSignalType.H2L_SWWE, new SystemVerilogDefinedSignal(HW, LOGIC, "h2l_", "swwe", "comp_type", "FIELD"));
		addDefinedSignal(DefSignalType.H2L_INTR, new SystemVerilogDefinedSignal(HW, LOGIC, "h2l_", "intr", "comp_type", "FIELD"));
		addDefinedSignal(DefSignalType.H2L_DATA, new SystemVerilogDefinedSignal(HW, LOGIC, "h2l_", "w", "comp_type", "FIELD"));
		addDefinedSignal(DefSignalType.H2L_INCR, new SystemVerilogDefinedSignal(HW, LOGIC, "h2l_", "incr", "comp_type", "FIELD"));
		addDefinedSignal(DefSignalType.H2L_DECR, new SystemVerilogDefinedSignal(HW, LOGIC, "h2l_", "decr", "comp_type", "FIELD"));
		addDefinedSignal(DefSignalType.H2L_INCRVALUE, new SystemVerilogDefinedSignal(HW, LOGIC, "h2l_", "incrvalue", "comp_type", "FIELD"));
		addDefinedSignal(DefSignalType.H2L_DECRVALUE, new SystemVerilogDefinedSignal(HW, LOGIC, "h2l_", "decrvalue", "comp_type", "FIELD"));
		
		addDefinedSignal(DefSignalType.PREVINTR, new SystemVerilogDefinedSignal(LOGIC, LOGIC, "intr_", "previntr", "comp_type", "FIELD"));
		addDefinedSignal(DefSignalType.CNTR_NEXT, new SystemVerilogDefinedSignal(LOGIC, LOGIC, "cntr_", "next", "comp_type", "FIELD"));
		addDefinedSignal(DefSignalType.INTR_DLY, new SystemVerilogDefinedSignal(LOGIC, LOGIC, "intr_", "delay", "comp_type", "FIELD"));
		
		addDefinedSignal(DefSignalType.D2L_DATA, new SystemVerilogDefinedSignal(DECODE, LOGIC, "d2l_", "w", "comp_type", "REG"));
		addDefinedSignal(DefSignalType.D2L_ENABLE, new SystemVerilogDefinedSignal(DECODE, LOGIC, "d2l_", "w_enable", "comp_type", "REG"));
		addDefinedSignal(DefSignalType.L2D_DATA, new SystemVerilogDefinedSignal(LOGIC, DECODE, "l2d_", "r", "comp_type", "REG"));
		addDefinedSignal(DefSignalType.D2L_WE, new SystemVerilogDefinedSignal(DECODE, LOGIC, "d2l_", "we", "comp_type", "REG"));
		addDefinedSignal(DefSignalType.D2L_RE, new SystemVerilogDefinedSignal(DECODE, LOGIC, "d2l_", "re", "comp_type", "REG"));
		
		addDefinedSignal(DefSignalType.D2H_ADDR, new SystemVerilogDefinedSignal(DECODE, HW, "d2h_", "addr", "comp_type", "REG"));
		addDefinedSignal(DefSignalType.D2H_DATA, new SystemVerilogDefinedSignal(DECODE, HW, "d2h_", "w", "comp_type", "REG"));
		addDefinedSignal(DefSignalType.D2H_ENABLE, new SystemVerilogDefinedSignal(DECODE, HW, "d2h_", "w_enable", "comp_type", "REG"));
		addDefinedSignal(DefSignalType.D2H_SIZE, new SystemVerilogDefinedSignal(DECODE, HW, "d2h_", "size", "comp_type", "REG"));
		addDefinedSignal(DefSignalType.D2H_WE, new SystemVerilogDefinedSignal(DECODE, HW, "d2h_", "we", "comp_type", "REG"));
		addDefinedSignal(DefSignalType.D2H_RE, new SystemVerilogDefinedSignal(DECODE, HW, "d2h_", "re", "comp_type", "REG"));
		
		addDefinedSignal(DefSignalType.H2D_RETSIZE, new SystemVerilogDefinedSignal(HW, DECODE, "h2d_", "retsize", "comp_type", "REG"));
		addDefinedSignal(DefSignalType.H2D_DATA, new SystemVerilogDefinedSignal(HW, DECODE, "h2d_", "r", "comp_type", "REG"));
		addDefinedSignal(DefSignalType.H2D_ACK, new SystemVerilogDefinedSignal(HW, DECODE, "h2d_", "ack", "comp_type", "REG"));
		addDefinedSignal(DefSignalType.H2D_NACK, new SystemVerilogDefinedSignal(HW, DECODE, "h2d_", "nack", "comp_type", "REG"));
		
		addDefinedSignal(DefSignalType.L2H_INTR, new SystemVerilogDefinedSignal(LOGIC, HW, "l2h_", "intr_o", "comp_type", "REG"));
		addDefinedSignal(DefSignalType.L2H_HALT, new SystemVerilogDefinedSignal(LOGIC, HW, "l2h_", "halt_o", "comp_type", "REG"));
		addDefinedSignal(DefSignalType.INTR_CLEAR, new SystemVerilogDefinedSignal(LOGIC, LOGIC, "intr_", "clear", "comp_type", "REG"));
		
		addDefinedSignal(DefSignalType.USR_SIGNAL, new SystemVerilogDefinedSignal(LOGIC, LOGIC, "sig_", null, "comp_type", "SIGNAL"));  // default to no from/to
		addDefinedSignal(DefSignalType.SIGSET, new SystemVerilogDefinedSignal(NONE, NONE, null, null, "comp_type", "ANY"));
		addDefinedSignal(DefSignalType.LH_INTERFACE, new SystemVerilogDefinedSignal(LOGIC, HW, "lh_", null, "comp_type", "ANY")); 
		addDefinedSignal(DefSignalType.DH_INTERFACE, new SystemVerilogDefinedSignal(DECODE, HW, "dh_", null, "comp_type", "ANY")); 
		addDefinedSignal(DefSignalType.LH_STRUCT, new SystemVerilogDefinedSignal(LOGIC, HW, "lh_", null, "comp_type", "ANY")); 
		addDefinedSignal(DefSignalType.DH_STRUCT, new SystemVerilogDefinedSignal(DECODE, HW, "dh_", null, "comp_type", "ANY")); 
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
	
	/** return true is specified signal deref corresponds to a valid rhs signal */
	private static boolean isValidRhsDeRef(String deRef) {
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
           System.out.println(st + ": " + SystemVerilogDefinedOrdtSignals.getFullName(rhsSigSet.get(st), "foo.bar", true));
    }

}
