/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved. 
 */
package ordt.extract.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import ordt.annotate.AnnotateCommand;
import ordt.extract.Ordt;
import ordt.extract.PropertyList;
import ordt.extract.RegNumber;
import ordt.output.InstanceProperties;
import ordt.output.OutputBuilder;

/** extracted model component type that can contain children */
public abstract class ModComponent extends ModBaseComponent {

	protected List<ModComponent> childComponents;   // sub-component definitions contained in this component
	private List<ModInstance> childInstances;   // instances contained in this component
	protected List<ModInstance> instancesOf;   // instances of this component
	protected List<ModEnum> enums;   // enums contained in this component
	protected CompParameterLists postPropertyAssignLists;  // list of post property assignments for descendent instances  
	protected boolean isRoot = false;   // root component
	public enum CompType { DEFAULT, ADDRMAP, REG, REGSET, FIELD, FIELDSET }  // subset of comp types used for annotation check and addrmap indication
	protected CompType compType = CompType.DEFAULT;
	protected RegNumber alignedSize;   // size of this component in bytes assuming js alignment rules (used for addr alignment)
	
	protected ModComponent() {
		childComponents = new ArrayList<ModComponent>();
		childInstances = new ArrayList<ModInstance>();
		instancesOf = new ArrayList<ModInstance>();
		enums = new ArrayList<ModEnum>();
		postPropertyAssignLists = new CompParameterLists();
	}

	/** return a new RegElement of the specified tpe
	 *  @return the name
	 */
	public static ModComponent createModComponent(String type) {
		// rdl 'addrmap' | 'regfile' | 'reg' | 'field' | 'fieldstruct' | 'signal'
		// ----- root
		if (type.equals("root")) {  
			ModRootComponent root = new ModRootComponent();
			return root;   
		}
		// ----- addrmap
		if (type.equals("addrmap")) {   
			ModRegSet addrMap = new ModRegSet();
			addrMap.setCompType(CompType.ADDRMAP);  // override REGSET type
			return addrMap;   
		}
		// ----- regfile
		else if (type.equals("regfile") || type.equals("regset")) {
			return new ModRegSet();   
		}
		// ----- reg
		else if (type.equals("reg")) {
			return new ModRegister();   
		}
		// ----- signal
		else if (type.equals("signal")) {
			return new ModSignal();   
		}
		// ----- field
		else if (type.equals("field")) {
			return new ModField();   
		}
		// ----- field
		else if (type.equals("enum")) {
			return new ModEnum();   
		}
		// ----- field set
		else if (type.equals("fieldstruct") || type.equals("fieldset")) {
			return new ModFieldSet();   
		}
		else return null;
	}

	/** process an annotation command */
	public void processAnnotation(AnnotateCommand cmd, int pathLevel) {
		// process cmd using specified recursion type
		if (cmd.pathUsesComponents()) processComponentAnnotation(cmd, pathLevel);
		else processInstanceAnnotation(cmd, pathLevel);
	}

	/** process an annotation command on specified model instances */
	public void processInstanceAnnotation(AnnotateCommand cmd, int pathLevel) {
		// process all child instances - path checks are done in instance
		for (ModInstance inst: getChildInstances()) inst.processInstanceAnnotation(cmd, pathLevel);
	}

	/** process an annotation command on specified model components  */
	private void processComponentAnnotation(AnnotateCommand cmd, int pathLevel) {
		// process this component if its a name/target match
		boolean isDone = cmd.processComponent(this, pathLevel);
		// recursively process component children
		if (!isDone) {
			for (ModComponent comp: childComponents) comp.processComponentAnnotation(cmd, pathLevel + 1);			
		}
	}

	/** write info to stdout */
	public void display () {
			String parID = "";
			if (parent != null) parID = parent.getId();
			System.out.println("\n---------" + this.getClass() + "   id=" + getFullId() + "   parent=" + parID + "   root=" + isRoot + "   compType=" + compType + "   alignedsize=" + alignedSize); 
			// display components
			System.out.println("    child components:");
			for (ModComponent comp: childComponents) {
				System.out.println("        id=" + comp.getId() + "  (" + comp.getClass() + ")");
			}	
			// display instances
			System.out.println("    child instances:");
			for (ModInstance inst: getChildInstances()) {
				System.out.println("        id=" + inst.getId() + " of " + inst.getRegComp().getId());
			}	
			// display instances of
			System.out.println("    instances of:");
			for (ModInstance inst: instancesOf) {
				System.out.println("        id=" + inst.getId());
			}	
			// display enums
			System.out.println("    child enums:");
			for (ModEnum regEnum: enums) {
				System.out.println("        id=" + regEnum.getId());
			}	
			// display properties
			System.out.println("    properties:");
			System.out.println("        " + properties);
			// display default properties
			System.out.println("    default properties:");
			System.out.println("        " + defaultProperties);
			// display default properties
			postPropertyAssignLists.display();
	}
	
	/** return a string representing this sub-class for messages - overridden by child types */
    @Override
	protected String getBaseComponentTypeName() {
		return "component";
	}

	/** returns true if this is the root component of the model */
	public boolean isRoot() {
		return isRoot;
	}

	/** set isRoot - set true by ModRootComponent child class */
	protected void setRoot(boolean isRoot) {
		this.isRoot = isRoot;
	}

	/** set type of this component - used for annotate searches and indication of addrmap/regset */
	public void setCompType(CompType compType) {
		this.compType = compType;
	}

	/** return true if this component is tagged as a field */
	public boolean isField() {
		return (compType == CompType.FIELD);
	}

	/** return true if this component is tagged as a fieldset */
	public boolean isFieldSet() {
		return (compType == CompType.FIELDSET);
	}

	/** return true if this component is tagged as an addrmap */
	public boolean isAddressMap() {
		return (compType == CompType.ADDRMAP);
	}

	/** return true if this component is tagged as a register */
	public boolean isReg() {
		return (compType == CompType.REG);
	}

	/** return true if this component is tagged as a regset */
	public boolean isRegSet() {
		return (compType == CompType.REGSET);
	}

	/** return true if this component is tagged as a regset or addrmap */
	public boolean isRegSetOrAddressMap() {
		return isAddressMap() || isRegSet();
	}

	public CompType getCompType() {
		return compType;
	}

	/** return regnumber containing size in bytes of this component assuming js alignment rules */
	public RegNumber getAlignedSize() {
		return alignedSize;
	}

	/** default setAlignedSize - overridden in ModRootComponent, ModRegSet, ModRegister */
	public void setAlignedSize() {
	}

	/** default sortRegisters - overridden in ModRootComponent, ModRegSet */
	public void sortRegisters() {
	}
	
	/** sort children by address */
	protected void sortChildrenByAddress() { 
		/*if (this.getId().equals("ppe_regs")) {
		System.out.println("ModComponent sortChildrenByAddress: id=" + this.getId()); 
		for(ModInstance inst: childInstances) {
			RegNumber addr = null;
			if (inst.isAddressable()) addr = ((ModAddressableInstance) inst).getAddress();
			System.out.println("-- " + inst.getId() + " at addr=" + addr);
		}
		}*/
		Collections.sort(getChildInstances(), new Comparator<ModInstance>(){
            public int compare(ModInstance a, ModInstance b) {
            	RegNumber addr_a = null, addr_b = null;
    			if (a.isAddressable()) {
    				addr_a = ((ModAddressableInstance) a).getAddress();
    				if ((addr_a == null) || !addr_a.isDefined()) addr_a = null;
    			}
    			if (b.isAddressable()) {
    				addr_b = ((ModAddressableInstance) b).getAddress();
    				if ((addr_b == null) || !addr_b.isDefined()) addr_b = null;
    			}
                if (addr_a == null) return -1;  // nulls eval as less than
                if (addr_b == null) return 1;  // nulls eval as less than
                if (addr_a.isGreaterThan(addr_b)) return 1;  // ascending list
                if (addr_a.isLessThan(addr_b)) return -1;
                return 0;
            }
        }); 
		/*if (this.getId().equals("ppe_regs")) {
		for(ModInstance inst: childInstances) {
			RegNumber addr = null;
			if (inst.isAddressable()) addr = ((ModAddressableInstance) inst).getAddress();
			System.out.println("++ " + inst.getId() + " at addr=" + addr);
		}
		System.exit(0);
		}*/
	}

	/** return true if out of order elements found */
	protected boolean needsAddressSort() {
		RegNumber lastAddr = null;
		for (ModInstance regInst : getChildInstances()) {
			if (regInst.isAddressable()) {
				RegNumber addr = ((ModAddressableInstance) regInst).getAddress();
				//if (this.getId().equals("ppe_regs")) System.out.println("ModComponent needsAddressSort: inst=" + regInst.getId() + " in " + getId() + ", lastAddr=" + lastAddr + ", addr=" + addr);
				if ((addr == null) || !addr.isDefined()) {
					//System.out.println("ModComponent needsAddressSort: null addr found, inst=" + regInst.getId() + " in " + getId() + ", lastAddr=" + lastAddr);
					//return false;
				}
				else {
					if ((lastAddr == null) || addr.isGreaterThan(lastAddr)) lastAddr = addr;
					else {
						//if (this.getId().equals("ppe_regs")) System.out.println("ModComponent needsAddressSort: out of order inst=" + regInst.getId() + " in " + getId() + ", lastAddr=" + lastAddr + ", addr=" + addr);
						return true;
					}
				}
			}
		}
		return false;
	}

	/** add a child component definition
	 *  @param ModComponent to add as child
	 */
	public void addChildComponent(ModComponent regComp) {
		if (findLocalCompDef(regComp.getId()) != null) Ordt.errorMessage("Duplicate component (" + regComp.getId() + ") declared in component " + getId());
		childComponents.add(regComp);		
	}
	
	/** get instancesOf
	 *  @return the instancesOf
	 */
	public List<ModInstance> getInstancesOf() {
		return instancesOf;
	}

	/** set instancesOf
	 *  @param instancesOf the instancesOf to set
	 */
	public void addInstanceOf(ModInstance instanceOf) {
		this.instancesOf.add(instanceOf);
	}

	/** add a child instance
	 *  @param component instance to add as child
	 */
	public void addCompInstance(ModInstance regInst) {
		if (findLocalInstance(regInst.getId()) != null) Ordt.errorMessage("Duplicate instance (" + regInst.getId() + ") declared in component " + getId());
		getChildInstances().add(regInst);		
	}

	/** add a child enum
	 *  @param regenum to add as child
	 */
	public void addCompEnum(ModEnum regEnum) {
		if (findLocalEnum(regEnum.getId()) != null) Ordt.errorMessage("Duplicate enum (" + regEnum.getId() + ") declared in component " + getId());
		enums.add(regEnum);		
	}
	
	/** get list of child enums for this comp
	 */
	public List<ModEnum> getCompEnumList() {
		return enums;		
	}
	
	/** set list of child enums for this comp
	 */
	public void setCompEnumList(List<ModEnum> enums) {
		//System.out.println("ModComponent setCompEnumList: id=" + this.getId() + ", n=" + enums.size());
		this.enums = enums;		
	}

	/** recursively search for a component of specified name
	 *  @param name of component
	 */
	public ModComponent findCompDef(String name) {
		// no name, return null
		if (name == null) return null;
		// else search for component locally
		ModComponent regComp = findLocalCompDef(name);
		// if found return it
		if (regComp != null) return regComp;
		// otherwise try parent
		else if (parent == null) return null;
		else return parent.findCompDef(name);
	}

	/** search for a component in the child component list
	 *  @param id of component
	 */
	public ModComponent findLocalCompDef(String id) {
		//System.out.println("*** looking for comp" + id + " in " + this.getId());
		// loop through child components
		for (ModComponent regComp : childComponents) {
			//System.out.println("  * looking for " + id + ", found " + regComp.getId());
			if ((regComp != null) && (regComp.getId().equals(id))) return regComp;
		}
		return null;
	}

	/** search for an instance in the child inst list
	 *  @param id of inst
	 */
	public ModInstance findLocalInstance(String id) {    
		//System.out.println("*** looking for inst" + id + " in " + this.getId());
		// loop through child instances
		for (ModInstance regInst : getChildInstances()) {
			//System.out.println("  * looking for " + id + ", found " + regComp.getId());
			if ((regInst != null) && (regInst.getId().equals(id))) return regInst;
		}
		return null;
	}

	/** search for an instance in local scope having specified path 
	 *  @param list containing instance path 
	 */
	public ModInstance findInstance(List<String> instances) {   
		//System.out.println("RegComponent: *** looking for inst=" + instances + " in " + this.getId() + ", path depth=" + instances.size());
		if (instances.size()<1) return null;
		// get first path in the list
		String baseInstName = instances.remove(0);
		// search for this instance locally
		//System.out.println("  * looking for inst=" + baseInstName);
		//display(getId());
		ModInstance regInst = findLocalInstance(baseInstName);
		if (regInst == null) return null;
		//System.out.println("RegComponent:  * found inst=" + baseInstName + ", remaining path instances=" + instances.size() + ", reps=" + regInst.getRepCount(true));
		// if no more instances in path we're done so exit
		if (instances.size()==0) return regInst;  
		// otherwise get the next instance recursively
		return regInst.getRegComp().findInstance(instances);
	}

	/** recursively search for a enum of specified name
	 *  @param name of enum
	 */
	public ModEnum findEnum(String name) {
		// no name, return null
		if (name == null) return null;
		// else search for component locally
		ModEnum enumVal = findLocalEnum(name);
		// if found return it
		if (enumVal != null) return enumVal;
		// otherwise try parent
		else if (parent == null) return null;
		else return parent.findEnum(name);
	}

	/** search for an enum in the child list
	 *  @param id of enum
	 */
	private ModEnum findLocalEnum(String id) {     
		//System.out.println("*** looking for enum" + id + " in " + this.getId());
		// loop through child enums
		for (ModEnum regEnum : enums) {
			//System.out.println("  * looking for " + id + ", found " + regEnum.getId());
			if ((regEnum != null) && (regEnum.getId().equals(id))) return regEnum;
		}
		return null;
	}
	
	/** return all ancestor components in specified instance path
	 *  @param instances - list containing instance path 
	 *  @param includeRoot - if true, the root component will be included in the returned list
	 */
	public List<ModComponent> getAncestorComponents(List<String> instances, boolean includeRoot) {   
		List <ModComponent> compList = new ArrayList<ModComponent>();
		// add root component
		if (includeRoot) compList.add(Ordt.getModel().getRoot());
		// add all components in the inst path
		compList.addAll(getBaseComponent().getPathComponents(instances));
		return compList;
	}
		
	/** return components in specified instance path by traversing from current component down
	 *  @param instances - list containing instance path 
	 */
	public List<ModComponent> getPathComponents(List<String> instances) {   
		List <ModComponent> compList = new ArrayList<ModComponent>();
		//if (this.getParent() == null) return compList;  // exit if this is root
		compList.add(this);  // add current component to the list
		//System.out.println("RegComponent: *** adding component " + this.getId() + ", incoming path depth=" + instances.size());
		if (instances.isEmpty()) return compList;
		// pop first path in the list
		String baseInstName = instances.remove(0);
		// search for this instance locally
		//System.out.println("  * looking for inst=" + baseInstName);
		ModInstance regInst = findLocalInstance(baseInstName);
		// if not found then try assuming a replicated element in instance path
		if (regInst == null) {
			String newBaseInstName = "";
			if (baseInstName.matches("\\S+_\\d+$")) {
				newBaseInstName = baseInstName.replaceFirst("_\\d+$", "");
				regInst = findLocalInstance(newBaseInstName);
			}
		}
		//if (regInst == null) {System.out.println("RegComponent: unable to find inst=" + baseInstName+ ",inst list size=" + instances.size()); this.display(this.getId()); System.exit(0); }
		if (regInst == null) return compList;
		//System.out.println("RegComponent: foound inst=" + baseInstName+ ",inst list size=" + instances.size());
		// if no more instances in path we're done so exit
		if (instances.isEmpty()) return compList;  
		// otherwise get the next instance recursively
		compList.addAll(regInst.getRegComp().getPathComponents(instances));
		return compList;
	}
	
	/** return the base component of this ModComponent (component instanced by root) */
	public ModComponent getBaseComponent() {
		// if root is found return component of its first (and should be only) sub-instance
		if (this.getParent() == null) {
			ModInstance subInst = this.getFirstChildInstance();
			if (subInst == null) return null;
			return subInst.getRegComp();  // return component for base instance
		}
		return this.getParent().getBaseComponent();
	}
	
	/** return the first child instance of this component */
	public ModInstance getFirstChildInstance() {
		if (getChildInstances().isEmpty()) return null;
		return getChildInstances().get(0);
	}

	/** return the list of child instances of this component */
	public List<ModInstance> getChildInstances() {
		return childInstances;
	}

	/** return true if this component has child instances */
	public Boolean hasChildInstances() {
		return !childInstances.isEmpty();
	}

	/** get the post property assigns in this component parents given an instance path */
	public PropertyList getPostPropertyAssigns(List<String> instPath) {
		// build a string to match
		Iterator<String> iter = instPath.iterator();
		String path = "";
		while (iter.hasNext()) {
			path = path + iter.next();
			if (iter.hasNext()) path = path + '.';
		}
		// now get the propertyList
		return postPropertyAssignLists.getParmList(path);
	}
	
	// ------------------------------------ inner classes ----------------------------------------

	/**
	 *  parameter assign lists for all instances of a component
	 */
	public class CompParameterLists {
		private HashMap<String, PropertyList> parmLists = new HashMap<String, PropertyList>();  // saved parm lists
		
		/** return parmList for the specified instance path
		 *  @param instance name of the parameter list to get
		 *  @return the parameter list
		 */
		public PropertyList getParmList(String inst) {
			return parmLists.get(inst);
		}
		
		public void display() {
			System.out.println("    post assigned properties:");
			for (String key: parmLists.keySet()) {
				System.out.println("        instance=" + key + ", " + parmLists.get(key).toString());
			}	
		}

		/** add a parmList
		 *  @param instance name key of the parameter list
		 *  @param parameter list to add
		 */
		public void addParmList(String inst, PropertyList pList) {
			parmLists.put(inst, pList);
		}

		@Override
		// NOTE: currently used for uvm class reuse
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			//result = prime * result + getOuterType().hashCode();
			result = prime * result + ((parmLists == null) ? 0 : parmLists.hashCode());
			return result;
		}

		@Override
		// NOTE: currently used for uvm class reuse
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			CompParameterLists other = (CompParameterLists) obj;
			//if (!getOuterType().equals(other.getOuterType()))
			//	return false;
			if (parmLists == null) {
				if (other.parmLists != null)
					return false;
			} else if (!parmLists.equals(other.parmLists))
				return false;
			return true;
		}

		@SuppressWarnings("unused")
		private ModComponent getOuterType() {
			return ModComponent.this;
		}
	}
	
	/** add a new parameter assignment
	 *  @param inst - instance name key of sub parameter list
	 *  @param parm - parameter/property key 
	 *  @param value - value to be assigned to this key
	 *  @return true is parm assignment was added sucessfully
	 */
	public boolean addParameter(String inst, String parm, String value) {
		// find instance key or add if needed
		if (inst == null) return false;
		PropertyList pList = postPropertyAssignLists.getParmList(inst);
		if (pList == null) {
			pList = new PropertyList();
			postPropertyAssignLists.addParmList(inst, pList);
		}
		// compute the ancestor depth of this assign from instance
		int depth = inst.length() - inst.replace(".", "").length() + 1;
		
		// now that parameter list is found add the assign
		pList.setProperty(parm, value, depth);
		//if (inst.contains("padoody")) {
			//System.out.println("RegComponent: added parm assign for c=" + getId() + ", i=" + inst + ", parm=" + parm + ", value=" + value + ", depth=" + depth); 
			//postPropertyAssignLists.display();
		//}
		return true;
	}

	/** create a new model instance of this component */
	public ModInstance createNewInstance() {
		ModInstance newInst = new ModInstance();  // create the new instance
		newInst.setRegComp(this);  // set component type of new instance
		addInstanceOf(newInst);  // add instance to list for this comp
		return newInst;
	}

	/** create instance suffix to indicate repeated regs */
	protected static String getRepSuffix(int rep, int repCount) {
		if (repCount>1) return "_" + rep;
		else return "";
	}
	
	/** recursively build user defined signal name list */
	public void getDefinedSignalNames(String newSuffix, HashSet<String> nameList) {
		// pass along to all instances of this component
		//System.out.println("ModComponent getDefinedSignalNames: id=" + getId() + ", instancesOf=" + instancesOf.size());
		if (instancesOf.size() == 0) nameList.add("sig_" + newSuffix);  // root components that are not explicitly instanced have no inst count
		else for (ModInstance inst: instancesOf) inst.getDefinedSignalNames(newSuffix, nameList);
		
	}

	// ------------------------------------ code gen methods ----------------------------------------
	
	/* generate output */
	public void generateOutput(ModInstance callingInst, OutputBuilder outputBuilder) {
	
		// each subclass should override to create appropriate code based on calling instance
		if (callingInst == null) return;
		// generate each direct instance in this component
		for (ModInstance regInst : getChildInstances()) {
			outputBuilder.pushInstance(new InstanceProperties(callingInst));
			regInst.generateOutput(outputBuilder);
			outputBuilder.popInstance();
		}
	}

	@Override
	// NOTE: currently used for uvm class reuse
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((alignedSize == null) ? 0 : alignedSize.hashCode());
		//result = prime * result + ((childComponents == null) ? 0 : childComponents.hashCode());
		result = prime * result + ((getChildInstances() == null) ? 0 : getChildInstances().hashCode());
		result = prime * result + ((compType == null) ? 0 : compType.hashCode());
		result = prime * result + ((enums == null) ? 0 : enums.hashCode());
		result = prime * result + (isRoot ? 1231 : 1237);
		result = prime * result + ((postPropertyAssignLists == null) ? 0 : postPropertyAssignLists.hashCode());
		return result;
	}

	@Override
	// NOTE: currently used for uvm class reuse
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		ModComponent other = (ModComponent) obj;
		if (alignedSize == null) {
			if (other.alignedSize != null)
				return false;
		} else if (!alignedSize.equals(other.alignedSize))
			return false;
		//if (childComponents == null) {
		//	if (other.childComponents != null)
		//		return false;
		//} else if (!childComponents.equals(other.childComponents))
		//	return false;
		if (getChildInstances() == null) {
			if (other.getChildInstances() != null)
				return false;
		} else if (!getChildInstances().equals(other.getChildInstances()))
			return false;
		if (compType != other.compType)
			return false;
		if (enums == null) {
			if (other.enums != null)
				return false;
		} else if (!enums.equals(other.enums))
			return false;
		if (isRoot != other.isRoot)
			return false;
		if (postPropertyAssignLists == null) {
			if (other.postPropertyAssignLists != null)
				return false;
		} else if (!postPropertyAssignLists.equals(other.postPropertyAssignLists))
			return false;
		return true;
	}


}
