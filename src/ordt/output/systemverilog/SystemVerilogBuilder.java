/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved.
 */
package ordt.output.systemverilog;

import java.io.BufferedWriter;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ordt.extract.Ordt;
import ordt.extract.ModInstance;
import ordt.extract.RegModelIntf;
import ordt.extract.RegNumber;
import ordt.extract.RegNumber.NumBase;
import ordt.extract.RegNumber.NumFormat;
import ordt.output.FieldProperties;
import ordt.output.InstanceProperties;
import ordt.output.OutputBuilder;
import ordt.output.RegProperties;
import ordt.output.RhsReference;
import ordt.output.FieldProperties.RhsRefType;
import ordt.output.InstanceProperties.ExtType;
import ordt.parameters.ExtParameters;
import ordt.parameters.Utils;
import ordt.parameters.ExtParameters.SVDecodeInterfaceTypes;

/**
 *  @author snellenbach      
 *  Jan 27, 2014
 */
public class SystemVerilogBuilder extends OutputBuilder {
	
	private String modulePrefix = null;  // module name prefix for nested addressmaps
	protected static boolean legacyVerilog = false;  // by default generate output with system verilog constructs (only valid in write stage)
	
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
	protected static final Integer HW = 1;
	protected static final Integer LOGIC = 2;
	protected static final Integer DECODE = 4;
	protected static final Integer PIO = 8;
	protected boolean usesInterfaces = false;  // detect if sv interfaces are needed  

    // define IO lists
	protected SystemVerilogIOSignalList cntlSigList = new SystemVerilogIOSignalList();   // clocks/resets
	protected SystemVerilogIOSignalList hwSigList = new SystemVerilogIOSignalList();     // logic to/from hw
	protected SystemVerilogIOSignalList pioSigList = new SystemVerilogIOSignalList();    // pio to/from decoder
	protected SystemVerilogIOSignalList intSigList = new SystemVerilogIOSignalList();    // logic to/from decoder
	
	// module defines  
	protected SystemVerilogDecodeModule decoder = new SystemVerilogDecodeModule(this, DECODE, decodeClk);
	protected SystemVerilogLogicModule logic = new SystemVerilogLogicModule(this, LOGIC, logicClk);
	protected SystemVerilogModule top = new SystemVerilogModule(this, DECODE|LOGIC, defaultClk);
	
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
		initIOLists();  // setup IO lists for logic, decode, and top modules
		decoder.setInterfaceType(ExtParameters.getSysVerRootDecoderInterface()); // set root pio interface type from specified params
		model.getRoot().generateOutput(null, this);   // generate output structures recursively starting at model root
	}
	
	// constructor for addressmap children
	public SystemVerilogBuilder(SystemVerilogBuilder parentBuilder, boolean isTestModule) {
		setNewBuilderID();   // set unique ID of this instance
		this.model = parentBuilder.model;    // inherit parent model
	    setVisitEachReg(true);   // gen code for each reg
	    setVisitEachRegSet(true);   // gen code for each reg set
	    setVisitExternalRegisters(false);  // do not visit externals - only ext root will be visited
		setLegacyVerilog(SystemVerilogBuilder.isLegacyVerilog());  // cascade state for systemverilog construct gen
		initIOLists();  // setup IO lists for logic, decode, and top modules
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
	    // FIXME - need to handle external replicated reg, single reg , replicated regsets cases for full test gen
	    // also, wont work because startMap() and finishMap() are not called - would need to modify model itself or allow regset as sv root instance

	    // save the regProperties state and set external names
	    this.topRegProperties = parentBuilder.regProperties;
		//System.out.println("SystemVerilogBuilder creating new builder, regset id=" + regSetProperties.getId() + ", regset ext=" + regSetProperties.getExternalType());
		//System.out.println("SystemVerilogBuilder                     , topreg id=" + topRegProperties.getId() + ", topreg ext=" + topRegProperties.getExternalType());
		//System.out.println("SystemVerilogBuilder                     , parent mod=" + parentBuilder.getModuleName() + ", new mod=" + this.getModuleName());

		// if child is in a bb root region, change its interface to leaf
		if (topRegProperties.hasExternalType(ExtType.BBV5))
			decoder.setInterfaceType(SVDecodeInterfaceTypes.LEAF); 
		// else if a serial8 region use this decoder interface
		else if (topRegProperties.hasExternalType(ExtType.SERIAL8))
			decoder.setInterfaceType(SVDecodeInterfaceTypes.SERIAL8); 
		// else if a ring16 region use this decoder interface
		else if (topRegProperties.hasExternalType(ExtType.RING16))
			decoder.setInterfaceType(SVDecodeInterfaceTypes.RING16); 

	    // now generate output starting at this regmap
		//System.out.println("SystemVerilogBuilder - regset inst id=" + regSetProperties.getId() + ", inst stack top=" + instancePropertyStack.peek().getId());
	    ModInstance mapInstance = regSetProperties.getExtractInstance();
		mapInstance.getRegComp().generateOutput(mapInstance, this);   // generate output structures recursively starting at new adressmap root
		//System.out.println("--- VerilogBuilder - creating child, inst=" + mapInstance.getFullId() + ", base addr=" + getBaseAddress());
		//mapInstance.getRegComp().display(null);
	}
	
	/** initialize signal lists used by generated modules */
	private void initIOLists() {
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
	}
	
	/** set legacyVerilog
	 *  @param boolean legacyVerilog value to set
	 */
	public static void setLegacyVerilog(boolean legacy) {
		legacyVerilog = legacy;
	}
	
	/** return true if ouput will be legacy verilog */
	public static boolean isLegacyVerilog() {
	   return legacyVerilog;	
	}
	
	/** pull over and update current regset and stacks */
	private void updateRegSetState(SystemVerilogBuilder parentBuilder) {
	    this.regSetProperties = parentBuilder.regSetProperties;  // this is a ref so all stack info changes - restore external when done?
	    this.regSetProperties.setExternal(null); // no longer external now that we're in child addrmap
	    this.regSetProperties.setRootExternal(false);
	    this.instancePropertyStack.addAll(parentBuilder.instancePropertyStack);
	    this.regSetPropertyStack.addAll(parentBuilder.regSetPropertyStack);
		//System.out.println("SystemVerilogBuilder updateRegSetState: updating state for path=" + getInstancePath() + ", rs base=" + regSetProperties.getBaseAddress());
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
				defaultReset = signalProperties.getRtlName();
				defaultResetActiveLow = signalProperties.isActiveLow();
				return;
			}
		}
		else if (signalProperties.isLogicReset()) {
			if (resetsLocked) Ordt.errorMessage("field_reset property ignored for signal " + signalProperties.getInstancePath());
			else {
				logicReset = signalProperties.getRtlName();
				logicResetActiveLow = signalProperties.isActiveLow();
				return;
			}			
		}
		
		// add user signal to hashmap of defined signals
		logic.addUserDefinedSignal(signalProperties.getRtlName(), signalProperties);
		//Jrdl.infoMessage("SystemVerilogBuilder: signal=" + signalProperties.getBaseName() + ", low=" + signalProperties.getLowIndex() + ", size=" + signalProperties.getSignalWidth());
		
		// if signal is lhs of assignment then add to logic assign/logic lists/output, else add it as an input.
		// io assign is done here so interface wrapper contains user sigs - all user sigs are either inputs or outputs
		// assign of signal is made after builder processing when all rhs references can be resolved.
		if (signalProperties.hasAssignExpr()) {
			logic.addVectorReg(signalProperties.getRtlName(), signalProperties.getLowIndex(), signalProperties.getSignalWidth());  
			logic.addVectorTo(HW, signalProperties.getRtlName(), 0, signalProperties.getSignalWidth());   // add output
			//System.out.println("SystemVerilogBuilder addSignal: signal basename=" + signalProperties.getRtlName() + ", sig.assignRefName=" + refName+ ", sigRef.getRawReference=" + signalProperties.getAssignRef().getRawReference());
            // add each signal in expression rhs to the master list of rhs signals
			List<RhsReference> rhsRefList = signalProperties.getAssignExpr().getRefList(); 
			for (RhsReference ref: rhsRefList) {
				String refName = ref.getReferenceName(signalProperties, false); 
				refName = logic.resolveAsSignalOrField(refName);
				logic.addRhsSignal(refName, getInstancePath() , ref.getRawReference());				
			}
		}
		else {
			//logic.addVectorWire(signalProperties.getRtlName(), signalProperties.getLowIndex(), signalProperties.getSignalWidth());  
			logic.addVectorFrom(HW, signalProperties.getRtlName(), 0, signalProperties.getSignalWidth());   // add input
		}
	}
	/*
					// if not used internally, add an output
				if (!sig.isRhsReference())
					this.addVectorTo(SystemVerilogBuilder.HW, sig.getRtlName(), 0, sig.getSignalWidth());   // add output
			}
			// if not assigned a ref, must be an input
			else {
				this.addVectorFrom(SystemVerilogBuilder.HW, sig.getRtlName(), 0, sig.getSignalWidth());   // add input

	 */
	
	/** add a field for a particular output */
	@Override
	public  void addField() {
		   // if new interface then add it to logic-hw list
		   startNewInterfaces(fieldProperties);
		   
		   // generate field write assignment stmts
		   genFieldWriteStmts();
		   
		   // generate field read assignment stmts
		   genFieldReadStmts();
		   
		   // add coverage info
		   if (!regProperties.isExternal()) logic.addFieldCoverPoints(fieldProperties);
		   
		   // if new interface was added, indicate finished using it
		   endNewInterfaces(fieldProperties);
	}

	/** add a field for a particular output */
	@Override
	public  void addAliasField() {
		   //System.out.println("VerilogBuilder: addAliasField " + fieldProperties.getInstancePath());
		   // if new interface then add it to logic-hw list
		   startNewInterfaces(fieldProperties);
		   
		   // generate alias field write assignment stmts
		   if (fieldProperties.swChangesValue()) {  // if sw can write or rclr/rset
			   genSwFieldNextWriteStmts(null, false);    // create statements to set value of next based on sw field settings 
		   }		   
		   
		   // generate alias field read assignment stmts
		   genSwFieldReadStmts();
		   
		   // if new interface was added, indicate finished using it
		   endNewInterfaces(fieldProperties);
	}

	/** add a register for a particular output */
	@Override
	public  void addRegister() {  
		   if (!resetsLocked) lockResets();  
		   if (!regProperties.isExternal()) decoder.addToDecode(regProperties);    // put reg info on address list for decoder gen

		   // if new interface then add it to logic-hw list
		   startNewInterfaces(regProperties);
		   
		   // create verilog names
		   String decodeToLogicName = regProperties.getDecodeToLogicName();   
		   String logicToDecodeName = regProperties.getLogicToDecodeName(); 
		   String swWeName = regProperties.getDecodeToLogicWeName(); 
		   String swReName = regProperties.getDecodeToLogicReName(); 
		   
		   // add to verilog structure lists
		   intSigList.addVector(DECODE, LOGIC, decodeToLogicName, 0, regProperties.getRegWidth());    // add write data to decode to logic signal list 
		   intSigList.addScalar(DECODE, LOGIC, swWeName);    // add we to decode to logic signal list 
		   intSigList.addScalar(DECODE, LOGIC, swReName);    // add re to decode to logic signal list 
		   intSigList.addVector(LOGIC, DECODE, logicToDecodeName, 0, regProperties.getRegWidth());    // add read data to logic to decode signal list 
		   
		   // if register has an interrupt output then add and initialize
		   if (regProperties.hasInterruptOutputDefined()) {
			   String intName = regProperties.getLogicToHwIntrName(); 
			   hwSigList.addScalar(LOGIC, HW, intName);    // add intr output signal  
			   logic.addScalarReg(intName);  
			   logic.addCombinAssign(regProperties.getBaseName(), intName + " = 1'b0;");    // init interrupt to 0
		   }
		   
		   tempAssignList.clear();   // clear the temp assign list
	}

	/** finish a register for a particular output */
	@Override
	public  void finishRegister() {
	       // handle registers that need unused fields zero'd out
		   logic.addVectorReg(regProperties.getLogicToDecodeName(), 0, regProperties.getRegWidth());  // make logic outputs regs
		   if (regProperties.hasNopBits()) {
			   logic.addCombinAssign(regProperties.getBaseName() + " (pio read data)", regProperties.getLogicToDecodeName() + " = " + regProperties.getRegWidth() + "'b0;");    
			   logic.addCombinAssign(regProperties.getBaseName() + " (pio read data)", tempAssignList);    
		   }
		   else  logic.addCombinAssign(regProperties.getBaseName() + " (pio read data)", tempAssignList);   // fields use all bits in this register 
		   
		   decoder.addVectorReg(regProperties.getDecodeToLogicName(), 0, regProperties.getRegWidth());  // make decode outputs regs
		   decoder.addScalarReg(regProperties.getDecodeToLogicWeName()); // add we and re output defs
		   decoder.addScalarReg(regProperties.getDecodeToLogicReName()); 
		   
		   // if new interface was added, indicate finished using it
		   endNewInterfaces(regProperties);
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
		   
		   startNewInterfaces(regProperties);  // if an interface is specified add it

		   // generate common external interface constructs
		   decoder.generateBaseExternalInterface(regProperties);
		   
		   //System.out.println("SystemVerilogBuilder.addRootExternalRegisters, inst=" + regProperties.getInstancePath() + ", isAddressMap=" + regProperties.isAddressMap() + ", baseAddress=" + regProperties.getBaseAddress());
		   //System.out.println("    SystemVerilogBuilder.addRootExternalRegisters, regset inst=" + regSetProperties.getInstancePath() + ", regset isAddressMap=" + regSetProperties.isAddressMap());
		   
		   // if an addrmap create a new VerilogBuilder at new root instance and push onto list
		   if (ExtParameters.sysVerGenerateChildAddrmaps() && regProperties.isAddressMap()) {
			   SystemVerilogBuilder childVerilog = new SystemVerilogBuilder(this, false); // inherit some parent properties
			   //System.out.println("SystemVerilogBuilder addRootExternalRegisters: current regset=" + regSetProperties.getInstancePath() + ", mod prefix=" + modulePrefix+ ", bid=" + getBuilderID() + ", mlevel=" + currentMapLevel());  
			   //System.out.println("SystemVerilogBuilder addRootExternalRegisters: reg=" + regProperties.getInstancePath() + ", regset=" + regSetProperties.getInstancePath() + ", mod prefix=" + modulePrefix + ", bId=" + getBuilderID() + ", mlevel=" + currentMapLevel() + ", testModule=" + isTestBuilder());  
			   childAddrMaps.add(childVerilog);
		   }
		   // otherwise if building external regs for test create a builder (if not already a testbuilder)
		   else if (ExtParameters.sysVerGenerateExternalRegs() && !isTestBuilder()) {
			   //regProperties.getExtractInstance().getRegComp().setCompType(CompType.ADDRMAP);  // TODO - this doesnt work?
			   SystemVerilogBuilder childVerilog = new SystemVerilogBuilder(this, true); // inherit some parent properties and mark child as testBuilder
			   //System.out.println("SystemVerilogBuilder addRootExternalRegisters: reg=" + regProperties.getInstancePath() + ", regset=" + regSetProperties.getInstancePath() + ", mod prefix=" + modulePrefix + ", bId=" + getBuilderID() + ", mlevel=" + currentMapLevel() );  
			   childAddrMaps.add(childVerilog);
		   }
		   
		   // if a special i/f type on this external
		   if (regProperties.hasExternalType(ExtType.BBV5)) decoder.generateExternalInterface_BBV5(regProperties);
		   else if (regProperties.hasExternalType(ExtType.SRAM)) decoder.generateExternalInterface_SRAM(regProperties);
		   else if (regProperties.hasExternalType(ExtType.SERIAL8)) decoder.generateExternalInterface_SERIAL8(regProperties);
		   else if (regProperties.hasExternalType(ExtType.RING16)) decoder.generateExternalInterface_RING16(regProperties);
		   
		   endNewInterfaces(regProperties);  // close out interface 
	}
	
	/** add a non-root addressmap - overridden by child builders (sv builder) */
	@Override
	protected void addNonRootExternalAddressMap() {
		   SystemVerilogBuilder childVerilog = new SystemVerilogBuilder(this, false); // inherit some parent properties  TODO - send in base address separately?
		   //System.out.println("SystemVerilogBuilder addNonRootExternalAddressMap: regset=" + regSetProperties.getInstancePath() + ", mod prefix=" + modulePrefix + ", bId=" + getBuilderID() + ", mlevel=" + currentMapLevel() + ", testModule=" + isTestBuilder());  
		   if (currentMapLevel() < 3) childAddrMaps.add(childVerilog);  // only add to child list if no other map is in parent hierarchy
	}

	/** add a register set for a particular output */
	@Override
	public  void addRegSet() {
		   // if new interface then add it to logic-hw list
		   startNewInterfaces(regSetProperties);
	}

	/** finish a register set for a particular output */
	@Override
	public  void finishRegSet() {	
		   // if new interface was added, indicate finished using it
		   endNewInterfaces(regSetProperties);
	}

	/** add a register map for a particular output */
	@Override
	public  void addRegMap() {
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
		decoder.genPioInterface(topRegProperties);
		// load list of clock, reset IOs   
		loadCntlSigList();
		// set names, etc of generated modules before write (so child classes can use in write override)
		initModuleInfo();
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

	/** return the default reset value */
	public String getDefaultReset() {
		return defaultReset;
	}

	/** return the addressRanges list */
	public ValidAddressRanges getAddressRanges() {
		return addressRanges;
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
			cntlSigList.addScalar(HW, DECODE, decodeClk);
			cntlSigList.addScalar(HW, LOGIC, logicClk);
		}
		// otherwise add single clock to both internal modules
		else cntlSigList.addScalar(HW, LOGIC|DECODE, decodeClk);
		
		// defined resets to input list 
		if (logicReset != null)
			cntlSigList.addScalar(HW, DECODE, defaultReset);  // logic module has its own reset so add separate decode module reset
		else
			cntlSigList.addScalar(HW, DECODE|LOGIC, defaultReset);  // else a common reset
		// add non-default logic resets
		for (String reset :logic.getRegisters().getResets().keySet())
			if (!reset.equals(defaultReset)) cntlSigList.addScalar(HW, LOGIC, reset);
	}
	
	/** initialize module info at end of builder extract */
	private void initModuleInfo() {
		//System.out.println("SystemVerilogBuilder initModuleInfo: moduleName=" + getModuleName() );
		// init decoder 
		decoder.setName(getModuleName() + "_jrdl_decode");
		decoder.setAddBaseAddrParameter(addBaseAddressParameter());
		
		// init logic 
		logic.setName(getModuleName() + "_jrdl_logic");
		
		// init top
		top.setName(getModuleName() + "_pio");  // set name
		top.setAddBaseAddrParameter(addBaseAddressParameter());
		// wire define stmts for signals between decoder and logic
		top.addWireDefs(intSigList.getSignalList(DECODE, LOGIC));
		top.addWireDefs(intSigList.getSignalList(LOGIC, DECODE));
		// add decode and logic modules
		top.addInstance(decoder, "pio_decode");
		top.addInstance(logic, "pio_logic");
	}
	
	/** add IO interface structure if this instance has interface property */
	private void startNewInterfaces(InstanceProperties properties) {
		   if (properties.useInterface() && !properties.isRootInstance()) {
			   usesInterfaces = true;
			   //System.out.println("*** SystemVerilogBuilder startNewInterfaces: name=" + properties.getBaseName() + ", repCount=" + properties.getRepCount());
			   if (properties.isExternal())
			      hwSigList.addInterface(DECODE, HW, "dh_" + getIndexedBaseName(), "dh_" + properties.getBaseName(), 1, properties.getExtInterfaceName());
			   else
				  hwSigList.addInterface(LOGIC, HW, "lh_" + getIndexedBaseName(), "lh_" + properties.getBaseName(), properties.getRepCount(), properties.getExtInterfaceName());
		   }		
	}

	/** close out active IO interface if this instance has interface property */
	private void endNewInterfaces(InstanceProperties properties) {
		if (properties.useInterface() && !properties.isRootInstance()) {
			//System.out.println("*** Popping interface:" + properties.getBaseName());
			hwSigList.popInterface();
		}
	}

	/** generate verilog statements to write field flops */  // TODO - move these methods to SystemVerilogLogicBuilder
	private  void genFieldWriteStmts() {
		   // get field-specific verilog signal names
		   String hwToLogicDataName = fieldProperties.getHwToLogicDataName();  // hwBaseName + "_w" 
		   String fieldRegisterName = fieldProperties.getFieldRegisterName();  //"reg_" + hwBaseName;
		   
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
					   logic.addVectorReg(fieldRegisterName, 0, fieldProperties.getFieldWidth());  // add field register to define list
					   hwSigList.addVector(HW, LOGIC, hwToLogicDataName, 0, fieldProperties.getFieldWidth());  // add write data input
					   logic.addCombinAssign(regProperties.getBaseName(), fieldRegisterName + " =  " + hwToLogicDataName + ";");
				   }
			   }
			   // nothing writable so assign to a constant 
			   else {
				   //System.out.println("SystemVerilogBuilder genFieldWriteStmts constant field, id=" + fieldProperties.getId() + 
				   //	   ", writeable=" + fieldProperties.isHwWriteable() + ", intr=" + fieldProperties.isInterrupt() + ", changes val=" + fieldProperties.hwChangesValue());
				   if (fieldProperties.getReset() != null ) {
					   logic.addVectorWire(fieldRegisterName, 0, fieldProperties.getFieldWidth());  // add field to wire define list
					   RegNumber resetValue = new RegNumber(fieldProperties.getReset());  // output reset in verilog format
					   resetValue.setNumFormat(RegNumber.NumFormat.Verilog);
					   logic.addWireAssign(fieldRegisterName + " = " + resetValue + ";");
				   }
				   else Ordt.errorMessage("invalid field constant - no reset value for non-writable field " + fieldProperties.getInstancePath());
			   }
		   }
	}
	
	/** create statements to define field registers and resets */
	private void genFieldRegWriteStmts() {
		   String fieldRegisterName = fieldProperties.getFieldRegisterName();  //"reg_" + hwBaseName;
		   String fieldRegisterNextName = fieldProperties.getFieldRegisterNextName();  //"reg_" + hwBaseName + "_next";
		   logic.addVectorReg(fieldRegisterName, 0, fieldProperties.getFieldWidth());  // add field registers to define list
		   logic.addVectorReg(fieldRegisterNextName, 0, fieldProperties.getFieldWidth());  // we'll be using next value since complex assign
		   // generate flop reset stmts
		   if (fieldProperties.getReset() != null ) {
			   String resetSignalName = defaultReset;
			   boolean resetSignalActiveLow = defaultResetActiveLow;
			   if (logicReset != null) {
				   resetSignalName = logicReset;
				   resetSignalActiveLow = logicResetActiveLow;
			   }
			   else if (fieldProperties.hasRef(RhsRefType.RESET_SIGNAL)) {
				   resetSignalActiveLow = false;  // user defined resets are active high 
				   resetSignalName = resolveRhsExpression(RhsRefType.RESET_SIGNAL);
			   }
			   logic.addReset(resetSignalName, resetSignalActiveLow);
			   RegNumber resetValue = new RegNumber(fieldProperties.getReset());  // output reset in verilog format
			   resetValue.setNumFormat(RegNumber.NumFormat.Verilog);
			   logic.addResetAssign(regProperties.getBaseName(), resetSignalName, fieldRegisterName + " <= #1 " + resetValue + ";");  // ff reset assigns			   
		   }
		   else if (!ExtParameters.sysVerSuppressNoResetWarnings()) Ordt.warnMessage("field " + fieldProperties.getInstancePath() + " has no reset defined");
		   
		   logic.addRegAssign(regProperties.getBaseName(),  fieldRegisterName + " <= #1  " + fieldRegisterNextName + ";");  // assign next to flop
	}

	/** create statements to set value of next based on field settings */ 
	private  void genFieldNextWriteStmts() {
		   // get field-specific verilog signal names
		   String hwToLogicDataName = fieldProperties.getHwToLogicDataName();  // hwBaseName + "_w" 
		   
		   String fieldRegisterName = fieldProperties.getFieldRegisterName();  //"reg_" + hwBaseName;  
		   String fieldRegisterNextName = fieldProperties.getFieldRegisterNextName();  //"reg_" + hwBaseName + "_next";
		   
		   // if hw is writable add the write data input
		   if (fieldProperties.isHwWriteable()) hwSigList.addVector(HW, LOGIC, hwToLogicDataName, 0, fieldProperties.getFieldWidth());

		   // first set default next value, if hw write w/o enable use hw data
		   if (fieldProperties.isHwWriteable()  && !fieldProperties.hasHwWriteControl() && !fieldProperties.isCounter()) 
			   logic.addCombinAssign(regProperties.getBaseName(), fieldRegisterNextName + " = " + hwToLogicDataName + ";");
		   // otherwise if a singlepulse field then default to zero value
		   else if (fieldProperties.isSinglePulse())
			   logic.addCombinAssign(regProperties.getBaseName(), fieldRegisterNextName + " = 0;");
		   // otherwise hold current registered data
		   else 
			   logic.addCombinAssign(regProperties.getBaseName(), fieldRegisterNextName + " = " + fieldRegisterName + ";");
		   	   
		   // set field precedence
		   boolean hwPrecedence = fieldProperties.hasHwPrecedence();
		   boolean swPrecedence = !(fieldProperties.hasHwPrecedence());
		   
		   // if a counter (special case, hw has precedence by default)  
		   if (fieldProperties.isCounter()) {
			   genCounterWriteStmts(hwPrecedence);
		   }
		   // if hw uses interrupt  (special case, hw has precedence by default)
		   else if (fieldProperties.isInterrupt()) {  
			   genInterruptWriteStmts(hwPrecedence);
		   }
		   
		   // if an explicit next assignment 
		   if (fieldProperties.hasRef(RhsRefType.NEXT)) {
			   String refName = resolveRhsExpression(RhsRefType.NEXT);
			   logic.addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, fieldRegisterNextName + " = " + refName + ";");	
		   }
		   
		   // if hw uses we
		   if (fieldProperties.hasWriteEnableH()) { 
			   String hwToLogicWeName = generateInputOrAssign(RhsRefType.WE, fieldProperties.getHwToLogicWeName(), 1, false, hwPrecedence); 
			   logic.addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, "if (" + hwToLogicWeName + ") " + fieldRegisterNextName + " = " + hwToLogicDataName + ";");				   
		   }
		   // if hw uses wel
		   else if (fieldProperties.hasWriteEnableL()) {  
			   String hwToLogicWelName = generateInputOrAssign(RhsRefType.WE, fieldProperties.getHwToLogicWelName(), 1, false, hwPrecedence); 
			   logic.addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, "if (~" + hwToLogicWelName + ") " + fieldRegisterNextName + " = " + hwToLogicDataName + ";");				   
		   }
		   
		   // if hw has hw set
		   if (fieldProperties.hasHwSet()) { 
			   String hwToLogicHwSetName = generateInputOrAssign(RhsRefType.HW_SET, fieldProperties.getHwToLogicHwSetName(), 1, false, hwPrecedence);
			   RegNumber constVal = new RegNumber(1);
			   constVal.setVectorLen(fieldProperties.getFieldWidth());
			   constVal.lshift(fieldProperties.getFieldWidth());
			   constVal.subtract(1);
			   logic.addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, "if (" + hwToLogicHwSetName + ") " + 
			      fieldRegisterNextName + " = " + constVal.toFormat(NumBase.Hex, NumFormat.Verilog) + ";");				   
		   }
		   
		   // if hw has hw clr
		   if (fieldProperties.hasHwClr()) { 
			   String hwToLogicHwClrName = generateInputOrAssign(RhsRefType.HW_CLR, fieldProperties.getHwToLogicHwClrName(), 1, false, hwPrecedence);
			   RegNumber constVal = new RegNumber(0);
			   constVal.setVectorLen(fieldProperties.getFieldWidth());
			   logic.addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, "if (" + hwToLogicHwClrName + ") " + 
			      fieldRegisterNextName + " = " + constVal.toFormat(NumBase.Hex, NumFormat.Verilog) + ";");				   
		   }

		   // add sw statements
		   String nameOverride = fieldProperties.isCounter() ? fieldProperties.getNextCountName() : null;
		   genSwFieldNextWriteStmts(nameOverride, swPrecedence);    // create statements to set value of next based on sw field settings
	}

	/** return rhs string expression for internal assignment or create an input
	 * 
	 * @param rType - reference type
	 * @param defaultName - defaultSignal name to be used for input define
	 * @param sigWidth - width of the input/internal signal created
	 * @param createDefaultSignal - if true create defaultName signal even if an assignment is made
	 * @param hiPrecedence - if true, combi assign of default signal will be given high priority (ignored if createDefaultSignal is false)
	 */
	private String generateInputOrAssign(RhsRefType rType, String defaultName, int sigWidth, boolean createDefaultSignal, boolean hiPrecedence) {
		String retName = defaultName;
		   // if an assigned reference signal use it rather than default 
		   if (fieldProperties.hasRef(rType)) { 
			   String refName = resolveRhsExpression(rType);
			   if (createDefaultSignal) {
				   logic.addVectorReg(defaultName, 0, sigWidth); 
				   logic.addPrecCombinAssign(regProperties.getBaseName(), hiPrecedence, defaultName + " = " + refName + ";");
			   }
			   else retName = refName;  // otherwise use new name in subsequent logic
		   }
		   // otherwise create an input
		   else 
			   hwSigList.addVector(HW, LOGIC, defaultName, 0, sigWidth); 
		return retName;
	}

	/** resolve rhs rtl expression, save in rhs signal list, and mark for post-gen name resolution.
	 *  (this method is for resolving refs defined in fieldProperties, not signalProperties) */
	private String resolveRhsExpression(RhsRefType rType) {
		String retExpression = fieldProperties.getRefRtlExpression(rType, false);   // create signal name from rhs
		retExpression = logic.resolveAsSignalOrField(retExpression);
		logic.addRhsSignal(retExpression, getInstancePath(), fieldProperties.getRef(rType).getRawReference() );
		return retExpression;
	}

	/** create statements to set value of next based on field settings for sw interface.
	 *  save sw write statements in alternate list so they can be moved depending on hw/sw precedence of field
	 * @param nextNameOverride - if non null, this will be the signal modified by sw write stmts
	 *  */ 
	private  void genSwFieldNextWriteStmts(String nextNameOverride, boolean swPrecedence) {  
		   // get base register and field names
		   String regBaseName = regProperties.getBaseName();
		   String fieldRegisterName = fieldProperties.getFieldRegisterName();  //"reg_" + hwBaseName; 
		   String fieldRegisterNextName = fieldProperties.getFieldRegisterNextName();  //"reg_" + hwBaseName + "_next";
		   String fieldArrayString = fieldProperties.getFieldArrayString();  
		   
		   // override names if an aliased register
		   if (regProperties.isAlias()) {
			   regBaseName = regProperties.getAliasBaseName();
			   fieldRegisterName = FieldProperties.getFieldRegisterName(regBaseName + "_" + fieldProperties.getPrefixedId(), true);  
			   fieldRegisterNextName = FieldProperties.getFieldRegisterNextName(regBaseName + "_" + fieldProperties.getPrefixedId(), true);  
		   }

		   // override the assigned name if specified
		   if (nextNameOverride != null) fieldRegisterNextName = nextNameOverride;
		   
		   String decodeToLogicDataName = regProperties.getDecodeToLogicName();  // write data from decoder
		   String decodeToLogicWeName = regProperties.getDecodeToLogicWeName();  // write enable from decoder
		   String decodeToLogicReName = regProperties.getDecodeToLogicReName();  // read enable from decoder
		   
		   // build an enable expression if swwe/swwel are used
		   String swWeStr = "";
		   if (fieldProperties.hasSwWriteEnableH()) { 
			   String hwToLogicSwWeName = generateInputOrAssign(RhsRefType.SW_WE, fieldProperties.getHwToLogicSwWeName(), 1, false, swPrecedence); 
			   swWeStr = " & " + hwToLogicSwWeName;				   
		   }
		   else if (fieldProperties.hasSwWriteEnableL()) {  
			   String hwToLogicSwWelName = generateInputOrAssign(RhsRefType.SW_WE, fieldProperties.getHwToLogicSwWelName(), 1, false, swPrecedence); 
			   swWeStr = " & ~" + hwToLogicSwWelName;				   
		   }
		   
		   // if a sw write one to clr/set
		   if (fieldProperties.isWoset()) {
			   logic.addPrecCombinAssign(regBaseName, swPrecedence, "if (" + decodeToLogicWeName + swWeStr + ") " + fieldRegisterNextName + " = (" + 
					   fieldRegisterName + " | " + decodeToLogicDataName + fieldArrayString + ");");				   
		   }
		   else if (fieldProperties.isWoclr()) {
			   logic.addPrecCombinAssign(regBaseName, swPrecedence, "if (" + decodeToLogicWeName + swWeStr + ") " + fieldRegisterNextName + " = (" + 
					   fieldRegisterName + " & ~" + decodeToLogicDataName + fieldArrayString + ");");				   
		   }
		   // if a sw write is alowed 
		   else if (fieldProperties.isSwWriteable()) {
			   logic.addPrecCombinAssign(regBaseName, swPrecedence, "if (" + decodeToLogicWeName + swWeStr + ") " + fieldRegisterNextName + " = " + regProperties.getDecodeToLogicName() + fieldArrayString + ";");				   
		   }
			   			   
		   // if a sw read set
		   if (fieldProperties.isRset()) {
			   logic.addPrecCombinAssign(regBaseName, swPrecedence, "if (" + decodeToLogicReName + swWeStr + ") " + fieldRegisterNextName + " = " + 
		           fieldProperties.getFieldWidth() + "'b" + Utils.repeat('1', fieldProperties.getFieldWidth()) + ";");
		   }
		   // if sw rclr 
		   else if (fieldProperties.isRclr()) {
			   logic.addPrecCombinAssign(regBaseName, swPrecedence, "if (" + decodeToLogicReName + swWeStr + ") " + fieldRegisterNextName + " = " + 
		           fieldProperties.getFieldWidth() + "'b0;");
		   }
		   
		   // if has sw access output
		   if (fieldProperties.hasSwAcc()) {
			   String logicToHwSwAccName = fieldProperties.getLogicToHwSwAccName();
			   hwSigList.addScalar(LOGIC, HW, logicToHwSwAccName);   // add sw access output
			   logic.addScalarReg(logicToHwSwAccName);  
			   logic.addPrecCombinAssign(regBaseName, swPrecedence, logicToHwSwAccName + 
					   " = " + decodeToLogicReName + " | " + decodeToLogicWeName + ";");
		   }
		   // if has sw modify output
		   if (fieldProperties.hasSwMod()) {
			   String logicToHwSwModName = fieldProperties.getLogicToHwSwModName();
			   hwSigList.addScalar(LOGIC, HW, logicToHwSwModName);   // add sw access output
			   logic.addScalarReg(logicToHwSwModName); 
			   String readMod = (fieldProperties.isRclr() || fieldProperties.isRset())? "(" + decodeToLogicReName + " | " + decodeToLogicWeName + ")" : decodeToLogicWeName;
			   logic.addPrecCombinAssign(regBaseName, swPrecedence, logicToHwSwModName + " = " + readMod + swWeStr + ";");
		   }
	}

	/** write interrupt field verilog 
	 * @param hwPrecedence */   
	private void genInterruptWriteStmts(boolean hwPrecedence) {
		   String fieldRegisterName = fieldProperties.getFieldRegisterName();  //"reg_" + hwBaseName;  
		   String fieldRegisterNextName = fieldProperties.getFieldRegisterNextName();  //"reg_" + hwBaseName + "_next";
		   int fieldWidth = fieldProperties.getFieldWidth();
		   
		   // if register is not already interrupt, then create output
		   String intrOutput = regProperties.getLogicToHwIntrName();
		   if (!regProperties.hasInterruptOutputDefined()) {
			   regProperties.setHasInterruptOutputDefined(true);
			   hwSigList.addScalar(LOGIC, HW, intrOutput);   // add hw interrupt output
			   logic.addScalarReg(intrOutput);
		       logic.addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, intrOutput + " = 1'b0;");  // default to intr off
		   }
		   
		   // if a halt field and register is not already halt, then create output
		   String haltOutput = regProperties.getLogicToHwHaltName();
		   if (fieldProperties.isHalt() && !regProperties.hasHaltOutputDefined()) {
			   regProperties.setHasHaltOutputDefined(true);
			   hwSigList.addScalar(LOGIC, HW, haltOutput);   // add hw interrupt output
			   logic.addScalarReg(haltOutput);
		       logic.addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, haltOutput + " = 1'b0;");  // default to halt off
		   }

		   // if a input intr reference then assign else create an input with width of field  
		   String hwToLogicIntrName = fieldProperties.getHwToLogicIntrName();  // hwBaseName + "_intr" 	   
		   if (fieldProperties.hasRef(RhsRefType.INTR)) {  //  intr assign not allowed by rdl1.0 spec, but allow for addl options vs next
			   //System.out.println("SystemVerilogBuilder genInterruptWriteStmts: " + fieldProperties.getInstancePath() + " has an intr reference, raw=" + fieldProperties.getIntrRef().getRawReference());
			   logic.addVectorReg(hwToLogicIntrName, 0, fieldProperties.getFieldWidth());
			   String refName = resolveRhsExpression(RhsRefType.INTR);
			   logic.addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, hwToLogicIntrName  +  " = " + refName + ";");
		   }
		   // otherwise, if next property isn't set then add an intr input
		   else if (!fieldProperties.hasRef(RhsRefType.NEXT)) {
			   hwSigList.addVector(HW, LOGIC, hwToLogicIntrName, 0, fieldProperties.getFieldWidth());   // add hw interrupt input
			   logic.addVectorWire(hwToLogicIntrName, 0, fieldProperties.getFieldWidth());			   
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
			   }
			   else if (fieldProperties.hasRef(RhsRefType.INTR_MASK)) {
				   String refName = resolveRhsExpression(RhsRefType.INTR_MASK);
				   if (fieldProperties.isMaskIntrBits()) intrBitModifier = " & ~" + refName;
				   else intrOutputModifier = " & ~" + refName;
			   }
			   
			   // create intr detect based on intrType (level, posedge, negedge, bothedge)
			   String detectStr = hwToLogicIntrName;  // default to LEVEL
			   String prevIntrName = fieldProperties.getPrevIntrName();  // hwBaseName + "_previntr" 	   
			   // if not LEVEL, need to store previous intr value
			   if (fieldProperties.getIntrType() != FieldProperties.IntrType.LEVEL) {
				   logic.addVectorReg(prevIntrName, 0, fieldProperties.getFieldWidth());
				   logic.addRegAssign(regProperties.getBaseName(), prevIntrName  +  " <= #1 " + hwToLogicIntrName + ";");
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
		      logic.addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, fieldRegisterNextName + " = " + detectStr + intrBitModifier + ";");
		   else if (fieldProperties.getIntrStickyType() == FieldProperties.IntrStickyType.STICKY) 
			  logic.addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, "if (" + detectStr + " != " + fieldWidth + "'b0) " +
		                         fieldRegisterNextName +  " = " + detectStr + intrBitModifier + ";");	
		   else // STICKYBIT default 
			  logic.addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, fieldRegisterNextName + " = (" + detectStr + " | " +
		                         fieldRegisterName + ")" + intrBitModifier + ";");
		   }

		   // if an enable/mask then gate interrupt output with this signal
		   String orStr = " | (";  String endStr = ");";
		   if (fieldWidth > 1) {
			   orStr = " | ( | (";  endStr = "));";  // use or reduction
		   }
	       logic.addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, intrOutput + " = " + intrOutput  + orStr + fieldRegisterName + intrOutputModifier + endStr);

		   // if an enable/mask then gate halt output with this signal
		   if (fieldProperties.hasRef(RhsRefType.HALT_ENABLE)) {
			   String refName = resolveRhsExpression(RhsRefType.HALT_ENABLE);
		       logic.addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, haltOutput + " = " + haltOutput  + orStr + fieldRegisterName + " & " + refName + endStr);
		   }
		   else if (fieldProperties.hasRef(RhsRefType.HALT_MASK)) {
			   String refName = resolveRhsExpression(RhsRefType.HALT_MASK);
		       logic.addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, haltOutput + " = " + haltOutput  + orStr + fieldRegisterName + " & ~" + refName + endStr);
		   }
		   else if (fieldProperties.isHalt())
		       logic.addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, haltOutput + " = " + haltOutput + orStr + fieldRegisterName + endStr);
	}
	
	/** write counter field verilog 
	 * @param hwPrecedence */   
	private void genCounterWriteStmts(boolean hwPrecedence) {
		   // get field-specific verilog signal names
		   String hwToLogicIncrName = fieldProperties.getHwToLogicIncrName();  // hwBaseName + "_incr" 
		   String logicToHwOverflowName = fieldProperties.getLogicToHwOverflowName();  // hwBaseName + "_overflow" 
		   
		   String hwToLogicDecrName = fieldProperties.getHwToLogicDecrName();  // hwBaseName + "_decr" 
		   String logicToHwUnderflowName = fieldProperties.getLogicToHwUnderflowName();  // hwBaseName + "_underflow" 
		   String nextCountName = fieldProperties.getNextCountName();   
		   
		   String logicToHwIncrSatName = fieldProperties.getLogicToHwIncrSatName();  // hwBaseName + "_incrsat_o" 
		   String logicToHwIncrTholdName = fieldProperties.getLogicToHwIncrTholdName();  // hwBaseName + "_incrthold_o" 
		   String logicToHwDecrSatName = fieldProperties.getLogicToHwDecrSatName();  // hwBaseName + "_decrsat_o" 
		   String logicToHwDecrTholdName = fieldProperties.getLogicToHwDecrTholdName();  // hwBaseName + "_decrthold_o" 

		   String fieldRegisterName = fieldProperties.getFieldRegisterName();  //"reg_" + hwBaseName;  
		   String fieldRegisterNextName = fieldProperties.getFieldRegisterNextName();  //"reg_" + hwBaseName + "_next";
		   
		   int fieldWidth = fieldProperties.getFieldWidth();
		   int countWidth = fieldWidth + 1;  // add a bit for over/underflow
		   
		   // create the next count value
		   logic.addVectorReg(nextCountName, 0, countWidth);  
		   logic.addCombinAssign(regProperties.getBaseName(), nextCountName + " = { 1'b0, " + fieldRegisterName + "};");  // no precedence - this stmt goes first  
		   
		   // if an incr is specified
		   if (fieldProperties.isIncrCounter()) {
			   
			   // add overflow output
			   if (fieldProperties.hasOverflow()) {
				   hwSigList.addScalar(LOGIC, HW, logicToHwOverflowName);   // add hw overflow output
				   logic.addScalarReg(logicToHwOverflowName);  
				   logic.addRegAssign(regProperties.getBaseName(), logicToHwOverflowName +
						   " <= #1 " + nextCountName + "[" + fieldWidth + "] & ~" +  logicToHwOverflowName + ";");  // only active for one cycle  
			   }

			   // if a ref is being used for increment assign it, else add an input
			   //System.out.println("SystemVerilogBuilder genCounterWriteStmts: " + fieldProperties.getInstancePath() + " is an incr counter, hasIncrRef=" + fieldProperties.hasIncrRef());
			   generateInputOrAssign(RhsRefType.INCR, hwToLogicIncrName, 1, true, hwPrecedence); 

			   // create incr value from reference, constant, or input
			   String incrValueString =getCountIncrValueString(countWidth);
			   
			   // increment the count
			   logic.addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, "if (" + hwToLogicIncrName + ")");
			   logic.addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, "   " + nextCountName + " = "  + nextCountName + " + " + incrValueString + ";");
		   }
		   
		   // if a decr is specified
		   if (fieldProperties.isDecrCounter()) {
			   
			   // add underflow output
			   if (fieldProperties.hasUnderflow()) {
				   hwSigList.addScalar(LOGIC, HW, logicToHwUnderflowName);   // add hw underflow output
				   logic.addScalarReg(logicToHwUnderflowName);  
				   logic.addRegAssign(regProperties.getBaseName(), logicToHwUnderflowName +
						   " <= #1 " + nextCountName + "[" + fieldWidth + "] & ~" +  logicToHwUnderflowName + ";");  // only active for one cycle  
			   }

			   // if a ref is being used for decrement assign it, else add an input
			   generateInputOrAssign(RhsRefType.DECR, hwToLogicDecrName, 1, true, hwPrecedence); 

			   // create decr value from reference, constant, or input
			   String decrValueString =getCountDecrValueString(countWidth);
			   
			   // decrement the count
			   logic.addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, "if (" + hwToLogicDecrName + ")");
			   logic.addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, "   " + nextCountName + " = "  + nextCountName + " - " + decrValueString + ";");
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
			   logic.addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, "if (" + nextCountName + " > " + incrSatValueString + ")");
			   logic.addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, "   " + nextCountName + " = "  + incrSatValueString + ";");
			   // add incrsat output
			   if (fieldProperties.hasSaturateOutputs()) hwSigList.addScalar(LOGIC, HW, logicToHwIncrSatName);   // add hw incrsaturate output
			   logic.addScalarReg(logicToHwIncrSatName);  
			   logic.addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, logicToHwIncrSatName + " = ( {1'b0, " + fieldRegisterName + "} == " + incrSatValueString + ");");
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
			   hwSigList.addScalar(LOGIC, HW, logicToHwIncrTholdName);   // add hw incrthreshold output
			   logic.addScalarReg(logicToHwIncrTholdName);  
			   logic.addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, logicToHwIncrTholdName + " = ( {1'b0, " + fieldRegisterName + "} == " + incrTholdValueString + ");");
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
			   logic.addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, "if (" + nextCountName + " < " + decrSatValueString + ")");
			   logic.addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, "   " + nextCountName + " = "  + decrSatValueString + ";");
			   // add decrsat output
			   if (fieldProperties.hasSaturateOutputs()) hwSigList.addScalar(LOGIC, HW, logicToHwDecrSatName);   // add hw decrsaturate output
			   logic.addScalarReg(logicToHwDecrSatName);  
			   logic.addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, logicToHwDecrSatName + " = ( {1'b0, " + fieldRegisterName + "} == " + decrSatValueString + ");");
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
			   hwSigList.addScalar(LOGIC, HW, logicToHwDecrTholdName);   // add hw decrthreshold output
			   logic.addScalarReg(logicToHwDecrTholdName);  
			   logic.addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, logicToHwDecrTholdName + " = ( {1'b0, " + fieldRegisterName + "} == " + decrTholdValueString + ");");
		   }
		   
		   // now assign the next count value to the reg
		   logic.addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, fieldRegisterNextName + " = " + nextCountName + SystemVerilogBuilder.genDefArrayString(0, fieldWidth) + ";");			
		
	}

	/** create count increment string */
	private String getCountIncrValueString(int countWidth) {
		String incrValueString = "0";
		String hwToLogicIncrValueName = fieldProperties.getHwToLogicIncrValueName();  // hwBaseName + "_incrvalue" 
		Integer incrWidth = fieldProperties.getIncrWidth();
		if (incrWidth != null) {  // if an external input is specified
			hwSigList.addVector(HW, LOGIC, hwToLogicIncrValueName, 0, incrWidth);   // add hw incr value input
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
		String hwToLogicDecrValueName = fieldProperties.getHwToLogicDecrValueName();  // hwBaseName + "_decrvalue" 
		Integer decrWidth = fieldProperties.getDecrWidth();
		if (decrWidth != null) {  // if an external input is specified
			hwSigList.addVector(HW, LOGIC, hwToLogicDecrValueName, 0, decrWidth);   // add hw decr value input
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
	private  void genFieldReadStmts() {
		   // create field-specific verilog signal names
		   String logicToHwDataName = fieldProperties.getLogicToHwDataName();  // hwBaseName + "_r" ;
		   String fieldRegisterName = fieldProperties.getFieldRegisterName();  //"ff_" + hwBaseName;

		   // if sw readable, set output (else return 0s)
		   genSwFieldReadStmts();
		   
		   // if hw readable, add the interface and set output
		   if (fieldProperties.isHwReadable()) {
			   // add read signals from logic to hw
			   hwSigList.addVector(LOGIC, HW,  logicToHwDataName, 0, fieldProperties.getFieldWidth());    // logic to hw list 
			   
			   // assign hw read data outputs
			   logic.addVectorReg(logicToHwDataName, 0, fieldProperties.getFieldWidth());  // add outputs to define list since we'll use block assign
			   logic.addCombinAssign(regProperties.getBaseName(), logicToHwDataName + " = " + fieldRegisterName + ";");  
		   }
		   
		   // if anded/ored/xored outputs specified
		   genBitwiseOutputStmts();
	}
	
	/** create bitwise outputs for this field */
	private void genBitwiseOutputStmts() {
		   // create field-specific verilog signal names
		   String logicToHwAndedName = fieldProperties.getLogicToHwAndedName();  
		   String logicToHwOredName = fieldProperties.getLogicToHwOredName();  
		   String logicToHwXoredName = fieldProperties.getLogicToHwXoredName();  
		   String fieldRegisterName = fieldProperties.getFieldRegisterName();  
		   
		   // anded output
		   if (fieldProperties.isAnded()) {
			   hwSigList.addScalar(LOGIC, HW, logicToHwAndedName);    // logic to hw list 
			   logic.addScalarReg(logicToHwAndedName);  // add outputs to define list since we'll use block assign
			   logic.addCombinAssign(regProperties.getBaseName(), logicToHwAndedName + " = & " + fieldRegisterName + ";");  			   
		   }
		   // ored output
		   if (fieldProperties.isOred()) {
			   hwSigList.addScalar(LOGIC, HW, logicToHwOredName);    // logic to hw list 
			   logic.addScalarReg(logicToHwOredName);  // add outputs to define list since we'll use block assign
			   logic.addCombinAssign(regProperties.getBaseName(), logicToHwOredName + " = | " + fieldRegisterName + ";");  			   
		   }
		   // xored output
		   if (fieldProperties.isXored()) {
			   hwSigList.addScalar(LOGIC, HW, logicToHwXoredName);    // logic to hw list 
			   logic.addScalarReg(logicToHwXoredName);  // add outputs to define list since we'll use block assign
			   logic.addCombinAssign(regProperties.getBaseName(), logicToHwXoredName + " = ^ " + fieldRegisterName + ";");  			   
		   }
		
	}

	/** generate alias register field read statements */  
	private  void genSwFieldReadStmts() {
		   // create field-specific verilog signal names
		   String fieldRegisterName = fieldProperties.getFieldRegisterName();  //"rg_" + hwBaseName;
		   String fieldArrayString = fieldProperties.getFieldArrayString(); 
		   // if an aliased register override the field name
		   if (regProperties.isAlias()) {
			   String aliasBaseName = regProperties.getAliasBaseName();
			   fieldRegisterName = FieldProperties.getFieldRegisterName(aliasBaseName + "_" + fieldProperties.getPrefixedId(), true);  //"rg_" + AliasBaseName;
		   }
		   // if sw readable, set output (else return 0s)
		   if (fieldProperties.isSwReadable()) {
			   tempAssignList.add(regProperties.getLogicToDecodeName() + fieldArrayString + " = " + fieldRegisterName + ";"); // need to set unused bits to 0 after all fields added		
		   }
		   
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
				RegNumber start = new RegNumber(ExtParameters.getLeafBaseAddress());
				start.setVectorLen(ExtParameters.getLeafAddressSize());
				start.add(ar.getStart());
				RegNumber end = new RegNumber(ExtParameters.getLeafBaseAddress());
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
	   	    
			// write the top level module
			writeTop(outName + getModuleName() + "_pio.sv", description, commentPrefix);

			// write the logic module
			writeLogic(outName + getModuleName() + "_jrdl_logic.sv", description, commentPrefix);
			
			// write the decode module
			writeDecode(outName + getModuleName() + "_jrdl_decode.sv", description, commentPrefix);
			
			// if IO interfaces are used, generate the interfaces and wrapper
			if ((usesInterfaces || ExtParameters.sysVerilogAlwaysGenerateIwrap()) && !legacyVerilog) {
				writeInterfaces(outName + getModuleName() + "_pio_interfaces.sv", description, commentPrefix);
				writeInterfaceWrapper(outName + getModuleName() + "_pio_iwrap.sv", description, commentPrefix);
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

	/** write interface output to specified output file  
	 * @param outName - output file or directory
	 * @param description - text description of file generated
	 * @param commentPrefix - comment chars for this file type */
	public void writeInterfaceWrapper(String outName, String description, String commentPrefix) {
    	BufferedWriter bw = openBufferedWriter(outName, description);
    	if (bw != null) {
    		// set bw as default
    		bufferedWriter = bw;

    		// write the file header
    		writeHeader(commentPrefix);
    		
    		// now write the output
	    	writeInterfaceWrapper();
    		closeBufferedWriter(bw);
    	}
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
		
		// if IO interfaces are used, generate the interfaces and wrapper
		if ((usesInterfaces  || ExtParameters.sysVerilogAlwaysGenerateIwrap()) && !legacyVerilog) {
			writeInterfaces();
			writeInterfaceWrapper();  
		}
					
		// loop through nested addrmaps and write these VerilogBuilders
		for (SystemVerilogBuilder childBuilder: childAddrMaps) {
			//System.out.println("--- VerilogBuilder - writing child");
			childBuilder.write(bw);
		}
	}
	
	/** write out the interface defines */
	protected  void writeInterfaces() {
		// get a list of all sv interfaces to be defined
		List<SystemVerilogIOSignal> intfSigs = hwSigList.getIntfsToBeDefined();
		//System.out.println("SystemVerilogBuilder writeInterfaces: found " + intfSigs.size() + " interfaces");
		for (SystemVerilogIOSignal intfSig : intfSigs) writeInterface(intfSig);
	}

	/** write out an interface define */
	private void writeInterface(SystemVerilogIOSignal intfSig) {
		int indentLevel = 0;
		// set interface name and list of sigs
		String intfName = intfSig.getInterfaceDefName();
		List<String> intfDefStrings = intfSig.getIntfDefStrings();
		// write the interface
		if (intfDefStrings.size() > 0) {
			writeInterfaceBegin(indentLevel++, intfName);
			for (String defStr: intfDefStrings) writeStmt(indentLevel, defStr);   
			writeInterfaceEnd(--indentLevel);
		}
		//System.out.println("SystemVerilogBuilder writeInterface: " + intfName + ", def str n=" + intfDefStrings.size());	
	}

	/** write out the interface wrapper */
	protected  void writeInterfaceWrapper() {		
		// create wrapper module
		SystemVerilogModule intfWrapper = new SystemVerilogModule(this, LOGIC|DECODE, defaultClk);
		intfWrapper.setName(getModuleName() + "_pio_iwrap");
		intfWrapper.setAddBaseAddrParameter(addBaseAddressParameter());
		intfWrapper.setUseInterfaces(true);  // wrapper will have interfaces in io
		// add io lists
		intfWrapper.useIOList(cntlSigList, null);
		intfWrapper.useIOList(hwSigList, HW);
		intfWrapper.useIOList(pioSigList, PIO); 
		// add pio_top instance
		intfWrapper.addInstance(top, "pio");
        // create mappings from top module to wrapper IO
		SystemVerilogIOSignalList wrapperIOList = intfWrapper.getFullIOSignalList();  
		//System.out.println("SystemVerilogBuilder writeInterfaceWrapper: wrapper n=" + wrapperIOList.size());	
		intfWrapper.addWireDefs(wrapperIOList.getIntfEncapsulatedSignalList(LOGIC|DECODE));
		intfWrapper.addWireAssigns(wrapperIOList.getInterfaceAssignStrList(LOGIC|DECODE));  // sig to intf assigns

		intfWrapper.write();  // write the wrapper using interfaces			
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
		return (decoder.hasInterface(SVDecodeInterfaceTypes.LEAF) || decoder.hasInterface(SVDecodeInterfaceTypes.RING16)) && ExtParameters.systemverilogBaseAddrIsParameter();
	}

	/** generate a (little endian) array definition string given a starting bit and size */
	public static String genDefArrayString(int lowIndex, int size) {
		if (size < 2) return "";
	   	return " [" + (size + lowIndex - 1) + ":" + lowIndex + "] ";
	}

	/** generate a (little endian) array reference string given a starting bit and size */
	public static String genRefArrayString(int lowIndex, int size) {
		if (size < 1) return " ";
		if (size == 1) return " [" + lowIndex + "] ";
	   	return " [" + (size + lowIndex - 1) + ":" + lowIndex + "] ";
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
	   	return SystemVerilogBuilder.genDefArrayString(lowIndex, size);  
	}
	
	/** generate a (little endian) reference array string corresponding to the internal address range for addrmap */  
	public String genRefAddressArrayString() {
		int lowIndex = getAddressLowBit();   // remove low bits using reg width in bytes
		// compute total address map size
		int size = getMapAddressWidth();   // get high bit of mapSize-1  
	   	return SystemVerilogBuilder.genRefArrayString(lowIndex, size);  
	}
	
	/** return the bit string for block select compare */
	public String getBlockSelectBits() {
		int lowIndex = getAddressLowBit();   // low bit in internal address
		// compute total address map size
		int size = getMapAddressWidth();   // get high bit of mapSize-1  
	   	int lowSelectBit =  lowIndex + size;  
		int selectSize = ExtParameters.getLeafAddressSize() - lowSelectBit;
		return SystemVerilogBuilder.genRefArrayString(lowSelectBit, selectSize);
	}
	
	/** return the number of bits to select max sized wide reg */   
	protected int getMaxWordBitSize() {
		return Utils.getBits(getMaxRegWordWidth());
	}
	
}
