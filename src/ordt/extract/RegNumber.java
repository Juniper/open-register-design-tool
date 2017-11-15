/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved. 
 */
package ordt.extract;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** class representing a number for addressing/hw vectors */
public class RegNumber implements Comparable<RegNumber> {
	private BigInteger value;  // value stored
	public enum NumFormat {Verilog, NoLengthVerilog, Int, Address}  // the format used for display
	private NumFormat numFormat;
	public enum NumBase {Hex, Dec, Oct, Bin}    // the base used for display
	private NumBase numBase;
	private Integer vectorLen;   // length of vector in bits
	
	@SuppressWarnings("unused")
	private RegNumber() {}
	
	/** create regnumber from a string 
     *    
     *    fragment VNUM
     *    : '\'' ( 'b' ('0' | '1' | '_')+
     *       | 'd' ('0'..'9' | '_')+
     *       | 'o' ('0'..'7' | '_')+
     *       | 'h' ('0'..'9' | 'a'..'f' | 'A'..'F' | '_')+
     *       )
     *    ;
     *    NUM
     *      : ('0'..'9')* (VNUM | ('0'..'9'))
     *      | '0x' ('0'..'9' | 'a'..'f' | 'A'..'F')+
     *      ;
     **/
	public RegNumber (String numStr) {
		if ((numStr == null) || (numStr.length()<1)) return;
		// check for plain integers
		Pattern p = Pattern.compile("^(\\d+)$");
		Matcher m = p.matcher(numStr);
		if (m.matches()) {
		  this.value = new BigInteger(m.group(1));
		  this.numFormat = NumFormat.Int;
		  this.numBase = NumBase.Dec;
		  this.vectorLen = null;
		  return;
		}
		// check for hex number
		p = Pattern.compile("^0x([\\d_[a-f][A-F]]+)$");
		m = p.matcher(numStr);
		if (m.matches()) {
			String num = m.group(1).replace("_","");  // remove underscores
			//if (!m.group(1).equals(num)) System.out.println("RegNumber constructor: removed '_', new=" + num);
			//else System.out.println("RegNumber constructor: no '_', new=" + num);
			this.value = new BigInteger(num, 16);
			this.numFormat = NumFormat.Address;
			this.numBase = NumBase.Hex;
			this.vectorLen = null;
		  return;
		}
		// check for bin number
		p = Pattern.compile("^0b([_[0-1]]+)$");
		m = p.matcher(numStr);
		if (m.matches()) {
			String num = m.group(1).replace("_","");  // remove underscores
			this.value = new BigInteger(num, 2);
			this.numFormat = NumFormat.Address;
			this.numBase = NumBase.Hex;
			this.vectorLen = null;
		  return;
		}
		// check for verilog number
		p = Pattern.compile("^(\\d+)\\s*'\\s*([bdoh])\\s*([\\d_[a-f][A-F]]+)$");
		m = p.matcher(numStr);
		if (m.matches()) {
			this.numFormat = NumFormat.Verilog;
			this.vectorLen = Integer.valueOf(m.group(1));
			String base = m.group(2);
			String num = m.group(3).replace("_","");  // remove underscores
			if ("h".equals(base)) {
				this.numBase = NumBase.Hex;
				this.value = new BigInteger(num, 16);
			}
			else if ("d".equals(base)) {
				this.numBase = NumBase.Dec;
				this.value = new BigInteger(num);
			}
			else if ("o".equals(base)) {
				this.numBase = NumBase.Oct;
				this.value = new BigInteger(num, 8);
			}
			else if ("b".equals(base)) {
				this.numBase = NumBase.Bin;
				this.value = new BigInteger(num, 2);
			}
		}
		//else System.err.println("RegNumber: string matcher failed s=" + numStr + ", len=" + numStr.length());
	}
	
	/** create a regnumber from an integer */
	public RegNumber (int num) {
		//this(String.valueOf(num));
		this((long) num);
	}
	
	/** create a regnumber from a long */
	public RegNumber(long num) {
		//this(String.valueOf(num));
		this.value = BigInteger.valueOf(num);
		this.numFormat = NumFormat.Int;
		this.numBase = NumBase.Dec;
		this.vectorLen = null;
	}

	/** create a regnumber copy from another regnumber */
	public RegNumber (RegNumber num) {
		  if (num == null) return;
		  this.value = num.value;
		  this.numFormat = num.numFormat;
		  this.numBase = num.numBase;
		  this.vectorLen = num.vectorLen;
	}

	/** create a regnumber from a numeric expression string array */
	public RegNumber (String [] expArray) {
		  if ((expArray == null) || (expArray.length < 1)) return;  // return an undefined regNumber
		  // first scan tokenList and load parm/operator/depth lists
		  List<RegNumber> expr = new ArrayList<RegNumber>();
		  List<String> op = new ArrayList<String>();
		  List<Integer> opDepth = new ArrayList<Integer>();
		  // load expression into arraylists
		  Integer parenDepth = 0;
		  Integer maxParenDepth = 0;
		  for (int idx=0; idx<expArray.length; idx++) {  
			  String elem = expArray[idx];
			  // if parens then bump the depth
			  if (elem.equals("(")) { parenDepth++; maxParenDepth++; }
			  else if (elem.equals(")")) parenDepth--;
			  // else if an operator save op and depth
			  else if ("+-*/<<>>&|^".contains(elem)) {
				  op.add(elem);
				  opDepth.add(parenDepth);
			  }
			  // otherwise a number
			  else {
				  RegNumber elemNum = new RegNumber(elem);
				  if (!elemNum.isDefined()) return;  // bad regnum so exit
				  expr.add(elemNum);
			  }
		  }  // for
		  //System.out.println("--- expr:" + expr);
		  //System.out.println("--- op:" + op);
		  //System.out.println("--- opDepth:" + opDepth);
		  
		  // check op vs elem array sizes
		  if (!(op.size() + 1 == expr.size())) return;
		  
		  // now resolve from highest precedence to lowest
		  for (int depth=maxParenDepth; depth>=0; depth--) {
			  // exponent first
			  for (int idx=0; idx<op.size(); idx++) {
				  // if op has this depth then process
				  if (opDepth.get(idx) == depth) {
					  RegNumber lhs = expr.get(idx);
					  RegNumber rhs = expr.get(idx + 1); 
					  if (op.get(idx).equals("^")) { lhs.pow(rhs); expr.remove(idx+1); op.remove(idx); opDepth.remove(idx); idx--;}  
				  }
			  }
			  // multiply divide next
			  for (int idx=0; idx<op.size(); idx++) {
				  // if op has this depth then process
				  if (opDepth.get(idx) == depth) {
					  RegNumber lhs = expr.get(idx);
					  RegNumber rhs = expr.get(idx + 1);
					  if (op.get(idx).equals("*")) { lhs.multiply(rhs); expr.remove(idx+1); op.remove(idx); opDepth.remove(idx); idx--;}  
					  else if (op.get(idx).equals("/")) { lhs.divide(rhs); expr.remove(idx+1); op.remove(idx); opDepth.remove(idx); idx--;}					  
				  }
			  }
			  // add sub shift next
			  for (int idx=0; idx<op.size(); idx++) {
				  // if op has this depth then process
				  if (opDepth.get(idx) == depth) {
					  RegNumber lhs = expr.get(idx);
					  RegNumber rhs = expr.get(idx + 1);
					  if (op.get(idx).equals("+")) { lhs.add(rhs); expr.remove(idx+1); op.remove(idx); opDepth.remove(idx); idx--;}  
					  else if (op.get(idx).equals("-")) { lhs.subtract(rhs); expr.remove(idx+1); op.remove(idx); opDepth.remove(idx); idx--;}					  
					  else if (op.get(idx).equals("<<")) { lhs.lshift(rhs.toInteger()); expr.remove(idx+1); op.remove(idx); opDepth.remove(idx); idx--;}					  
					  else if (op.get(idx).equals(">>")) { lhs.rshift(rhs.toInteger()); expr.remove(idx+1); op.remove(idx); opDepth.remove(idx); idx--;}				  
					  else if (op.get(idx).equals("&")) { lhs.and(rhs); expr.remove(idx+1); op.remove(idx); opDepth.remove(idx); idx--;}					  
					  else if (op.get(idx).equals("|")) { lhs.or(rhs); expr.remove(idx+1); op.remove(idx); opDepth.remove(idx); idx--;}					  
				  }
			  }
			  //System.out.println(depth + ":--- expr:" + expr);
			  //System.out.println(depth + ":--- op:" + op);
			  //System.out.println(depth + ":--- opDepth:" + opDepth);
		  }	// for depth  
		  
		  // there should be only 1 expr elem remaining
		  if (expr.size() == 1) {
			  this.value = expr.get(0).getValue();
			  this.numBase = NumBase.Hex;  // default to hex/address format
			  this.numFormat = NumFormat.Address;
		  }
	}

	/** return true if value is defined
	 *  @return boolean
	 */
	public boolean isDefined() {
		return (value != null);
	}
	
	/** return true if value and vectorLen are defined
	 *  @return boolean
	 */
	public boolean isDefinedVector() {
		return (isDefined() && (vectorLen != null));
	}

	/** get value
	 *  @return the value
	 */
	public BigInteger getValue() {
		return value;
	}

	/** set value
	 *  @param value the value to set
	 */
	public void setValue(BigInteger value) {
		this.value = value;
	}

	/** get numFormat
	 *  @return the numFormat
	 */
	public NumFormat getNumFormat() {
		return numFormat;
	}

	/** set numFormat
	 *  @param numFormat the numFormat to set
	 */
	public void setNumFormat(NumFormat numFormat) {
		this.numFormat = numFormat;
	}

	/** get numBase
	 *  @return the numBase
	 */
	public NumBase getNumBase() {
		return numBase;
	}

	/** set numBase
	 *  @param numBase the numBase to set
	 */
	public void setNumBase(NumBase numBase) {
		this.numBase = numBase;
	}

	/** get vectorLen
	 *  @return the vectorLen
	 */
	public Integer getVectorLen() {
		return vectorLen;
	}

	/** set vectorLen
	 *  @param vectorLen the vectorLen to set
	 */
	public void setVectorLen(Integer vectorLen) {
		this.vectorLen = vectorLen;
	}

	@Override
	public String toString() { 
		return this.toFormat(numBase, numFormat);
	}

	/** create output string of specified format */
	public String toFormat(NumBase numBase, NumFormat numFormat) {
		if (!isDefined()) return "null";
		if ((numFormat == NumFormat.Verilog) || (numFormat == NumFormat.NoLengthVerilog)) {
			String vectorLenStr = (numFormat == NumFormat.NoLengthVerilog) ? "" : vectorLen.toString();
			if (numBase == NumBase.Bin)
			   return vectorLenStr + "\'b" + value.toString(2);   // binary
			if (numBase == NumBase.Oct)
			   return vectorLenStr + "\'o" + value.toString(8);   // octal
			if (numBase == NumBase.Dec)
			   return vectorLenStr + "\'d" + value.toString(10);   // decimal
		   return vectorLenStr + "\'h" + value.toString(16);   // default to hex
		}
		else if (numFormat == NumFormat.Address) { 
			return "0x" + value.toString(16);   // hex address output format
		}
		else {
			if (numBase == NumBase.Hex) return value.toString(16);
			return value.toString();  // integer format
		}
	}

	// ----------------------- in-place RegNumber transform methods -----------------------
	
	/** add arg value to base value keeping current format/base settings.
	 *  NOTE: this method modifies the calling RegNumber 
	 * @return false if add failed */
	public boolean add(RegNumber inc) {
		if (inc == null) return false;
		else setValue(getValue().add(inc.getValue()));
		return true;
	}

	public void add(Integer inc) {
		add(new RegNumber(inc));	
	}

	/** subtract arg value from base value keeping current format/base settings 
	 *  NOTE: this method modifies the calling RegNumber 
	 * @return false if add failed */
	public boolean subtract(RegNumber dec) {
		if (dec == null) return false;
		else setValue(getValue().subtract(dec.getValue()));
		return true;
	}

	public void subtract(Integer dec) {
		subtract(new RegNumber(dec));			
	}

	/** raise base value to power of arg value keeping current format/base settings 
	 *  NOTE: this method modifies the calling RegNumber 
	 * @return false if pow failed */
	public boolean pow(Integer exp) {
		if (exp == null) return false;
		else setValue(getValue().pow(exp));
		return true;
	}

	/** raise base value to power of arg value keeping current format/base settings 
	 *  NOTE: this method modifies the calling RegNumber 
	 * @return false if pow failed */
	public boolean pow(RegNumber exp) {
		if ((exp == null) || !exp.isDefined()) return false;
		else setValue(getValue().pow(exp.getValue().intValue()));
		return true;
	}

	/** peform a bitwise or with arg value keeping current format/base settings 
	 *  NOTE: this method modifies the calling RegNumber 
	 * @return false if or failed */
	private boolean or(RegNumber rhs) {
		if (rhs == null) return false;
		else setValue(getValue().or(rhs.getValue()));
		return true;
	}

	/** peform a bitwise and with arg value keeping current format/base settings 
	 *  NOTE: this method modifies the calling RegNumber 
	 * @return false if or failed */
	private boolean and(RegNumber rhs) {
		//System.out.println("RegNumber and, lhs=" + this + ", rhs=" + rhs);
		if (rhs == null) return false;
		else setValue(getValue().and(rhs.getValue()));
		return true;
	}

	/** left shift base value by arg value keeping current format/base settings 
	 *  NOTE: this method modifies the calling RegNumber 
	 * @return false if shift failed */
	public boolean lshift(Integer rhs) {
		if (rhs == null) return false;
		else setValue(getValue().shiftLeft(rhs));
		return true;		
	}

	/** right shift base value by arg value keeping current format/base settings 
	 *  NOTE: this method modifies the calling RegNumber 
	 * @return false if shift failed */
	public boolean rshift(Integer rhs) {
		if (rhs == null) return false;
		else setValue(getValue().shiftRight(rhs));
		return true;		
	}

	/** multiply int arg value to base value keeping current format/base settings 
	 *  NOTE: this method modifies the calling RegNumber 
	 * @return false if add failed */
	public boolean multiply(Integer mult) {
		if (mult == null) return false;
		else setValue(getValue().multiply(BigInteger.valueOf(mult)));
		return true;
	}

	/** divide by int arg value keeping current format/base settings 
	 *  NOTE: this method modifies the calling RegNumber 
	 * @return false if add failed */
	public boolean divide(Integer divisor) {
		if (divisor == null) return false;
		else setValue(getValue().divide(BigInteger.valueOf(divisor)));
		return true;
	}
	
	/** multiply int regnumber value to base value keeping current format/base settings 
	 *  NOTE: this method modifies the calling RegNumber 
	 * @return false if add failed */
	public boolean multiply(RegNumber mult) {
		if ((mult == null) || !mult.isDefined()) return false;
		else setValue(getValue().multiply(mult.getValue()));
		return true;
	}

	/** divide by regnumber arg value keeping current format/base settings 
	 *  NOTE: this method modifies the calling RegNumber 
	 * @return false if add failed */
	public boolean divide(RegNumber divisor) {
		if ((divisor == null) || !divisor.isDefined()) return false;
		else setValue(getValue().divide(divisor.getValue()));
		return true;
	}

	// -------------------------------------------------
	
	/** set value of this regnum to next highest power of 2 
	 *  NOTE: this method modifies the calling RegNumber */
	public void setNextHighestPowerOf2() {
		BigInteger newValue = BigInteger.valueOf((long) 0);
		int idx = this.getMinusOneHighestBit() + 1;
		if (idx<1) return;
		newValue = newValue.setBit(idx);
		this.value = newValue;
	}
	
	/** return a regnumber w/ value of next highest power of 2 of this regnumber */
	public RegNumber getNextHighestPowerOf2() {
		RegNumber newNum = new RegNumber(this);
		newNum.setNextHighestPowerOf2();
		return newNum;
	}
		
	/** return true is RegNumber mod modValue == 0 
	 * @param modValue
	 * @return true if this regnumber mod modValue == 0 */
	public boolean isModulus(RegNumber modValue) {
		if (modValue == null) return false;
		// calculate the remainder, if 0 we're done
		BigInteger rem = getValue().mod(modValue.getValue()); 
		return (rem.compareTo(new BigInteger("0")) == 0); 
	}
	
	/** return true is RegNumber mod modValue == 0 
	 * @param modValue Integer
	 * @return true if this regnumber mod modValue == 0 */
	public boolean isModulus(Integer modValue) {
		return isModulus(new RegNumber(String.valueOf(modValue))); 
	}
	
	/** modify value to round up to next integer value with mod == 0 
	 * @return rounded up value */
	public void roundUpToModulus(RegNumber modValue) {
		if (modValue == null) return;
		// calculate the remainder, if 0 we're done
		BigInteger rem = getValue().mod(modValue.getValue()); 
		if (rem.compareTo(new BigInteger("0")) == 0) return; 
		// otherwise need to remove remainder and bump up by mod value
		//System.out.println("value=" + this + ", modValue=" + modValue + ", rem=" + rem);
		setValue(getValue().subtract(rem));
		//System.out.println("value post sub=" + this);
		setValue(getValue().add(modValue.getValue()));
		//System.out.println("value post add=" + this);
	}

	/** return a binary string representing value from low to high bit index */
	public String getBinString(int lowBit, int highBit) {
		// create a pad string
		String padStr = "";
		for (int i=0; i<highBit; i++) padStr = padStr +  "0";
		String binStr = padStr + getValue().toString(2);  // get padded binary value
		int len = binStr.length();  
		//System.out.println("len=" + len + ", binstr=" + binStr+ ", low=" + lowBit+ ", high=" + highBit);
		if ((lowBit>len-1) || (highBit>len-1)) return "";  // if bad indices return empty
		return binStr.substring(len-highBit-1, len-lowBit);
	}

	/** RegNumber comparisons based on value */
    @Override
	public int compareTo(RegNumber arg0) {
		return this.getValue().compareTo(arg0.getValue());
	}

	/** return true if instance value is less than this of argument */
	public boolean isLessThan(RegNumber arg0) {
		return (this.compareTo(arg0) < 0);
	}

	/** return true if instance value is greater than this of argument */
	public boolean isGreaterThan(RegNumber arg0) {
		return (this.compareTo(arg0) > 0);
	}
	
	/** return index of highest set bit in binary representation of value */  
    public int getHighestBit () {   
		return this.getValue().bitLength() - 1;
	}
    
	/** return index of highest set bit in binary representation of value minus 1 */   
    public int getMinusOneHighestBit () {   
    	BigInteger valMinus1 = this.getValue().subtract(new BigInteger("1"));
		return valMinus1.bitLength() - 1;
	}

    /** set a subvector within this RegNumber */
	public void setSubVector(RegNumber subvector, Integer lowIndex) {
		if ((lowIndex == null) || !subvector.isDefinedVector()) return;  // exit if invalid inputs
		if (!isDefinedVector()) return; //System.out.println("RegNumber: attempting setSubVector in undefined vector");
		//else System.out.println("RegNumber: base vector=" + getVectorLen());
		
		int firstHighBit = subvector.getVectorLen() + lowIndex;
		if (firstHighBit > getVectorLen()) return;  // out of range index
		// get the msbs of the original number
		BigInteger highBits = getValue().shiftRight(firstHighBit);
		highBits = highBits.shiftLeft(firstHighBit);
		// get the lsbs of the original number
		BigInteger notLowBits = getValue().shiftRight(lowIndex);
		notLowBits = notLowBits.shiftLeft(lowIndex);
		BigInteger lowBits = getValue().subtract(notLowBits);
		// create the new subvector
		BigInteger newVector = subvector.getValue().shiftLeft(lowIndex);
		// reassemble and save new value
    	//System.out.println("high=" + highBits + ", low=" + lowBits);
		setValue(highBits.or(lowBits).or(newVector));
	}
	
	/** return a subvector from within this RegNumber */
	public RegNumber getSubVector(Integer lowIndex, Integer width) {
		if ((lowIndex == null) || (width == null) || (width < 1)) return null;  // exit if invalid inputs
		// if not a defined length then create using input values
		int vectorLen = isDefinedVector() ? getVectorLen() : width + lowIndex;
		//if (!isDefinedVector()) return null; //System.out.println("RegNumber: attempting setSubVector in undefined vector");
		// check for out of range lowIndex
		if ((lowIndex + 1) > vectorLen) return null;  
		
		RegNumber newNum = new RegNumber(this);
		newNum.setVectorLen(width);
		// subtract off any msbs
		int firstHighBit = width + lowIndex;
		if (firstHighBit <= vectorLen) {
			// get the msbs of the original number
			BigInteger highBits = getValue().shiftRight(firstHighBit);
			highBits = highBits.shiftLeft(firstHighBit);
			newNum.setValue(getValue().subtract(highBits));
		}
		// shift off lsbs of the original number
		BigInteger notLowBits = newNum.getValue().shiftRight(lowIndex);
		newNum.setValue(notLowBits);
		return newNum;
	}
	
    /** return true if value of this RegNumber is non-zero */
	public boolean isNonZero() {
		return (getValue().bitCount() > 0);
	}

	/** return integer rep of this regnumber or null if unable to successfully convert */
	public Integer toInteger() {
		// try integer conversion
		Integer retVal = null;
		try {
			if (isDefined()) retVal = Integer.valueOf(toFormat(NumBase.Dec, NumFormat.Int));
		}
		catch (NumberFormatException e) {		
		}
		return retVal;
	}

	/** return long rep of this regnumber or null if unable to successfully convert */
	public long toLong() {
		// try integer conversion
		Long retVal = null;
		try {
			if (isDefined()) retVal = Long.valueOf(toFormat(NumBase.Dec, NumFormat.Int));
		}
		catch (NumberFormatException e) {		
		}
		return retVal;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((numBase == null) ? 0 : numBase.hashCode());
		result = prime * result + ((numFormat == null) ? 0 : numFormat.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		result = prime * result + ((vectorLen == null) ? 0 : vectorLen.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RegNumber other = (RegNumber) obj;
		if (numBase != other.numBase)
			return false;
		if (numFormat != other.numFormat)
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		if (vectorLen == null) {
			if (other.vectorLen != null)
				return false;
		} else if (!vectorLen.equals(other.vectorLen))
			return false;
		return true;
	}

    public static void main (String[] args) {
    	//BigInteger bigint = new BigInteger("0"); bigint.bitLength();
    	//RegNumber val = new RegNumber("0x28");
    	//RegNumber val = new RegNumber("8'h28");
    	//RegNumber val = new RegNumber(Long.MAX_VALUE);
    	//RegNumber sub = new RegNumber("2'h1");
    	//System.out.println("val=" + val + ", sub=" + sub);
    	//val.setSubVector(sub, 0);
    	
    	//RegNumber mod = new RegNumber("0x4");
    	//val.roundUpToModulus(mod);
    	//System.out.println("val=" + val + ", sub=" + val.getSubVector(2, 4));
    	//System.out.println("max=" + Long.MAX_VALUE);
    	//System.out.println("val=" + val);
    	
    	/*RegNumber val = new RegNumber("0x28");
    	System.out.println("val=" + val + ", getHighestBit=" + val.getHighestBit()+ ", BI bitLength=" + val.getValue().bitLength());
    	val = new RegNumber(Long.MAX_VALUE);
    	System.out.println("val=" + val + ", getHighestBit=" + val.getHighestBit()+ ", BI bitLength=" + val.getValue().bitLength());
    	val = new RegNumber("2'h1");
    	System.out.println("val=" + val + ", getHighestBit=" + val.getHighestBit()+ ", BI bitLength=" + val.getValue().bitLength());
    	val = new RegNumber("0");
    	System.out.println("val=" + val + ", getHighestBit=" + val.getHighestBit()+ ", BI bitLength=" + val.getValue().bitLength());
    	*/
    	RegNumber val = new RegNumber("na");
    	System.out.println("val is null=" + (val == null) + ", isDefined=" + ((val == null)? "-" : val.isDefined()) + ", val=" +  ((val == null)? "-" : val));
    	//String [] expr = {"1", "+", "2"};
    	//String [] expr = {"1", "+", "0xf"};
    	//String [] expr = {"1"};
    	//String [] expr = {"11", "-", "2", "*", "0x2", "-", "3" };
    	
    	//String [] expr = {"11", "-", "2", "-", "0x2", "-", "5" };
    	//String [] expr = {"11", "-", "2", "-", "0x2", "-", "5" };
    	//String [] expr = {"(", "1", "+", "2", ")", "|", "0x1"};
    	//RegNumber val = new RegNumber(expr);
    	//System.out.println("expression resolved to " + val);
    	/*
    	for (int i=0; i<100; i++) {
    	  RegNumber val = new RegNumber(i);
    	  System.out.println("val=" + val + ", m1hibit=" + val.getMinusOneHighestBit() + ", next_pow_2=" + val.getNextHighestPowerOf2());
    	}*/
    	//RegNumber nextpow2 = new RegNumber(String.valueOf(Math.pow(2, (val.getHighestBit()+1))));
    	//System.out.println("val=" + val + ", next_pow_2=" + val.getNextHighestPowerOf2());
    	//Integer i;
    	//BigInteger big; // = new BigInteger(i);
    	//big.

    }

}
