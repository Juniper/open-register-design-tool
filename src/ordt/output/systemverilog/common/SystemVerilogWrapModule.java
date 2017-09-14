package ordt.output.systemverilog.common;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

import ordt.extract.Ordt;
import ordt.output.OutputWriterIntf;
import ordt.output.systemverilog.SystemVerilogBuilder;
import ordt.output.systemverilog.common.RemapRuleList.RemapRuleType;
import ordt.output.systemverilog.io.SystemVerilogIOSignalList;

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
		// TODO dup child modules not fully supported
		if (childModuleHasMultipleInstances(mod.getName())) Ordt.warnMessage("Wrapper modules with multiple children of same type may cause generation issues.");
        // add new instance w/o rules for now
		SystemVerilogInstance newInst = super.addInstance(mod, name, null);
		// add child using remap rule to add name prefix 
		RemapRuleList rules = getChildRemapRuleList(newInst);
		newInst.setRemapRules(rules);
		//System.out.println("SystemVerilogWrapModule addInstance: adding instance " + name + " of " + mod.getName() + " to " + getName() + ", child #=" + instanceList.size());
        return newInst;
	}

	/** add an instance to wrap module.
	 * note that instance remap rules are disabled in wrap, which uses a set remapping scheme */
	@Override
	public SystemVerilogInstance addInstance(SystemVerilogModule mod, String name, RemapRuleList rules) {
		Ordt.warnMessage("Remap rules applied to child instance " + name + " of " + mod.getName() + " will be ignored in wrap module " + getName());
		return addInstance(mod, name);
	}
	
	/** return the IO renaming rules for a wrapped child
	 * 
	 * @param newInst - child module being wrapped
	 */
	private RemapRuleList getChildRemapRuleList(SystemVerilogInstance newInst) {
		// add instance name prefix to uniquify signals
		String prefix = getPrefixedInstanceSignalName(newInst.getName()); 
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

		private WrapperRemapDestination(int srcLowIndex, int srcSize, SystemVerilogSignal dest, WrapperRemapType type) {  // TODO change type to dest map rules?
			this.srcLowIndex = srcLowIndex;
			this.srcSize = srcSize;
			this.dest = dest;
			this.type = type;
		}
		
        protected int getSrcLowIndex() {
			return srcLowIndex;
		}

		protected int getSrcSize() {
			return srcSize;
		}

		protected SystemVerilogSignal getDest() {
			return dest;
		}

		protected WrapperRemapType getType() {
			return type;
		}

		@Override
		public String toString() {
			return "source " + ((srcSize>1)? SystemVerilogBuilder.genRefArrayString(srcLowIndex, srcSize) : "") + " -> " + dest + ", type=" + type;
		}
	}
	
	private class WrapperRemapInstance {
		private SystemVerilogSignal source;  // source signal name
		private boolean isValidSource = true;  // indication that source is valid - false indicates destination(s) with no matching source
		private boolean isInput = false;  // indication that source is an external input
		private List<WrapperRemapDestination> dests = new ArrayList<WrapperRemapDestination>();
		
		public WrapperRemapInstance(String signalName, Integer lowIndex, Integer size, boolean isValidSource, boolean isInput) {
			this.source = new SystemVerilogSignal(signalName, lowIndex, size);
			this.isValidSource = isValidSource;
			this.isInput = isInput;
		}

		protected SystemVerilogSignal getSource() {
			return source;
		}

		protected boolean isValidSource() {
			return isValidSource;
		}

		protected boolean isInput() {
			return isInput;
		}

		protected List<WrapperRemapDestination> getDests() {
			return dests;
		}

		public void addDestination(String signalName, Integer lowIndex, Integer size, boolean isOutput) {
			SystemVerilogSignal newDest = new SystemVerilogSignal(signalName, lowIndex, size);
			dests.add(new WrapperRemapDestination(lowIndex, size, newDest, WrapperRemapType.ASSIGN));  // TODO - use same lowIndex/size and assign mode only for now
		}
		
		@Override
		public String toString() {
        	String retStr = source + ", isValidSource=" + isValidSource + ", isInput=" + isInput;
        	for (WrapperRemapDestination dest: dests)
    			retStr += "\n   " + dest;
			return retStr;
		}
	}
	
	/** mapping of wrapper source signals to destinations */
	public class WrapperSignalMap {
		private LinkedHashMap<String, WrapperRemapInstance> mappings = new LinkedHashMap<String, WrapperRemapInstance>();

		/** add a source signal to the mapping
		 * @param rootName - root signal name used for matching source/destination maps
		 * @param signalName - source signal name that will drive this node
		 * @param lowIndex - low bit index of source signal
		 * @param size - size in bits of this source signal
		 * @param isInput - if true, source signal will be tagged as an external input
		 */
		public void addSource(String rootName, String signalName, Integer lowIndex, Integer size, boolean isInput) {
			if (mappings.containsKey(rootName)) Ordt.errorExit("Wrapper module found multiple source signals named " + rootName);
			mappings.put(rootName, new WrapperRemapInstance(signalName, lowIndex, size, true, isInput));	// add a valid source		
		}

		/** add a destination signal to the mapping
		 * @param rootName - root signal name used for matching source/destination maps
		 * @param signalName - destination signal name 
		 * @param lowIndex - low bit index of destination signal
		 * @param size - size in bits of this destination signal
		 * @param isOutput - if true, destination signal will be tagged as an external output
		 */
		public void addDestination(String rootName, String signalName, Integer lowIndex, Integer size, boolean isOutput) {
			// if this destination has no source, add it and tag as invalid
			if (!mappings.containsKey(rootName)) {
				Ordt.errorMessage("Wrapper module did not find a source for destination signal " + rootName);
				mappings.put(rootName, new WrapperRemapInstance(rootName, lowIndex, size, false, false));	// add an invalid source		
			}
			// now add a destination
			mappings.get(rootName).addDestination(signalName, lowIndex, size, isOutput);	// add a destination		
		}
		
		public void display() {
        	for (String root: mappings.keySet())
    			System.out.println("root=" + root + ", map=" + mappings.get(root));
		}

		/** generate wire, register defines and specified mapping statements */
		public void generateMapOutput() {
			// loop through map sources
        	for (String root: mappings.keySet()) {
        		WrapperRemapInstance wrapInst = mappings.get(root);
        		// add non-input sources to wire def list
        		SystemVerilogSignal src = wrapInst.getSource();
        		if (wrapInst.isValidSource() && !wrapInst.isInput()) {
        			wireDefList.addVector(src.getName(), src.getLowIndex(), src.getSize());
        		}
        		// add all destinations to wire list
    			for (WrapperRemapDestination remapDest: wrapInst.getDests()) {
    				// TODO - should check src sizes and remap type here
    				SystemVerilogSignal dst = remapDest.getDest();
    				if (wrapInst.isValidSource()) {
        				wireDefList.addVector(dst.getName(), dst.getLowIndex(), dst.getSize());
        				wireAssignList.add(dst.getName() + " = " + src.getName() + ";");
    				}
    			}
		    }
		}
	}
	
	// ------------------- wrapper IO/signal generation  -----------------------
	
    /** generate IOs, signals, and assignments for this wrap module.  Must be called after child instances are added */
	public void generateWrapperInfo() {  
		generateIOListFromChildren();  // gen wrapper IO list
		inheritChildParameters();  // use child parameters
		// create signal mappings
		setIOsourceSignals(useInterfaces);
		setChildSourceSignals();
		setIODestinationSignals(useInterfaces);
		setChildDestinationSignals();
		// create wire/reg defs and assigns for mapping
		wrapMappings.generateMapOutput();
		
		//wrapMappings.display();  // TODO
	}
	
	// TODO add alternate mode to use a specified IO list rather than child extracted IO
	
    /** set the external IO lists for this module using unique child IO */
	private void generateIOListFromChildren() {  
		SystemVerilogIOSignalList newList = new SystemVerilogIOSignalList("IO");  // external IO
		HashSet<String> uniqueIONames = new HashSet<String>();  // maintain list of unique child IO signal names
		HashSet<String> uniqueModules = new HashSet<String>();  // maintain list of unique child module names
		Integer outsideLocs = getOutsideLocs(); // get outside locations for this module
		for (SystemVerilogInstance inst: instanceList) {
			String modName = inst.getMod().getName();
			// if module is new, process it  // TODO add ability to support rep'd modules by adding prefixed child name in IO list (for outputs)
			if (!uniqueModules.contains(modName)) {
				uniqueModules.add(modName);
				SystemVerilogIOSignalList fullList = inst.getMod().getFullIOSignalList(); // extract full list from this child
				//fullList.display();
				// build remap rule for all external inputs
				RemapRuleList rules = new RemapRuleList();
				Integer insideLocs = inst.getMod().getInsideLocs();  // get this child's internal locations
				rules.addRule(outsideLocs, insideLocs, RemapRuleType.SAME, ""); // input rule
				rules.addRule(insideLocs, outsideLocs, RemapRuleType.SAME, ""); // output rule
				newList.addList(new SystemVerilogIOSignalList(fullList, rules, uniqueIONames));  // get unique child IOs
			}
		}
        // add these child IOs to this modules IO list
		useIOList(newList, null);
	}
	
	/** load wrapMappings w/ source signals from external inputs */
	private void setIOsourceSignals(boolean useHierSignalNames) {
		Integer insideLocs = getInsideLocs(); // get inside locations for this module  
		Integer outsideLocs = getOutsideLocs(); // get outside locations for this module
		// build remap rule for all external inputs
		RemapRuleList rules = new RemapRuleList();
		rules.addRule(outsideLocs, insideLocs, RemapRuleType.SAME, "");
		// load wrapMappings with sources from IO inputs 
		SystemVerilogIOSignalList fullIOList = getFullIOSignalList(); // use full IO list as there could be encapsulated IO
		fullIOList.loadWrapperMapSources(wrapMappings, rules, useHierSignalNames, true);
	}
	
	/** load wrapMappings w/ source signals from child outputs */
	private void setChildSourceSignals() {
		for (SystemVerilogInstance inst: instanceList) {
			// if module is new, process it
			SystemVerilogIOSignalList fullList = inst.getMod().getFullIOSignalList(); // extract full list from this child
			Integer insideLocs = inst.getMod().getInsideLocs();  // get this child's internal locations
			Integer outsideLocs = inst.getMod().getOutsideLocs();  // get this child's internal locations
			// build remap rule for all child outputs
			RemapRuleList rules = new RemapRuleList();
			rules.addRule(insideLocs, outsideLocs, RemapRuleType.ADD_PREFIX, getPrefixedInstanceSignalName(inst.getName()));
			// load wrapMappings  
			fullList.loadWrapperMapSources(wrapMappings, rules, false, false);
		}
	}
	
	private static String getPrefixedInstanceSignalName(String baseName) {
		return "inst_" + baseName + "_";
	}

	/** load wrapMappings w/ destination signals to external IO list (outputs) */
	private void setIODestinationSignals(boolean useHierSignalNames) {
		Integer insideLocs = getInsideLocs(); // get inside locations for this module  
		Integer outsideLocs = getOutsideLocs(); // get outside locations for this module
		// build remap rule for all external outputs
		RemapRuleList rules = new RemapRuleList();
		rules.addRule(insideLocs, outsideLocs, RemapRuleType.SAME, "");
		// load wrapMappings with destinations from IO outputs 
		SystemVerilogIOSignalList fullIOList = getFullIOSignalList(); // use full IO list as there could be encapsulated IO
		fullIOList.loadWrapperMapDestinations(wrapMappings, rules, useHierSignalNames, true);
	}
	
	/** load wrapMappings w/ destination signals to child IO list (inputs) */
	private void setChildDestinationSignals() {
		for (SystemVerilogInstance inst: instanceList) {
			// if module is new, process it
			SystemVerilogIOSignalList fullList = inst.getMod().getFullIOSignalList(); // extract full list from this child
			Integer insideLocs = inst.getMod().getInsideLocs();  // get this child's internal locations
			Integer outsideLocs = inst.getMod().getOutsideLocs();  // get this child's internal locations
			// build remap rule for all child inputs
			RemapRuleList rules = new RemapRuleList();
			rules.addRule(outsideLocs, insideLocs, RemapRuleType.ADD_PREFIX, getPrefixedInstanceSignalName(inst.getName()));
			// load wrapMappings  
			fullList.loadWrapperMapDestinations(wrapMappings, rules, false, false);
		}
	}
			
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
