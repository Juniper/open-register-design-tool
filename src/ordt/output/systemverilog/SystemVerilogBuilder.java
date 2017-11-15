/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved. 
 */
package ordt.output.systemverilog;

import java.io.BufferedWriter;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import ordt.extract.Ordt;
import ordt.extract.RegModelIntf;
import ordt.extract.RegNumber;
import ordt.extract.RegNumber.NumBase;
import ordt.extract.RegNumber.NumFormat;
import ordt.extract.model.ModInstance;
import ordt.output.InstanceProperties;
import ordt.output.OutputBuilder;
import ordt.output.RegProperties;
import ordt.output.RhsReference;
import ordt.output.systemverilog.SystemVerilogDefinedSignals.DefSignalType;
import ordt.output.systemverilog.common.SystemVerilogModule;
import ordt.output.systemverilog.common.SystemVerilogSignal;
import ordt.output.systemverilog.common.wrap.SystemVerilogWrapModule;
import ordt.output.systemverilog.io.SystemVerilogIOSignalList;
import ordt.output.systemverilog.io.SystemVerilogIOSignalSet;
import ordt.output.AddressableInstanceProperties.ExtType;
import ordt.parameters.ExtParameters;
import ordt.parameters.Utils;
import ordt.parameters.ExtParameters.SVDecodeInterfaceTypes;

/**
 *  @author snellenbach      
 *  Jan 27, 2014
 */
public class SystemVerilogBuilder extends OutputBuilder {
	
	private String modulePrefix = null;  // module name prefix for nested addressmaps
	private static boolean legacyVerilog = false;  // by default generate output with system verilog constructs (only valid in write stage)
	
	// clk names	
	protected static final String defaultClk = "clk";	
	protected static String decodeClk = ExtParameters.systemverilogUseGatedLogicClk() ? "uclk" : defaultClk;	
	protected static String logicClk = ExtParameters.systemverilogUseGatedLogicClk() ? "gclk" : defaultClk;	

	// reset names
	protected String defaultReset = "reset";  
	protected boolean defaultResetActiveLow = false;
	protected String logicReset = null;
	protected boolean logicResetActiveLow = false;
	// indication that default resets are defined for this builder
	private boolean resetsLocked = false;  // set to true before first reg instance
	private boolean isTestBuilder = false;  // true if modules are generated for test only (eg via generate_external_regs) 
	
	private static ValidAddressRanges addressRanges;
	
	// define io locations
	protected static final Integer HW = SystemVerilogDefinedSignals.HW;
	protected static final Integer LOGIC = SystemVerilogDefinedSignals.LOGIC;
	protected static final Integer DECODE = SystemVerilogDefinedSignals.DECODE;
	protected static final Integer PIO = SystemVerilogDefinedSignals.PIO;
	protected boolean usesInterfaces = false;  // detect if sv interfaces are needed  

    // define common IO lists
	protected SystemVerilogIOSignalList cntlSigList = new SystemVerilogIOSignalList("cntl");   // clocks/resets
	protected SystemVerilogIOSignalList hwSigList = new SystemVerilogIOSignalList("hw");     // logic/decode to/from hw
	protected SystemVerilogIOSignalList pioSigList = new SystemVerilogIOSignalList("pio");    // pio to/from decoder
	protected SystemVerilogIOSignalList intSigList = new SystemVerilogIOSignalList("internal");    // logic to/from decoder
	
	// module defines  
	protected SystemVerilogDecodeModule decoder = new SystemVerilogDecodeModule(this, DECODE, decodeClk);
	protected SystemVerilogLogicModule logic = new SystemVerilogLogicModule(this, LOGIC, logicClk);
	protected SystemVerilogModule top = new SystemVerilogModule(this, 0, defaultClk, getDefaultReset());  // TODO was DECODE|LOGIC
	
	private  List<String> tempAssignList = new ArrayList<String>();    // temp list of assign statements for current register (used in finishReg)

	// child address maps
	protected List<SystemVerilogBuilder> childAddrMaps = new ArrayList<SystemVerilogBuilder>();
		
	private  RegProperties topRegProperties;  // register properties at first call of this SystemVerilogBuilder (for i/f name generation)

	//---------------------------- constructors ----------------------------------
	
	public SystemVerilogBuilder(RegModelIntf model) {
		setBaseBuilderID();   // set unique ID of this instance
		this.model = model;
		addressRanges = new ValidAddressRanges();  // base builder so clear the address range list
		addressRanges.clear();
	    setVisitEachReg(true);   // gen code for each reg
	    setVisitEachRegSet(true);   // gen code for each reg set
	    setVisitExternalRegisters(false);  // do not visit externals - only ext root will be visited
	    setVisitEachExternalRegister(false);	    // handle externals as a group
		setLegacyVerilog(false);  // rtl uses systemverilog constructs
		initIOLists(null);  // setup IO lists for logic, decode, and top modules
		decoder.setPrimaryInterfaceType(ExtParameters.getSysVerRootDecoderInterface()); // set root pio interface type from specified params
		model.getRoot().generateOutput(null, this);   // generate output structures recursively starting at model root
	}
	
	// constructor for addressmap children
	public SystemVerilogBuilder(SystemVerilogBuilder parentBuilder, boolean isTestModule) {
		setNewBuilderID();   // set unique ID of this instance
		this.model = parentBuilder.model;    // inherit parent model
	    setVisitEachReg(true);   // gen code for each reg
	    setVisitEachRegSet(true);   // gen code for each reg set
	    setVisitExternalRegisters(false);  // do not visit externals - only ext root will be visited
	    setVisitEachExternalRegister(false);	    // handle externals as a group
		setLegacyVerilog(SystemVerilogBuilder.isLegacyVerilog());  // cascade state for systemverilog construct gen
		initIOLists(parentBuilder);  // setup IO lists for logic, decode, and top modules
	    // inherit name prefixes from parent
	    this.setModulePrefix(parentBuilder.getModuleName());
	    // save state of the current regSet and the instance stacks
	    updateRegSetState(parentBuilder);
	    // now that static regSetProperties is valid, use to initialize builder next and base addresses
	    this.setBaseAddress(regSetProperties.getBaseAddress());  
	    this.setNextAddress(regSetProperties.getBaseAddress());
	    // if this child is for test, then tag it
	    this.setTestBuilder(isTestModule);
	    if (isTestModule) setAddressMapName(regSetProperties.getId());
	    // FIXME - testBuilder flow is broken - need to handle external replicated regs, single reg , replicated regsets cases for full test gen
	    // also, wont work because startMap() and finishMap() are not called - would need to modify model itself or allow regset as sv root instance

	    // save the regProperties state and set external names
	    this.topRegProperties = parentBuilder.regProperties;
		//System.out.println("SystemVerilogBuilder creating new builder, regset id=" + regSetProperties.getId() + ", regset ext=" + regSetProperties.getExternalType());
		//System.out.println("SystemVerilogBuilder                     , topreg id=" + topRegProperties.getId() + ", topreg ext=" + topRegProperties.getExternalType());
		//System.out.println("SystemVerilogBuilder                     , parent mod=" + parentBuilder.getModuleName() + ", new mod=" + this.getModuleName()+ ", bid=" + this.getBuilderID());

		// if child is in a bb root region, change its interface to leaf
		if (topRegProperties.hasExternalType(ExtType.BBV5))
			decoder.setPrimaryInterfaceType(SVDecodeInterfaceTypes.LEAF); 
		// else if a serial8 region use this decoder interface
		else if (topRegProperties.hasExternalType(ExtType.SERIAL8))
			decoder.setPrimaryInterfaceType(SVDecodeInterfaceTypes.SERIAL8); 
		// else if a ring16 region use this decoder interface
		else if (topRegProperties.hasExternalType(ExtType.RING)) {
			int width = topRegProperties.getExternalType().getParm("width");
			if (width == 8) decoder.setPrimaryInterfaceType(SVDecodeInterfaceTypes.RING8); 
			else if (width == 16) decoder.setPrimaryInterfaceType(SVDecodeInterfaceTypes.RING16); 
			else if (width == 32) decoder.setPrimaryInterfaceType(SVDecodeInterfaceTypes.RING32); 			
		}

	    // now generate output starting at this regmap
		//System.out.println("SystemVerilogBuilder: pre generate - regset inst id=" + regSetProperties.getId() + ", ext=" + regSetProperties.getExternalType() + ", amap=" + regSetProperties.isAddressMap() + ", inst stack top=" + instancePropertyStack.peek().getId());
		//System.out.println("SystemVerilogBuilder: pre generate - topreg inst id=" + topRegProperties.getId() + ", ext=" + topRegProperties.getExternalType() + ", builderID=" + getBuilderID());
	    ModInstance mapInstance = regSetProperties.getExtractInstance();
		mapInstance.getRegComp().generateOutput(mapInstance, this);   // generate output structures recursively starting at new adressmap root
		//System.out.println("--- VerilogBuilder - creating child, inst=" + mapInstance.getFullId() + ", base addr=" + getBaseAddress());
		//mapInstance.getRegComp().display(null);
	    this.regSetProperties.setExternalType(topRegProperties.getExternalType()); // restore parent ext type now that child gen is complete
		//System.out.println("SystemVerilogBuilder: post generate - regset inst id=" + regSetProperties.getId() + ", ext=" + regSetProperties.getExternalType() + ", amap=" + regSetProperties.isAddressMap() + ", inst stack top=" + instancePropertyStack.peek().getId());
		//System.out.println("SystemVerilogBuilder: post generate - topreg inst id=" + topRegProperties.getId() + ", ext=" + topRegProperties.getExternalType()+ ", builderID=" + getBuilderID());
	}
	
	/** initialize signal lists used by generated modules 
	 * @param parentBuilder - if non-null, init the hwSigList stack */
	private void initIOLists(SystemVerilogBuilder parentBuilder) {
		// add decoder io lists
		decoder.useIOList(cntlSigList, null);
		decoder.useIOList(pioSigList, PIO); 
		decoder.useIOList(intSigList, LOGIC); 
		decoder.useIOList(hwSigList, HW);
		// add logic io lists
		logic.useIOList(cntlSigList, null);
		logic.useIOList(intSigList, DECODE); 
		logic.useIOList(hwSigList, HW);
		// add top io lists
		top.useIOList(cntlSigList, null);
		top.useIOList(hwSigList, HW);
		top.useIOList(pioSigList, PIO); 
		// copy the hw signal list stack
		if (parentBuilder != null) hwSigList.copyActiveSetStack(parentBuilder.hwSigList);
	}
	
	/** set legacyVerilog
	 *  @param boolean legacyVerilog value to set
	 */
	public static void setLegacyVerilog(boolean legacy) {
		legacyVerilog = legacy;
		SystemVerilogModule.setLegacyVerilog(legacy);  // also set in modules
	}
	
	/** return true if ouput will be legacy verilog */
	public static boolean isLegacyVerilog() {
	   return legacyVerilog;	
	}
	
	/** pull over and update current regset and stacks */
	private void updateRegSetState(SystemVerilogBuilder parentBuilder) {
	    this.regSetProperties = parentBuilder.regSetProperties;  // this is a ref so all stack info changes - restore external when done?
	    this.regSetProperties.setExternalTypeFromString(null); // no longer external now that we're in child addrmap
	    this.regSetProperties.setRootExternal(false);
	    this.instancePropertyStack.addAll(parentBuilder.instancePropertyStack);
	    this.regSetPropertyStack.addAll(parentBuilder.regSetPropertyStack);
		//System.out.println("SystemVerilogBuilder updateRegSetState: updating state for path=" + getInstancePath() + ", rs addrmap=" + regSetProperties.isAddressMap());
	}

	//---------------------------- OutputBuilder methods to load verilog structures ----------------------------------------

	/** add a signal for a particular output */
	@Override
	public  void addSignal() {
		if (signalProperties == null) return;
		
		// set as a default reset if specified (exit if specified since these are handled separately from other sigs)
		if (signalProperties.isDefaultReset()) {
			if (resetsLocked) Ordt.errorMessage("cpuif_reset property ignored for signal " + signalProperties.getInstancePath());
			else {
				defaultReset = signalProperties.getFullSignalName(DefSignalType.USR_SIGNAL);
				defaultResetActiveLow = signalProperties.isActiveLow();
				return;
			}
		}
		else if (signalProperties.isLogicReset()) {
			if (resetsLocked) Ordt.errorMessage("field_reset property ignored for signal " + signalProperties.getInstancePath());
			else {
				logicReset = signalProperties.getFullSignalName(DefSignalType.USR_SIGNAL);
				logicResetActiveLow = signalProperties.isActiveLow();
				return;
			}			
		}
		
		// add user signal to hashmap of defined signals for the active logic module  // TODO - create currentInstIsLocalExternal??
		if (!activeInstanceIsExternal()) logic.addUserDefinedSignal(signalProperties.getFullSignalName(DefSignalType.USR_SIGNAL), signalProperties);
		//System.out.println("SystemVerilogBuilder addSignal: signal=" + signalProperties.getFullSignalName(DefSignalType.USR_SIGNAL) + ", low=" + signalProperties.getLowIndex() + ", size=" + signalProperties.getSignalWidth() + ", builder=" + getBuilderID() + ", ext=" + signalProperties.isExternal());
		
		// if signal is lhs of assignment then add to logic assign/logic lists/output, else add it as an input.
		// io assign is done here so interface wrapper contains user sigs - all user sigs are either inputs or outputs
		// assign of signal is made after builder processing when all rhs references can be resolved.
		if (signalProperties.hasAssignExpr()) {
			logic.addVectorReg(signalProperties.getFullSignalName(DefSignalType.USR_SIGNAL), signalProperties.getLowIndex(), signalProperties.getSignalWidth());  
			logic.addSimpleVectorTo(HW, signalProperties.getFullSignalName(DefSignalType.USR_SIGNAL), 0, signalProperties.getSignalWidth());   // add output
            // add each signal in expression rhs to the master list of rhs signals
			List<RhsReference> rhsRefList = signalProperties.getAssignExpr().getRefList(); 
			for (RhsReference ref: rhsRefList) {
				String refName = ref.getReferenceName(signalProperties, false);   
				refName = logic.resolveAsSignalOrField(refName);
				logic.addRhsSignal(refName, getInstancePath() , ref.getRawReference());				
				//System.out.println("SystemVerilogBuilder addSignal, rhs ref=" + refName + ", lhs sig=" + getInstancePath() + ", raw rhs ref=" + ref.getRawReference());
			}
		}
		else {
			//logic.addVectorWire(signalProperties.getRtlName(), signalProperties.getLowIndex(), signalProperties.getSignalWidth());  
			logic.addSimpleVectorFrom(HW, signalProperties.getFullSignalName(DefSignalType.USR_SIGNAL), 0, signalProperties.getSignalWidth());   // add input
		}
	}
	
	/** add a field for a particular output */
	@Override
	public  void addField() {
		   // if new interface then add it to logic-hw list
		   startIOHierarchy(fieldProperties);
		   
		   // pass current instances to logic module
		   logic.setActiveInstances(regProperties, fieldProperties);
		   
		   // generate field write assignment stmts
		   logic.genFieldWriteStmts();
		   
		   // generate field read assignment stmts
		   logic.genFieldReadStmts();
		   
		   // add coverage info
		   if (!regProperties.isExternal()) logic.addFieldCoverPoints(fieldProperties);
		   
		   // if new interface was added, indicate finished using it
		   endIOHierarchy(fieldProperties);
	}

	/** add a field for a particular output */
	@Override
	public  void addAliasField() {
		   //System.out.println("VerilogBuilder: addAliasField " + fieldProperties.getInstancePath());
		   // if new interface then add it to logic-hw list
		   startIOHierarchy(fieldProperties);
		   
		   // pass current instances to logic module
		   logic.setActiveInstances(regProperties, fieldProperties);
		   
		   // generate alias field write assignment stmts
		   if (fieldProperties.swChangesValue()) {  // if sw can write or rclr/rset
			   logic.genSwFieldNextWriteStmts(false);    // create statements to set value of next based on sw field settings 
		   }	
		   
		   // generate alias field read assignment stmts
		   logic.genSwFieldReadStmts();
		   
		   // if new interface was added, indicate finished using it
		   endIOHierarchy(fieldProperties);
	}

	/** add a fieldset for a particular output - concrete since most builders do not use */
	protected void addFieldSet() {
		   startIOHierarchy(fieldSetProperties);
		   //System.out.println("SystemVerilogBuilder addFieldSet: " + fieldSetProperties.getInstancePath() + ",  id=" + fieldSetProperties.getExtractInstance().getRegComp().getId());
	}

	/** finish a fieldset for a particular output - concrete since most builders do not use */
	protected void finishFieldSet() {
		   endIOHierarchy(fieldSetProperties);
	}

	/** add a register for a particular output */
	@Override
	public  void addRegister() {  
		   if (!resetsLocked) lockResets();  
		   if (!regProperties.isExternal()) decoder.addToDecode(regProperties);    // put reg info on address list for decoder gen

		   //System.out.println("SystemVerilogBuilder " + getBuilderID() + " addRegister: adding reg=" + regProperties.getInstancePath());
		   // if new interface then add it to logic-hw list
		   startIOHierarchy(regProperties);
		   
		   // add to verilog structure lists
		   intSigList.addSimpleVector(DefSignalType.D2L_DATA, regProperties.getBaseName(), 0, regProperties.getRegWidth());    // add write data to decode to logic signal list 
		   if (SystemVerilogDecodeModule.hasWriteEnables()) intSigList.addSimpleVector(DefSignalType.D2L_ENABLE, regProperties.getBaseName(), 0, regProperties.getRegWidth());    // add write enable to decode to logic signal list 
		   intSigList.addSimpleScalar(DefSignalType.D2L_WE, regProperties.getBaseName());    // add we to decode to logic signal list 
		   intSigList.addSimpleScalar(DefSignalType.D2L_RE, regProperties.getBaseName());    // add re to decode to logic signal list 
		   intSigList.addSimpleVector(DefSignalType.L2D_DATA, regProperties.getBaseName(), 0, regProperties.getRegWidth());    // add read data to logic to decode signal list 
		   		   
		   tempAssignList.clear();   // clear the temp assign list
	}

	/** finish a register for a particular output */
	@Override
	public  void finishRegister() {
		   // if register has an interrupt output then create it
		   if (regProperties.hasInterruptOutputDefined()) {
			   hwSigList.addScalar(DefSignalType.L2H_INTR);    // add intr output signal  
		       // if pulse on clear is set then qualify intr output with clear detect signal
		       if (ExtParameters.sysVerPulseIntrOnClear()) {
				   String intrOutput = regProperties.getFullSignalName(DefSignalType.L2H_INTR);
				   String intrClear = regProperties.getFullSignalName(DefSignalType.INTR_CLEAR);  // interrupt clear detect signal
			       logic.addPrecCombinAssign(regProperties.getBaseName(), false, intrOutput + " = " + intrOutput + " & ~" + intrClear + ";");  // qualify output with clear
		       }
		   }

		   // if register has an halt output then create it
		   if (regProperties.hasHaltOutputDefined()) {
			   hwSigList.addScalar(DefSignalType.L2H_HALT);    // add halt output signal  
		   }

		   // handle registers that need unused fields zero'd out
		   logic.addVectorReg(regProperties.getFullSignalName(DefSignalType.L2D_DATA), 0, regProperties.getRegWidth());  // make logic outputs regs
		   if (regProperties.hasNopBits()) {
			   logic.addCombinAssign(regProperties.getBaseName() + " (pio read data)", regProperties.getFullSignalName(DefSignalType.L2D_DATA) + " = " + regProperties.getRegWidth() + "'b0;");    
			   logic.addCombinAssign(regProperties.getBaseName() + " (pio read data)", tempAssignList);    
		   }
		   else  logic.addCombinAssign(regProperties.getBaseName() + " (pio read data)", tempAssignList);   // fields use all bits in this register 
		   
		   decoder.addVectorReg(regProperties.getFullSignalName(DefSignalType.D2L_DATA), 0, regProperties.getRegWidth());  // make decode outputs regs
		   if (SystemVerilogDecodeModule.hasWriteEnables()) decoder.addVectorReg(regProperties.getFullSignalName(DefSignalType.D2L_ENABLE), 0, regProperties.getRegWidth());
		   decoder.addScalarReg(regProperties.getFullSignalName(DefSignalType.D2L_WE)); // add we and re output defs
		   decoder.addScalarReg(regProperties.getFullSignalName(DefSignalType.D2L_RE)); 
		   
		   // if new interface was added, indicate finished using it
		   endIOHierarchy(regProperties);
	}

	/** add an external register (or child addressmap) interface */
	@Override
	public  void addRootExternalRegisters() {
		   if (!resetsLocked) lockResets();  
		   
		   // save address gap and exit if external decode is set - no decoder address or IO signals for this case
		   if (regProperties.isExternalDecode()) {
			   //System.out.println("SystemVerilogBuilder.addRootExternalRegisters externalDecode, inst=" + regProperties.getInstancePath() + ", base=" + regProperties.getBaseAddress() + ", size=" + getExternalRegBytes());
			   //addressRanges.list();
			   addressRanges.addGap(regProperties.getBaseAddress(), getExternalRegBytes(), regProperties.getInstancePath());
			   //addressRanges.list();
			   return;
		   }
		   
		   decoder.addToDecode(regProperties);    // put external reg info on address list for decoder gen 
		   
		   startIOHierarchy(regProperties);  // if an interface is specified add it

		   //System.out.println("SystemVerilogBuilder.addRootExternalRegisters, inst=" + regProperties.getInstancePath() + ", isAddressMap=" + regProperties.isAddressMap() + ", baseAddress=" + regProperties.getBaseAddress());
		   //System.out.println("    SystemVerilogBuilder.addRootExternalRegisters, regset inst=" + regSetProperties.getInstancePath() + ", regset isAddressMap=" + regSetProperties.isAddressMap());
		   
		   // if an addrmap create a new VerilogBuilder at new root instance and push onto list
		   if (ExtParameters.sysVerGenerateChildAddrmaps() && regProperties.isAddressMap()) {
			   SystemVerilogBuilder childVerilog = new SystemVerilogBuilder(this, false); // inherit some parent properties
			   //System.out.println("SystemVerilogBuilder addRootExternalRegisters: current regset=" + regSetProperties.getInstancePath() + ", mod prefix=" + modulePrefix+ ", bid=" + getBuilderID() + ", mlevel=" + currentMapLevel() + ", stack=" + hwSigList.listStack());  
			   //System.out.println("SystemVerilogBuilder addRootExternalRegisters: reg=" + regProperties.getInstancePath() + ", regset=" + regSetProperties.getInstancePath() + ", mod prefix=" + modulePrefix + ", bId=" + getBuilderID() + ", mlevel=" + currentMapLevel() + ", testModule=" + isTestBuilder());  
			   childAddrMaps.add(childVerilog);
		   }
		   // otherwise if building external regs for test create a builder (if not already a testbuilder)  // TODO - this is broken
		   else if (ExtParameters.sysVerGenerateExternalRegs() && !isTestBuilder()) {
			   SystemVerilogBuilder childVerilog = new SystemVerilogBuilder(this, true); // inherit some parent properties and mark child as testBuilder
			   //System.out.println("SystemVerilogBuilder addRootExternalRegisters: reg=" + regProperties.getInstancePath() + ", regset=" + regSetProperties.getInstancePath() + ", mod prefix=" + modulePrefix + ", bId=" + getBuilderID() + ", mlevel=" + currentMapLevel() );  
			   childAddrMaps.add(childVerilog);
		   }
		   
		   // generate specific i/f type for this external region
		   if (regProperties.hasExternalType(ExtType.PARALLEL)) {
			   // pick up any instance overrides
			   boolean optimize = regProperties.getExternalType().hasParm("optimize")? true :
				   regProperties.getExternalType().hasParm("no_optimize")? false : ExtParameters.sysVerOptimizeParallelExternals();
			   boolean keepNack = regProperties.getExternalType().hasParm("keep_nack")? true : false;
			   decoder.generateExternalInterface_PARALLEL(regProperties, optimize, keepNack);
		   }
		   else if (regProperties.hasExternalType(ExtType.BBV5)) decoder.generateExternalInterface_BBV5(regProperties);
		   else if (regProperties.hasExternalType(ExtType.SRAM)) decoder.generateExternalInterface_SRAM(regProperties);
		   else if (regProperties.hasExternalType(ExtType.SERIAL8)) decoder.generateExternalInterface_SERIAL8(regProperties);
		   else if (regProperties.hasExternalType(ExtType.RING)) decoder.generateExternalInterface_RING(regProperties.getExternalType().getParm("width"), regProperties);
		   
		   endIOHierarchy(regProperties);  // close out interface 
	}
	
	/** add a non-root addressmap - overridden by child builders (sv builder) */
	@Override
	protected void addNonRootExternalAddressMap() {
		   SystemVerilogBuilder childVerilog = new SystemVerilogBuilder(this, false); // inherit some parent properties 
		   //System.out.println("SystemVerilogBuilder addNonRootExternalAddressMap: regset=" + regSetProperties.getInstancePath() + ", mod prefix=" + modulePrefix + ", bId=" + getBuilderID() + ", mlevel=" + currentMapLevel() + ", testModule=" + isTestBuilder());  
		   if (currentMapLevel() < 3) childAddrMaps.add(childVerilog);  // only add to child list if no other map is in parent hierarchy
	}

	/** add a register set for a particular output */
	@Override
	public  void addRegSet() {
		   // if new interface then add it to logic-hw list
		   startIOHierarchy(regSetProperties);
	}

	/** finish a register set for a particular output */
	@Override
	public  void finishRegSet() {	
		   // if new interface was added, indicate finished using it
		   endIOHierarchy(regSetProperties);
	}

	/** add a register map for a particular output */
	@Override
	public  void addRegMap() {
		// set names, etc of generated modules
		initModuleInfo();
	}

	/** finish a register map for a particular output */
	@Override
	public  void finishRegMap() {	
		//Ordt.warnMessage("SystemVerilogBuilder: finishRegMap, builder=" + getBuilderID());
		//System.out.println("SystemVerilogBuilder: finishRegMap: rqto_err_log_padoody2 #0 isRhs=" + definedSignals.get("rqto_err_log_padoody2").isRhsReference());
		//logic.resolveNames();
		//System.out.println("SystemVerilogBuilder: finishRegMap: rqto_err_log_padoody2 #1 isRhs=" + definedSignals.get("rqto_err_log_padoody2").isRhsReference());
		// create statements to assign signals and create IO ports
		logic.createSignalAssigns();  
		//System.out.println("SystemVerilogBuilder: finishRegMap: rqto_err_log_padoody2 #3 isRhs=" + definedSignals.get("rqto_err_log_padoody2").isRhsReference());
		// done with this builder addrmap, so generate pio interface info
		decoder.genPioInterfaces(topRegProperties);
		// load list of clock, reset IOs   
		loadCntlSigList();
		// create wire defines in top level module
		initTopModuleDefines();
	}

	//---------------------------- add/finish support methods ----------------------------------------
	
	/** set default reset signal names and prohibit addl name changes */
	private void lockResets() {
		// disallow name changes
		resetsLocked=true;  
		// add default resets to logic and decode regs
		decoder.addReset(defaultReset, defaultResetActiveLow);
		if (logicReset != null) logic.addReset(logicReset, logicResetActiveLow);
		else logic.addReset(defaultReset, defaultResetActiveLow);
	}

	/** return the default reset name */
	public String getDefaultReset() {
		return defaultReset;
	}

	/** return true if the default reset is active low */
	public boolean getDefaultResetActiveLow() {
		return defaultResetActiveLow;
	}

	/** return the logic reset name */
	public String getLogicReset() {
		return logicReset;
	}

	/** return true if the logic reset is active low */
	public boolean getLogicResetActiveLow() {
		return logicResetActiveLow;
	}

	/** return the addressRanges list */
	public ValidAddressRanges getAddressRanges() {
		return addressRanges;
	}

	// ------------

	/** return true if write enables are specified */
	public static boolean hasWriteEnables() {
		return (ExtParameters.sysVerWriteEnableSize()>0);
	}

	/** return a verilog hex string representing N bits of value 1 */
	public static String getHexOnesString(int nbits) {
		RegNumber num = new RegNumber(1 << nbits);
		num.subtract(1);
		num.setVectorLen(nbits);
		return num.toFormat(NumBase.Hex, NumFormat.Verilog);
	}

	// ------------
	
	/** returns true if this builder is generating rtl for test use only */
	public boolean isTestBuilder() {
		return isTestBuilder;
	}

	/** set true if this builder is generating rtl for test use only */
	public void setTestBuilder(boolean isTestBuilder) {
		this.isTestBuilder = isTestBuilder;
	}
	
	// --------------------- methods run at end of extract in finishMap --------------------------

	/** initialize clock, reset IO list */
	private void loadCntlSigList() {
		// if using a gated clock for logic add it
		if (ExtParameters.systemverilogUseGatedLogicClk()) {
			cntlSigList.addSimpleScalar(HW, DECODE, decodeClk);
			cntlSigList.addSimpleScalar(HW, LOGIC, logicClk);
		}
		// otherwise add single clock to both internal modules
		else cntlSigList.addSimpleScalar(HW, LOGIC|DECODE, decodeClk);
		
		// defined resets to input list 
		if (logicReset != null) {
			cntlSigList.addSimpleScalar(HW, DECODE, defaultReset);  // only decoder gets default reset
			cntlSigList.addSimpleScalar(HW, LOGIC, logicReset);  // logic module has its own reset 
		}
		else
			cntlSigList.addSimpleScalar(HW, DECODE|LOGIC, defaultReset);  // else a common reset
	}
	
	/** initialize module info at end of builder extract */
	private void initModuleInfo() {
		//System.out.println("SystemVerilogBuilder initModuleInfo: moduleName=" + getModuleName() );
		// init decoder 
		decoder.setName(getModuleName() + "_jrdl_decode");
		if (addBaseAddressParameter()) setAddBaseAddrParameter(decoder);		
		// init logic 
		logic.setName(getModuleName() + "_jrdl_logic");		
		// init top
		top.setName(getModuleName() + "_pio");  // set name
		if (addBaseAddressParameter()) setAddBaseAddrParameter(top);
		// add decode and logic modules
		top.addInstance(decoder, "pio_decode");
		top.addInstance(logic, "pio_logic");
	}
	
	/** initialize top module wire defines at end of builder extract */
	private void initTopModuleDefines() {
		//System.out.println("SystemVerilogBuilder initTopModuleDefines: moduleName=" + getModuleName() );
		// wire define stmts for signals between decoder and logic
		top.addWireDefs(intSigList.getSignalList(DECODE, LOGIC));
		top.addWireDefs(intSigList.getSignalList(LOGIC, DECODE));
	}
	
	/** add IO hierarchy level */
	private void startIOHierarchy(InstanceProperties properties) {
		// if an interface, push intf type onto IO stack
		if ((properties.useInterface() || properties.useStruct()) && !properties.isRootInstance()) {
			usesInterfaces = true;
			// set signal set type
			DefSignalType sType = null;
			if (activeInstanceIsExternal()) sType = properties.useInterface()? DefSignalType.DH_INTERFACE : DefSignalType.DH_STRUCT;
			else sType = properties.useInterface()? DefSignalType.LH_INTERFACE : DefSignalType.LH_STRUCT;
			// save component Id if it's not anonymous
			String compId = properties.getExtractInstance().getRegComp().getId();
			if ((compId != null) && compId.startsWith("aNON")) compId = null;
			//System.out.println("SystemVerilogBuilder startIOHierarchy: " + properties.getInstancePath() + ", compId=" + compId + ", repCount=" + properties.getRepCount() + ", ExtInterfaceName=" + properties.getExtInterfaceName());
			hwSigList.pushIOSignalSet(sType, properties.getNoRepId(), properties.getRepCount(), properties.isFirstRep(), properties.getExtInterfaceName(), compId);
		}	
		// otherwise a non-interface hierarchy level
		else hwSigList.pushIOSignalSet(DefSignalType.SIGSET, properties.getNoRepId(), properties.getRepCount(), properties.isFirstRep(), null, null);
	}

	/** close out active IO hierarchy level */
	private void endIOHierarchy(InstanceProperties properties) {
			//System.out.println("*** Popping interface:" + properties.getBaseName());
			hwSigList.popIOSignalSet();
	}

	/** add BASE_ADDR parameter to a module */
	private void setAddBaseAddrParameter(SystemVerilogModule mod) {
		// set default baseaddr for this module
		RegNumber baseAddr = new RegNumber(ExtParameters.getPrimaryBaseAddress());
		baseAddr.setVectorLen(ExtParameters.getLeafAddressSize());
		mod.addParameter("BASE_ADDR", baseAddr.toFormat(NumBase.Hex, NumFormat.Verilog));
	}
		
	//---------------------------- inner classes ----------------------------------------
	
	/** class to hold valid address ranges for this builder */
	public class ValidAddressRanges {
		List<AddressRange> ranges = new ArrayList<AddressRange>();
		List<NamedAddressRange> gaps = new ArrayList<NamedAddressRange>();
		
		/** clear the list and init with full range */
		public void clear() {
			ranges.clear();
			gaps.clear();
			RegNumber start = new RegNumber(0);
			ranges.add(new AddressRange(start, null));  // end is set to null until list is processed
		}
		
		/** add a gap to the valid ranges */
		public void addGap(RegNumber gapStart, RegNumber gapSize, String instName) {
			// get the last valid range
			int idx = ranges.size() - 1;
			AddressRange lastRange = ranges.get(idx);
			// set the new range end to gapstart-1
			if (gapStart.isGreaterThan(lastRange.getStart())) {
				RegNumber newEnd = new RegNumber(gapStart);
				newEnd.subtract(new RegNumber(1));
			    lastRange.setEnd(newEnd);
			}
			else ranges.remove(idx);  // otherwise this range is gone
			// add a new range at end of list w/ start = gapend + 1
			RegNumber newStart = new RegNumber(gapStart);
			newStart.add(gapSize);
			ranges.add(new AddressRange(newStart, null));
			// compute gap end and save the gap info
			RegNumber gapEnd = new RegNumber(newStart); // gapStart+gapSize
			gapEnd.subtract(new RegNumber(1));
			gaps.add(new NamedAddressRange(gapStart, gapEnd, instName));
		}
		
		/** return size of the valid range list */
		public int size() {
			return ranges.size();
		}
		
		/** return start of range at specified index */
		public RegNumber getStart(int idx) {
			return ranges.get(idx).getStart();
		}
		
		/** return end of range at specified index */
		public RegNumber getEnd(int idx) {
			return ranges.get(idx).getEnd();
		}
		
		/** given end address, set last valid range element */
		public void setListEnd(RegNumber endSize) {
			// get the last valid range
			int idx = ranges.size() - 1;
			AddressRange lastRange = ranges.get(idx);
            // if last range is invalid remove it, else set end value
			RegNumber end = new RegNumber(endSize);
			end.subtract(new RegNumber(1));
			if (lastRange.getStart().isGreaterThan(end)) ranges.remove(idx);
			else lastRange.setEnd(end);
		}

		/** list ranges */
		public void list() {
			int idx = 0;
			System.out.println("valid address ranges:");
			for (AddressRange ar : ranges) {
				System.out.println((idx++) + ": start=" + ar.getStart() + ", end=" + ar.getEnd() );
			}
		}
		
		/** write comments with gap ranges */
		public void writeGapComments() {
			if (gaps.isEmpty()) return;
			int idx = 0;
			System.out.println("----------- decoder gaps found:");
			for (NamedAddressRange ar : gaps) {
				System.out.println("  // gap " + (++idx) + ": " + ar.getLabel());
				RegNumber start = new RegNumber(ExtParameters.getPrimaryBaseAddress());
				start.setVectorLen(ExtParameters.getLeafAddressSize());
				start.add(ar.getStart());
				RegNumber end = new RegNumber(ExtParameters.getPrimaryBaseAddress());
				end.setVectorLen(ExtParameters.getLeafAddressSize());
				end.add(ar.getEnd());
				//System.out.println("  .addr_start(" + ar.getStart() + "), .addr_end(" + ar.getEnd() + ")," );
				System.out.println("  .addr_start(" + start.toFormat(NumBase.Hex, NumFormat.Verilog) + 
						            "), .addr_end(" + end.toFormat(NumBase.Hex, NumFormat.Verilog) + ")," );
			}
			System.out.println("----------- ");
		}

		private class AddressRange {
		   private RegNumber start;
		   private RegNumber end;

		   public AddressRange(RegNumber start, RegNumber end) {
			   this.start = start;
			   this.end = end;
		   }
		   /** get start
		    */
		   public RegNumber getStart() {
			   this.start.setVectorLen(ExtParameters.getLeafAddressSize());
			   return start;
		   }
		   /** get end
		    */
		   public RegNumber getEnd() {
			   this.end.setVectorLen(ExtParameters.getLeafAddressSize());
			   return end;
		   }
		   /** set end
		    *  @param end the end to set
		    */
		   public void setEnd(RegNumber end) {
			   this.end = end;
		   }
		}
		
		private class NamedAddressRange extends AddressRange {
			   private String label;
			   
			   public NamedAddressRange(RegNumber start, RegNumber end, String label) {
				   super(start, end);
				   this.setLabel(label);
			   }

			/** get label
			 *  @return the label
			 */
			public String getLabel() {
				return label;
			}

			/** set label
			 *  @param label the label to set
			 */
			public void setLabel(String label) {
				this.label = label;
			}
		}
	
	}	
	
	/** return string list of signal names from verilogsig list */
	protected static List<String> getNameList (List<SystemVerilogSignal> sigList) {
		List<String> retStr = new ArrayList<String>();
		for (SystemVerilogSignal vsig : sigList) retStr.add(vsig.getDefName());
		return retStr;
	}
		
	//---------------------------- methods to output verilog ----------------------------------------

	/** write systemverilog output to specified output file(s)  
	 * @param outName - output file or directory
	 * @param description - text description of file generated
	 * @param commentPrefix - comment chars for this file type */
	@Override
	public void write(String outName, String description, String commentPrefix) {		
		// before starting write, check that this addrmap is valid
		//int mapSize = this.getMapAddressWidth();
		//if (mapSize < 1) Ordt.errorExit("Minimum allowed address map size is " + (this.getMinRegByteWidth() * 2) + "B (addrmap=" + getAddressMapName() + ")");
		if (decoder.getDecodeList().isEmpty()) Ordt.errorExit("Minimum allowed address map size is " + this.getMinRegByteWidth() + "B (addrmap=" + getAddressMapName() + ")");
		//System.out.println("SystemVerilogBuilder write: Minimum allowed address map size is " + (this.getMinRegByteWidth() * 2) + "B, mapSize=" + mapSize + " (addrmap=" + getAddressMapName() + ")");

		// determine if a single output file or multiple
		boolean multipleOutputFiles = outName.endsWith("/");
   	    
		//genPioInterfaceSignals();   // add the pio interface and internal decoder signals
		//addressRanges.list();                     
		addressRanges.writeGapComments();

		// if multiple file output generate one ea for top, logic, decode
		if (multipleOutputFiles) {
			File saveDir = new File(outName);
	   	    saveDir.mkdirs();   // make sure directory exists
	   	    String extension = legacyVerilog? ".v" : ".sv";
	   	    
			// write the top level module
			writeTop(outName + getModuleName() + "_pio" + extension, description, commentPrefix);

			// write the logic module
			writeLogic(outName + getModuleName() + "_jrdl_logic" + extension, description, commentPrefix);
			
			// write the decode module
			writeDecode(outName + getModuleName() + "_jrdl_decode" + extension, description, commentPrefix);
			
			// if IO interfaces are used, generate the interfaces and wrapper
			if ((usesInterfaces || ExtParameters.sysVerGenerateWrapperModule()) && !legacyVerilog) {
				writeInterfaces(outName + getModuleName() + "_pio_interfaces.sv", description, commentPrefix);
				writeTopWrapperModules(true, true, outName, getModuleName() + "_pio_iwrap", ".sv", description, commentPrefix);
			}
			// verilog wrapper
			else if (ExtParameters.sysVerGenerateWrapperModule() && legacyVerilog) {
				writeTopWrapperModules(false, true, outName, getModuleName() + "_pio_iwrap", ".v", description, commentPrefix);
				//System.out.println("SystemVerilogBuilder write:   ExtParameters.sysVerilogAlwaysGenerateIwrap()= " + ExtParameters.sysVerilogAlwaysGenerateIwrap());
			}
			
			// loop through nested addrmaps and write these VerilogBuilders
			for (SystemVerilogBuilder childBuilder: childAddrMaps) {
				//System.out.println("--- VerilogBuilder - writing child");
				childBuilder.write(outName, description, commentPrefix);
			}		
		}	
		// otherwise just use single file
		else {
	    	BufferedWriter bw = openBufferedWriter(outName, description);
	    	if (bw != null) {
	    		// set bw as default
	    		bufferedWriter = bw;

	    		// write the file header
	    		writeHeader(commentPrefix);
	    		
				// write all modules
				write(bw);

	    		closeBufferedWriter(bw);
	    	}
		}
	}
	
	/** write top module output to specified output file  
	 * @param outName - output file or directory
	 * @param description - text description of file generated
	 * @param commentPrefix - comment chars for this file type */
	public void writeTop(String outName, String description, String commentPrefix) {
    	BufferedWriter bw = openBufferedWriter(outName, description);
    	if (bw != null) {
    		// set bw as default
    		bufferedWriter = bw;

    		// write the file header
    		writeHeader(commentPrefix);
    		
    		// now write the output
	    	top.write();
    		closeBufferedWriter(bw);
    	}
	}
	
	/** write logic module output to specified output file  
	 * @param outName - output file or directory
	 * @param description - text description of file generated
	 * @param commentPrefix - comment chars for this file type */
	public void writeLogic(String outName, String description, String commentPrefix) {
    	BufferedWriter bw = openBufferedWriter(outName, description);
    	if (bw != null) {
    		// set bw as default
    		bufferedWriter = bw;

    		// write the file header
    		writeHeader(commentPrefix);
    		
    		// now write the output
	    	logic.write();
    		closeBufferedWriter(bw);
    	}
	}

	/** write decode module output to specified output file  
	 * @param outName - output file or directory
	 * @param description - text description of file generated
	 * @param commentPrefix - comment chars for this file type */
	public void writeDecode(String outName, String description, String commentPrefix) {
    	BufferedWriter bw = openBufferedWriter(outName, description);
    	if (bw != null) {
    		// set bw as default
    		bufferedWriter = bw;

    		// write the file header
    		writeHeader(commentPrefix);
    		
    		// now write the output
	    	decoder.write();
    		closeBufferedWriter(bw);
    	}
	}

	/** write interface defines to specified output file  
	 * @param outName - output file or directory
	 * @param description - text description of file generated
	 * @param commentPrefix - comment chars for this file type */
	public void writeInterfaces(String outName, String description, String commentPrefix) {
    	BufferedWriter bw = openBufferedWriter(outName, description);
    	if (bw != null) {
    		// set bw as default
    		bufferedWriter = bw;

    		// write the file header
    		writeHeader(commentPrefix);
    		
    		// now write the output
	    	writeInterfaces();
    		closeBufferedWriter(bw);
    	}
	}

	/** write wrap module and any required wrapper transform modules to specified output file(s)  
	 * @param useInterfaces - if true, wrapper will use hierarchical
	 * @param separateXformFiles - if true, xform modules will be output in separate files
	 * @param outPrefix - output file path prefix to be used for wrapper and generated xform modules if using separate files
	 * @param outName - output wrapper file base name if using separate files
	 * @param outSuffix- output file suffix to be used for wrapper and all generated xform modules if using separate files 
	 * @param description - text description of file generated
	 * @param commentPrefix - comment chars for this file type */
	public void writeTopWrapperModules(boolean useInterfaces, boolean separateXformFiles, 
			String outPrefix, String outName, String outSuffix, String description, String commentPrefix) {
        // override bufferedWriter if separate output files specified
		if (separateXformFiles) {
			BufferedWriter bw = openBufferedWriter(outPrefix + outName + outSuffix, description);
	    	if (bw == null) return;   // exit on file error
	    	bufferedWriter = bw;  // set bw as default  TODO - ugly, fix bw override handling
	    	writeHeader(commentPrefix); // write the file header
		}
		// create wrapper module 
		SystemVerilogWrapModule intfWrapper = createTopWrapperModule(useInterfaces);
		// write xform modules
		intfWrapper.writeXformModules(separateXformFiles, outPrefix, outSuffix, commentPrefix); 
		// write wrapper module
		intfWrapper.write();

		if (separateXformFiles) closeBufferedWriter(bufferedWriter);
	}
	
	/** write wrap module and any required wrapper transform modules to default writer  
	 * @param useInterfaces - if true, wrapper will use hierarchical */
	public void writeTopWrapperModules(boolean useInterfaces) {
		writeTopWrapperModules(useInterfaces, false, "", "", "", "", "");
	}

	/** generate the wrapper module
	 * @param useInterfaces - if true, hierarchical IO will be allowed in wrapper
	 * @param separateXformFiles - if true, xform modules will be output in separate files
	 * @param outPrefix - prefix to be used on generated xform modules if using separate files
	 * @param outSuffix- suffix to be used on generated xform modules if using separate files 
	 */
	protected  SystemVerilogWrapModule createTopWrapperModule(boolean useInterfaces) {		
		SystemVerilogWrapModule intfWrapper = new SystemVerilogWrapModule(this, 0, defaultClk, getDefaultReset());
		intfWrapper.setName(getModuleName() + "_pio_iwrap");
		intfWrapper.setUseInterfaces(useInterfaces);
		// add pio_top instance
		intfWrapper.addInstance(top, "pio");
        // create wrapper
		intfWrapper.generateWrapperInfo();
		return intfWrapper;
	}

	// ------------------
	
	/** write out the systemverilog if a child builder using the same output file
	 * @param bw */
	public void write(BufferedWriter bw) {
		// set bw as default
		bufferedWriter = bw;
		
		// before starting write, check that this addrmap is valid
		//int mapSize = this.getMapAddressWidth();
		if (decoder.getDecodeList().isEmpty()) Ordt.errorExit("Minimum allowed address map size is " + this.getMinRegByteWidth() + "B (addrmap=" + getAddressMapName() + ")");
		//System.out.println("SystemVerilogBuilder write: Minimum allowed address map size is " + (this.getMinRegByteWidth() * 2) + "B, mapSize=" + getCurrentMapSize() + " - " + mapSize + "b (addrmap=" + getAddressMapName() + ")");
		//System.out.println("SystemVerilogBuilder write:   Decoder elements= " + decoder.getDecodeList().size());

		// write the logic module
		logic.write();   
		
		// write the decode module
		decoder.write();  
		
		// write the top level module
		top.write();
		
		// if IO interfaces are used, generate the systemerilog interfaces and wrapper
		if ((usesInterfaces  || ExtParameters.sysVerGenerateWrapperModule()) && !legacyVerilog) {
			writeInterfaces();
			writeTopWrapperModules(true);
		}
		// verilog wrapper
		else if (ExtParameters.sysVerGenerateWrapperModule() && legacyVerilog) {
			writeTopWrapperModules(false);
			//System.out.println("SystemVerilogBuilder write:   ExtParameters.sysVerilogAlwaysGenerateIwrap()= " + ExtParameters.sysVerilogAlwaysGenerateIwrap());
		}
					
		// loop through nested addrmaps and write these VerilogBuilders
		for (SystemVerilogBuilder childBuilder: childAddrMaps) {
			//System.out.println("--- VerilogBuilder - writing child");
			childBuilder.write(bw);
		}
	}
	
	/** write out the interface/struct defines */
	protected  void writeInterfaces() {
		// get a list of all sv interfaces to be defined
		List<SystemVerilogIOSignalSet> sigSets = hwSigList.getNonVirtualSignalSets(true);
		
		// uniquify structure names if reuse option enabled
		if (ExtParameters.sysVerReuseIwrapStructures()) {
			//System.out.println("SystemVerilogBuilder writeInterfaces: found " + sigSets.size() + " structures");
			HashMap<SystemVerilogIOSignalSet, String> uniqueStructures = new HashMap<SystemVerilogIOSignalSet, String>();
			HashSet<String> uniqueCompIds = new HashSet<String>();
			Iterator<SystemVerilogIOSignalSet> it = sigSets.iterator();
			while(it.hasNext()) {
				SystemVerilogIOSignalSet sset = it.next();
				// if a duplicate of an existing structure, use the existing structure's type
				if (uniqueStructures.containsKey(sset)) {
					sset.setType(uniqueStructures.get(sset));  // replace type for this sigset
					it.remove();  // remove this sigset from the list needing defines
					//System.out.println("   " + sset.getType() + " is a dup of compId=" + uniqueStructures.get(sset));
				}
				// otherwise this is a new unique structure
				else {
					String newType = sset.getCompIdType(); // get structure name based on component id
					String uniqueType = ((sset.getCompId()!=null) && !uniqueCompIds.contains(newType))? newType : sset.getType();  // use newType if it is unique
					uniqueStructures.put(sset, uniqueType);
					uniqueCompIds.add(uniqueType);  // save the new structure name to insure it isnt reused
					sset.setType(uniqueType);  // replace type for this sigset
					//System.out.println("   " + sset.getType() + " is new, compId=" + uniqueType);
				}
			}
			//System.out.println("SystemVerilogBuilder writeInterfaces: found " + sigSets.size() + " unique structures");
		}
		
		// now write the defines
		for (SystemVerilogIOSignalSet sset : sigSets) writeIOSignalSetDefine(sset);
	}

	/** write out an interface define */
	private void writeIOSignalSetDefine(SystemVerilogIOSignalSet sset) {
		int indentLevel = 0;
		// set interface name and list of sigs
		List<String> intfDefStrings = sset.getDefStrings();
		// write the interface
		if (intfDefStrings.size() > 0) {
			for (String defStr: intfDefStrings) writeStmt(indentLevel, defStr);   
		}
		//System.out.println("SystemVerilogBuilder writeInterface: " + intfName + ", def str n=" + intfDefStrings.size());	
	}

	// ------------------ testbench support methods
	
	/** recursively add external child module io to specified modules IO list */  
	protected  void addDutIOs(SystemVerilogModule mod) {
		// if this is base builder add the cntl list
		if (isBaseBuilder()) mod.useIOList(cntlSigList, null);
		mod.useIOList(pioSigList, null);  // signals to/from pio
		mod.useIOList(hwSigList, null);  // signals to/from hw
        // now add child io recursively
		for (SystemVerilogBuilder childBuilder: childAddrMaps) {
			childBuilder.addDutIOs(mod);
		}		
	}
	
	/** recursively add all builder top modules as instances in the specified module  */  
	protected void addDutInstances(SystemVerilogModule mod) {
		mod.addInstance(top, "pio_dut_" + getBuilderID());  // add the top module
        // now add child modules
		for (SystemVerilogBuilder childBuilder: childAddrMaps) {
			childBuilder.addDutInstances(mod);
		}		
	}
	
	/** recursively set default bufferedwriter to bw in all child builders so we can write to same file using module methods */
	protected void setChildBufferedWriters(BufferedWriter bw) {
		for (SystemVerilogBuilder childBuilder: childAddrMaps) {
			childBuilder.setBufferedWriter(bw);
			childBuilder.setChildBufferedWriters(bw);
		}		
	}
	
	// --------------------------
	
	/** write interface stmt */
	protected  void writeInterfaceBegin(int indentLevel, String interfaceName) {
		writeStmt(indentLevel, "//");
		writeStmt(indentLevel, "//---------- interface " + interfaceName);
		writeStmt(indentLevel, "//");
		writeStmt(indentLevel, "interface " + interfaceName + ";");		
	}
	
	/** write module end */
	protected  void writeInterfaceEnd(int indentLevel) {
		writeStmt(indentLevel, "endinterface\n");	
	}

	// --------------------------
	
	/** get module name for this VerilogBuilder
	 *  @return the moduleName
	 */
	public String getModuleName() {
		if (getModulePrefix() != null) return getModulePrefix() + "_" + getAddressMapName();
		return getAddressMapName() + ExtParameters.getSystemverilogModuleTag();  // add a tag to base regmap if needed
	}

	/** get modulePrefix
	 *  @return the modulePrefix
	 */
	public String getModulePrefix() {
		return modulePrefix;
	}

	/** set modulePrefix
	 *  @param modulePrefix the modulePrefix to set
	 */
	public void setModulePrefix(String modulePrefix) {
		this.modulePrefix = modulePrefix;
	}

	/** return true if base address parameter is to be added to base module and decoder */ 
	public boolean addBaseAddressParameter() {
		return decoder.usesBaseAddress() && ExtParameters.systemverilogBaseAddrIsParameter();
	}

	/** return address bit width from current addrmap size (high bit of mapSize-1) */
	public int getMapAddressWidth() {
		return getAddressWidth(getCurrentMapSize());
	}
	
	/** generate a (little endian) array string corresponding to the internal address range for addrmap */  
	public String genDefAddressArrayString() {
		int lowIndex = getAddressLowBit();   // remove low bits using reg width in bytes
		// compute total address map size
		int size = getMapAddressWidth();   // get high bit of mapSize-1  
	   	return SystemVerilogSignal.genDefArrayString(lowIndex, size);  
	}
	
	/** generate a (little endian) reference array string corresponding to the internal address range for addrmap */  
	public String genRefAddressArrayString() {
		int lowIndex = getAddressLowBit();   // remove low bits using reg width in bytes
		// compute total address map size
		int size = getMapAddressWidth();   // get high bit of mapSize-1  
	   	return SystemVerilogSignal.genRefArrayString(lowIndex, size);  
	}
	
	/** return the bit string for block select compare */
	public String getBlockSelectBits() {
		int lowIndex = getAddressLowBit();   // low bit in internal address
		// compute total address map size
		int size = getMapAddressWidth();   // get high bit of mapSize-1  
	   	int lowSelectBit =  lowIndex + size;  
		int selectSize = ExtParameters.getLeafAddressSize() - lowSelectBit;
		return SystemVerilogSignal.genRefArrayString(lowSelectBit, selectSize);
	}
	
	/** return the number of bits to select max sized wide reg */   
	protected int getMaxWordBitSize() {
		return Utils.getBits(getMaxRegWordWidth());
	}

	/** add to the assign list for a register - called by logic module when building fields */
	public void addToTempAssignList(String assignStr) {
		   tempAssignList.add(assignStr);		
	}
	
	/** return true if specified name is a user-defined signal name */
	public boolean isUserDefinedSignal(String name) {
		return model.isUserDefinedSignal(name);  
	}

}
