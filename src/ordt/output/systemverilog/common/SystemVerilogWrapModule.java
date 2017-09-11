package ordt.output.systemverilog.common;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

import ordt.extract.Ordt;
import ordt.output.OutputWriterIntf;
import ordt.output.systemverilog.common.RemapRuleList.RemapRuleType;
import ordt.output.systemverilog.common.SystemVerilogWrapModule.WrapperSignalMap;
import ordt.output.systemverilog.io.SystemVerilogIOSignalList;
import ordt.parameters.ExtParameters;
import ordt.parameters.ExtParameters.SVDecodeInterfaceTypes;

public class SystemVerilogWrapModule extends SystemVerilogModule {

	// remap list w/ hash by base source signal
	WrapperSignalMap wrapMappings = new WrapperSignalMap();
	
	/** create a wrapper module
	 * @param writer - OutputWriterIntf to be used for output generation 
	 * @param insideLocs - ORed Integer of locations in top level in this module (not including children) 
	 * @param defaultClkName - default clock name used for generated registers
	 */
	public SystemVerilogWrapModule(OutputWriterIntf writer, int insideLocs, String defaultClkName,
			String coverageResetName) {
		super(writer, insideLocs, defaultClkName, coverageResetName);
	}

	// ------------------- override child instance methods/classes  -----------------------

	@Override
	/** add an instance to wrap module.
	 * check for duplicate child modules and set remap rules for child IO */
	public SystemVerilogInstance addInstance(SystemVerilogModule mod, String name) {
		// TODO - dup modules are OK, but need to check for dup signal sources and exit
		if (childModuleHasMultipleInstances(mod.getName())) Ordt.warnMessage("Wrapper modules with multiple children of same type may cause generation issues.");
        // add new instance
		SystemVerilogInstance newInst = super.addInstance(mod, name);
		// add child using remap info
		RemapRuleList rules = getChildRemapRuleList(newInst);
		newInst.setRemapRules(rules);
		//System.out.println("SystemVerilogWrapModule addInstance: adding instance " + name + " of " + mod.getName() + " to " + getName() + ", child #=" + instanceList.size());
        return newInst;
	}

	/** add an instance to wrap module.
	 * note that instance remap rules are disabled in wrap, which uses a set remapping scheme */
	@Override
	public SystemVerilogInstance addInstance(SystemVerilogModule mod, String name, RemapRuleList rules) {
		Ordt.warnMessage("Remap rules applied to child instance " + name + " of " + mod.getName() + "will be ignored in wrap module " + getName());
		return addInstance(mod, name);
	}
	
	/** return the IO renaming rules for a wrapped child
	 * 
	 * @param newInst - child module being wrapped
	 */
	private RemapRuleList getChildRemapRuleList(SystemVerilogInstance newInst) {
		// add instance name prefix to uniquify signals
		String prefix = "inst_" + newInst.getName() + "_"; 
		RemapRuleList rules = new RemapRuleList();
		// prefix all child inputs/outputs
		Integer insideLocs = newInst.getMod().getInsideLocs();
		Integer outsideLocs = newInst.getMod().getOutsideLocs();
		rules.addRule("^.*$", outsideLocs, insideLocs, RemapRuleType.ADD_PREFIX, prefix);  // inputs
		rules.addRule("^.*$", insideLocs, outsideLocs, RemapRuleType.ADD_PREFIX, prefix);  // outputs
		return rules;
	}
	
	// ------------------- remap instance nested classes  -----------------------
	
	protected enum WrapperRemapType { ASSIGN, FLOPS, ASYNC }
	
	private class WrapperRemapDestination {
		private int srcLowIndex;  // low index in source
		private int srcSize;  // size in bits of source 
		private SystemVerilogSignal dest;  // destination signal and vector info
		private WrapperRemapType type = WrapperRemapType.ASSIGN;  // remap type
	}
	
	private class WrapperRemapInstance {
		private SystemVerilogSignal source;  // source signal and vector info
		private List<WrapperRemapDestination> dests = new ArrayList<WrapperRemapDestination>();
		
		public WrapperRemapInstance(String signalName, Integer lowIndex, Integer size) {
			this.source = new SystemVerilogSignal(signalName, lowIndex, size);
		}
	}
	
	/** mapping of wrapper source signals to destinations */
	public class WrapperSignalMap {
		private LinkedHashMap<String, WrapperRemapInstance> mappings = new LinkedHashMap<String, WrapperRemapInstance>();

		/** add a source signal to the mapping */
		public void addSource(String rootName, String signalName, Integer lowIndex, Integer size) {
			if (mappings.containsKey(rootName)) Ordt.errorExit("Wrapper module found multiple source signals named " + rootName);
			mappings.put(rootName, new WrapperRemapInstance(signalName, lowIndex, size));			
		}
	}
	
	// ------------------- wrapper IO/signal generation  -----------------------
	
    /** geneate IOs, signals, and assignments for this wrap module */
	public void generateWrapperInfo() {  
		generateIOListFromChildren();  // gen wrapper IO list
		// create signal mappings
		setIOsourceSignals(useInterfaces);
		setChildSourceSignals();
		setIODestinationSignals();
		setChildDestinationSignals();
		// TODO create wire/reg degs and assigns for mapping
	}
	
	// TODO add alternate mode to use a specified IO list rather than child extracted IO
	
    /** set the external IO lists for this module using unique child IO */
	private void generateIOListFromChildren() {  
		SystemVerilogIOSignalList inputList = new SystemVerilogIOSignalList("inputs");  // external inputs
		SystemVerilogIOSignalList outputList = new SystemVerilogIOSignalList("outputs");  // external outputs
		HashSet<String> uniqueSignalNames = new HashSet<String>();  // maintain list of unique child IO signal names
		HashSet<String> uniqueModules = new HashSet<String>();  // maintain list of unique child module names
		Integer outsideLocs = getOutsideLocs(); // get outside locations for this module
		for (SystemVerilogInstance inst: instanceList) {
			String modName = inst.getMod().getName();
			// if module is new, process it  // TODO add ability to support rep'd modules by adding prefixed child name in IO list (for outputs)
			if (!uniqueModules.contains(modName)) {
				uniqueModules.add(modName);
				SystemVerilogIOSignalList fullList = inst.getMod().getFullIOSignalList(); // extract full list from this child
				Integer insideLocs = inst.getMod().getInsideLocs();  // get this child's internal locations
				inputList.addList(fullList.copyIOSignalList(outsideLocs, insideLocs, uniqueSignalNames));  // get unique child inputs
				outputList.addList(fullList.copyIOSignalList(insideLocs, outsideLocs, uniqueSignalNames));  // get unique child outputs
			}
		}
        // add these child IOs to this modules IO list
		useIOList(inputList, null);
		useIOList(outputList, null);
	}
	
	/** inherit parameters from children */
	public void inheritChildParameters() {
		// TODO - either add parms to IO, set parms, or use default values / move this to mod super class
	}
	
	/** load wrapMappings w/ source signals from external inputs */
	private void setIOsourceSignals(boolean useHierSignalNames) {
		Integer insideLocs = getInsideLocs(); // get inside locations for this module  
		Integer outsideLocs = getOutsideLocs(); // get outside locations for this module
		// build remap rule for all external inputs
		RemapRuleList rules = new RemapRuleList();
		rules.addRule(outsideLocs, insideLocs, RemapRuleType.SAME, "");
		// load wrapMappings - call on IOList - 
		SystemVerilogIOSignalList fullIOList = getFullIOSignalList(); // use full IO list as there could be encapsulated IO
		fullIOList.loadWrapperMapSources(wrapMappings, rules, useHierSignalNames);
	}
	
	/** load wrapMappings w/ source signals from child outputs */
	private void setChildSourceSignals() {
		for (SystemVerilogInstance inst: instanceList) {
			// if module is new, process it
			SystemVerilogIOSignalList fullList = inst.getMod().getFullIOSignalList(); // extract full list from this child
			Integer insideLocs = inst.getMod().getInsideLocs();  // get this child's internal locations
			Integer outsideLocs = inst.getMod().getOutsideLocs();  // get this child's internal locations
			// build remap rule for all external inputs
			RemapRuleList rules = new RemapRuleList();
			rules.addRule(outsideLocs, insideLocs, RemapRuleType.ADD_PREFIX, "inst_" + inst.getName());
			// load wrapMappings  
			fullList.loadWrapperMapSources(wrapMappings, rules, false);
		}
	}
	
	/** load wrapMappings w/ destination signals to external IO list (outputs) */
	private void setIODestinationSignals() {
		// TODO load wrapMappings
	}
	
	/** load wrapMappings w/ destination signals to child IO list (inputs) */
	private void setChildDestinationSignals() {
		// TODO load wrapMappings
	}
		
	// TODO generate wires
	// TODO generate regs
	// TODO generate assignments
	
	/** FIXME - this is from SystemVerilogBuilder - write out the interface wrapper
	protected  void writeInterfaceWrapper() {		
		// create wrapper module
		SystemVerilogModule intfWrapper = new SystemVerilogModule(this, 0, defaultClk, getDefaultReset());
		intfWrapper.setName(getModuleName() + "_pio_iwrap");
		if (addBaseAddressParameter()) setAddBaseAddrParameter(intfWrapper);
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
		intfWrapper.addWireDefs(wrapperIOList.getEncapsulatedSignalList(LOGIC|DECODE));
		intfWrapper.addWireAssigns(wrapperIOList.getNonVirtualAssignStrings(LOGIC|DECODE));  // sig to intf assigns

		intfWrapper.write();  // write the wrapper using interfaces			
	} */
	
	/** FIXME - this is from SystemVerilogTestBuilder - write out the basic testbench    
	private  void writeTB() {		
				
		int clkPeriod = 10;  // use an even number so half period is int also
		
		// define the top module
		benchtop.setName(getModuleName() + "_test");
		
		// add bench io list
		benchtop.useIOList(benchSigList, null); 
		
		// write primary interface bfm module and add bfm control signals to bench list
		createPrimaryBfm();   // TODO handle secondary bfm
	
		// add dut child modules 
		addDutInstances(benchtop);
		
		// add primary bfm instance with names remapped
		if (decoder.hasSecondaryInterface()) {
			RemapRuleList rules = new RemapRuleList();
			if (ExtParameters.getSysVerRootDecoderInterface()==SVDecodeInterfaceTypes.PARALLEL) {
				rules.addRule("^dec_pio_.*$", RemapRuleType.ADD_PREFIX, "p1_d2h_");  // parallel (note h2d/d2h reflects decoder cascaded output i/f direction, not root i/f)
				rules.addRule("^pio_dec_.*$", RemapRuleType.ADD_PREFIX, "p1_h2d_");
				benchtop.addInstance(primaryBfm, "parallel_bfm", rules);
			}
			else {
				rules.addRule("^dec_leaf_.*$", RemapRuleType.ADD_PREFIX, "p1_");  // leaf 
				rules.addRule("^leaf_dec_.*$", RemapRuleType.ADD_PREFIX, "p1_");
				benchtop.addInstance(primaryBfm, "leaf_bfm", rules);
			}
		}
		else if (ExtParameters.getSysVerRootDecoderInterface()==SVDecodeInterfaceTypes.PARALLEL) {
			RemapRuleList rules = new RemapRuleList();
			rules.addRule("^dec_pio_.*$", RemapRuleType.ADD_PREFIX, "d2h_");  // parallel (note h2d/d2h reflects decoder cascaded output i/f direction, not root i/f)
			rules.addRule("^pio_dec_.*$", RemapRuleType.ADD_PREFIX, "h2d_");
			benchtop.addInstance(primaryBfm, "parallel_bfm", rules);  
		}
		else benchtop.addInstance(primaryBfm, "leaf_bfm");  // leaf
		
		// add all dut extern signals to the bench list
		addDutIOs(benchtop);  
		
		// add wire defs to bench
		benchtop.setShowDuplicateSignalErrors(false);
		//benchtop.addWireDefs(benchtop.getSignalList(PIO, DECODE));  // all ports from pio to decoder will be wires
		benchtop.addWireDefs(getPIOInputWires());  // all except p2 ports from pio to decode will be wires		
		benchtop.addWireDefs(benchtop.getSignalList(DECODE, PIO));  // all ports from decoder to pio will be wires
		benchtop.addWireDefs(benchtop.getSignalList(LOGIC, HW));  // all ports to hw will be wires
		benchtop.addWireDefs(benchtop.getSignalList(DECODE, HW));  // all ports to hw will be wires
		benchtop.addWireDefs(benchtop.getSignalList(PIO, HW));  // all ports to hw will be wires
		
		// add some local reg defines
		benchtop.addScalarReg("CLK");
		benchtop.addScalarReg("CLK_div2");
		benchtop.addScalarReg("CLK_div4");
		if (ExtParameters.systemverilogUseGatedLogicClk()) benchtop.addScalarReg("delayed_gclk_enable");
		
		// add reg defs to bench
		benchtop.addRegDefs(benchtop.getSignalList(HW, LOGIC));  // all ports from hw will be regs
		benchtop.addRegDefs(benchtop.getSignalList(HW, DECODE));  // all ports from hw will be regs
		benchtop.addRegDefs(benchtop.getSignalList(HW, PIO));  // all ports from hw will be regs
		benchtop.addRegDefs(getPIOInputRegs());  // p2 ports from pio to decode will be regs		
		
		// add sim block statements
		addSimBlocks(clkPeriod);
		
		// ----------- write out the testbench
				
		// add timescale
		writeStmt(0, "`timescale 1 ns / 100 ps");
		
		// write leaf bfm module and define interfacing signals
		int indentLevel = 0;
		primaryBfm.write();   
		
		// write the top module
		benchtop.writeNullModuleBegin(indentLevel);
		indentLevel++;

		benchtop.writeWireDefs(indentLevel);
		
		benchtop.writeRegDefs(indentLevel);  
		
		// write the simulation activity block
		benchtop.writeStatements(indentLevel);
		
		// write dut instances
		benchtop.writeChildInstances(indentLevel);
		
		// end the module
		indentLevel--;
		benchtop.writeModuleEnd(indentLevel);	
	}	*/

}
