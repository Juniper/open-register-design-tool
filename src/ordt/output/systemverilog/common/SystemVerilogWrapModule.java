package ordt.output.systemverilog.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

import ordt.extract.Ordt;
import ordt.output.OutputWriterIntf;
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
		// dup child modules not fully supported
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
	
	protected enum WrapperRemapType { ASSIGN, FLOPS, ASYNC_LEVEL, ASYNC_DATA } // flops(num_stages), async_level(aclk), async_data(aclk, valid_sig, num_data) <-- need to group

	/** default remap transform (simple assign) */
	private class WrapperRemapXform {
		WrapperRemapType type = WrapperRemapType.ASSIGN;
		public WrapperRemapType getType() {
			return type;
		}
	}
	
	private class WrapperRemapDestination {
		private int srcLowIndex;  // low index in source (source size is dest.size)
		private SystemVerilogSignal dest;  // destination signal and vector info
		private WrapperRemapXform xform;  // remap transform

		private WrapperRemapDestination(int srcLowIndex, SystemVerilogSignal dest, WrapperRemapXform xform) { 
			this.srcLowIndex = srcLowIndex;
			this.dest = dest;
			this.xform = xform;
		}
		
        @SuppressWarnings("unused")
		public int getSrcLowIndex() {
			return srcLowIndex;
		}

        public SystemVerilogSignal getDest() {
			return dest;
		}

        public WrapperRemapXform getXform() {
			return xform;
		}

		public void setXform(WrapperRemapXform xform) {
			this.xform = xform;
		}

		@Override
		public String toString() {
			return "  -> " + dest + ", type=" + xform.getType();
		}
	}
	
	/** a mapping from source signal to its destinations */
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
			dests.add(new WrapperRemapDestination(lowIndex, newDest, new WrapperRemapXform())); 
		}
		
		@Override
		public String toString() {
        	String retStr = source + ", isValidSource=" + isValidSource + ", isInput=" + isInput;
        	for (WrapperRemapDestination dest: dests)
    			retStr += "\n   " + dest;
			return retStr;
		}
	}
	
	/** mapping of all wrapper source signals to destinations */
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
        		SystemVerilogSignal src = wrapInst.getSource();  // get source signal info
        		// add non-input/non-hierarchical sources to wire def list
        		if (wrapInst.isValidSource() && !wrapInst.isInput() && !src.getName().contains(".")) {
        			wireDefList.addVector(src.getName(), src.getLowIndex(), src.getSize()); // add the source define
        		}
        		// check for source with no destinations
        		List<WrapperRemapDestination> remapDestList = wrapInst.getDests();
        		if (remapDestList.isEmpty())
        			Ordt.warnMessage("no destinations found for wrapper source signal " + src.getName());
        		// add all destinations to wire list and use specified xform
    			for (WrapperRemapDestination remapDest: wrapInst.getDests()) {
    				SystemVerilogSignal dst = remapDest.getDest();   // get destination signal info
    				// check src/dest sizes
    				if (src.getSize() != dst.getSize()) 
    					Ordt.errorMessage("sizes of wrapper source (" + src.getName() + ", n=" + src.getSize() +
    							") and destination (" + dst.getName() + ", n=" + dst.getSize() + ") pair do not match");  // TODO - change to allow slice assigns
    				// add transform is source is valid
    				if (wrapInst.isValidSource()) {
    					WrapperRemapXform xform = remapDest.getXform();
    			        switch (xform.getType()) {  // TODO - enable other xforms
    		            case ASSIGN: 
    		            	if (!dst.getName().contains(".")) wireDefList.addVector(dst.getName(), dst.getLowIndex(), dst.getSize());  // add the destination define
            				wireAssignList.add(dst.getName() + " = " + src.getName() + ";");  // add the assignment
    		                break;
   		                default: 
   		                	Ordt.errorMessage("invalid wrapper transform (" + xform.getType() + ") specified from source " + src.getName() + " to destination " + dst.getName());
   		                	break;
    		        }
    					
    				}
    				else Ordt.errorMessage("no valid source found for wrapper destination signal " + dst.getName());  // TODO add internal tieoff option

    			}
		    }
		}
		
		/** update remap transforms for certain signals - keys off root name */
		public void setRemapTransforms(HashMap<String, WrapperRemapXform> xformMap) {
			// loop through map sources
        	for (String root: mappings.keySet()) {
        		if (xformMap.containsKey(root)) {
            		WrapperRemapInstance wrapInst = mappings.get(root);
            		// set same xform on all destinations for this source
        			for (WrapperRemapDestination remapDest: wrapInst.getDests()) {
        				remapDest.setXform(xformMap.get(root));
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

}
