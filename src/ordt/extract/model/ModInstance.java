/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved. 
 */
package ordt.extract.model;

import java.util.HashMap;
import java.util.HashSet;

import ordt.annotate.AnnotateCommand;
import ordt.extract.Ordt;
import ordt.extract.Ordt.InputType;
import ordt.extract.PropertyList.PropertyValue;
import ordt.output.OutputBuilder;

/** class of model component instance */
public class ModInstance extends ModBaseComponent {
	
	protected ModComponent regComp;  // reg component to be instanced (vs parent, which a component that contains this inst)
	protected boolean isAddressable = false;
	protected boolean isIndexed = false;
	protected Integer repCount = 1;   // replication count of this instance
    
	public ModInstance() {
	}

	/** get isAddressable
	 *  @return the isAddressable
	 */
	public boolean isAddressable() {
		return isAddressable;
	}

	/** get isIndexed
	 *  @return the isIndexed
	 */
	public boolean isIndexed() {
		return isIndexed;
	}

	/** process an annotation command on specified model instances  */
	public void processInstanceAnnotation(AnnotateCommand cmd, int pathLevel) {
		// process this component if its a name/target match
		boolean isDone = cmd.processInstance(this, pathLevel);
		// recursively process component children
		if (!isDone) this.getRegComp().processInstanceAnnotation(cmd, pathLevel + 1);
	}

	/** write info to stdout */
	public void display (boolean showInstanceComponent) {
			String compID = "";
			if (regComp != null) {
				compID = regComp.getId();
				if ((compID == null) || compID.isEmpty()) compID = "<anonymous>";
			}
			System.out.println("\n--------- Instance of " + compID + "   id=" + getFullId() ); 
			//System.out.println("         full id=" + getFullId()); 
			System.out.println("    replication count=" + getRepCount());   
			System.out.println("    root instance=" + isRootInstance());   
			// display parms
			System.out.println("    properties:");
			System.out.println("        " + properties);
			// display default properties
			System.out.println("    default properties:");
			System.out.println("        " + defaultProperties);
			
        // optionally display component
		if ((regComp != null) && showInstanceComponent) regComp.display();
	}
	
	/** default check for implicit default property assignments - overridden by child types */
    @Override
	protected boolean isImplicitDefaultProperty(String propertyName) {
		HashSet<String> implicitDefaultProperties = new HashSet<String>();
		// implicit default properties...
		implicitDefaultProperties.add("donttest");
		implicitDefaultProperties.add("dontcompare");
		// if jspec, interpret as implicit default
		if (Ordt.hasInputType(InputType.JSPEC)) {
			implicitDefaultProperties.add("js_attributes");
			implicitDefaultProperties.add("category");
			implicitDefaultProperties.add("sub_category");			
		}
		return implicitDefaultProperties.contains(propertyName);
	}
	
	/** return a string representing this sub-class for messages - overridden by child types */
    @Override
	protected String getBaseComponentTypeName() {
		return "instance";
	}

	/** returns the component of this instance
	 */
	public ModComponent getRegComp() {
		return regComp;
	}

	/** sets the component of this instance
	 *  @param regComp - the ModComponent to set for this instance
	 */
	public void setRegComp(ModComponent regComp) {
		this.regComp = regComp;
	}

	/** set repCount
	 *  @param repCount the repCount to set
	 */
	public void setRepCount(Integer repCount) {
		this.repCount = repCount;
	}

	/** get repCount - number of times this instance is replicated  
	 *  @return the repCount
	 */
	public Integer getRepCount() {
		return repCount;
	}

	/** set parent component 
	 *  @param parent - parent ModComponent where this instance is made
	 */
	@Override
	public void setParent(ModComponent parent) {
		this.parent = parent;
		//System.out.println("ModInstance setParent: parent.isRoot()=" + parent.isRoot() + ", parent id=" + parent.getId());
	}

	/** returns true if parent component is root
	 *  @return the isRootInstance
	 */
	public boolean isRootInstance() {
		return parent.isRoot();
	}

	/** update parms using values in supplied hashmap
	 *  @param parms the parms to set
	 *
	@Override */
	public void updateProperties(HashMap<String, PropertyValue> updates) {
		//if (getId().equals("str")) System.out.println("ModInstance updateProperties: " + this.getFullId() + ", updates=" + updates);
		for (String key: updates.keySet()) {
			PropertyValue prop = updates.get(key);
			String val = prop.getValue();
			int depth = prop.getDepth();
			//if (getId().equals("spin") && "category".equals(key)) System.out.println("ModInstance updateProperties: " + this.getFullId() + ", cat update=" + val);
			// only update if instance doesnt already have the property
			if (!properties.getProperties().containsKey(key)) {     // TODO - simplify this?
				// update numeric instance var values (info passed from components)
				if ("repcount".equals(key)) setRepCount(Integer.valueOf(val));
				// set other numerics (overriden by child classes)
				updateInstanceVar(key, val);
				// update parameter
				setProperty(key, val, depth);
			}
			//else if (getId().equals("spin") && "category".equals(key)) System.out.println("ModInstance updateProperties: " + this.getFullId() + ": attempt to update key=" + key + " with value=" + updates.get(key) + " failed");
		}	
	}

	/** set a numeric instance variable - overridden in ModInstance child classes
	 * 
	 * @param key - if this key is found, instance value will be updated to val
	 * @param val - value to be used
	 */
	protected void updateInstanceVar(String key, String val) {
		// no additional updates for base ModInstance
	}

	/** recursively build user defined signal name list */
	public void getDefinedSignalNames(String suffix, HashSet<String> nameList) {
		String separator = suffix.isEmpty()? "" : "_"; 
		// append inst name to suffix including replication
		if (repCount>1) {
			for (int rep = 0; rep < repCount; rep++) {
				String newSuffix = getId() + "_" + rep + separator + suffix;
				if (isRootInstance()) nameList.add("sig_" + newSuffix);  // add to list if root instance  
				else parent.getDefinedSignalNames(newSuffix, nameList);  // otherwise call ancestors
			}
				
		}
		// otherwise if a single rep
		else {
			String newSuffix = getId() + separator + suffix;
			if (isRootInstance()) nameList.add("sig_" + suffix);  // add to list if root instance
			else parent.getDefinedSignalNames(newSuffix, nameList);  // otherwise call ancestors
		}
		
	}

	// ------------------------------------ code gen  ----------------------------------------

	/** generate some code by calling instanced components with specified inst parms 
	 * @param outputBuilder */
	public void generateOutput(OutputBuilder outputBuilder) {
		String baseAddr = getProperty("address");  
		if (baseAddr == null) baseAddr = "n/a -or- next available";
		//System.out.println(RegExtractor.repeat(' ', depth) + "Generating instance " + getId() + ", calling gen of " + getRegComp().getId() +
		//		" (" + getRegComp().getClass() + ") starting at address=" + baseAddr);
		//display();
		getRegComp().generateOutput(this, outputBuilder);
	}

	@Override
	// NOTE: currently used for uvm class reuse
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + (isAddressable ? 1231 : 1237);
		result = prime * result + (isIndexed ? 1231 : 1237);
		result = prime * result + ((regComp == null) ? 0 : regComp.hashCode());
		result = prime * result + ((repCount == null) ? 0 : repCount.hashCode());
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
		ModInstance other = (ModInstance) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (isAddressable != other.isAddressable)
			return false;
		if (isIndexed != other.isIndexed)
			return false;
		if (regComp == null) {
			if (other.regComp != null)
				return false;
		} else if (!regComp.equals(other.regComp))
			return false;
		if (repCount == null) {
			if (other.repCount != null)
				return false;
		} else if (!repCount.equals(other.repCount))
			return false;
		return true;
	}
}
