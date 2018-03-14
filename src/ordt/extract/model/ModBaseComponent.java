/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved. 
 */
package ordt.extract.model;

import java.util.HashMap;

import ordt.extract.Ordt;
import ordt.extract.PropertyList;
import ordt.extract.PropertyList.PropertyValue;

/** model base class (ModComponent and ModInstance are child types) */
public abstract class ModBaseComponent {

	protected String id = "";   // id (type) of component
	protected ModComponent parent;   // parent component of this entity
	protected Integer inputLineNumber;  // line number in rdl file where component is defined
	
	protected PropertyList properties;   // properties defined for this component    
	protected PropertyList defaultProperties;   // default properties defined in this component

	protected ModBaseComponent() {
		id = "";
		parent = null;
		properties = new PropertyList();
		defaultProperties = new PropertyList();
	}

	/** get id
	 *  @return the id
	 */
	public String getId() {
		return id;
	}
	
	/** get the fully qualified id by traversing ancestors
	 *  @return the id
	 */
	public String getFullId() {
		// if parent is null or root, we're done
		if ((parent == null) || ("root".equals(parent.getId()))) return getId();
		// else call parent recursively
		else return parent.getFullId() + "." + getId();
	}

	/** set id
	 *  @param id the id to set
	 */
	public void setId(String id) {
		if (id != null) id = id.replace("\\", "");  // remove escapes
		this.id = id;
	}

	/** get inputLineNumber
	 *  @return the inputLineNumber
	 */
	public Integer getInputLineNumber() {
		return inputLineNumber;
	}

	/** set inputLineNumber
	 *  @param inputLineNumber the inputLineNumber to set
	 */
	public void setInputLineNumber(Integer inputLineNumber) {
		this.inputLineNumber = inputLineNumber;
	}
	
	/** get string containing inputLineNumber
	 */
	public String getInputLineString(String prefix, String suffix) {
		return (inputLineNumber == null) ? "" : prefix + inputLineNumber + suffix;
	}
	
	// ------ property methods 

	/** return true if a specified property (or default property) exists
	 *  @param name of the property  
	 *  @return boolean
	 */
	public Boolean hasProperty(String name) {
		return (properties.hasProperty(name) || defaultProperties.hasProperty(name));
	}

	/** return true if a default property exists
	 *  @param name of the property  
	 *  @return boolean
	 */
	public Boolean hasDefaultProperty(String name) {
		return defaultProperties.hasProperty(name);
	}
	
	/** return true if a property=true
	 *  @param name of the property  
	 *  @return boolean
	 */
	public Boolean hasTrueProperty(String name) {   
		return properties.hasTrueProperty(name);
	}
	
	/** get a property (or default property)
	 *  @param name of the property value to get
	 *  @return the property value
	 */
	public String getProperty(String name) {
		if (properties.hasProperty(name)) return properties.getProperty(name);
		return getDefaultProperty(name);
	}
	
	/** get a default property
	 *  @param name of the property value to get
	 *  @return the property value
	 */
	public String getDefaultProperty(String name) {
		return defaultProperties.getProperty(name);
	}
	
	/** get an integer property or null if exception
	 *  @param name of the property value to get
	 *  @return the integer property value
	 */
	public Integer getIntegerProperty(String name) {
		Integer outInt = properties.getIntegerProperty(name);
		if (outInt == null) outInt = defaultProperties.getIntegerProperty(name);
		return outInt;
	}

	/** get an integer default property or null if exception
	 *  @param name of the property value to get
	 *  @return the integer property value
	 */
	public Integer getDefaultIntegerProperty(String name) {
		return defaultProperties.getIntegerProperty(name);
	}

	/** get properties
	 *  @return the properties
	 */
	public HashMap<String, PropertyValue> getProperties() {
		return properties.getProperties();
	}
	
	/** get defaultProperties
	 *  @return the defaultProperties
	 */
	public HashMap<String, PropertyValue> getDefaultProperties() {
		return defaultProperties.getProperties();
	}
	
	/** set a property  
	 *  @param name of the property to set
	 *  @param value of the property
	 *  @param depth - ancestor depth of assignment lhs (eg xxx.yyy -> depth=2) 
	 */
	public void setProperty(String name, String value, int depth) {   
		if (isValidProperty(name)) {
			if (isImplicitDefaultProperty(name)) {
				this.defaultProperties.setProperty(name, value, depth);
				//Jrdl.infoMessage("assignment of implicit default property " + name + " in " + getBaseComponentTypeName() + " " + id + this.getInputLineString(" near line ", ""));
			}
			else this.properties.setProperty(name, value, depth);
		}
		else {
			String id = this.getId().startsWith("aNON") ? "(anonymous)" : this.getId();
			Ordt.warnMessage("assignment of property " + name + " will be ignored in " + getBaseComponentTypeName() + " " + id + this.getInputLineString(" near line ", ""));
		}
		//System.out.println("ModBaseComponent " + this.getFullId() + ": updated key=" + name + " with value=" + value);
	}

	/** set a  default property
	 *  @param name of the property to set
	 *  @param value of the property
	 */
	public void setDefaultProperty(String name, String value) {
		defaultProperties.setProperty(name, value);
	}
	
	/** default check on valid property assignments - overridden by child types */
	protected boolean isValidProperty(String propertyName) {
		return true;
	}
	
	/** default check for implicit default property assignments - overridden by child types */
	protected boolean isImplicitDefaultProperty(String propertyName) {
		return false;
	}
	
	/** remove a property  
	 *  @param name of the property to be removed
	 */
	public void removeProperty(String name) {
		properties.removeProperty(name);
	}
	
	/** remove a default property  
	 *  @param name of the property to be removed
	 */
	public void removeDefaultProperty(String name) {
		defaultProperties.removeProperty(name);
	}
	
	/** update default properties using values in supplied hashmap
	 *  @param hashmap of properties to be set
	 */
	public void updateDefaultProperties(HashMap<String, PropertyValue> updates) {
		defaultProperties.updateProperties(updates);
	}	

	// -----------------------------
	
	/** return a string representing this sub-class for messages - overridden by child types */
	abstract protected String getBaseComponentTypeName(); /* {
		return "";
	}*/
	
	/** get parent
	 *  @return the parent
	 */
	public ModComponent getParent() {
		return parent;
	}

	/** set parent
	 *  @param parent the parent to set
	 */
	public void setParent(ModComponent parent) {
		this.parent = parent;
	}

	@Override
	// NOTE: currently used for uvm class reuse - id is omitted
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((defaultProperties == null) ? 0 : defaultProperties.hashCode());
		result = prime * result + ((properties == null) ? 0 : properties.hashCode());
		return result;
	}

	@Override
	// NOTE: currently used for uvm class reuse - id is omitted
	public boolean equals(Object obj) {
		//System.out.println("ModBaseComponent equals: entering " + id);
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ModBaseComponent other = (ModBaseComponent) obj;
		if (defaultProperties == null) {
			if (other.defaultProperties != null)
				return false;
		} else if (!defaultProperties.equals(other.defaultProperties))
			return false;
		if (properties == null) {
			if (other.properties != null)
				return false;
		} else if (!properties.equals(other.properties))
			return false;
		//System.out.println("ModBaseComponent equals: **true** " + id);
		return true;
	}

}
