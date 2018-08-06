package ordt.output.common;

import java.util.Date;

public class MsgUtils {

	private final static int ERROR_CONTINUE_RC = 4;
	private final static int ERROR_EXIT_RC = 8;
	private static int returnCode = 0;
	private static String codeName = "Ordt";

	/** display error message and exit */
	public static void errorExit(String msg) {
		errorMessage(msg);	
		System.out.println(codeName + " exited due to error " + new Date());
		System.exit(ERROR_EXIT_RC);
	}

	/** display error message */
	public static void errorMessage(String msg) {
		System.err.println("*** ERROR ***: " + msg);
		returnCode = ERROR_CONTINUE_RC;
	}
	
	/** display error message */
	public static void infoMessage(String msg) {
		System.out.println("*** INFO ***: " + msg);		
	}
	
	/** display error message */
	public static void warnMessage(String msg) {
		System.err.println("*** WARNING ***: " + msg);		
	}

	/** get return code */
	public static int getReturnCode() {
		return returnCode;
	}

	protected static void setCodeName(String codeName) {
		MsgUtils.codeName = codeName;
	}

}
