/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved. 
 */
package ordt.output;

import java.util.HashMap;

import ordt.output.common.MsgUtils;
import ordt.extract.DefinedProperties;
import ordt.extract.Ordt;
import ordt.extract.PropertyList;
import ordt.extract.RegNumber;
import ordt.extract.model.ModComponent;
import ordt.extract.model.ModEnum;
import ordt.extract.model.ModIndexedInstance;
import ordt.extract.model.ModInstance;
import ordt.output.systemverilog.SystemVerilogDefinedOrdtSignals;
import ordt.output.systemverilog.SystemVerilogDefinedOrdtSignals.DefSignalType;
import ordt.parameters.ExtParameters;

/** class of properties needed for display of active field instance */
public class FieldProperties extends InstanceProperties { 
	
	private String fieldArrayString = ""; // string representing array range of this field within register 
	private String fieldSetPrefixString = ""; // fieldset prefix string for this field 
	private Integer fieldWidth = 1;   // default to scalar field        
	private Integer lowIndex = null;   // no specific index set
	private JspecSubCategory subcategory = new JspecSubCategory("");  // subcategory property
	
	private RegNumber reset;     // reset value for this field 
	
	private boolean hwPrecedence = false;  // default to sw precedence
	
	private boolean isSwReadable = true;  // default to sw read/write, hw read
	private boolean isSwWriteable = true;
	private boolean isHwReadable = true;
	private boolean isHwWriteable = ExtParameters.rdlDefaultRwHwAccess();  
	
	private boolean hasSwWriteEnableH = false;
	private boolean hasSwWriteEnableL = false;
	
	private boolean hasHwSet = false;
	private boolean hasHwClr = false;
	
	private boolean hasWriteEnableH = false;
	private boolean hasWriteEnableL = false;
	
	private boolean isRclr = false;
	private boolean isRset = false;
	private boolean isWoset = false;
	private boolean isWoclr = false;
	
	private boolean hasSwAcc = false;
	private boolean hasSwMod = false;
	
	private boolean isAnded = false;
	private boolean isOred = false;
	private boolean isXored = false;
	private boolean isSinglePulse = false;
	
	private boolean hasOverflow = false;  // generate overflow output
	private boolean hasUnderflow = false;  // generate underflow outpu
	private boolean hasSaturateOutputs = false;  // generate incr/decr saturate outputs

	private RegNumber dontTestMask;
	private RegNumber dontCompareMask;
	private boolean rtlCoverage = false;

	private ModEnum encoding = null;
	
	// counter info
	private boolean isCounter = false;  
	private RegNumber incrValue;  // increment value
	private RegNumber decrValue;  // decrement value
	private Integer incrWidth;    // external interface width for increment value
	private Integer decrWidth;    // external interface width for decrement value
	
	private RegNumber incrSatValue;  // increment saturation value
	private RegNumber decrSatValue;  // decrement saturation value
	
	private RegNumber incrTholdValue;  // increment threshold value
	private RegNumber decrTholdValue;  // decrement threshold value
	
	// interrupt info
	private boolean isInterrupt = false;   // field is set by interrupt input
		
	public enum IntrType {
		LEVEL(0), POSEDGE(1), NEGEDGE(2), BOTHEDGE(3);
	    private int value;
	    IntrType(int value) {
	        this.value = value;
	    }
	    public int getValue() {
	        return value;
	    }
	}
	
	private IntrType intrType;
	
	public enum IntrStickyType {
		STICKYBIT(0), STICKY(1), NONSTICKY(2);
	    private int value;
	    IntrStickyType(int value) {
	        this.value = value;
	    }
	    public int getValue() {
	        return value;
	    }
	}
	
	private IntrStickyType intrStickyType;
	private boolean maskIntrBits = false;   // intr field bits are not set by enable/mask by default
	
	// halt info
	private boolean isHalt = false;   // field is set by halt input

	// rhs references
	public enum RhsRefType {
		RESET_SIGNAL, RESET_VALUE, NEXT, WE, SW_WE, HW_SET, HW_CLR,   
		INTR, INTR_ENABLE, INTR_MASK, 
		HALT_ENABLE, HALT_MASK, 
		INCR, INCR_SAT_VALUE, INCR_THOLD_VALUE, INCR_VALUE,
		DECR, DECR_SAT_VALUE, DECR_THOLD_VALUE, DECR_VALUE
	}
	
	private HashMap<RhsRefType, RhsReference> rhsReferences = new HashMap<RhsRefType, RhsReference>();

	/** init properties using defaults -> component -> instance values */
	public FieldProperties(ModInstance regInst) {
		super(regInst);  // init instance, id and copy base properties in
	}	
	
	/** extract properties from the calling instance */
    @Override
	public void extractProperties(PropertyList pList) {
		super.extractProperties(pList);  // extract common parameters
		
		// go directly to extractInstance to get field width/indices
		setFieldWidth(getExtractInstance());  // set width of field
		
		/*if (getInstancePath().contains("sopf_sip0_par_addr_reg")) { 
			//display();
		    System.out.println("FieldProperties extractInstance: inst=" + getInstancePath() + ", parmlist=" + pList);
		}*/
		
		// now extract from the combined instance properties
		
		// category property
		if (pList.hasProperty("sub_category")) setSubCategory(pList.getProperty("sub_category")); 
		//if (getId().equals("str")) System.out.println("--- FieldProperties for str:" + this.getSubCategory());
		
		// now use pList to extract info
		if (pList.hasProperty("reset")) {
			RegNumber regNum = new RegNumber(pList.getProperty("reset"));
			if (regNum.isDefined()) {
				if (!regNum.isDefinedVector()) {
					//System.out.println("fieldProperties: reset has no vector length, r=" + pList.getProperty("reset"));
					regNum.setVectorLen(getFieldWidth());
				}
				//else System.out.println("fieldProperties: reset vector length=" + regNum.getVectorLen() + ", r=" + pList.getProperty("reset"));
				setReset(regNum);   // assignment of value
			}
			else if ("na".equals(pList.getProperty("reset")) || "unknown".equals(pList.getProperty("reset"))) setReset(null); // na indicates no reset
			else setRef(RhsRefType.RESET_VALUE, pList.getProperty("reset"), pList.getDepth("reset"));  // assignment by reference
		}
		
		// set resetsignal to default if not specified
		if (pList.hasProperty("resetsignal")) 
			setRef(RhsRefType.RESET_SIGNAL, pList.getProperty("resetsignal"), pList.getDepth("resetsignal")); 

		// set enum encode 
		if (pList.hasProperty("encode")) {
			ModComponent parent = getExtractInstance().getParent();
			if (parent != null) {
				ModEnum enumComp = parent.findEnum(pList.getProperty("encode"));
				if (enumComp == null) MsgUtils.errorMessage("Unable to find enum encoding " + pList.getProperty("encode") + " for field " + getId());
                setEncoding(enumComp);
			}
		}
		
		// set hw read/write properties
		if (pList.hasProperty("hw")) {
			String hwVal = pList.getProperty("hw");
			if (hwVal.contains("r")) setHwReadable(true);
			else setHwReadable(false);
			if (hwVal.contains("w")) setHwWriteable(true);
			else setHwWriteable(false);
		}
		
		// set sw read/write properties
		if (pList.hasProperty("sw")) {
			String swVal = pList.getProperty("sw");
			if (swVal.contains("r")) setSwReadable(true);
			else setSwReadable(false);
			if (swVal.contains("w")) setSwWriteable(true);
			else setSwWriteable(false);
		}
		
		// set sw write enable properties 
		if (pList.hasTrueOrRefProperty("swwe")) {
			setHasSwWriteEnableH(true);
			// if a reference signal than set
			if (!pList.hasBooleanProperty("swwe"))
				setRef(RhsRefType.SW_WE, pList.getProperty("swwe"), pList.getDepth("swwe"));
		}
		else if (pList.hasTrueOrRefProperty("swwel")) {
			setHasSwWriteEnableL(true);
			// if a reference signal than set
			if (!pList.hasBooleanProperty("swwel"))
				setRef(RhsRefType.SW_WE, pList.getProperty("swwel"), pList.getDepth("swwel"));
		}	
		
		// set hw write enable properties 
		if (pList.hasTrueOrRefProperty("we")) {
			setHwWriteable(true);  // add write data bus
			setHasWriteEnableH(true);
			// if a reference signal than set
			if (!pList.hasBooleanProperty("we"))
				setRef(RhsRefType.WE, pList.getProperty("we"), pList.getDepth("we"));
		}
		else if (pList.hasTrueOrRefProperty("wel")) {
			//System.out.println("fieldProperties extractProperties: wel found, inst=" + this.getInstancePath() + ", wel=" + pList.getProperty("wel"));
			setHwWriteable(true);  // add write data bus
			setHasWriteEnableL(true);
			// if a reference signal than set
			if (!pList.hasBooleanProperty("wel"))
				setRef(RhsRefType.WE, pList.getProperty("wel"), pList.getDepth("wel"));
		}	
		
		// set hw set property 
		if (pList.hasTrueOrRefProperty("hwset")) {
			setHasHwSet(true);
			// if a reference signal than set
			if (!pList.hasBooleanProperty("hwset"))
				setRef(RhsRefType.HW_SET, pList.getProperty("hwset"), pList.getDepth("hwset"));
		}
		
		// set hw clr property 
		if (pList.hasTrueOrRefProperty("hwclr")) {
			setHasHwClr(true);
			// if a reference signal than set
			if (!pList.hasBooleanProperty("hwclr"))
				setRef(RhsRefType.HW_CLR, pList.getProperty("hwclr"), pList.getDepth("hwclr"));
		}
		
		// set test mask values
		if (pList.hasProperty("donttest")) {
			RegNumber mask;
			if (pList.hasTrueProperty("donttest"))  // entire field is donttest
			   mask = new RegNumber(getFieldWidth() + "'b" + MsgUtils.repeat('1', getFieldWidth()));
			else
			   mask = new RegNumber(pList.getProperty("donttest"));
			if (mask.isDefined()) {
				setDontTest(true);
				setDontTestMask(mask);
			}
		}
		else if (pList.hasProperty("dontcompare")) {
			RegNumber mask;
			if (pList.hasTrueProperty("dontcompare"))  // entire field is dontcompare
			   mask = new RegNumber(getFieldWidth() + "'b" + MsgUtils.repeat('1', getFieldWidth()));
			else
			   mask = new RegNumber(pList.getProperty("dontcompare"));
			if (mask.isDefined()) {
				setDontCompare(true);
				setDontCompareMask(mask);
			}
		}
		
		// set rtl coverage gen boolean 
		if (pList.hasTrueProperty("rtl_coverage")) setRtlCoverage(true);

		// set bit operator outputs
		if (pList.hasTrueProperty("anded")) setAnded(true);
		if (pList.hasTrueProperty("ored")) setOred(true);
		if (pList.hasTrueProperty("xored")) setXored(true);
		
		// set sw access props
		if (pList.hasTrueProperty("swacc")) setSwAcc(true);
		if (pList.hasTrueProperty("swmod")) setSwMod(true);
		
		// set singlepulse property
		if (pList.hasTrueProperty("singlepulse")) setSinglePulse(true);

		// set intr parms 
		if (pList.hasTrueOrRefProperty("intr")) {  
			setInterrupt(true);
			//System.out.println("FieldProperties extractProperties: inst=" + getInstancePath() + ",intr=" + pList.getProperty("intr") );
			// if a reference signal than set
			if (!pList.hasTrueProperty("intr"))
				setRef(RhsRefType.INTR, pList.getProperty("intr"), pList.getDepth("intr"));
			
			// set the interrupt type
			if (pList.hasProperty("intrType")) {
				if ("posedge".equals(pList.getProperty("intrType"))) setIntrType(IntrType.POSEDGE);
				else if ("negedge".equals(pList.getProperty("intrType"))) setIntrType(IntrType.NEGEDGE);
				else if ("bothedge".equals(pList.getProperty("intrType"))) setIntrType(IntrType.BOTHEDGE);
				else setIntrType(IntrType.LEVEL);   // default to level intr				
			}
			else setIntrType(IntrType.LEVEL);   // default to level intr				
			// set sticky state
			if (pList.hasProperty("intrStickyType")) {
				if ("nonsticky".equals(pList.getProperty("intrStickyType"))) setIntrStickyType(IntrStickyType.NONSTICKY);
				else if ("sticky".equals(pList.getProperty("intrStickyType"))) setIntrStickyType(IntrStickyType.STICKY);
				else setIntrStickyType(IntrStickyType.STICKYBIT);   // default to stickybit intr				
			}
			else setIntrStickyType(IntrStickyType.STICKYBIT);   // default to stickybit intr				

			// set interrupt enable/mask reference 
			if (pList.hasProperty("enable")) {
				setRef(RhsRefType.INTR_ENABLE, pList.getProperty("enable"), pList.getDepth("enable")); 
			}
			else if (pList.hasProperty("mask")) {
				setRef(RhsRefType.INTR_MASK, pList.getProperty("mask"), pList.getDepth("mask"));  
			}
			
			// set bits in addition to output intr using enable/mask
			if (pList.hasTrueProperty("maskintrbits")) setMaskIntrBits(true);  

			// if field has halt output
			if (pList.hasProperty("halt")) {
				setHalt(true);
				//System.out.println("FieldProperties extractProperties: inst=" + getInstancePath() + ",halt=" + pList.getProperty("halt") );
				// set halt enable/mask reference 
				if (pList.hasProperty("haltenable")) {
					setRef(RhsRefType.HALT_ENABLE, pList.getProperty("haltenable"), pList.getDepth("haltenable"));  
				}
				else if (pList.hasProperty("haltmask")) {
					setRef(RhsRefType.HALT_MASK, pList.getProperty("haltmask"), pList.getDepth("haltmask"));  
				}
			}
		}
		else if (pList.hasProperty("halt")) {
			MsgUtils.warnMessage("halt property ignored on non-interrupt field " + getId());
		}
		
		// set other sw read/write properties (these override sw= setting)
		if (pList.hasTrueProperty("rclr")) {
			setSwReadable(true);
			setRclr(true);
		}
		else if (pList.hasTrueProperty("rset")) {
			setSwReadable(true);
			setRset(true);
		}	
		if (pList.hasTrueProperty("woclr")) {
			setSwWriteable(true);
			setWoclr(true);
		}
		else if (pList.hasTrueProperty("woset")) {
			setSwWriteable(true);
			setWoset(true);
		}	
		
		// counter parm extract
		if (pList.hasTrueProperty("counter") || pList.hasProperty("incr") || pList.hasProperty("decr"))  {
			setCounter(true);
			
			boolean incrSizeDefined=false;
			boolean decrSizeDefined=false;
			
			// extract incr reference if supplied
			if (pList.hasProperty("incr") && !pList.hasBooleanProperty("incr")) {  
				setRef(RhsRefType.INCR, pList.getProperty("incr"), pList.getDepth("incr")); 
			}
			
			// extract decr reference if supplied
			if (pList.hasProperty("decr") && !pList.hasBooleanProperty("decr")) {  
				setRef(RhsRefType.DECR, pList.getProperty("decr"), pList.getDepth("decr")); 
			}
			
			// extract incr value settings
			if (pList.hasProperty("incrwidth")) {  // external input
				setIncrWidth(Integer.valueOf(pList.getProperty("incrwidth"))); 
				incrSizeDefined=true;
			}
			else if (pList.hasProperty("incrvalue")) {
			   RegNumber regNum = new RegNumber(pList.getProperty("incrvalue"));
			   if (regNum.isDefined()) setIncrValue(regNum);   // assignment of value
			   else setRef(RhsRefType.INCR_VALUE, pList.getProperty("incrvalue"), pList.getDepth("incrvalue"));  // assignment by reference
			   incrSizeDefined=true;
			}
			
			// extract decr value settings
			if (pList.hasProperty("decrwidth")) {  // external input
				setDecrWidth(Integer.valueOf(pList.getProperty("decrwidth"))); 
				decrSizeDefined=true;
			}
			else if (pList.hasProperty("decrvalue")) {
			   RegNumber regNum = new RegNumber(pList.getProperty("decrvalue"));
			   if (regNum.isDefined()) setDecrValue(regNum);   // assignment of value
			   else setRef(RhsRefType.DECR_VALUE, pList.getProperty("decrvalue"), pList.getDepth("decrvalue"));  // assignment by reference
			   decrSizeDefined=true;
			}

			// if no count value is specified default to increment by 1 (non-null also sets incr state)
			boolean notADecrCounter = pList.hasTrueProperty("counter") && !(pList.hasProperty("decr") || decrSizeDefined); 
			if ( !incrSizeDefined && (pList.hasProperty("incr") || notADecrCounter))  // also default to incr if no incr/decr is specified
				setIncrValue(new RegNumber("1'b1"));   // use 1 as default value

			// if no count value is specified default to decrement by 1 (non-null also sets decr state)
			if ( pList.hasProperty("decr") && !decrSizeDefined ) 
				setDecrValue(new RegNumber("1'b1"));   // use 1 as default value
			
			// extract incr saturate settings
			if (pList.hasProperty("saturate")) pList.copyProperty("saturate", "incrsaturate");  // handle saturate alias
			
			if (pList.hasProperty("incrsaturate")) {
				if (pList.hasTrueProperty("incrsaturate")) {
					setIncrSatValue(new RegNumber(fieldWidth + "'b" + MsgUtils.repeat('1', fieldWidth))); // default to max count
				}
				else {
					RegNumber regNum = new RegNumber(pList.getProperty("incrsaturate"));
					if (regNum.isDefined()) {
						regNum.setVectorLen(fieldWidth);
						setIncrSatValue(regNum);   // assignment of value
					}
					else setRef(RhsRefType.INCR_SAT_VALUE, pList.getProperty("incrsaturate"), pList.getDepth("incrsaturate"));  // assignment by reference
				}
			}
			
			// extract decr saturate settings
			if (pList.hasProperty("decrsaturate")) {
				if (pList.hasTrueProperty("decrsaturate")) setDecrSatValue(new RegNumber(fieldWidth + "'b1")); // default to 1
				else {
					RegNumber regNum = new RegNumber(pList.getProperty("decrsaturate"));
					if (regNum.isDefined()) {
						regNum.setVectorLen(fieldWidth);
						setDecrSatValue(regNum);   // assignment of value
					}
					else setRef(RhsRefType.DECR_SAT_VALUE, pList.getProperty("decrsaturate"), pList.getDepth("decrsaturate"));  // assignment by reference
				}
			}
			
			// extract incr threshold settings
			if (pList.hasProperty("threshold")) pList.copyProperty("threshold", "incrthreshold");  // handle threshold alias
			if (pList.hasProperty("incrthreshold")) {
				if (pList.hasTrueProperty("incrthreshold")) setIncrTholdValue(new RegNumber(fieldWidth + "'b" + MsgUtils.repeat('1', fieldWidth))); // default to max count
				else {
					RegNumber regNum = new RegNumber(pList.getProperty("incrthreshold"));
					if (regNum.isDefined()) {
						regNum.setVectorLen(fieldWidth);
						setIncrTholdValue(regNum);   // assignment of value
					}
					else setRef(RhsRefType.INCR_THOLD_VALUE, pList.getProperty("incrthreshold"), pList.getDepth("incrthreshold"));  // assignment by reference
				}
			}
			
			// extract decr threshold settings
			if (pList.hasProperty("decrthreshold")) {
				if (pList.hasTrueProperty("decrthreshold")) setDecrTholdValue(new RegNumber(fieldWidth + "'b1")); // default to 1
				else {
					RegNumber regNum = new RegNumber(pList.getProperty("decrthreshold"));
					if (regNum.isDefined()) {
						regNum.setVectorLen(fieldWidth);
						setDecrTholdValue(regNum);   // assignment of value
					}
					else setRef(RhsRefType.DECR_THOLD_VALUE, pList.getProperty("decrthreshold"), pList.getDepth("decrthreshold"));  // assignment by reference
				}
			}
			
			// extract over/underflow output gen settings
			if (pList.hasTrueProperty("overflow")) setHasOverflow(true);
			if (pList.hasTrueProperty("underflow")) setHasUnderflow(true);
			if (pList.hasTrueProperty("satoutput")) setHasSaturateOutputs(true);
		}  // is counter

		// set precedence
		if (pList.hasProperty("precedence")) {
			if (pList.getProperty("precedence").equals("hw")) setHwPrecedence(true);   
			else if (pList.getProperty("precedence").equals("sw")) setHwPrecedence(false);   
		}
		//else if ((isCounter() || isInterrupt())) setHwPrecedence(true); // give hw precedence if a counter or interrupt
		else if (isCounter()) setHwPrecedence(true); // give hw precedence if a counter
		else setHwPrecedence(false);  // default to sw precedence
		
		// set next value specified
		if (pList.hasProperty("next")) {
			setRef(RhsRefType.NEXT, pList.getProperty("next"), pList.getDepth("next"));   // save raw signal name, convert in output gen
			//System.out.println("FieldProperties extractProperties: inst=" + getInstancePath() + ", next=" + pList.getProperty("next") + ", depth=" + pList.getDepth("next"));
			//System.err.println("** FieldProperties: next=" + pList.getProperty("next"));
			//System.err.println("   FieldProperties: intr=" + pList.getProperty("intr"));
			//System.err.println("   FieldProperties: incr=" + pList.getProperty("incr"));
		}

		//System.out.println();
	}
    
	/** extract a PropertyList of user defined parameters for this instance */
    @Override
	protected void extractSpecialPropertyLists(PropertyList pList) {
		setUserDefinedProperties(pList, DefinedProperties.userDefFieldPropertyNames);
		setJsPassthruProperties(pList, DefinedProperties.jsPassthruFieldPropertyNames);
	}

	/** get reset value */
	public RegNumber getReset() {
		return reset;
	}

	/** set reset value	 */
	public void setReset(RegNumber reset) {
		this.reset = reset;
	}

	/** returns true if field has a reset value defined */
	public boolean hasReset() {
		return ((getReset() != null) || hasRef(RhsRefType.RESET_VALUE));
	}
	
	/** returns true if at least one subcategory is set */
	public boolean hasSubCategory() {
		return subcategory.hasValue();
	}

	/** returns true if specified subcategory string is set  */
	public boolean hasSubCategory(String string) {
		return subcategory.hasValue(string);
	}

	/** get subcategory */
	public JspecSubCategory getSubCategory() {
		return subcategory;
	}

	/** set subcategory */
	public void setSubCategory(String subcategory) {
		this.subcategory = new JspecSubCategory(subcategory);
	}

	// -------------------------- field width/array display methods ------------------------------

	/** get fieldWidth
	 *  @return the fieldWidth
	 */
	public Integer getFieldWidth() {
		return fieldWidth;
	}

	/** set fieldwidth from inst/comp properties
	 *  @param instance
	 */
	public void setFieldWidth(ModIndexedInstance fldInst) {
		ModComponent regComp = fldInst.getRegComp();  // get the component of this instance
		// otherwise look for width set in instance 
		if (fldInst.getWidth() != null)   
			this.fieldWidth = fldInst.getWidth(); 
		// if instance prop is set then use it
		else if (fldInst.hasProperty("fieldwidth")) 
			this.fieldWidth = fldInst.getIntegerProperty("fieldwidth"); 
		// otherwise look for a fieldwidth set in component
		else if ((regComp != null) && (regComp.hasProperty("fieldwidth"))) {
			this.fieldWidth = regComp.getIntegerProperty("fieldwidth");
		}
		else this.fieldWidth = 1;
	}

	/** get field array string with low range index starting at 0 (for defines)
	 *  @param instance
	 */
	public String getBaseFieldArrayString() {
		String retStr = "";
		// if a scalar then return empty string
		int width = getFieldWidth();
		if (width < 2) retStr = "";
		// if no explicit bit locations
		else retStr = " [" + (getFieldWidth() - 1) + ":0] ";
		return retStr;
	}

	/** get fieldArrayString
	 *  @return the fieldArrayString
	 */
	public String getFieldArrayString() {
		return fieldArrayString;
	}

	/** set fieldArrayString
	 *  @param fieldArrayString the fieldArrayString to set
	 */
	public void setFieldArrayString(String fieldArrayString) {
		this.fieldArrayString = fieldArrayString;
	}

	/** get lowIndex
	 *  @return the lowIndex
	 */
	public Integer getLowIndex() {
		return lowIndex;
	}
	
	/** set lowIndex 
	 *  @param lowIndex the lowIndex to set
	 */
	public void setLowIndex(Integer lowIdx) {
		this.lowIndex = lowIdx;
	}

	// -------------------------- name generation methods ------------------------------

	/** get encoding
	 *  @return the encoding
	 */
	public ModEnum getEncoding() {
		return encoding;
	}

	/** set encoding
	 *  @param encoding the encoding to set
	 */
	public void setEncoding(ModEnum encoding) {
		this.encoding = encoding;
	}

	public static String getFieldRegisterName(String fieldPath, boolean addPrefix) {
		return SystemVerilogDefinedOrdtSignals.getFullName(DefSignalType.FIELD, fieldPath, addPrefix);   
	}
	
	public static String getFieldRegisterNextName(String fieldPath, boolean addPrefix) {
		return SystemVerilogDefinedOrdtSignals.getFullName(DefSignalType.FIELD_NEXT, fieldPath, addPrefix);   
	}

	// -------------------------- field property methods ------------------------------

	/** get hwPrecedence
	 *  @return the hwPrecedence
	 */
	public boolean hasHwPrecedence() {
		return hwPrecedence;
	}

	/** set hwPrecedence
	 *  @param hwPrecedence the hwPrecedence to set
	 */
	public void setHwPrecedence(boolean hwPrecedence) {
		this.hwPrecedence = hwPrecedence;
	}

	/** get isSwReadable
	 *  @return the isSwReadable
	 */
	public boolean isSwReadable() {
		return isSwReadable;
	}

	/** set isSwReadable
	 *  @param isSwReadable the isSwReadable to set
	 */
	public void setSwReadable(boolean isSwReadable) {
		this.isSwReadable = isSwReadable;
	}

	/** get isSwWriteable
	 *  @return the isSwWriteable
	 */
	public boolean isSwWriteable() {
		return isSwWriteable;
	}

	/** get swChangesValue - if sw writes or affects via read
	 *  @return the swChangesValue
	 */
	public boolean swChangesValue() {
		return isSwWriteable || isRclr() || isRset();
	}

	/** set isSwWriteable
	 *  @param isSwWriteable the isSwWriteable to set
	 */
	public void setSwWriteable(boolean isSwWriteable) {
		this.isSwWriteable = isSwWriteable;
	}

	/** get isHwReadable
	 *  @return the isHwReadable
	 */
	public boolean isHwReadable() {
		return isHwReadable;
	}

	/** set isHwReadable
	 *  @param isHwReadable the isHwReadable to set
	 */
	public void setHwReadable(boolean isHwReadable) {
		this.isHwReadable = isHwReadable;
	}

	/** set true if field is writable by hw (has write data input)  */
	public void setHwWriteable(boolean isHwWriteable) {
		this.isHwWriteable = isHwWriteable;
	}

	/** return true if field is writable by hw (has write data input)  */
	public boolean isHwWriteable() {
		return isHwWriteable;
	}
	
	/** returns true if field has a hw write control signal (we, wel, hwset, hwclr etc) */
	public boolean hasHwWriteControl() {
		return hasWriteEnableL() || hasWriteEnableH() || hasHwSet() || hasHwClr();
	}
	
	/** true if hw writes or affects the field value */
	public boolean hwChangesValue() {
		return isHwWriteable() || hasHwWriteControl() || isInterrupt() || isCounter();
	}

	/** true if readable by either sw or hw
	 */
	public boolean isReadable() {
		return isHwReadable || isSwReadable;
	}

	public boolean hasSwWriteEnableH() {
		return hasSwWriteEnableH;
	}

	public void setHasSwWriteEnableH(boolean hasSwWriteEnableH) {
		this.hasSwWriteEnableH = hasSwWriteEnableH;
	}

	public boolean hasSwWriteEnableL() {
		return hasSwWriteEnableL;
	}

	public void setHasSwWriteEnableL(boolean hasSwWriteEnableL) {
		this.hasSwWriteEnableL = hasSwWriteEnableL;
	}

	public boolean hasHwSet() {
		return hasHwSet;
	}

	public void setHasHwSet(boolean hasHwSet) {
		this.hasHwSet = hasHwSet;
	}

	public boolean hasHwClr() {
		return hasHwClr;
	}

	public void setHasHwClr(boolean hasHwClr) {
		this.hasHwClr = hasHwClr;
	}

	/** returns true if field has an active high hw write enable */
	public boolean hasWriteEnableH() {
		return hasWriteEnableH;
	}

	/** set true if field has active high hw write enable */
	private void setHasWriteEnableH(boolean hasWriteEnableH) {
		this.hasWriteEnableH = hasWriteEnableH;
	}

	/** returns true if field has an active low hw write enable */
	public boolean hasWriteEnableL() {
		return hasWriteEnableL;
	}

	/** set true if field has active low hw write enable */
	private void setHasWriteEnableL(boolean hasWriteEnableL) {
		this.hasWriteEnableL = hasWriteEnableL;
	}
	
	public boolean hasSwAcc() {
		return hasSwAcc;
	}

	public void setSwAcc(boolean hasSwAcc) {
		this.hasSwAcc = hasSwAcc;
	}

	public boolean hasSwMod() {
		return hasSwMod;
	}

	public void setSwMod(boolean hasSwMod) {
		this.hasSwMod = hasSwMod;
	}

	/** returns true if field is hw accessible (either read or write) */
	public boolean isHwAccessible() {
		return isHwReadable() || isHwWriteable();
	}

	/** return true if the specified rhs reference exists */
	public boolean hasRef(RhsRefType rType) {
		return rhsReferences.containsKey(rType);
	}

	/** return the specified rhs reference */
	public RhsReference getRef(RhsRefType rType) {
		return rhsReferences.get(rType);
	}

	/** return the rtl expression associated the specified rhs reference */
	public String getRefRtlExpression(RhsRefType rType, boolean relativePath) {
		return rhsReferences.get(rType).getReferenceName(this, relativePath);
	}

	/** save the specified rhs reference */
	private void setRef(RhsRefType rType, String rawReference, int depth) {
		// if this reference is defined at root, push into base component so names are generated correctly
		String rootInst = Ordt.getModel().getRootInstance().getId() + ".";
		if (rawReference.startsWith(rootInst)) {
			rawReference = rawReference.replaceFirst(rootInst, "");
			depth = depth - 1;
		}
		RhsReference newRef = new RhsReference(rawReference, depth);
		rhsReferences.put(rType, newRef);
	}

	/** true if read/write settings are invalid
	 */
	public boolean isInvalid() {
		return !isReadable();
	}

	/** true if read/write settings are invalid
	 */
	public boolean isWire() {
		return isHwAccessible() && !hasHwWriteControl() && (!isSwWriteable() || isSwReadable());
	}

	/** get isRclr
	 *  @return the isRclr
	 */
	public boolean isRclr() {
		return isRclr;
	}

	/** set isRclr
	 *  @param isRclr the isRclr to set
	 */
	public void setRclr(boolean isRclr) {
		this.isRclr = isRclr;
	}

	/** get isRset
	 *  @return the isRset
	 */
	public boolean isRset() {
		return isRset;
	}

	/** set isRset
	 *  @param isRset the isRset to set
	 */
	public void setRset(boolean isRset) {
		this.isRset = isRset;
	}

	/** get isWoset
	 *  @return the isWoset
	 */
	public boolean isWoset() {
		return isWoset;
	}

	/** set isWoset
	 *  @param isWoset the isWoset to set
	 */
	public void setWoset(boolean isWoset) {
		this.isWoset = isWoset;
	}

	/** get isWoclr
	 *  @return the isWoclr
	 */
	public boolean isWoclr() {
		return isWoclr;
	}

	/** set isWoclr
	 *  @param isWoclr the isWoclr to set
	 */
	public void setWoclr(boolean isWoclr) {
		this.isWoclr = isWoclr;
	}

	/** get isAnded
	 *  @return the isAnded
	 */
	public boolean isAnded() {
		return isAnded;
	}

	/** set isAnded
	 *  @param isAnded the isAnded to set
	 */
	public void setAnded(boolean isAnded) {
		this.isAnded = isAnded;
	}

	/** get isOred
	 *  @return the isOred
	 */
	public boolean isOred() {
		return isOred;
	}

	/** set isOred
	 *  @param isOred the isOred to set
	 */
	public void setOred(boolean isOred) {
		this.isOred = isOred;
	}

	/** get isXored
	 *  @return the isXored
	 */
	public boolean isXored() {
		return isXored;
	}

	/** set isXored
	 *  @param isXored the isXored to set
	 */
	public void setXored(boolean isXored) {
		this.isXored = isXored;
	}

	public boolean isSinglePulse() {
		return isSinglePulse;
	}

	public void setSinglePulse(boolean isSinglePulse) {
		this.isSinglePulse = isSinglePulse;
	}

	/** get dontTestMask
	 *  @return the dontTestMask
	 */
	public RegNumber getDontTestMask() {
		return dontTestMask;
	}

	/** set dontTestMask
	 *  @param dontTestMask the dontTestMask to set
	 */
	public void setDontTestMask(RegNumber dontTestMask) {
		this.dontTestMask = dontTestMask;
	}

	/** get dontCompareMask
	 *  @return the dontCompareMask
	 */
	public RegNumber getDontCompareMask() {
		return dontCompareMask;
	}

	/** set dontCompareMask
	 *  @param dontCompareMask the dontCompareMask to set
	 */
	public void setDontCompareMask(RegNumber dontCompareMask) {
		this.dontCompareMask = dontCompareMask;
	}

	/** get isCounter
	 *  @return the isCounter
	 */
	public boolean isCounter() {
		return isCounter;
	}
	
	/** return true if an incrementing counter
	 */
	public boolean isIncrCounter() {
		return ((getIncrWidth() != null) || (getIncrValue() != null) || hasRef(RhsRefType.INCR_VALUE));
	}
	
	/** return true if an decrementing counter
	 */
	public boolean isDecrCounter() {
		return ((getDecrWidth() != null) || (getDecrValue() != null) || hasRef(RhsRefType.DECR_VALUE));
	}
	
	/** return true if a saturating incr counter
	 */
	public boolean isIncrSatCounter() {
		return ((getIncrSatValue() != null) || hasRef(RhsRefType.INCR_SAT_VALUE) );
	}
	
	/** return true if a saturating decr counter
	 */
	public boolean isDecrSatCounter() {
		return ((getDecrSatValue() != null) || hasRef(RhsRefType.DECR_SAT_VALUE) );
	}
	
	/** return true if a threshold incr counter
	 */
	public boolean isIncrTholdCounter() {
		return ((getIncrTholdValue() != null) || hasRef(RhsRefType.INCR_THOLD_VALUE) );
	}
	
	/** return true if a threshold decr counter
	 */
	public boolean isDecrTholdCounter() {
		return ((getDecrTholdValue() != null) || hasRef(RhsRefType.DECR_THOLD_VALUE) );
	}

	/** set isCounter
	 *  @param isCounter the isCounter to set
	 */
	public void setCounter(boolean isCounter) {
		this.isCounter = isCounter;
	}

	/** get incrValue
	 *  @return the incrValue
	 */
	public RegNumber getIncrValue() {
		return incrValue;
	}

	/** set incrValue
	 *  @param incrValue the incrValue to set
	 */
	public void setIncrValue(RegNumber incrValue) {
		this.incrValue = incrValue;
	}

	/** get decrValue
	 *  @return the decrValue
	 */
	public RegNumber getDecrValue() {
		return decrValue;
	}

	/** set decrValue
	 *  @param decrValue the decrValue to set
	 */
	public void setDecrValue(RegNumber decrValue) {
		this.decrValue = decrValue;
	}

	/** get incrWidth
	 *  @return the incrWidth
	 */
	public Integer getIncrWidth() {
		return incrWidth;
	}

	/** set incrWidth
	 *  @param incrWidth the incrWidth to set
	 */
	public void setIncrWidth(Integer incrWidth) {
		this.incrWidth = incrWidth;
	}

	/** get decrWidth
	 *  @return the decrWidth
	 */
	public Integer getDecrWidth() {
		return decrWidth;
	}

	/** set decrWidth
	 *  @param decrWidth the decrWidth to set
	 */
	public void setDecrWidth(Integer decrWidth) {
		this.decrWidth = decrWidth;
	}

	/** get incrSatValue
	 *  @return the incrSatValue
	 */
	public RegNumber getIncrSatValue() {
		return incrSatValue;
	}

	/** set incrSatValue
	 *  @param incrSatValue the incrSatValue to set
	 */
	public void setIncrSatValue(RegNumber incrSatValue) {
		this.incrSatValue = incrSatValue;
	}

	/** get decrSatValue
	 *  @return the decrSatValue
	 */
	public RegNumber getDecrSatValue() {
		return decrSatValue;
	}

	/** set decrSatValue
	 *  @param decrSatValue the decrSatValue to set
	 */
	public void setDecrSatValue(RegNumber decrSatValue) {
		this.decrSatValue = decrSatValue;
	}

	/** get incrTholdValue
	 *  @return the incrTholdValue
	 */
	public RegNumber getIncrTholdValue() {
		return incrTholdValue;
	}

	/** set incrTholdValue
	 *  @param incrTholdValue the incrTholdValue to set
	 */
	public void setIncrTholdValue(RegNumber incrTholdValue) {
		this.incrTholdValue = incrTholdValue;
	}

	/** get decrTholdValue
	 *  @return the decrTholdValue
	 */
	public RegNumber getDecrTholdValue() {
		return decrTholdValue;
	}

	/** set decrTholdValue
	 *  @param decrTholdValue the decrTholdValue to set
	 */
	public void setDecrTholdValue(RegNumber decrTholdValue) {
		this.decrTholdValue = decrTholdValue;
	}

	/** return true if overflow output will be generated */
	public boolean hasOverflow() {
		return hasOverflow;
	}

	/** return true if underflow output will be generated */
	public void setHasOverflow(boolean hasOverflow) {
		this.hasOverflow = hasOverflow;
	}

	public boolean hasUnderflow() {
		return hasUnderflow;
	}

	public void setHasUnderflow(boolean hasUnderflow) {
		this.hasUnderflow = hasUnderflow;
	}

	public boolean hasSaturateOutputs() {
		return hasSaturateOutputs;
	}

	public void setHasSaturateOutputs(boolean hasSaturateOutputs) {
		this.hasSaturateOutputs = hasSaturateOutputs;
	}

	/** get isInterrupt
	 *  @return the isInterrupt
	 */
	public boolean isInterrupt() {
		return isInterrupt;
	}

	/** set isInterrupt
	 *  @param isInterrupt the isInterrupt to set
	 */
	public void setInterrupt(boolean isInterrupt) {
		this.isInterrupt = isInterrupt;
	}

	/** get intrType
	 *  @return the intrType
	 */
	public IntrType getIntrType() {
		return intrType;
	}

	/** set intrType
	 *  @param intrType the intrType to set
	 */
	public void setIntrType(IntrType intrType) {
		this.intrType = intrType;
	}

	/** get intrStickyType
	 *  @return the intrStickyType
	 */
	public IntrStickyType getIntrStickyType() {
		return intrStickyType;
	}

	/** set intrStickyType
	 *  @param intrStickyType the intrStickyType to set
	 */
	public void setIntrStickyType(IntrStickyType intrStickyType) {
		this.intrStickyType = intrStickyType;
	}
	
	/** get isHalt
	 *  @return the isHalt
	 */
	public boolean isHalt() {
		return isHalt;
	}

	/** set isHalt
	 *  @param isHalt the isHalt to set
	 */
	public void setHalt(boolean isHalt) {
		this.isHalt = isHalt;
	}

	/** get maskIntrBits
	 *  @return the maskIntrBits
	 */
	public boolean isMaskIntrBits() {
		return maskIntrBits;
	}

	/** set maskIntrBits
	 *  @param maskIntrBits the maskIntrBits to set
	 */
	public void setMaskIntrBits(boolean maskIntrBits) {
		this.maskIntrBits = maskIntrBits;
	}

	/** get fieldSetPrefixString
	 *  @return the fieldSetPrefixString
	 */
	public String getFieldSetPrefixString() {
		return fieldSetPrefixString;
	}

	/** get getPrefixedId - return Id string with jspec fieldset prefixes
	 *  @return the fieldSetPrefixString
	 */
	public String getPrefixedId() {
		return getFieldSetPrefixString() + getId();
	}

	/** set fieldSetPrefixString
	 *  @param fieldSetPrefixString the fieldSetPrefixString to set
	 */
	public void setFieldSetPrefixString(String fieldSetPrefixString) {
		this.fieldSetPrefixString = fieldSetPrefixString;
	}

	/** get extractInstance
	 *  @return the extractInstance
	 */
	@Override
	public ModIndexedInstance getExtractInstance() {
		return (ModIndexedInstance) extractInstance;
	}

	/** return true if rtl coverage is specified for this field */
	public boolean generateRtlCoverage() {
		return rtlCoverage;
	}

	/** set rtl coverage setting for this field */
	private void setRtlCoverage(boolean rtlCoverage) {
		this.rtlCoverage = rtlCoverage;
		
	}

	/** hashcode/equals overrides 
	 * - ignores rhsReferences in compare
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((decrSatValue == null) ? 0 : decrSatValue.hashCode());
		result = prime * result + ((decrTholdValue == null) ? 0 : decrTholdValue.hashCode());
		result = prime * result + ((decrValue == null) ? 0 : decrValue.hashCode());
		result = prime * result + ((decrWidth == null) ? 0 : decrWidth.hashCode());
		result = prime * result + ((dontCompareMask == null) ? 0 : dontCompareMask.hashCode());
		result = prime * result + ((dontTestMask == null) ? 0 : dontTestMask.hashCode());
		//result = prime * result + ((encoding == null) ? 0 : encoding.hashCode());
		result = prime * result + ((fieldArrayString == null) ? 0 : fieldArrayString.hashCode());
		result = prime * result + ((fieldSetPrefixString == null) ? 0 : fieldSetPrefixString.hashCode());
		result = prime * result + ((fieldWidth == null) ? 0 : fieldWidth.hashCode());
		result = prime * result + (hasHwClr ? 1231 : 1237);
		result = prime * result + (hasHwSet ? 1231 : 1237);
		result = prime * result + (hasOverflow ? 1231 : 1237);
		result = prime * result + (hasSaturateOutputs ? 1231 : 1237);
		result = prime * result + (hasSwAcc ? 1231 : 1237);
		result = prime * result + (hasSwMod ? 1231 : 1237);
		result = prime * result + (hasSwWriteEnableH ? 1231 : 1237);
		result = prime * result + (hasSwWriteEnableL ? 1231 : 1237);
		result = prime * result + (hasUnderflow ? 1231 : 1237);
		result = prime * result + (hasWriteEnableH ? 1231 : 1237);
		result = prime * result + (hasWriteEnableL ? 1231 : 1237);
		result = prime * result + (hwPrecedence ? 1231 : 1237);
		result = prime * result + ((incrSatValue == null) ? 0 : incrSatValue.hashCode());
		result = prime * result + ((incrTholdValue == null) ? 0 : incrTholdValue.hashCode());
		result = prime * result + ((incrValue == null) ? 0 : incrValue.hashCode());
		result = prime * result + ((incrWidth == null) ? 0 : incrWidth.hashCode());
		result = prime * result + ((intrStickyType == null) ? 0 : intrStickyType.hashCode());
		result = prime * result + ((intrType == null) ? 0 : intrType.hashCode());
		result = prime * result + (isAnded ? 1231 : 1237);
		result = prime * result + (isCounter ? 1231 : 1237);
		result = prime * result + (isHalt ? 1231 : 1237);
		result = prime * result + (isHwReadable ? 1231 : 1237);
		result = prime * result + (isHwWriteable ? 1231 : 1237);
		result = prime * result + (isInterrupt ? 1231 : 1237);
		result = prime * result + (isOred ? 1231 : 1237);
		result = prime * result + (isRclr ? 1231 : 1237);
		result = prime * result + (isRset ? 1231 : 1237);
		result = prime * result + (isSinglePulse ? 1231 : 1237);
		result = prime * result + (isSwReadable ? 1231 : 1237);
		result = prime * result + (isSwWriteable ? 1231 : 1237);
		result = prime * result + (isWoclr ? 1231 : 1237);
		result = prime * result + (isWoset ? 1231 : 1237);
		result = prime * result + (isXored ? 1231 : 1237);
		result = prime * result + ((lowIndex == null) ? 0 : lowIndex.hashCode());
		result = prime * result + (maskIntrBits ? 1231 : 1237);
		result = prime * result + ((reset == null) ? 0 : reset.hashCode());
		result = prime * result + (rtlCoverage ? 1231 : 1237);
		result = prime * result + ((subcategory == null) ? 0 : subcategory.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		FieldProperties other = (FieldProperties) obj;
		if (decrSatValue == null) {
			if (other.decrSatValue != null)
				return false;
		} else if (!decrSatValue.equals(other.decrSatValue))
			return false;
		if (decrTholdValue == null) {
			if (other.decrTholdValue != null)
				return false;
		} else if (!decrTholdValue.equals(other.decrTholdValue))
			return false;
		if (decrValue == null) {
			if (other.decrValue != null)
				return false;
		} else if (!decrValue.equals(other.decrValue))
			return false;
		if (decrWidth == null) {
			if (other.decrWidth != null)
				return false;
		} else if (!decrWidth.equals(other.decrWidth))
			return false;
		if (dontCompareMask == null) {
			if (other.dontCompareMask != null)
				return false;
		} else if (!dontCompareMask.equals(other.dontCompareMask))
			return false;
		if (dontTestMask == null) {
			if (other.dontTestMask != null)
				return false;
		} else if (!dontTestMask.equals(other.dontTestMask))
			return false;
		//if (encoding == null) {
		//	if (other.encoding != null)
		//		return false;
		//} else if (!encoding.equals(other.encoding))
		//	return false;
		if (fieldArrayString == null) {
			if (other.fieldArrayString != null)
				return false;
		} else if (!fieldArrayString.equals(other.fieldArrayString))
			return false;
		if (fieldSetPrefixString == null) {
			if (other.fieldSetPrefixString != null)
				return false;
		} else if (!fieldSetPrefixString.equals(other.fieldSetPrefixString))
			return false;
		if (fieldWidth == null) {
			if (other.fieldWidth != null)
				return false;
		} else if (!fieldWidth.equals(other.fieldWidth))
			return false;
		if (hasHwClr != other.hasHwClr)
			return false;
		if (hasHwSet != other.hasHwSet)
			return false;
		if (hasOverflow != other.hasOverflow)
			return false;
		if (hasSaturateOutputs != other.hasSaturateOutputs)
			return false;
		if (hasSwAcc != other.hasSwAcc)
			return false;
		if (hasSwMod != other.hasSwMod)
			return false;
		if (hasSwWriteEnableH != other.hasSwWriteEnableH)
			return false;
		if (hasSwWriteEnableL != other.hasSwWriteEnableL)
			return false;
		if (hasUnderflow != other.hasUnderflow)
			return false;
		if (hasWriteEnableH != other.hasWriteEnableH)
			return false;
		if (hasWriteEnableL != other.hasWriteEnableL)
			return false;
		if (hwPrecedence != other.hwPrecedence)
			return false;
		if (incrSatValue == null) {
			if (other.incrSatValue != null)
				return false;
		} else if (!incrSatValue.equals(other.incrSatValue))
			return false;
		if (incrTholdValue == null) {
			if (other.incrTholdValue != null)
				return false;
		} else if (!incrTholdValue.equals(other.incrTholdValue))
			return false;
		if (incrValue == null) {
			if (other.incrValue != null)
				return false;
		} else if (!incrValue.equals(other.incrValue))
			return false;
		if (incrWidth == null) {
			if (other.incrWidth != null)
				return false;
		} else if (!incrWidth.equals(other.incrWidth))
			return false;
		if (intrStickyType != other.intrStickyType)
			return false;
		if (intrType != other.intrType)
			return false;
		if (isAnded != other.isAnded)
			return false;
		if (isCounter != other.isCounter)
			return false;
		if (isHalt != other.isHalt)
			return false;
		if (isHwReadable != other.isHwReadable)
			return false;
		if (isHwWriteable != other.isHwWriteable)
			return false;
		if (isInterrupt != other.isInterrupt)
			return false;
		if (isOred != other.isOred)
			return false;
		if (isRclr != other.isRclr)
			return false;
		if (isRset != other.isRset)
			return false;
		if (isSinglePulse != other.isSinglePulse)
			return false;
		if (isSwReadable != other.isSwReadable)
			return false;
		if (isSwWriteable != other.isSwWriteable)
			return false;
		if (isWoclr != other.isWoclr)
			return false;
		if (isWoset != other.isWoset)
			return false;
		if (isXored != other.isXored)
			return false;
		if (lowIndex == null) {
			if (other.lowIndex != null)
				return false;
		} else if (!lowIndex.equals(other.lowIndex))
			return false;
		if (maskIntrBits != other.maskIntrBits)
			return false;
		if (reset == null) {
			if (other.reset != null)
				return false;
		} else if (!reset.equals(other.reset))
			return false;
		if (rtlCoverage != other.rtlCoverage)
			return false;
		if (subcategory == null) {
			if (other.subcategory != null)
				return false;
		} else if (!subcategory.equals(other.subcategory))
			return false;
		return true;
	}

	
}
