package ordt.output.systemverilog.common;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	
	protected enum WrapperRemapType { ASSIGN, SYNC_DELAY, ASYNC_LEVEL, ASYNC_DATA } // flops(num_stages), async_level(aclk), async_data(aclk, valid_sig, num_data) <-- need to group

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
		private boolean isOutput;  // true if this destination signal is an external output

		private WrapperRemapDestination(int srcLowIndex, SystemVerilogSignal dest, WrapperRemapXform xform, boolean isOutput) { 
			this.srcLowIndex = srcLowIndex;
			this.dest = dest;
			this.xform = xform;
			this.isOutput = isOutput;
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
		
        public boolean isOutput() {
			return isOutput;
		}

		@Override
		public String toString() {
			return "  -> " + dest + ", type=" + xform.getType();
		}

		/** Given updated slice info, update size/index of this WrapperRemapDestination and return a list of new WrapperRemapDestinations.
		 *  Creates max 2 new WrapperRemapDestinations (before and after updated bit range)
		 * 
		 * @param sliceInfo - SystemVerilogSignal containing lowIndex and size of a matching signalName pattern
		 * @param isSourceSlice - true if sourceInfo refers to a source signal slice
		 * @return List of new WrapperRemapDestinations created
		 */
		public List<? extends WrapperRemapDestination> createNewDestSlices(SystemVerilogSignal sliceInfo, boolean isSourceSlice) {
			List<WrapperRemapDestination> retList = new ArrayList<WrapperRemapDestination>();
			// save current size and lowIndex
			int origSrcLowIndex = srcLowIndex;
			int origDstLowIndex = dest.getLowIndex();
			int origLowIndex = isSourceSlice? origSrcLowIndex : origDstLowIndex; 
			int origSize = dest.getSize();
			int indexChange = sliceInfo.getLowIndex() - origLowIndex;
			int sizeChange = sliceInfo.getSize() - origSize;
					
			// if same slice then just return with no change
			if ((sliceInfo.getSize() == origSize) && (indexChange == 0)) return retList;
			// create a new slice before current if needed
			if (indexChange > 0) {
				// TODO
			}
			else if (indexChange < 0)
				Ordt.errorExit("wrapper slice " + sliceInfo.getRefArray() + " is invalid for " + (isSourceSlice? "source of signal " : "signal ") + dest.getName());
			// create a new slice after current if needed
			else if (indexChange + sizeChange < 0) {
				// TODO
			}
			else if (indexChange + sizeChange > 0)
				Ordt.errorExit("wrapper slice " + sliceInfo.getRefArray() + " is invalid for " + (isSourceSlice? "source of signal " : "signal ") + dest.getName());
            // update size and index of this object
			// TODO
			return retList;
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
			dests.add(new WrapperRemapDestination(lowIndex, newDest, new WrapperRemapXform(), isOutput)); 
		}
		
		@Override
		public String toString() {
        	String retStr = source + ", isValidSource=" + isValidSource + ", isInput=" + isInput;
        	for (WrapperRemapDestination dest: dests)
    			retStr += "\n   " + dest;
			return retStr;
		}

		/** update remap transforms for specified IO signals in this remapInstance.
		 *  
		 * @param xformMap - map of IO signal pattern match keys and corresponding transform to be used on match.
		 *                   The key string can either be a direct match or else signals will be checked against patterns in order.
		 *                   If key is of the form <name>[range] the transform will only be applied to the specified slice
		 *                   and the source/destination mapping will be changed accordingly
		 */
		public void setRemapTransforms(LinkedHashMap<String, WrapperRemapXform> xformMap) {
    		SystemVerilogSignal src = this.getSource();  // get source signal info
    		// if source is an input, try to match it
    		if (this.isInput()) {
    			Pair<WrapperRemapXform, SystemVerilogSignal> retPair = getMatchingXform(src, xformMap);
        		WrapperRemapXform xform = retPair.first();
        		SystemVerilogSignal sliceInfo = retPair.second();
        		// if transform was assigned, apply to all destinations
        		if (xform != null) { 
            		// set same xform on all destinations for this source
					List<WrapperRemapDestination> newDests = new ArrayList<WrapperRemapDestination>();
        			for (WrapperRemapDestination remapDest: this.getDests()) {
        				// add remaining slices to dest list with updated src index, dest index, dest size, and same xform
        				newDests.addAll(remapDest.createNewDestSlices(sliceInfo, true));
        				// update src index, dest index, dest size, and transform, 
        				remapDest.setXform(xform);
        			}
        			this.getDests().addAll(newDests);
        		}
    		}
    		// otherwise, check each destination for a match
    		else {
    			for (WrapperRemapDestination remapDest: this.getDests()) {
    				if (remapDest.isOutput()) {
    		    		SystemVerilogSignal dst = remapDest.getDest();  // get destination signal info
    	    			Pair<WrapperRemapXform, SystemVerilogSignal> retPair = getMatchingXform(dst, xformMap);
    	        		WrapperRemapXform xform = retPair.first();
    	        		SystemVerilogSignal sliceInfo = retPair.second();
    	        		if (xform != null) {
    						List<WrapperRemapDestination> newDests = new ArrayList<WrapperRemapDestination>();
            				// add remaining slices to dest list with updated src index, dest index, dest size, and same xform
            				newDests.addAll(remapDest.createNewDestSlices(sliceInfo, false));
            				// update src index, dest index, dest size, and transform, 
            				remapDest.setXform(xform);
                			this.getDests().addAll(newDests);
    	        		}
    				}
    			}			
    		}
		}

		/** return a remap transform if a target signal name matches a set of patterns.
		 * @param src - target signal
		 * @param xformMap - map of IO signal pattern match keys and corresponding transform to be used on match.
		 *                   The key string can either be a direct match or else signals will be checked against patterns in order.
		 *                   If key is of the form <name>[range] the transform will only be applied to the specified slice
		 *                   and the source/destination mapping will be changed accordingly
		 * @return valid WrapperRemapXform if a match or null otherwise
		 */                   
		private Pair<WrapperRemapXform, SystemVerilogSignal> getMatchingXform(SystemVerilogSignal target, LinkedHashMap<String, WrapperRemapXform> xformMap) {
    		// look for exact source match first
			String targetName = target.getName();
    		if (xformMap.containsKey(targetName)) {
    			return new Pair<WrapperRemapXform, SystemVerilogSignal>(xformMap.get(targetName), target);  // direct match, so no slice
    		}
    		// otherwise, loop through patterns looking for a match
    		else for (String rawPattern: xformMap.keySet()) {
    			// only treat as a pattern if wildcards or slice info  
    			if (rawPattern.contains("*") || rawPattern.contains(",")) {
    				// extract the name match pattern and range from the raw pattern 
    				String namePattern = rawPattern;  // by default pattern is as-is 
    				SystemVerilogSignal patternInfo = getNamePatternInfo(rawPattern, target);
    				if (patternInfo != null) {  // proceed if pattern is valid
    					namePattern = patternInfo.getName();
    					// now look for a target name match
        				Pattern nPattern = Pattern.compile(namePattern);
        				Matcher m = nPattern.matcher(targetName);
        				if (m.matches()) {
            				// return the transform and slice info for this pattern (first match)
        	    			return new Pair<WrapperRemapXform, SystemVerilogSignal>(xformMap.get(rawPattern), patternInfo);
        				}
    				}
    			}
    		}
			return null;  // no match
		}

		/** check for a valid raw pattern and return as a signal instance (pattternName, lowindex, size) or null if invalid */
		private SystemVerilogSignal getNamePatternInfo(String rawPattern, SystemVerilogSignal target) {
            String namePattern;
			Pattern slicePattern = Pattern.compile("^(.*)(\\s*\\[(\\d+)(:(\\d+))\\])$"); // detect slice form 
			Matcher m = slicePattern.matcher(rawPattern);
			if (m.matches()) {
				namePattern = m.group(1);
				Integer leftIdx = (m.groupCount()>2)? Integer.valueOf(m.group(3)) : null;
				Integer rightIdx = (m.groupCount()>4)? Integer.valueOf(m.group(5)) : null;
				Integer newLowIdx = (rightIdx != null)? rightIdx : (leftIdx != null)? leftIdx : target.getLowIndex();
				Integer newSize = (rightIdx != null)? leftIdx - rightIdx - 1 : (leftIdx != null)? 1 : target.getSize();
				// check for a name match
				namePattern = namePattern.replaceAll("\\*", ".*");  // allow wildcards
				namePattern = "^" + namePattern + "$";
				return new SystemVerilogSignal(namePattern, newLowIdx, newSize);
			}
			return null;
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
    				SystemVerilogSignal dst = remapDest.getDest();   // get destination signal info   TODO - if a slice then qualify src/dst ranges
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
		
		/** update remap transforms for specified IO signals in this WrapperSignalMap.
		 *  
		 * @param xformMap - map of IO signal pattern match keys and corresponding transform to be used on match.
		 *                   The key string can either be a direct match or else signals will be checked against patterns in order.
		 *                   If key is of the form <name>[range] the transform will only be applied to the specified slice
		 *                   and the source/destination mapping will be changed accordingly
		 */
		public void setRemapTransforms(LinkedHashMap<String, WrapperRemapXform> xformMap) {
			// loop through map sources
        	for (String root: mappings.keySet()) {
        		WrapperRemapInstance wrapInst = mappings.get(root);
        		// process IO xform on this remap instance (src-dests grouping)
        		wrapInst.setRemapTransforms(xformMap);
		    }
		}
	}
	
	/** utility class for passing around obj pairs */
	public class Pair<U, V> {
		    private U first;
		    private V second;

		    public Pair(U first, V second) {
		        this.first = first;
		        this.second = second;
		    }
		    public U first() {
		        return first;
		    }
		    public V second() {
		        return second;
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
		//wrapMappings.display(); 
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

	public static void main(String[] args) {
		String  namePattern = "mysignal_*_bla";
		String  targetName = "mysignal_1_bla";
		namePattern = namePattern.replaceAll("\\*", ".*");
		namePattern = "^" + namePattern + "$";
		System.out.println("namePattern=" + namePattern);
		Pattern nPattern = Pattern.compile(namePattern);
		Matcher m = nPattern.matcher(targetName);
		if (m.matches())  System.out.println("--- match");
		else  System.out.println("--- no match");
	}

}
