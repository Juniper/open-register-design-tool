/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved.
 */
package ordt.output.systemverilog;

import java.util.HashMap;
import java.util.List;

import ordt.extract.Ordt;
import ordt.output.FieldProperties;
import ordt.output.RhsReference;
import ordt.output.SignalProperties;
import ordt.output.systemverilog.SystemVerilogDefinedSignals.DefSignalType;
import ordt.output.FieldProperties.RhsRefType;
import ordt.parameters.ExtParameters;

/** derived class for logic module 
 *  note: this class is tightly coupled with builder - uses several builder methods
 */
public class SystemVerilogLogicModule extends SystemVerilogModule {
	private HashMap<String, SignalProperties> userDefinedSignals = new HashMap<String, SignalProperties>();  // all user defined signals in module
	private HashMap<String, RhsReferenceInfo> rhsSignals = new HashMap<String, RhsReferenceInfo>();  // all right hand side assignment references in module (used to create usable error messages)

	public SystemVerilogLogicModule(SystemVerilogBuilder builder, int insideLocs, String defaultClkName) {
		super(builder, insideLocs, defaultClkName);
	}
	
	// -------------- user defined signal methods

	/** save user defined signal info */
	public void addUserDefinedSignal(String rtlName, SignalProperties signalProperties) {
		userDefinedSignals.put(signalProperties.getFullSignalName(DefSignalType.USR_SIGNAL), signalProperties);  
	}
	
	/** determine if a rhs reference is a signal or a field and return modified name if a signal.
	 *  if a signal, it is tagged as a rhsReference. 
	 * this method should only be called after entire signal list is created at addrmap exit */
	public String resolveAsSignalOrField(String ref) {
		//if (ref.startsWith("rg_")) System.out.println("SystemVerilogBuilder resolveAsSignalOrField: ref=" + ref);
		String retStr = ref;  // no name change by default
		if ((ref.startsWith("rg_") && userDefinedSignals.containsKey(ref.replaceFirst("rg_", "sig_")))) {
			retStr = ref.replace("rg_", "sig_");
			userDefinedSignals.get(retStr).setRhsReference(true);  // indicate that this signal is used internally as rhs
			//System.out.println("SystemVerilogBuilder: resolveAsSignalOrField: marked signal " + retStr + " as rhsReference, rhs=" + definedSignals.get(retStr).isRhsReference());
		}	
		return retStr;
	}
	
	/** loop through user defined signals and add assign statements to set these signals - call after build */  
	public void createSignalAssigns() {
		// first loop through signals, detect any signals on rhs, and verify each sig in rhs exists
		for (String key: userDefinedSignals.keySet()) {
			SignalProperties sig = userDefinedSignals.get(key);
			//System.out.println("SystemVerilogBuilder createSignalAssigns: sig key=" + key + ", sig id=" + sig.getId());
			// if signal is assigned internally and has simple rhs, check for valid vlog define and resolve sig vs reg
			if (sig.hasAssignExpr() ) {  
				// loop thru refs here... check each for well formed
				List<RhsReference> rhsRefList = sig.getAssignExpr().getRefList(); 
				for (RhsReference ref: rhsRefList) {
					if (ref.isWellFormedSignalName()) {
						String refName = ref.getReferenceName(sig, false); 
						refName = resolveAsSignalOrField(refName);  // resolve and tag any signals as rhsReference
						// check for a valid signal
						if (!this.hasDefinedSignal(refName) && (rhsSignals.containsKey(refName))) {  
							RhsReferenceInfo rInfo = rhsSignals.get(refName);
							Ordt.errorMessage("unable to resolve " + rInfo.getRhsRefString() + " referenced in rhs dynamic property assignment for " + rInfo.getLhsInstance()); 
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
			if (sig.hasAssignExpr()) {
				//System.out.println("SystemVerilogLogicModule createSignalAssigns: raw expr=" + sig.getAssignExpr().getRawExpression() + ", res expr=" + sig.getAssignExpr().getResolvedExpression(sig, userDefinedSignals));
				String rhsSigExpression = sig.getAssignExpr().getResolvedExpression(sig, userDefinedSignals); 
				this.addCombinAssign("user defined signal assigns", sig.getFullSignalName(DefSignalType.USR_SIGNAL) + " = " + rhsSigExpression + ";");
			}
			// if not assigned a ref, must be an input, so verify use in an assign
			else {
				// if not used internally, issue an error
				if (!sig.isRhsReference())
					Ordt.errorMessage("user defined signal " + sig.getFullSignalName(DefSignalType.USR_SIGNAL) + " is not used");		
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
				Ordt.errorMessage("unable to resolve " + rInfo.getRhsRefString() + " referenced in rhs dynamic property assignment for " + rInfo.getLhsInstance()); 
			}
			else
				Ordt.errorMessage("unable to resolve signal " + postResolveName + " inferred in rhs dynamic property assignment" ); 
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
				this.addCoverPoint("interrupt_cg", intrName, intrName, null);
			}			
		}
		// if a counter field, cover incr/decr signals, rollover/saturate, threshold
		else if ((fieldProperties.generateRtlCoverage() || ExtParameters.sysVerIncludeDefaultCoverage()) && fieldProperties.isCounter()) {
			// add coverage on input incr signal (if it exists)
			if (fieldProperties.isIncrCounter() && !fieldProperties.hasRef(RhsRefType.INCR)) {
				String incrName = fieldProperties.getFullSignalName(DefSignalType.H2L_INCR);
				this.addCoverPoint("counter_cg", incrName, incrName, null);
			}			
			// add coverage on input decr signal (if it exists)
			if (fieldProperties.isDecrCounter() && !fieldProperties.hasRef(RhsRefType.DECR)) {
				String decrName = fieldProperties.getFullSignalName(DefSignalType.H2L_DECR);
				this.addCoverPoint("counter_cg", decrName, decrName, null);
			}			
			// TODO - add rollover / saturate test?
		}
		// otherwise if rtl_coverage is explicitly specified
		else if (fieldProperties.generateRtlCoverage()) {
			String fldReg = fieldProperties.getFullSignalName(DefSignalType.FIELD);
			this.addCoverPoint("field_cg", fldReg, fldReg, null);
		}
		
	}
	
	//---------------------------- inner classes ----------------------------------------
	
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
