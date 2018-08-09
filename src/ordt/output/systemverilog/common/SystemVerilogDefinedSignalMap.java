package ordt.output.systemverilog.common;

import java.util.HashMap;
import java.util.Map;

import ordt.output.common.MsgUtils;

/** abstract class containing a mapping of application-specific signal types to their characteristics (from, to, prefix, suffix, etc).
 *  A concrete child class will typically define an enum of types that implements SystemVerilogDefSignalTypeIntf and load the map 
 *  at via clearMap() and addDefinedSignal() methods */
public abstract class SystemVerilogDefinedSignalMap {
	
	private static Map<SystemVerilogDefSignalTypeIntf, SystemVerilogDefinedSignal> sigMap = 
			new HashMap<SystemVerilogDefSignalTypeIntf, SystemVerilogDefinedSignal>();  // set of defined signals by type
		
	/** clear the map */
	protected static void clearMap() {
		sigMap.clear();
	}
	
	/** clear the map */
	protected static void addDefinedSignal(SystemVerilogDefSignalTypeIntf sigType, SystemVerilogDefinedSignal signal) {
		sigMap.put(sigType, signal);
	}
	
	
	/** verify the signal map has been initialized before accessing */
	private static void checkMapInit() {
		if (sigMap.isEmpty()) 
			MsgUtils.errorExit("Attempted to access SystemVerilogDefinedSignalMap prior to initialization - insure map is defined in concrete child class");	
	}
	
	/** return the prefix for specified defined signal type */
	public static String getPrefix(SystemVerilogDefSignalTypeIntf sigType) {
		checkMapInit();
		if (!sigMap.containsKey(sigType)) return null;
		return sigMap.get(sigType).getPrefix();
	}
	
	/** return the suffix for specified defined signal type */
	public static String getSuffix(SystemVerilogDefSignalTypeIntf sigType) {
		checkMapInit();
		if (!sigMap.containsKey(sigType)) return null;
		return sigMap.get(sigType).getSuffix();
	}
	
	/** return the from location for specified defined signal type */
	public static Integer getFrom(SystemVerilogDefSignalTypeIntf sigType) {
		checkMapInit();
		if (!sigMap.containsKey(sigType)) return null;
		return sigMap.get(sigType).getFrom();
	}
	
	/** return the to location for specified defined signal type */
	public static Integer getTo(SystemVerilogDefSignalTypeIntf sigType) {
		checkMapInit();
		if (!sigMap.containsKey(sigType)) return null;
		return sigMap.get(sigType).getTo();
	}
	
	/** return the name for specified defined signal type 
	 *  This is a catenation of prefix (if addPrefix is true), input pathStr, and suffix */
	public static String getFullName(SystemVerilogDefSignalTypeIntf sigType, String pathStr, boolean addPrefix) {
		checkMapInit();
		if (!sigMap.containsKey(sigType)) return null;
		return sigMap.get(sigType).getFullName(pathStr, addPrefix);
	}

}

