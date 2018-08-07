package ordt.output.common;

import java.util.Date;

public class MsgUtils {

	private final static int ERROR_CONTINUE_RC = 4;
	private final static int ERROR_EXIT_RC = 8;
	private static int returnCode = 0;
	private static String progName = "Ordt";

	/** display error message and exit */
	public static void errorExit(String msg) {
		errorMessage(msg);	
		System.out.println(progName + " exited due to error " + new Date());
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

	/** set name of this program for display */
	public static void setProgName(String progName) {
		MsgUtils.progName = progName;
	}

	/** get name of this program for display */
	public static String getProgName() {
		return progName;
	}

	/** generate string of spaces of specified length */
	public static String repeat (char c, int num) {
		String retstr = "";
		for (int i=0; i<num; i++)  retstr = retstr + c;
	   return retstr;	
	}

}
