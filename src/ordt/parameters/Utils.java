/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved.
 */
package ordt.parameters;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ordt.extract.Ordt;

/** utilities class */
public class Utils {
	
	/** return number of bits required to represent int of a given size */
	public static int getBits(int value) {
		if (value == 0) return 0;
		return ceilLog2(value);
	}
	
	/** return next highest power of 2 for a given int */
	public static int getNextHighestPowerOf2(int value) {
		return (int) Math.pow(2, ceilLog2(value));
	}
	
	/** return true if a power of 2  */
	public static boolean isPowerOf2(int value) {
		return ((value & (value - 1)) == 0);  // assumes positive value
	}
	
	/** return next lowest power of 2 for a given int */
	public static int getNextLowestPowerOf2(int value) {
		return (int) Math.pow(2, floorLog2(value));
	}
	
	/** return true if int is in inclusive range */
	public static boolean isInRange(int value, int low, int hi) {
		return ((value >= low) && (value <= hi)); 
	}

	/** compute floor of log2 of integer */
	public static int floorLog2(int n){
	    if(n <= 0) throw new IllegalArgumentException();
	    return 31 - Integer.numberOfLeadingZeros(n);
	}

	/** compute ceil of log2 of integer */
	public static int ceilLog2(int n){
	    if(n <= 0) throw new IllegalArgumentException();
	    return 32 - Integer.numberOfLeadingZeros(n-1);
	}
	
	/** return an integer from string using specified radix or null if invalid format
	 * 
	 * @param intStr - input text string
	 * @param radix - radix to be used for conversion
	 * @param messageSuffix - optional text error message (if null, no error message)
	 * @return converted integer or null if convert failed
	 */
	public static Integer strToInteger (String intStr, int radix, String messageSuffix) {
		Integer intval;
		try {
			intval = Integer.valueOf(intStr, radix);
			return intval;
		} catch (NumberFormatException e) {
			if (messageSuffix != null ) Ordt.errorMessage("Unable to convert " + intStr + " to integer" +  messageSuffix);
			return null;
		}
	}
	
	/** return an integer from string using specified radix or null if invalid format 
     **/
	public static Integer strToInteger (String intStr, int radix) {
		return strToInteger(intStr, radix, null);
	}
	
	/** convert string to integer and issue error message on fail
	 */
	public static Integer strToInteger(String intStr, String messageSuffix) {
		return strToInteger(intStr, 10, messageSuffix);
	} 
	
	/** return an integer from string  or null if invalid format 
     **/
	public static Integer strToInteger (String intStr) {
		return strToInteger(intStr, 10, null);
	}
	
	/** create a pos integer from string input of various forms (vlog, addr, int) 
	 * @param numStr - input text string
	 * @param messageSuffix - optional text error message (if null, no error message)
	 * @return converted integer or null if convert failed
     **/
	public static Integer numStrToPosInteger (String numStr, String messageSuffix) {
		if ((numStr == null) || (numStr.length()<1)) return null;
		// check for plain integers
		Pattern p = Pattern.compile("^(\\d+)$");
		Matcher m = p.matcher(numStr);
		if (m.matches()) {
		  return strToInteger(m.group(1), messageSuffix);
		}
		// check for hex number
		p = Pattern.compile("^0x([\\d_[a-f][A-F]]+)$");
		m = p.matcher(numStr);
		if (m.matches()) {
			String num = m.group(1).replace("_","");  // remove underscores
			  return strToInteger(num, 16, messageSuffix);
		}
		// check for bin number
		p = Pattern.compile("^0b([_[0-1]]+)$");
		m = p.matcher(numStr);
		if (m.matches()) {
			String num = m.group(1).replace("_","");  // remove underscores
			  return strToInteger(num, 2, messageSuffix);
		}
		// check for verilog number
		p = Pattern.compile("^(\\d+)\\s*'\\s*([bdoh])\\s*([\\d_[a-f][A-F]]+)$");
		m = p.matcher(numStr);
		if (m.matches()) {
			String base = m.group(2);
			String num = m.group(3).replace("_","");  // remove underscores
			if ("h".equals(base)) return strToInteger(num, 16, messageSuffix);
			else if ("d".equals(base)) return strToInteger(num, messageSuffix);
			else if ("o".equals(base)) return strToInteger(num, 8, messageSuffix);
			else if ("b".equals(base)) return strToInteger(num, 2, messageSuffix);
		}
		//else System.err.println("Utils numStrToInteger: string match failed s=" + numStr);
		return null;
	}
	
	/** create a pos integer from string input of various forms (vlog, addr, int) 
	 * @param numStr - input text string
	 * @return converted integer or null if convert failed
     **/
	public static Integer numStrToInteger (String numStr) {
		return numStrToPosInteger (numStr, null);
	}

	/** generate string of spaces of specified length */
	public static String repeat (char c, int num) {
		String retstr = "";
		for (int i=0; i<num; i++)  retstr = retstr + c;
	   return retstr;	
	}
	
	/** catenate 2 strings with a single underscore between */
	public static String usCatenate(String string1, String string2) {
		String newString1 = (string1 == null)? "" : string1;
		String newString2 = (string2 == null)? "" : string2;
		boolean noUnderscore = newString1.isEmpty() || newString2.isEmpty() || newString1.endsWith("_") || newString2.startsWith("_");
		return newString1 + (noUnderscore? "" : "_") + newString2;
	}
	
    public static void main(String[] args) throws Exception {
    	//for (int i=1; i<34; i++) {
    	//	System.out.println("i=" + i + ", floor=" + floorLog2(i) + ", ceil=" + ceilLog2(i) + ", nextHigh=" + getNextHighestPowerOf2(i) + ", isP2=" + isPowerOf2(i));
    	//}
    	//String inStr="1033300";
    	//System.out.println("str=" + inStr + ", simple int=" + strToInteger(inStr) + ", full int=" + numStrToInteger(inStr));
    	for(int i=0; i<10; i++)
    	   System.out.println(i + ":" + getBits(i));
    }

}
