/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved.
 */
package ordt.output.systemverilog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ordt.output.common.MsgUtils;
import ordt.extract.RegNumber;
import ordt.extract.RegNumber.NumBase;
import ordt.extract.RegNumber.NumFormat;
import ordt.output.FieldProperties;
import ordt.output.RhsReference;
import ordt.output.SignalProperties;
import ordt.output.systemverilog.SystemVerilogDefinedOrdtSignals.DefSignalType;
import ordt.output.systemverilog.common.SystemVerilogCoverGroups;
import ordt.output.systemverilog.common.SystemVerilogModule;
import ordt.output.systemverilog.common.SystemVerilogSignal;
import ordt.output.systemverilog.common.io.SystemVerilogIOSignalList;
import ordt.output.FieldProperties.RhsRefType;
import ordt.output.JspecSubCategory;
import ordt.output.RegProperties;
import ordt.parameters.ExtParameters;

/** derived class for logic module 
 *  note: this class is tightly coupled with builder - uses several builder methods
 *  
 * signal resolution flow:
 *    - RdlModel extractor flags all lhs instance ref assignments w/o a property deref as new signalAssign property
 *          extractor also pre-computes list of user-defined signals and exposes isUserDefinedSignal method
 *    - in generate, when signal encountered, addSignal is called in SVBuilder
 *          at this point signalProperties is updated and signal assignment has been extracted via setAssignExpression in extractProperties
 *          signalProperties is added to module-specific userDefinedSignals using sig_* prefix string name as key 
 *          if an assignment is found, output port strings are created
 *             for each rhs reference, saveUserSignalInfo is called, addRhsSignal is called
 *                saveUserSignalInfo - if in userDefinedSignals, converts to name to sig_* form and marks the sig as a rhs reference
 *                addRhsSignal - adds signal to list of rhs signals
 *          if no assignment an input port is generated
 *    - after generate, createSignalAssigns is called, which resolves all rhs signals in assign rhs
 *        isRhsReference is called to determine if a signal is unused
 *    - resolveRhsExpression is called by WriteStatements routines << FIXME problem if lhs property reference is prior to signal def

 */
public class SystemVerilogLogicModule extends SystemVerilogModule {
	private HashMap<String, SignalProperties> userDefinedSignals = new HashMap<String, SignalProperties>();  // all user defined signals in current addrmap module / only valid after generate
	private HashMap<String, RhsReferenceInfo> rhsSignals = new HashMap<String, RhsReferenceInfo>();  // all right hand side assignment references in module (used to create usable error messages)

	private FieldProperties fieldProperties;
	private RegProperties regProperties;
	protected SystemVerilogBuilder builder;  // builder creating this module
	
	protected List<IntrDiagInfo> intrInfoList = new ArrayList<IntrDiagInfo>();  // saved list of interrupt signal info for diagnostic module gen
	
	public SystemVerilogLogicModule(SystemVerilogBuilder builder, int insideLocs, String defaultClkName) {
		super(builder, insideLocs, defaultClkName, builder.getDefaultReset(), ExtParameters.sysVerUseAsyncResets());
		this.builder = builder;  // save reference to calling builder
	}
	
	// -------------- 

	/** set active instances to be used by logic generation methods */
	public void setActiveInstances(RegProperties regProperties, FieldProperties fieldProperties) {
		this.fieldProperties = fieldProperties;
		this.regProperties = regProperties;
	}

	/** add a new scalar IO signal to the hw list based on sigType */
	public void addHwScalar(DefSignalType sigType) {
		this.addHwVector(sigType, 0, 1);
	}

	/** add a new vector IO signal  to the hw list based on sigType */
	public void addHwVector(DefSignalType sigType, int lowIndex, int size) {
		SystemVerilogIOSignalList sigList = ioHash.get(SystemVerilogBuilder.HW);  // get the hw siglist
		if (sigList == null) return;
		sigList.addVector(sigType, lowIndex, size); 
	}
	
	// -------------- field logic generation methods
	
	/** generate verilog statements to write field flops */  
	void genFieldWriteStmts() {
		   // get field-specific verilog signal names
		   String hwToLogicDataName = fieldProperties.getFullSignalName(DefSignalType.H2L_DATA);  // hwBaseName + "_w" 
		   String fieldRegisterName = fieldProperties.getFullSignalName(DefSignalType.FIELD);  //"reg_" + hwBaseName;
		   
		   // if sw can write, qualify by sw we
		   if (fieldProperties.swChangesValue()) {  // if sw can write or rclr/rset
			   genFieldRegWriteStmts();    // create statements to define field registers and resets
			   genFieldNextWriteStmts();   // create statements to set value of next based on field settings
		   }
		   
		   // sw cant write so ignore sw we
		   else {
			   // hw only can write, so add write interface  
			   if (fieldProperties.hwChangesValue()) {
				   // if hw uses we or is interrupt/counter we'll need to build next
				   if (fieldProperties.hasHwWriteControl() || fieldProperties.isInterrupt() || fieldProperties.isCounter()) { 
					   genFieldRegWriteStmts();    // create statements to define field registers and resets
					   genFieldNextWriteStmts();   // create statements to set value of next based on field settings
				   }
				   // else no hw we/control sigs, so hw data value is just passed in (no register)
				   else {
					   addVectorReg(fieldRegisterName, 0, fieldProperties.getFieldWidth());  // add field register to define list
					   addHwVector(DefSignalType.H2L_DATA, 0, fieldProperties.getFieldWidth());  // add write data input
					   addCombinAssign(regProperties.getBaseName(), fieldRegisterName + " =  " + hwToLogicDataName + ";");
				   }
			   }
			   // nothing writable so assign to a constant 
			   else {
				   //System.out.println("SystemVerilogBuilder genFieldWriteStmts constant field, id=" + fieldProperties.getPrefixedId() + 
				   //	   ", writeable=" + fieldProperties.isHwWriteable() + ", intr=" + fieldProperties.isInterrupt() + ", changes val=" + fieldProperties.hwChangesValue());
				   if (fieldProperties.hasReset()) {
					   addVectorWire(fieldRegisterName, 0, fieldProperties.getFieldWidth());  // add field to wire define list
					   addWireAssign(fieldRegisterName + " = " + getResetValueString() + ";");
					   if (fieldProperties.hasRef(RhsRefType.RESET_SIGNAL))
						   MsgUtils.warnMessage("resetsignal property will have no effect in constant field " + fieldProperties.getInstancePath());
				   }
				   else MsgUtils.errorMessage("invalid field constant - no reset value defined for non-writable field " + fieldProperties.getInstancePath());
			   }
		   }
	}

	/** create statements to define field registers and resets */
	private void genFieldRegWriteStmts() {
		   String fieldRegisterName = fieldProperties.getFullSignalName(DefSignalType.FIELD);  //"reg_" + hwBaseName;
		   String fieldRegisterNextName = fieldProperties.getFullSignalName(DefSignalType.FIELD_NEXT);  //"reg_" + hwBaseName + "_next";
		   addVectorReg(fieldRegisterName, 0, fieldProperties.getFieldWidth());  // add field registers to define list
		   addVectorReg(fieldRegisterNextName, 0, fieldProperties.getFieldWidth());  // we'll be using next value since complex assign
		   String groupName = regProperties.getBaseName();
		   // generate flop reset stmts
		   if (fieldProperties.hasReset()) {
			   String resetSignalName = builder.getDefaultReset();
			   boolean resetSignalActiveLow = builder.getDefaultResetActiveLow();
			   if (fieldProperties.hasRef(RhsRefType.RESET_SIGNAL)) {
				   resetSignalActiveLow = false;  // user defined resets are active high 
				   resetSignalName = resolveRhsExpression(RhsRefType.RESET_SIGNAL);
				   groupName += " (reset=" + resetSignalName + ")";  // use a different always group for each unique resetsignal
				   //System.out.println("SystemVerilogModule genFieldRegWriteStmts: field " + fieldProperties.getId() + " has reset signal=" + resetSignalName);
				   if (!(definedSignals.contains(resetSignalName) || userDefinedSignals.containsKey(resetSignalName)))
					   MsgUtils.errorMessage("reset signal " + resetSignalName + " for field " + fieldProperties.getInstancePath() + " has not been defined");
			   }
			   else if (builder.getLogicReset() != null) {
				   resetSignalName = builder.getLogicReset();
				   resetSignalActiveLow = builder.getLogicResetActiveLow();
			   }
			   addReset(resetSignalName, resetSignalActiveLow);
			   addResetAssign(groupName, resetSignalName, fieldRegisterName + " <= " + ExtParameters.sysVerSequentialAssignDelayString() + "" + getResetValueString() + ";");  // ff reset assigns			   
		   }
		   else if (!ExtParameters.sysVerSuppressNoResetWarnings()) MsgUtils.warnMessage("field " + fieldProperties.getInstancePath() + " has no reset defined");
		   
		   addRegAssign(groupName,  fieldRegisterName + " <= " + ExtParameters.sysVerSequentialAssignDelayString() + " " + fieldRegisterNextName + ";");  // assign next to flop
	}

	/** create statements to set value of next based on field settings */ 
	private  void genFieldNextWriteStmts() {
		   // get field-specific verilog signal names
		   String hwToLogicDataName = fieldProperties.getFullSignalName(DefSignalType.H2L_DATA);  // hwBaseName + "_w" 
		   
		   String fieldRegisterName = fieldProperties.getFullSignalName(DefSignalType.FIELD);  //"reg_" + hwBaseName;  
		   String fieldRegisterNextName = fieldProperties.getFullSignalName(DefSignalType.FIELD_NEXT);  //"reg_" + hwBaseName + "_next";
		   
		   // if hw is writable add the write data input
		   if (fieldProperties.isHwWriteable()) addHwVector(DefSignalType.H2L_DATA, 0, fieldProperties.getFieldWidth());

		   // first set default next value, if hw write w/o enable use hw data
		   if (fieldProperties.isHwWriteable()  && !fieldProperties.hasHwWriteControl() && !fieldProperties.isCounter()) 
			   addCombinAssign(regProperties.getBaseName(), fieldRegisterNextName + " = " + hwToLogicDataName + ";");
		   // otherwise if a singlepulse field then default to zero value
		   else if (fieldProperties.isSinglePulse())
			   addCombinAssign(regProperties.getBaseName(), fieldRegisterNextName + " = 0;");
		   // otherwise hold current registered data
		   else 
			   addCombinAssign(regProperties.getBaseName(), fieldRegisterNextName + " = " + fieldRegisterName + ";");
		   	   
		   // set field precedence
		   boolean hwPrecedence = fieldProperties.hasHwPrecedence();
		   boolean swPrecedence = !(fieldProperties.hasHwPrecedence());
		   
		   // if a counter (special case, hw has precedence in counters by default)  
		   if (fieldProperties.isCounter()) {
			   genCounterWriteStmts(hwPrecedence);
		   }
		   
		   // if hw uses interrupt  
		   else if (fieldProperties.isInterrupt()) {  
			   genInterruptWriteStmts(hwPrecedence);
		   }
		   
		   // if an explicit next assignment 
		   if (fieldProperties.hasRef(RhsRefType.NEXT)) {
			   String refName = resolveRhsExpression(RhsRefType.NEXT);
			   addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, fieldRegisterNextName + " = " + refName + ";");	
		   }
		   
		   // if hw uses we
		   if (fieldProperties.hasWriteEnableH()) { 
			   String hwToLogicWeName = generateInputOrAssign(DefSignalType.H2L_WE, RhsRefType.WE, 1, false, hwPrecedence); 
			   addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, "if (" + hwToLogicWeName + ") " + fieldRegisterNextName + " = " + hwToLogicDataName + ";");				   
		   }
		   // if hw uses wel
		   else if (fieldProperties.hasWriteEnableL()) {  
			   String hwToLogicWelName = generateInputOrAssign(DefSignalType.H2L_WEL, RhsRefType.WE, 1, false, hwPrecedence); 
			   addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, "if (~" + hwToLogicWelName + ") " + fieldRegisterNextName + " = " + hwToLogicDataName + ";");				   
		   }
		   
		   // if hw has hw set
		   if (fieldProperties.hasHwSet()) { 
			   String hwToLogicHwSetName = generateInputOrAssign(DefSignalType.H2L_HWSET, RhsRefType.HW_SET, 1, false, hwPrecedence);
			   RegNumber constVal = new RegNumber(1);
			   constVal.setVectorLen(fieldProperties.getFieldWidth());
			   constVal.lshift(fieldProperties.getFieldWidth());
			   constVal.subtract(1);
			   addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, "if (" + hwToLogicHwSetName + ") " + 
			      fieldRegisterNextName + " = " + constVal.toFormat(NumBase.Hex, NumFormat.Verilog) + ";");				   
		   }
		   
		   // if hw has hw clr
		   if (fieldProperties.hasHwClr()) { 
			   String hwToLogicHwClrName = generateInputOrAssign(DefSignalType.H2L_HWCLR, RhsRefType.HW_CLR, 1, false, hwPrecedence);
			   RegNumber constVal = new RegNumber(0);
			   constVal.setVectorLen(fieldProperties.getFieldWidth());
			   addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, "if (" + hwToLogicHwClrName + ") " + 
			      fieldRegisterNextName + " = " + constVal.toFormat(NumBase.Hex, NumFormat.Verilog) + ";");				   
		   }

		   // add sw statements
		   genSwFieldNextWriteStmts(swPrecedence);    // create statements to set value of next based on sw field settings
	}

	/** if a fieldProperty signal of specified type has a rhs assignment, generate appropriate defines/assign stmts for 
	 *   an internal signal, else create an input signal.  return the resolved assign/input name or the resolved
	 *   rhs expression if createDefaultSignal is false
	 * 
	 * @param sigType - type of signal being assigned or made an input
	 * @param rType - rhs reference type
	 * @param sigWidth - width of the input/internal signal created
	 * @param createDefaultSignal - if false, do not create internal signal/assigns, just output the resolved rhs expression
	 * @param hiPrecedence - if true, combi assign of default signal will be given high priority (ignored if createDefaultSignal is false)
	 */
	private String generateInputOrAssign(DefSignalType sigType, RhsRefType rType, int sigWidth, boolean createDefaultSignal, boolean hiPrecedence) {
		String defaultName = fieldProperties.getFullSignalName(sigType);
		   // if an assigned reference signal use it rather than default 
		   if (fieldProperties.hasRef(rType)) { 
			   String refName = resolveRhsExpression(rType);
			   if (createDefaultSignal) {
				   addVectorReg(defaultName, 0, sigWidth); 
				   addPrecCombinAssign(regProperties.getBaseName(), hiPrecedence, defaultName + " = " + refName + ";");
			   }
			   else return refName;  // otherwise use new name in subsequent logic
		   }
		   // otherwise create an input
		   else 
			   addHwVector(sigType, 0, sigWidth); 
		return defaultName;
	}

	/** resolve rhs rtl expression, save in rhs signal list, and mark for post-gen name resolution.
	 *  (this method is for resolving refs defined in fieldProperties, not signalProperties) */
	private String resolveRhsExpression(RhsRefType rType) {
		String retExpression = fieldProperties.getRefRtlExpression(rType, false);   // create signal name from rhs
		if (fieldProperties.getRef(rType).isUserSignal()) retExpression = saveUserSignalInfo(retExpression);
		// detect remote signals (from another logic module) that will become a new input to current module
		boolean isRemoteSignal = !fieldProperties.getRef(rType).isSameAddrmap() && fieldProperties.getRef(rType).isRegRef();  // only halt/intr remotes allowed currently
		if (isRemoteSignal) {
			retExpression = "remote_" + retExpression;
			if (!rhsSignals.containsKey(retExpression))
				addSimpleScalarFrom(SystemVerilogBuilder.HW, retExpression);  // add remote input if not already added
		}
		addRhsSignal(retExpression, builder.getInstancePath(), fieldProperties.getRef(rType).getRawReference() );
		return retExpression;
	}

	/** create statements to set value of next based on field settings for sw interface.
	 *  save sw write statements in alternate list so they can be moved depending on hw/sw precedence of field
	 *  */ 
	void genSwFieldNextWriteStmts(boolean swPrecedence) {  
		   // set base reg, next and array names
		   String regBaseName = regProperties.getBaseName();
		   String fieldRegisterNextName = fieldProperties.getFullSignalName(DefSignalType.FIELD_NEXT);  //"reg_" + hwBaseName + "_next";
		   String fieldArrayString = fieldProperties.getFieldArrayString();  
		   
		   // override reg and next names if an aliased register
		   if (regProperties.isAlias()) {
			   regBaseName = regProperties.getAliasBaseName();
			   fieldRegisterNextName = FieldProperties.getFieldRegisterNextName(regBaseName + "_" + fieldProperties.getPrefixedId(), true);  
		   }
		   // override the next name if a counter
		   else if (fieldProperties.isCounter()) {
			   fieldRegisterNextName = fieldProperties.getFullSignalName(DefSignalType.CNTR_NEXT);
		   }
		   
		   String decodeToLogicDataName = regProperties.getFullSignalName(DefSignalType.D2L_DATA);  // write data from decoder
		   String decodeToLogicEnableName = regProperties.getFullSignalName(DefSignalType.D2L_ENABLE);  // write enable from decoder
		   String decodeToLogicWeName = regProperties.getFullSignalName(DefSignalType.D2L_WE);  // write enable from decoder
		   String decodeToLogicReName = regProperties.getFullSignalName(DefSignalType.D2L_RE);  // read enable from decoder
		   
		   // build an enable expression if swwe/swwel are used
		   String swWeStr = "";
		   if (fieldProperties.hasSwWriteEnableH()) { 
			   String hwToLogicSwWeName = generateInputOrAssign(DefSignalType.H2L_SWWE, RhsRefType.SW_WE, 1, false, swPrecedence); 
			   swWeStr = " & " + hwToLogicSwWeName;				   
		   }
		   else if (fieldProperties.hasSwWriteEnableL()) {  
			   String hwToLogicSwWelName = generateInputOrAssign(DefSignalType.H2L_SWWEL, RhsRefType.SW_WE, 1, false, swPrecedence); 
			   swWeStr = " & ~" + hwToLogicSwWelName;				   
		   }
		   
		   // build write data string qualified by enables
		   String woEnabledDataString = (!SystemVerilogBuilder.hasWriteEnables())? decodeToLogicDataName + fieldArrayString :
			   "(" + decodeToLogicDataName + fieldArrayString + " & " + decodeToLogicEnableName + fieldArrayString + ")";
		   
		   // if a sw write one to clr/set
		   if (fieldProperties.isWoset()) {
			   addPrecCombinAssign(regBaseName, swPrecedence, "if (" + decodeToLogicWeName + swWeStr + ") " + fieldRegisterNextName + " = (" + 
					   fieldRegisterNextName + " | " + woEnabledDataString + ");");				   
		   }
		   else if (fieldProperties.isWoclr()) {
			   String andValString = fieldProperties.isCounter()? "{1'b0, ~" + woEnabledDataString + "}": "~" + woEnabledDataString;
			   addPrecCombinAssign(regBaseName, swPrecedence, "if (" + decodeToLogicWeName + swWeStr + ") " + fieldRegisterNextName + " = (" + 
					   fieldRegisterNextName + " & " + andValString + ");");				   
		   }
		   // if a sw write is alowed 
		   else if (fieldProperties.isSwWriteable()) {		   
			   // build write data string qualified by enables
			   String fullEnabledDataString = (!SystemVerilogBuilder.hasWriteEnables())? decodeToLogicDataName + fieldArrayString :
				   "(" + woEnabledDataString + " | (" + fieldRegisterNextName + " & ~" + decodeToLogicEnableName + fieldArrayString + "))";
			   addPrecCombinAssign(regBaseName, swPrecedence, "if (" + decodeToLogicWeName + swWeStr + ") " + fieldRegisterNextName + " = " + fullEnabledDataString + ";");				   
		   }
			   			   
		   // if a sw read set
		   if (fieldProperties.isRset()) {
			   addPrecCombinAssign(regBaseName, swPrecedence, "if (" + decodeToLogicReName + swWeStr + ") " + fieldRegisterNextName + " = " + 
		           fieldProperties.getFieldWidth() + "'b" + MsgUtils.repeat('1', fieldProperties.getFieldWidth()) + ";");
		   }
		   // if sw rclr 
		   else if (fieldProperties.isRclr()) {
			   addPrecCombinAssign(regBaseName, swPrecedence, "if (" + decodeToLogicReName + swWeStr + ") " + fieldRegisterNextName + " = " + 
		           fieldProperties.getFieldWidth() + "'b0;");
		   }
		   
		   // if has sw access output
		   if (fieldProperties.hasSwAcc()) {
			   String logicToHwSwAccName = fieldProperties.getFullSignalName(DefSignalType.L2H_SWACC);
			   addHwScalar(DefSignalType.L2H_SWACC);   // add sw access output
			   addScalarReg(logicToHwSwAccName);  
			   addPrecCombinAssign(regBaseName, swPrecedence, logicToHwSwAccName + 
					   " = " + decodeToLogicReName + " | " + decodeToLogicWeName + ";");
		   }
		   // if has sw modify output
		   if (fieldProperties.hasSwMod()) {
			   String logicToHwSwModName = fieldProperties.getFullSignalName(DefSignalType.L2H_SWMOD);
			   addHwScalar(DefSignalType.L2H_SWMOD);   // add sw access output
			   addScalarReg(logicToHwSwModName); 
			   String readMod = (fieldProperties.isRclr() || fieldProperties.isRset())? "(" + decodeToLogicReName + " | " + decodeToLogicWeName + ")" : decodeToLogicWeName;
			   addPrecCombinAssign(regBaseName, swPrecedence, logicToHwSwModName + " = " + readMod + swWeStr + ";");
		   }
	}

	/** write interrupt field verilog 
	 * @param hwPrecedence */   
	private void genInterruptWriteStmts(boolean hwPrecedence) {
		   String fieldRegisterName = fieldProperties.getFullSignalName(DefSignalType.FIELD);  //"reg_" + hwBaseName;  
		   String fieldRegisterNextName = fieldProperties.getFullSignalName(DefSignalType.FIELD_NEXT);  //"reg_" + hwBaseName + "_next";
		   int fieldWidth = fieldProperties.getFieldWidth();
		   
		   // always create intr diag info (only save if a leaf intr)
		   IntrDiagInfo intrInfo = new IntrDiagInfo(fieldRegisterName, fieldWidth, fieldProperties.getInstancePath(), fieldProperties.getSubCategory());
		   
		   // if register is not already interrupt, then create signal assigns and mark for output creation in finishRegister
		   String intrOutput = regProperties.getFullSignalName(DefSignalType.L2H_INTR);
		   String intrClear = regProperties.getFullSignalName(DefSignalType.INTR_CLEAR);  // interrupt clear detect signal
		   if (!regProperties.hasInterruptOutputDefined()) {
			   regProperties.setHasInterruptOutputDefined(true);
			   addScalarReg(intrOutput);
		       addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, intrOutput + " = 1'b0;");  // default to intr off
		       // if pulse on clear is set then add clear detect signal
		       if (ExtParameters.sysVerPulseIntrOnClear()) {
				   addScalarReg(intrClear);
			       addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, intrClear + " = 1'b0;");  // default to clear off
		       }
		   }
		   
		   // if a halt field and register is not already halt, then create signal assigns and mark for output creation in finishRegister
		   String haltOutput = regProperties.getFullSignalName(DefSignalType.L2H_HALT);
		   if (fieldProperties.isHalt() && !regProperties.hasHaltOutputDefined()) {
			   regProperties.setHasHaltOutputDefined(true);
			   addScalarReg(haltOutput);
		       addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, haltOutput + " = 1'b0;");  // default to halt off
		   }

		   // if a input intr reference then assign else create an input with width of field  
		   String hwToLogicIntrName = fieldProperties.getFullSignalName(DefSignalType.H2L_INTR);  // hwBaseName + "_intr" 	   
		   if (fieldProperties.hasRef(RhsRefType.INTR)) {  //  intr assign not allowed by rdl1.0 spec, but allow for addl options vs next
			   //System.out.println("SystemVerilogBuilder genInterruptWriteStmts: " + fieldProperties.getInstancePath() + " has an intr reference, raw=" + fieldProperties.getIntrRef().getRawReference());
			   addVectorReg(hwToLogicIntrName, 0, fieldProperties.getFieldWidth());
			   String refName = resolveRhsExpression(RhsRefType.INTR);
			   addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, hwToLogicIntrName  +  " = " + refName + ";");
		   }
		   // otherwise, if next property isn't set then add an intr input
		   else if (!fieldProperties.hasRef(RhsRefType.NEXT)) {
			   addHwVector(DefSignalType.H2L_INTR, 0, fieldProperties.getFieldWidth());   // add hw interrupt input
			   addVectorWire(hwToLogicIntrName, 0, fieldProperties.getFieldWidth());
			   intrInfoList.add(intrInfo);  // this is a leaf interrupt, so add to diagnostic list
		   }

		   // if next is assigned then skip all the intr-specific next generation
		   String intrOutputModifier = "";
		   if (!fieldProperties.hasRef(RhsRefType.NEXT)) {
				   
			   // create mask/enable output and bit modifier if specified
			   String intrBitModifier = "";
			   if (fieldProperties.hasRef(RhsRefType.INTR_ENABLE)) {
				   String refName = resolveRhsExpression(RhsRefType.INTR_ENABLE);
				   if (fieldProperties.isMaskIntrBits()) intrBitModifier = " & " + refName;
				   else intrOutputModifier = " & " + refName;
				   intrInfo.setIntrEnableInfo(true, refName);  // save signal in diag info
			   }
			   else if (fieldProperties.hasRef(RhsRefType.INTR_MASK)) {
				   String refName = resolveRhsExpression(RhsRefType.INTR_MASK);
				   if (fieldProperties.isMaskIntrBits()) intrBitModifier = " & ~" + refName;
				   else intrOutputModifier = " & ~" + refName;
				   intrInfo.setIntrEnableInfo(false, refName);  // save signal in diag info
			   }
			   
			   // create intr detect based on intrType (level, posedge, negedge, bothedge)
			   String detectStr = hwToLogicIntrName;  // default to LEVEL
			   String prevIntrName = fieldProperties.getFullSignalName(DefSignalType.PREVINTR);  // hwBaseName + "_previntr" 	   
			   // if not LEVEL, need to store previous intr value
			   if (fieldProperties.getIntrType() != FieldProperties.IntrType.LEVEL) {
				   addVectorReg(prevIntrName, 0, fieldProperties.getFieldWidth());
				   addRegAssign(regProperties.getBaseName(), prevIntrName  +  " <= " + ExtParameters.sysVerSequentialAssignDelayString() + "" + hwToLogicIntrName + ";");
				   // if posedge detect
				   if (fieldProperties.getIntrType() == FieldProperties.IntrType.POSEDGE) 
					   detectStr = "(" + hwToLogicIntrName + " & ~" + prevIntrName + ")";
				   else if (fieldProperties.getIntrType() == FieldProperties.IntrType.NEGEDGE) 
					   detectStr = "(" + prevIntrName + " & ~" + hwToLogicIntrName + ")";
				   else // BOTHEDGE detect  
					   detectStr = "(" + hwToLogicIntrName + " ^ " + prevIntrName + ")";
		   }
		   
		   // assign field based on detect and intrStickyype (nonsticky, sticky, stickybit)  
		   if (fieldProperties.getIntrStickyType() == FieldProperties.IntrStickyType.NONSTICKY) 
		      addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, fieldRegisterNextName + " = " + detectStr + intrBitModifier + ";");
		   else if (fieldProperties.getIntrStickyType() == FieldProperties.IntrStickyType.STICKY) 
			  addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, "if (" + detectStr + " != " + fieldWidth + "'b0) " +
		                         fieldRegisterNextName +  " = " + detectStr + intrBitModifier + ";");	
		   else // STICKYBIT default 
			  addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, fieldRegisterNextName + " = (" + detectStr + " | " +
		                         fieldRegisterName + ")" + intrBitModifier + ";");
		   }

		   // if an enable/mask then gate interrupt output with this signal
		   String orStr = " | (";  String endStr = ");";
		   if (fieldWidth > 1) {
			   orStr = " | ( | (";  endStr = "));";  // use or reduction
		   }
	       addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, intrOutput + " = " + intrOutput  + orStr + fieldRegisterName + intrOutputModifier + endStr);
	       // if pulse on clear is set then create delayed intr value and add to clear detect signal
	       if (ExtParameters.sysVerPulseIntrOnClear()) {
			   String intrDlyName = fieldProperties.getFullSignalName(DefSignalType.INTR_DLY); 	   
			   addVectorReg(intrDlyName, 0, fieldProperties.getFieldWidth());
			   addRegAssign(regProperties.getBaseName(), intrDlyName  +  " <= " + ExtParameters.sysVerSequentialAssignDelayString() + "" + fieldRegisterName + ";");
		       addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, intrClear + " = " + intrClear  + orStr + intrDlyName + " & ~" + fieldRegisterName + endStr);  // negedge detect
	       }

		   // if an enable/mask then gate halt output with this signal
		   if (fieldProperties.hasRef(RhsRefType.HALT_ENABLE)) {
			   String refName = resolveRhsExpression(RhsRefType.HALT_ENABLE);
		       addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, haltOutput + " = " + haltOutput  + orStr + fieldRegisterName + " & " + refName + endStr);
			   intrInfo.setIntrHaltInfo(true, refName);  // save signal in diag info
		   }
		   else if (fieldProperties.hasRef(RhsRefType.HALT_MASK)) {
			   String refName = resolveRhsExpression(RhsRefType.HALT_MASK);
		       addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, haltOutput + " = " + haltOutput  + orStr + fieldRegisterName + " & ~" + refName + endStr);
			   intrInfo.setIntrHaltInfo(false, refName);  // save signal in diag info
		   }
		   else if (fieldProperties.isHalt())
		       addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, haltOutput + " = " + haltOutput + orStr + fieldRegisterName + endStr);
	}
	
	/** write counter field verilog 
	 * @param hwPrecedence */   
	private void genCounterWriteStmts(boolean hwPrecedence) {
		   // get field-specific verilog signal names
		   String hwToLogicIncrName = fieldProperties.getFullSignalName(DefSignalType.H2L_INCR);  // hwBaseName + "_incr" 
		   String logicToHwOverflowName = fieldProperties.getFullSignalName(DefSignalType.L2H_OVERFLOW);  // hwBaseName + "_overflow" 
		   
		   String hwToLogicDecrName = fieldProperties.getFullSignalName(DefSignalType.H2L_DECR);  // hwBaseName + "_decr" 
		   String logicToHwUnderflowName = fieldProperties.getFullSignalName(DefSignalType.L2H_UNDERFLOW);  // hwBaseName + "_underflow" 
		   String nextCountName = fieldProperties.getFullSignalName(DefSignalType.CNTR_NEXT);   
		   
		   String logicToHwIncrSatName = fieldProperties.getFullSignalName(DefSignalType.L2H_INCRSAT);  // hwBaseName + "_incrsat_o" 
		   String logicToHwIncrTholdName = fieldProperties.getFullSignalName(DefSignalType.L2H_INCRTHOLD);  // hwBaseName + "_incrthold_o" 
		   String logicToHwDecrSatName = fieldProperties.getFullSignalName(DefSignalType.L2H_DECRSAT);  // hwBaseName + "_decrsat_o" 
		   String logicToHwDecrTholdName = fieldProperties.getFullSignalName(DefSignalType.L2H_DECRTHOLD);  // hwBaseName + "_decrthold_o" 

		   String fieldRegisterName = fieldProperties.getFullSignalName(DefSignalType.FIELD);  //"reg_" + hwBaseName;  
		   String fieldRegisterNextName = fieldProperties.getFullSignalName(DefSignalType.FIELD_NEXT);  //"reg_" + hwBaseName + "_next";
		   
		   int fieldWidth = fieldProperties.getFieldWidth();
		   int countWidth = fieldWidth + 1;  // add a bit for over/underflow
		   
		   // create the next count value
		   addVectorReg(nextCountName, 0, countWidth);  
		   addCombinAssign(regProperties.getBaseName(), nextCountName + " = { 1'b0, " + fieldRegisterName + "};");  // no precedence - this stmt goes first  
		   
		   // if an incr is specified
		   if (fieldProperties.isIncrCounter()) {
			   //if (fieldProperties.getInstancePath().contains("reg4")) System.out.println("SystemVerilogBuilder genCounterWriteStmts: " + fieldProperties.getInstancePath() + " is an incr counter");

			   // add overflow output
			   if (fieldProperties.hasOverflow()) {
				   addHwScalar(DefSignalType.L2H_OVERFLOW);   // add hw overflow output
				   addScalarReg(logicToHwOverflowName);  
				   addRegAssign(regProperties.getBaseName(), logicToHwOverflowName +
						   " <= " + ExtParameters.sysVerSequentialAssignDelayString() + "" + nextCountName + "[" + fieldWidth + "] & ~" +  logicToHwOverflowName + ";");  // only active for one cycle  
			   }

			   // if a ref is being used for increment assign it, else add an input
			   //System.out.println("SystemVerilogBuilder genCounterWriteStmts: " + fieldProperties.getInstancePath() + " is an incr counter, hasIncrRef=" + fieldProperties.hasIncrRef());
			   generateInputOrAssign(DefSignalType.H2L_INCR, RhsRefType.INCR, 1, true, hwPrecedence); 

			   // create incr value from reference, constant, or input
			   String incrValueString =getCountIncrValueString(countWidth);
			   
			   // increment the count
			   addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, "if (" + hwToLogicIncrName + ")");
			   addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, "   " + nextCountName + " = "  + nextCountName + " + " + incrValueString + ";");
		   }
		   
		   // if a decr is specified
		   if (fieldProperties.isDecrCounter()) {
			   
			   // add underflow output
			   if (fieldProperties.hasUnderflow()) {
				   addHwScalar(DefSignalType.L2H_UNDERFLOW);   // add hw underflow output
				   addScalarReg(logicToHwUnderflowName);  
				   addRegAssign(regProperties.getBaseName(), logicToHwUnderflowName +
						   " <= " + ExtParameters.sysVerSequentialAssignDelayString() + "" + nextCountName + "[" + fieldWidth + "] & ~" +  logicToHwUnderflowName + ";");  // only active for one cycle  
			   }

			   // if a ref is being used for decrement assign it, else add an input
			   generateInputOrAssign(DefSignalType.H2L_DECR, RhsRefType.DECR, 1, true, hwPrecedence); 

			   // create decr value from reference, constant, or input
			   String decrValueString =getCountDecrValueString(countWidth);
			   
			   // decrement the count
			   addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, "if (" + hwToLogicDecrName + ")");
			   addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, "   " + nextCountName + " = "  + nextCountName + " - " + decrValueString + ";");
		   }
		   
		   // if a incr saturating counter add checks
		   if (fieldProperties.isIncrSatCounter()) {
			   String incrSatValueString = "0";
			   
			   // set saturate value from constant or ref
			   if (fieldProperties.hasRef(RhsRefType.INCR_SAT_VALUE)) {  // if a reference is specified
				   incrSatValueString = resolveRhsExpression(RhsRefType.INCR_SAT_VALUE);
			   }
			   else {  // otherwise a constant
				   RegNumber regNum = fieldProperties.getIncrSatValue();
				   regNum.setVectorLen(countWidth);
				   incrSatValueString = regNum.toString();
			   }
			   // limit next count to value of saturate
			   addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, "if (" + nextCountName + " > " + incrSatValueString + ")");
			   addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, "   " + nextCountName + " = "  + incrSatValueString + ";");
			   // add incrsat output
			   if (fieldProperties.hasSaturateOutputs()) addHwScalar(DefSignalType.L2H_INCRSAT);   // add hw incrsaturate output
			   addScalarReg(logicToHwIncrSatName);  
			   addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, logicToHwIncrSatName + " = ( {1'b0, " + fieldRegisterName + "} == " + incrSatValueString + ");");
		   }
		   
		   // check for incrthreshold
		   if (fieldProperties.isIncrTholdCounter()) {
			   String incrTholdValueString = "0";
			   
			   // set saturate value from constant or ref
			   if (fieldProperties.hasRef(RhsRefType.INCR_THOLD_VALUE)) {  // if a reference is specified
				   incrTholdValueString = resolveRhsExpression(RhsRefType.INCR_THOLD_VALUE);
			   }
			   else {  // otherwise a constant
				   RegNumber regNum = fieldProperties.getIncrTholdValue();
				   regNum.setVectorLen(countWidth);
				   if (countWidth > 7) regNum.setNumBase(RegNumber.NumBase.Hex);
				   incrTholdValueString = regNum.toString();
			   }
			   // add incrthold output
			   addHwScalar(DefSignalType.L2H_INCRTHOLD);   // add hw incrthreshold output
			   addScalarReg(logicToHwIncrTholdName);  
			   addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, logicToHwIncrTholdName + " = ( {1'b0, " + fieldRegisterName + "} == " + incrTholdValueString + ");");
		   }
		   
		   // if a decr saturating counter add checks
		   if (fieldProperties.isDecrSatCounter()) {
			   String decrSatValueString = "0";
			   
			   // set saturate value from constant or ref
			   if (fieldProperties.hasRef(RhsRefType.DECR_SAT_VALUE) ) {  // if a reference is specified
				   decrSatValueString = resolveRhsExpression(RhsRefType.DECR_SAT_VALUE);
			   }
			   else {  // otherwise a constant
				   RegNumber regNum = fieldProperties.getDecrSatValue();
				   regNum.setVectorLen(countWidth);
				   if (countWidth > 7) regNum.setNumBase(RegNumber.NumBase.Hex);
				   decrSatValueString = regNum.toString();
			   }
			   // limit next count to value of saturate
			   addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, "if (" + nextCountName + " < " + decrSatValueString + ")");
			   addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, "   " + nextCountName + " = "  + decrSatValueString + ";");
			   // add decrsat output
			   if (fieldProperties.hasSaturateOutputs()) addHwScalar(DefSignalType.L2H_DECRSAT);   // add hw decrsaturate output
			   addScalarReg(logicToHwDecrSatName);  
			   addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, logicToHwDecrSatName + " = ( {1'b0, " + fieldRegisterName + "} == " + decrSatValueString + ");");
		   }
		   
		   // check for decrthreshold
		   if (fieldProperties.isDecrTholdCounter()) {
			   String decrTholdValueString = "0";
			   
			   // set saturate value from constant or ref
			   if (fieldProperties.hasRef(RhsRefType.DECR_THOLD_VALUE)) {  // if a reference is specified
				   decrTholdValueString = resolveRhsExpression(RhsRefType.DECR_THOLD_VALUE);
			   }
			   else {  // otherwise a constant
				   RegNumber regNum = fieldProperties.getDecrTholdValue();
				   regNum.setVectorLen(countWidth);
				   if (countWidth > 7) regNum.setNumBase(RegNumber.NumBase.Hex);
				   decrTholdValueString = regNum.toString();
			   }
			   // add decrthold output
			   addHwScalar(DefSignalType.L2H_DECRTHOLD);   // add hw decrthreshold output
			   addScalarReg(logicToHwDecrTholdName);  
			   addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, logicToHwDecrTholdName + " = ( {1'b0, " + fieldRegisterName + "} == " + decrTholdValueString + ");");
		   }
		   
		   // now assign the next count value to the reg
		   addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, fieldRegisterNextName + " = " + nextCountName + SystemVerilogSignal.genDefArrayString(0, fieldWidth) + ";");			
		
	}

	/** create reset value string */
	private String getResetValueString() {
		String resetValueString = null;
		if (fieldProperties.getReset() != null) {  // if reset value specified
			RegNumber resetValue = new RegNumber(fieldProperties.getReset());  
			resetValue.setNumFormat(RegNumber.NumFormat.Verilog);  // output reset in verilog format
			resetValueString = resetValue.toString();
		}
		else if (fieldProperties.hasRef(RhsRefType.RESET_VALUE)) {  // if a reset reference is specified
			resetValueString = resolveRhsExpression(RhsRefType.RESET_VALUE);
		}
		return resetValueString;
	}

	/** create count increment string */
	private String getCountIncrValueString(int countWidth) {
		String incrValueString = "0";
		String hwToLogicIncrValueName = fieldProperties.getFullSignalName(DefSignalType.H2L_INCRVALUE);  // hwBaseName + "_incrvalue" 
		Integer incrWidth = fieldProperties.getIncrWidth();
		if (incrWidth != null) {  // if an external input is specified
			addHwVector(DefSignalType.H2L_INCRVALUE, 0, incrWidth);   // add hw incr value input
			incrValueString = "{" + (countWidth - incrWidth) + "'b0, " + hwToLogicIncrValueName + "}";
		}
		else if (fieldProperties.hasRef(RhsRefType.INCR_VALUE)) {  // if a reference is specified
			incrValueString = resolveRhsExpression(RhsRefType.INCR_VALUE);
		}
		else {  // otherwise a constant
			RegNumber regNum = fieldProperties.getIncrValue();
			regNum.setVectorLen(countWidth);
			if (countWidth > 7) regNum.setNumBase(RegNumber.NumBase.Hex);
			incrValueString = regNum.toString();
		}
		return incrValueString;
	}
	
	
	/** create count decrement string */
	private String getCountDecrValueString(int countWidth) {
		String decrValueString = "0";
		String hwToLogicDecrValueName = fieldProperties.getFullSignalName(DefSignalType.H2L_DECRVALUE);  // hwBaseName + "_decrvalue" 
		Integer decrWidth = fieldProperties.getDecrWidth();
		if (decrWidth != null) {  // if an external input is specified
			addHwVector(DefSignalType.H2L_DECRVALUE, 0, decrWidth);   // add hw decr value input
			decrValueString = "{" + (countWidth - decrWidth) + "'b0, " + hwToLogicDecrValueName + "}";
		}
		else if (fieldProperties.hasRef(RhsRefType.DECR_VALUE)) {  // if a reference is specified
			decrValueString = resolveRhsExpression(RhsRefType.DECR_VALUE);
		}
		else {  // otherwise a constant
			RegNumber regNum = fieldProperties.getDecrValue();
			regNum.setVectorLen(countWidth);
			if (countWidth > 7) regNum.setNumBase(RegNumber.NumBase.Hex);
			decrValueString = regNum.toString();
		}
		return decrValueString;
	}

	/** generate field read statements */  
	void genFieldReadStmts() {
		   // create field-specific verilog signal names
		   String logicToHwDataName = fieldProperties.getFullSignalName(DefSignalType.L2H_DATA);  // hwBaseName + "_r" ;
		   String fieldRegisterName = fieldProperties.getFullSignalName(DefSignalType.FIELD);  //"ff_" + hwBaseName;

		   // if sw readable, set output (else return 0s)
		   genSwFieldReadStmts();
		   
		   // if hw readable, add the interface and set output
		   if (fieldProperties.isHwReadable()) {
			   // add read signals from logic to hw
			   addHwVector(DefSignalType.L2H_DATA, 0, fieldProperties.getFieldWidth());    // logic to hw list 
			   
			   // assign hw read data outputs
			   addVectorReg(logicToHwDataName, 0, fieldProperties.getFieldWidth());  // add outputs to define list since we'll use block assign
			   addCombinAssign(regProperties.getBaseName(), logicToHwDataName + " = " + fieldRegisterName + ";");  
		   }
		   
		   // if anded/ored/xored outputs specified
		   genBitwiseOutputStmts();
	}
	
	/** create bitwise outputs for this field */
	private void genBitwiseOutputStmts() {
		   // create field-specific verilog signal names
		   String logicToHwAndedName = fieldProperties.getFullSignalName(DefSignalType.L2H_ANDED);  
		   String logicToHwOredName = fieldProperties.getFullSignalName(DefSignalType.L2H_ORED);  
		   String logicToHwXoredName = fieldProperties.getFullSignalName(DefSignalType.L2H_XORED);  
		   String fieldRegisterName = fieldProperties.getFullSignalName(DefSignalType.FIELD);  
		   
		   // anded output
		   if (fieldProperties.isAnded()) {
			   addHwScalar(DefSignalType.L2H_ANDED);    // logic to hw list 
			   addScalarReg(logicToHwAndedName);  // add outputs to define list since we'll use block assign
			   addCombinAssign(regProperties.getBaseName(), logicToHwAndedName + " = & " + fieldRegisterName + ";");  			   
		   }
		   // ored output
		   if (fieldProperties.isOred()) {
			   addHwScalar(DefSignalType.L2H_ORED);    // logic to hw list 
			   addScalarReg(logicToHwOredName);  // add outputs to define list since we'll use block assign
			   addCombinAssign(regProperties.getBaseName(), logicToHwOredName + " = | " + fieldRegisterName + ";");  			   
		   }
		   // xored output
		   if (fieldProperties.isXored()) {
			   addHwScalar(DefSignalType.L2H_XORED);    // logic to hw list 
			   addScalarReg(logicToHwXoredName);  // add outputs to define list since we'll use block assign
			   addCombinAssign(regProperties.getBaseName(), logicToHwXoredName + " = ^ " + fieldRegisterName + ";");  			   
		   }	
	}

	/** generate alias register field read statements */  
	void genSwFieldReadStmts() {
		   // create field-specific verilog signal names
		   String fieldRegisterName = fieldProperties.getFullSignalName(DefSignalType.FIELD);  //"rg_" + hwBaseName;
		   String fieldArrayString = fieldProperties.getFieldArrayString(); 
		   // if an aliased register override the field name
		   if (regProperties.isAlias()) {
			   String aliasBaseName = regProperties.getAliasBaseName();
			   fieldRegisterName = FieldProperties.getFieldRegisterName(aliasBaseName + "_" + fieldProperties.getPrefixedId(), true);  //"rg_" + AliasBaseName;
		   }
		   // if sw readable, set output (else return 0s)
		   if (fieldProperties.isSwReadable()) {
			   builder.addToTempAssignList(regProperties.getFullSignalName(DefSignalType.L2D_DATA) + fieldArrayString + " = " + fieldRegisterName + ";"); // need to set unused bits to 0 after all fields added		
		   }   
	}

	// -------------- user defined signal methods

	/** save user defined signal info */
	public void addUserDefinedSignal(String rtlName, SignalProperties signalProperties) {
		//System.out.println("SystemVerilogLogicModule addUserDefinedSignal: ref=" + rtlName + ", key=" + signalProperties.getFullSignalName(DefSignalType.USR_SIGNAL));
		String sigName = signalProperties.getFullSignalName(DefSignalType.USR_SIGNAL);
		boolean setAsRhsReference = userDefinedSignals.containsKey(sigName) && (userDefinedSignals.get(sigName) == null);
		if (setAsRhsReference) signalProperties.setRhsReference(true);
		userDefinedSignals.put(sigName, signalProperties); 
	}
	
	/** store user-defined signal as rhs reference in current module and return name with modified prefix */
	public String saveUserSignalInfo(String rawName) {
		String sigName = rawName.replaceFirst("rg_", "sig_");  // convert to signal prefix
		// the local list may not have been populated, but can load with null to indicate that it's been seen on rhs of an assign
		if (!userDefinedSignals.containsKey(sigName)) {
			//System.out.println("SystemVerilogLogicModule saveUserSignalInfo: " + sigName + " was found in master list, but not in module-specific list");
			userDefinedSignals.put(sigName, null);
		}
		else {
			//if (userDefinedSignals.get(sigNameStr)==null) System.out.println("SystemVerilogLogicModule saveUserSignalInfo: marking null signal " + sigNameStr + " as rhsReference");
			if (userDefinedSignals.get(sigName)!=null) userDefinedSignals.get(sigName).setRhsReference(true);  // indicate that this signal is used internally as rhs  
		}
		return sigName;  // return signal form if found
	}
	
	/** loop through user defined signals and add assign statements to set these signals - call after build when userDefinedSignals is valid */  
	public void createSignalAssigns() {
		// first loop through signals, detect any signals on rhs, and verify each sig in rhs exists
		for (String key: userDefinedSignals.keySet()) {
			SignalProperties sig = userDefinedSignals.get(key);
			//if (sig == null) System.out.println("SystemVerilogLogicModule createSignalAssigns: null sig key=" + key);
			//else System.out.println("SystemVerilogLogicModule createSignalAssigns: sig key=" + key + ", sig id=" + sig.getId());
			// if signal in current module is assigned internally and has simple rhs, check for valid vlog define and resolve sig vs reg
			if ((sig != null) && sig.hasAssignExpr() ) {  
				// loop thru refs here... check each for well formed
				List<RhsReference> rhsRefList = sig.getAssignExpr().getRefList(); 
				for (RhsReference ref: rhsRefList) {
					//System.out.println("SystemVerilogLogicModule createSignalAssigns: sig key=" + key + ": ref=" + ref.getRawReference() + ", depth=" + ref.getDepth() + ", inst=" + ref.getInstancePath());
					if (ref.isWellFormedSignalName()) {
						String refName = ref.getReferenceName(sig, false); 
						//System.out.println("SystemVerilogLogicModule createSignalAssigns: sig prop inst=" + sig.getInstancePath() + ", refName=" + refName);
						if (ref.isUserSignal()) refName = saveUserSignalInfo(refName);  // tag any signals as rhsReference
						// check for a valid signal
						if (!this.hasDefinedSignal(refName) && (rhsSignals.containsKey(refName))) {  
							RhsReferenceInfo rInfo = rhsSignals.get(refName);
							//System.out.println("SystemVerilogLogicModule createSignalAssigns: refName=" + refName + ", hasDefinedSignal=" + this.hasDefinedSignal(refName) + ", rhsSignals.containsKey=" + rhsSignals.containsKey(refName));
							MsgUtils.errorMessage("unable to resolve " + rInfo.getRhsRefString() + " referenced in rhs dynamic property assignment for " + rInfo.getLhsInstance()); 
						}						
					}
				}
			}
		}
		// now that rhs references have been detected, create assigns and detect unused signals
		for (String key: userDefinedSignals.keySet()) {
			SignalProperties sig = userDefinedSignals.get(key);
			//System.out.println("SystemVerilogLogicModule createSignalAssigns: signal key=" + key + ", isRhs=" + sig.isRhsReference());
			// if signal is assigned internally add an assign else an input
			if (sig != null) {
				if (sig.hasAssignExpr()) {
					//System.out.println("SystemVerilogLogicModule createSignalAssigns: raw expr=" + sig.getAssignExpr().getRawExpression() + ", res expr=" + sig.getAssignExpr().getResolvedExpression(sig, userDefinedSignals));
					String rhsSigExpression = sig.getAssignExpr().getResolvedExpression(sig, userDefinedSignals); 
					this.addCombinAssign("user defined signal assigns", sig.getFullSignalName(DefSignalType.USR_SIGNAL) + " = " + rhsSigExpression + ";");
				}
				// if not assigned a ref, must be an input, so verify use in an assign
				else {
					// if not used internally, issue an error
					if (!sig.isRhsReference())
						MsgUtils.errorMessage("user defined signal " + sig.getFullSignalName(DefSignalType.USR_SIGNAL) + " is not used");		
				}
			}
		}
	}
	
	// -------------- rhs assign signal methods

	/** add a signal to the list of rhs references */
	public void addRhsSignal(String refName, String instancePath, String rawReference) {
		rhsSignals.put(refName, new RhsReferenceInfo(instancePath, rawReference ));
	}
	
	/** check that a resolved signal is in valid list of logic module signals and issue an
	 *  error message if not. 
	 * this method should only be called after entire signal list is created at addrmap exit 
	 * @param preResolveName - name of signal before resolution (used to lookup error msg info)
	 * @param postResolveName - name of signal after resolution
	 * */
	public void checkSignalName(String preResolveName, String postResolveName) {
		// issue an error if resolved name is not in the defined signal list
		if (!this.hasDefinedSignal(postResolveName)) {
			if (rhsSignals.containsKey(preResolveName)) {
				RhsReferenceInfo rInfo = rhsSignals.get(preResolveName);
				//System.out.println("SystemVerilogLogicModule checkSignalName: preResolveName=" + preResolveName + " found in rhsSignals, but postResolveName=" + postResolveName + " not found in definedSignals");
				MsgUtils.errorMessage("unable to resolve " + rInfo.getRhsRefString() + " referenced in rhs dynamic property assignment for " + rInfo.getLhsInstance()); 
			}
			else
				MsgUtils.errorMessage("unable to resolve signal " + postResolveName + " inferred in rhs dynamic property assignment" ); 
		}
	}
	
	// -------------- coverpoints
	
	/** add coverpoint associated with this field */
	public void addFieldCoverPoints(FieldProperties fieldProperties) {
		// if an interrupt field, cover input signal
		if ((fieldProperties.generateRtlCoverage() || ExtParameters.sysVerIncludeDefaultCoverage()) && fieldProperties.isInterrupt()) {
			// add coverage on input intr signal (if it exists)
			if (!fieldProperties.hasRef(RhsRefType.INTR) && !fieldProperties.hasRef(RhsRefType.NEXT)) {
				String intrName = fieldProperties.getFullSignalName(DefSignalType.H2L_INTR);
				this.addCoverPoint("interrupt_cg", intrName, intrName, fieldProperties.getFieldWidth(), null);
			}			
		}
		// if a counter field, cover incr/decr signals, rollover/saturate, threshold
		else if ((fieldProperties.generateRtlCoverage() || ExtParameters.sysVerIncludeDefaultCoverage()) && fieldProperties.isCounter()) {
			// add coverage on input incr signal (if it exists)
			if (fieldProperties.isIncrCounter() && !fieldProperties.hasRef(RhsRefType.INCR)) {
				String incrName = fieldProperties.getFullSignalName(DefSignalType.H2L_INCR);
				this.addCoverPoint("counter_cg", incrName, incrName, 1, null);
			}			
			// add coverage on input decr signal (if it exists)
			if (fieldProperties.isDecrCounter() && !fieldProperties.hasRef(RhsRefType.DECR)) {
				String decrName = fieldProperties.getFullSignalName(DefSignalType.H2L_DECR);
				this.addCoverPoint("counter_cg", decrName, decrName, 1, null);
			}			
			// TODO - add rollover / saturate test?
		}
		// otherwise if rtl_coverage is explicitly specified
		else if (fieldProperties.generateRtlCoverage()) {
			String fldReg = fieldProperties.getFullSignalName(DefSignalType.FIELD);
			this.addCoverPoint("field_cg", fldReg, fldReg, fieldProperties.getFieldWidth(), null);
		}
		
	}
	
	//---------------------------- interrupt diagnostic bind module ----------------------------------------

	/** create an interrupt assertion bind module */
	public SystemVerilogModule createIntrBindModule() {
		String intrBindModName = getName() + "_intr_bind";
		SystemVerilogModule intrBindMod = new SystemVerilogModule(builder, intrBindModName, getInsideLocs(), defaultClkName, builder.getDefaultReset(), ExtParameters.sysVerUseAsyncResets());
		Integer defaultOutputLoc = getOutsideLocs();
		SystemVerilogIOSignalList bindIOList = new SystemVerilogIOSignalList("default");
		intrBindMod.useIOList(bindIOList, defaultOutputLoc);
		// add default clock and reset
		intrBindMod.addSimpleScalarFrom(defaultOutputLoc, defaultClkName);
		intrBindMod.addSimpleScalarFrom(defaultOutputLoc, builder.getDefaultReset());
		// add control signal defines
		String pkgPrefix = "";
		if (ExtParameters.sysVerUseGlobalDvBindControls()) {
			intrBindMod.addStatement("//------- intr detect controls are set in global package ordt_dv_bind_controls");
			intrBindMod.addStatement("//------- which includes the following for intr control:");
			intrBindMod.addStatement("//package ordt_dv_bind_controls;");
			intrBindMod.addStatement("//   bit enable_intr_check;  // if 0 shut down all intr assertions");
			intrBindMod.addStatement("//   bit only_error_asserts; // if 1 shut down all non-error intr assertions");
			intrBindMod.addStatement("//   bit allow_intr;         // if 1 and use_subcategories isn't set, all intr outputs will be warning, not error");
			intrBindMod.addStatement("//   bit use_subcategories;  // if 1 specified subcategory will be used to set assert severity (only if info currently)");
			intrBindMod.addStatement("//   bit use_enable_mask;    // if 1 intr will only assert if its enable or mask value is set");
			intrBindMod.addStatement("//   bit use_halt;           // if 1 halt mask/enable will be used when use_enable_mask is set");
			intrBindMod.addStatement("//endpackage");
			intrBindMod.addStatement("//ordt_dv_bind_controls::enable_intr_check = 1'b1;  // if 0 shut down all intr assertions");
			intrBindMod.addStatement("//ordt_dv_bind_controls::only_error_asserts = 1'b0; // if 1 shut down all non-error intr assertions");
			intrBindMod.addStatement("//ordt_dv_bind_controls::allow_intr = 1'b0;         // if 1 and use_subcategories isn't set, all intr outputs will be warning, not error");
			intrBindMod.addStatement("//ordt_dv_bind_controls::use_subcategories = 1'b1;  // if 1 specified subcategory will be used to set assert severity (only if info currently)");
			intrBindMod.addStatement("//ordt_dv_bind_controls::use_enable_mask = 1'b0;    // if 1 intr will only assert if its enable or mask value is set");
			intrBindMod.addStatement("//ordt_dv_bind_controls::use_halt = 1'b0;           // if 1 halt mask/enable will be used when use_enable_mask is set");
			pkgPrefix = "ordt_dv_bind_controls::";
		}
		else {
			intrBindMod.addStatement("//------- intr detect controls");
			intrBindMod.addStatement("bit enable_intr_check = 1'b1;  // if 0 shut down all intr assertions");
			intrBindMod.addStatement("bit only_error_asserts = 1'b0; // if 1 shut down all non-error intr assertions");
			intrBindMod.addStatement("bit allow_intr = 1'b0;         // if 1 and use_subcategories isn't set, all intr outputs will be warning, not error");
			intrBindMod.addStatement("bit use_subcategories = 1'b1;  // if 1 specified subcategory will be used to set assert severity (only if info currently)");
			intrBindMod.addStatement("bit use_enable_mask = 1'b0;    // if 1 intr will only assert if its enable or mask value is set");
			intrBindMod.addStatement("bit use_halt = 1'b0;           // if 1 halt mask/enable will be used when use_enable_mask is set");
		}
		// add get_severity function 
		intrBindMod.addStatement("");
		intrBindMod.addStatement("//------- intr severity settings");
		intrBindMod.addStatement("typedef enum {INFO, WARN, ERROR} intr_sev_t;");
		intrBindMod.addStatement("");
		intrBindMod.addStatement("function intr_sev_t get_severity (string subcat);");
		intrBindMod.addStatement("  if (" + pkgPrefix + "use_subcategories && (subcat==\"INFO\")) get_severity=INFO;");
		intrBindMod.addStatement("  else if (" + pkgPrefix + "allow_intr) get_severity=WARN;");
		intrBindMod.addStatement("  else get_severity=ERROR;");
		intrBindMod.addStatement("endfunction : get_severity");
		// add get_message function
		intrBindMod.addStatement("");
		intrBindMod.addStatement("//------- intr output message function");
		intrBindMod.addStatement("function string get_message (string subcat, string sigpath, logic sig, string intr_type, logic intr_en, string halt_type, logic halt_en);");
		intrBindMod.addStatement("  string bstr, istr, hstr;");
		intrBindMod.addStatement("  intr_sev_t sev;");
		intrBindMod.addStatement("  sev = get_severity(subcat);");
		intrBindMod.addStatement("  bstr = $sformatf(\"Interrupt %s fired (sub-category = %s). Value = %h.\", sigpath, subcat, sig);");
		intrBindMod.addStatement("  if (intr_type != \"none\") istr = $sformatf(\" %s value = %h.\", intr_type, intr_en);");
		intrBindMod.addStatement("  if (halt_type != \"none\") hstr = $sformatf(\" Halt(poll) %s value = %h.\", halt_type, halt_en);");
		intrBindMod.addStatement("  get_message = {bstr, istr, hstr};");
		intrBindMod.addStatement("endfunction : get_message");	
		// add property define
		intrBindMod.addStatement("");
		intrBindMod.addStatement("//------- intr detect property");
		intrBindMod.addStatement("property no_rising_intr (logic sig, logic inhibit, logic intr_mask, logic halt_mask);");
		intrBindMod.addStatement("  @(posedge "+ defaultClkName + ")");
		intrBindMod.addStatement("  disable iff (inhibit || (" + pkgPrefix + "use_enable_mask && ((!" + pkgPrefix + "use_halt && intr_mask) || (" + pkgPrefix + "use_halt && halt_mask))))");
		intrBindMod.addStatement("    not $rose(sig);");
		intrBindMod.addStatement("endproperty : no_rising_intr");	
		// add interrupts inputs and inhibit signals
		intrBindMod.addStatement("");
		intrBindMod.addStatement("//------- intr inhibits");
		intrBindMod.setShowDuplicateSignalErrors(false);  // could be leaf interrupts being used as masks/enables
		for(IntrDiagInfo intrInfo: intrInfoList) {
			// add signal inputs from reg logic module
			intrBindMod.addSimpleVectorFrom(defaultOutputLoc, intrInfo.getSigName(), 0, intrInfo.getWidth());
			if (intrInfo.hasIntrEnableOrMask()) intrBindMod.addSimpleVectorFrom(defaultOutputLoc, intrInfo.getIntrEnableName(), 0, intrInfo.getWidth());
			if (intrInfo.hasHaltEnableOrMask()) intrBindMod.addSimpleVectorFrom(defaultOutputLoc, intrInfo.getHaltEnableName(), 0, intrInfo.getWidth());
			// define inhibit and assign
			String inhibitName = "inhibit_" + intrInfo.getSigName();
			intrBindMod.addScalarWire(inhibitName);
			intrBindMod.addWireAssign(inhibitName + " = !" + pkgPrefix + "enable_intr_check || "+ builder.getDefaultReset() + " || (" + pkgPrefix + "only_error_asserts && (get_severity(\""+ intrInfo.getSubCategory() + "\") != ERROR));");
		}
		// add all leaf interrupts to to monitored
		intrBindMod.addStatement("");
		intrBindMod.addStatement("//------- intr detect assertions");
		for(IntrDiagInfo intrInfo: intrInfoList) {
			// add assertions 
			String redPfix = intrInfo.isVector()? "|" : ""; // use or reduction on vectors
			String sigName = redPfix + intrInfo.getSigName();
			String inhibitName = "inhibit_" + intrInfo.getSigName();
			String invPfix = (intrInfo.hasIntrEnableOrMask() && intrInfo.getIntrEnableType().startsWith("En"))? "!" : ""; // invert if an enable
			String intrMaskName = intrInfo.hasIntrEnableOrMask()? invPfix + redPfix + intrInfo.getIntrEnableName() : "1'b0";
			invPfix = (intrInfo.hasHaltEnableOrMask() && intrInfo.getHaltEnableType().startsWith("en"))? "!" : ""; // invert if an enable
			String haltMaskName = intrInfo.hasHaltEnableOrMask()? invPfix + redPfix + intrInfo.getHaltEnableName() : "1'b0";
			intrBindMod.addStatement("intr_assert_" + intrInfo.getSigName() + " : assert property (no_rising_intr(" + sigName + ", "  + inhibitName + ", " + intrMaskName + ", " + haltMaskName + "))");
			String intrEnStr = intrInfo.hasIntrEnableOrMask()? "\"" + intrInfo.getIntrEnableType() + "\", " + intrInfo.getIntrEnableName() : "\"none\", " + sigName;
			String haltEnStr = intrInfo.hasHaltEnableOrMask()? ", \"" + intrInfo.getHaltEnableType() + "\", " + intrInfo.getHaltEnableName() : ", \"none\", " + sigName;
			intrBindMod.addStatement("else begin");
			intrBindMod.addStatement("  string message;");
			intrBindMod.addStatement("  intr_sev_t sev;");
			intrBindMod.addStatement("  sev = get_severity(\""+ intrInfo.getSubCategory() + "\");");
			intrBindMod.addStatement("  message = get_message(\""+ intrInfo.getSubCategory() + "\", \"" + intrInfo.getSigPath() + "\", " + intrInfo.getSigName() + ", " + intrEnStr + haltEnStr + ");");
			intrBindMod.addStatement("  if (sev==INFO) $info(message);");
			intrBindMod.addStatement("  else if (sev==WARN) $warning(message);");
			intrBindMod.addStatement("  else $error(message);");
			intrBindMod.addStatement("end");
			intrBindMod.addStatement("");
		}
		// print a usage message
		intrBindMod.addStatement("//------- " + intrBindModName + " interrupt debug module.  Use by binding as follows in tb:");
		intrBindMod.addStatement("//        bind " + getName() + " " + intrBindModName + " intr_bind_inst(.*);");
		// return the module
		return intrBindMod;
	}

	public SystemVerilogModule createCoverBindModule() {
		String bindModName = getName() + "_cover_bind";
		SystemVerilogModule coverBindMod = new SystemVerilogModule(builder, bindModName, getInsideLocs(), defaultClkName, builder.getDefaultReset(), ExtParameters.sysVerUseAsyncResets());
		Integer defaultOutputLoc = getOutsideLocs();
		SystemVerilogIOSignalList bindIOList = new SystemVerilogIOSignalList("default");
		coverBindMod.useIOList(bindIOList, defaultOutputLoc);
		// add default clock and reset
		coverBindMod.addSimpleScalarFrom(defaultOutputLoc, defaultClkName);
		coverBindMod.addSimpleScalarFrom(defaultOutputLoc, builder.getDefaultReset());
		// use defined coverage info in the logic module
		SystemVerilogCoverGroups definedCGs = getCoverGroups();
		if (definedCGs.isEmpty()) return null;  // exit if no CGs
		coverBindMod.setCoverGroups(definedCGs);
		// add control signal defines  TODO
		
		// add bind module inputs
		for(SystemVerilogSignal sig: definedCGs.getSignalList()) {
			// add signal inputs
			coverBindMod.addSimpleVectorFrom(defaultOutputLoc, sig.getName(), sig.getLowIndex(), sig.getSize());
		}
		// print a usage message
		coverBindMod.addStatement("");
		coverBindMod.addStatement("//------- " + bindModName + " coverage module.  Use by binding as follows in tb:");
		coverBindMod.addStatement("//        bind " + getName() + " " + bindModName + " cover_bind_inst(.*);");
		// return the module
		return coverBindMod;
	}

	//---------------------------- inner classes ----------------------------------------
	
	/** class to hold interrupt bind info */
	private class IntrDiagInfo {
		private String sigName;  // intr field register name
		private int width = 1;  // size in bits
		private String sigPath;  // path of this instance
		private String intrEnableName; // enable/mask name
		private boolean intrEnable = true;  // true is enable, false if mask
		private String haltEnableName; // enable/mask name
		private boolean haltEnable = true;  // true is enable, false if mask
		private JspecSubCategory subCategory; // subcategory
		
		IntrDiagInfo(String sigName, int width, String sigPath, JspecSubCategory subCategory) {
			this.sigName = sigName;
			this.width = width;
			this.sigPath = sigPath;
			this.subCategory = subCategory;
		}
		protected String getIntrEnableName() {
			return intrEnableName;
		}
		public void setIntrEnableInfo(boolean intrEnable, String intrEnableName) {
			this.intrEnable = intrEnable;
			this.intrEnableName = intrEnableName;
		}
		protected boolean hasIntrEnableOrMask() {
			return intrEnableName != null;
		}
		protected String getIntrEnableType() {
			return intrEnable? "Enable" : "Mask";
		}
		
		protected String getHaltEnableName() {
			return haltEnableName;
		}
		public void setIntrHaltInfo(boolean haltEnable, String haltEnableName) {
			this.haltEnable = haltEnable;
			this.haltEnableName = haltEnableName;
		}
		protected boolean hasHaltEnableOrMask() {
			return haltEnableName != null;
		}
		protected String getHaltEnableType() {
			return haltEnable? "enable" : "mask";
		}

		protected String getSigName() {
			return sigName;
		}
		protected int getWidth() {
			return width;
		}
		protected boolean isVector() {
			return width>1;
		}
		protected String getSigPath() {
			return sigPath;
		}
		protected String getSubCategory() {
			return subCategory.toString();
		}
	}
	
	/** class to hold rhs assignment info for performing sig checks once all instances have been added to builder */
	private class RhsReferenceInfo {
		String lhsInstance;
		String rhsRefString;
		
		public RhsReferenceInfo(String lhsInstance, String rhsRefString) {
			this.lhsInstance = lhsInstance;
			this.rhsRefString = rhsRefString;
		}

		/** get lhsInstance
		 *  @return the lhsInstance
		 */
		public String getLhsInstance() {
			return lhsInstance;
		}

		/** get rhsRefString
		 *  @return the rhsRefString
		 */
		public String getRhsRefString() {
			return rhsRefString;
		}
	}

}
