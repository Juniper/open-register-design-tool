/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved. 
 */
package ordt.extract;

import java.util.HashMap;
import java.util.Set;

/**
 *  class for storage of assigned properties
 */
public class PropertyList {
	private HashMap<String, PropertyValue> values = new HashMap<String, PropertyValue>();  // saved parm values
	
	/** clear all params */
	public void clear() {
		values.clear();
	}

	/** get a property string value
	 *  @param name of the property value to get
	 *  @return the value
	 */
	public String getProperty(String name) {
		PropertyValue prop = values.get(name);
		return (prop == null) ? null : prop.getValue();
	}

	/** get a property assignment depth
	 *  @param name of the property depth to get
	 *  @return the depth
	 */
	public int getDepth(String name) {
		PropertyValue prop = values.get(name);
		return (prop == null) ? 0 : prop.getDepth();
	}
	
	/** get an integer property or null if exception
	 *  @param name of the property value to get
	 *  @return the integer property value
	 */
	public Integer getIntegerProperty(String name) {
		Integer retval;
		try {
			retval = Integer.valueOf(getProperty(name));
		} catch (NumberFormatException e) {
			return null;
		}
		return retval;
	}
	
	/** return true if a key exists
	 *  @param name of the property  
	 *  @return boolean
	 */
	public Boolean hasProperty(String name) {
		return (getProperty(name) != null);
	}

	/** return true if a prop value=true
	 *  @param name of the property 
	 *  @return boolean
	 */
	public Boolean hasTrueProperty(String name) {
		String prop = getProperty(name);
		return (prop==null)? false : "true".equals(prop.toLowerCase());
	}

	/** return true if a prop value=false
	 *  @param name of the property 
	 *  @return boolean
	 */
	public Boolean hasFalseProperty(String name) {
		String prop = getProperty(name);
		return (prop==null)? false : "false".equals(prop.toLowerCase());
	}
	
	/** return true if a prop value=true or false
	 *  @param name of the property 
	 *  @return boolean
	 */
	public Boolean hasBooleanProperty(String name) {
		return hasTrueProperty(name) || hasFalseProperty(name);
	}
	
	/** return true if a prop value is non-boolean and non-null
	 *  @param name of the property 
	 *  @return boolean
	 */
	public Boolean hasRefProperty(String name) {
		return hasProperty(name) && !hasBooleanProperty(name);
	}
	
	/** return true if a prop value=true or is non-boolean and non-null
	 *  @param name of the property 
	 *  @return boolean
	 */
	public Boolean hasTrueOrRefProperty(String name) {
		return hasTrueProperty(name) || (hasProperty(name) && !hasFalseProperty(name));
	}

	/** get property hashmap
	 *  @return values
	 */
	public HashMap<String, PropertyValue> getProperties() {
		return values;
	}
	
	/** set a prop value and reconcile values
	 *  @param name of the value to set
	 *  @param value - string
	 *  @param depth - depth in instancepath ancestors of assignment statement lhs
	 */
	public void setProperty(String name, String value, int depth) {  // TODO use depth
		PropertyValue prop;
		//if (depth>0) System.out.println("PropertyList setProperty: " + name + ", val=" + value + ", d=" + depth);
		// create storage object according to depth
		if (depth > 0) prop = new DynamicPropertyValue(depth);
		else prop = new PropertyValue();
		
		// reconcile woset/woclr assigns
		if (name.equals("woclr") && isTrueString(value)) values.remove("woset"); 
		else if (name.equals("woset") && isTrueString(value)) values.remove("woclr"); 
		// reconcile rset/rclr assigns
		else if (name.equals("rclr") && isTrueString(value)) values.remove("rset"); 
		else if (name.equals("rset") && isTrueString(value)) values.remove("rclr");
		// reconcile intrType
		else if (name.equals("posedge") || name.equals("negedge") || name.equals("bothedge") || name.equals("level")) { 
			if (isTrueString(value)) {
				prop.setValue(name); setProperty("intrType", prop);
			}
			else if (isFalseString(value) && name.equals(getProperty("intrType"))) {
				prop.setValue("level"); setProperty("intrType", prop);  // otherwise back to default
			}
			return; 
		} 
		// reconcile intrStickyType
		else if (name.equals("nonsticky") || name.equals("sticky") || name.equals("stickybit")) { 
			if (isTrueString(value)) {
				prop.setValue(name); setProperty("intrStickyType", prop);
			}
			else if (isFalseString(value) && name.equals(getProperty("intrStickyType"))) {
				prop.setValue("stickybit"); setProperty("intrStickyType", prop);  // back to default
			}
			return; 
		} 
		
		prop.setValue(RdlModelExtractor.noEscapes(value)); 
		setProperty(name, prop); 
	}
	
	private static boolean isTrueString(String value) {
		return (value==null)? false : "true".equals(value.toLowerCase());
	}
	
	private static boolean isFalseString(String value) {
		return (value==null)? false : "false".equals(value.toLowerCase());
	}

	/** set a prop value and reconcile values assuming 0 depth (non-dynamic assign)
	 *  @param name of the value to set
	 *  @param value - string
	 */
	public void setProperty(String name, String value) {
		setProperty(name, value, 0);
	}
	
	/** copy a property stored as name1 to name2
	 *  @param name1 - key of the value to be copied
	 *  @param name2 - key of copy result
	 */
	public void copyProperty(String name1, String name2) {
		PropertyValue prop = values.get(name1);
		if (prop == null) return;
		setProperty(name2, prop.getValue(), prop.getDepth());
	}
	
	/** set a prop value directly (no property reconciliation is done 
	 *  @param name of the value to set
	 *  @param value - PropertyValue
	 */
	private void setProperty(String name, PropertyValue value) {
		values.put(name, value);
	}
	
	/** remove a property  
	 *  @param name of the property to be removed
	 */
	public void removeProperty(String name) {
		values.remove(name);
	}
	
	/** update parameters using values in supplied hashmap
	 *  @param hashmap of properties to be set
	 *  @param keepOldValues - true if values with keys already in hash will not be updated
	 */
	public void updateProperties(HashMap<String, PropertyValue> updates, boolean keepOldValues) {    
		for (String key: updates.keySet()) {
			PropertyValue prop = updates.get(key);
			if (!(keepOldValues && hasProperty(key)))
			   setProperty(key, prop.getValue(), prop.getDepth());   // update parameter  
		}	
	}
	
	/** update parameters using values in supplied hashmap
	 *  @param hashmap of properties to be set
	 */
	public void updateProperties(HashMap<String, PropertyValue> updates) {    
		updateProperties(updates, false);
	}
	
	/** update parameters using values in supplied ParameterList
	 *  @param list of properties to be set
	 *  @param keepOldValues - true if values with keys already in hash will not be updated
	 */
	public void updateProperties(PropertyList updateList, boolean keepOldValues) {
		if (updateList != null) updateProperties(updateList.getProperties(), keepOldValues);
	}

	/** update parameters using values in supplied ParameterList
	 *  @param list of properties to be set
	 */
	public void updateProperties(PropertyList updateList) {
		if (updateList != null) updateProperties(updateList.getProperties());
	}

	public String toString() {
		return values.toString();
	}

	public boolean isEmpty() {
		return values.isEmpty();
	}
		
	/** return a subset PropertyList given a set of names
	 *  @param name of the property value to get
	 *  @return the a new list containing properties with specified name
	 */
	public PropertyList getSubsetList(Set<String> names) {
		PropertyList newList = new PropertyList();
		for (String name: names) {
			if (values.containsKey(name)) newList.setProperty(name, getProperty(name));
		}
		return newList;
	}
	
	// ----------------- inner classes --------------------
	/**
	 *  class for storage of property values
	 */
	public class PropertyValue {
		String value;
		
		public PropertyValue() {
		}
		
		public PropertyValue(String value) {
			this.value = value;
		}
		
		public String getValue() {
			return value;
		}
		
		public void setValue(String value) {
			this.value = value;
		}
		
		public int getDepth() {
			return 0;
		}
		
		public String toString() {
			return getValue() + "(" + getDepth() + ")";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			//result = prime * result + getOuterType().hashCode();
			result = prime * result + ((value == null) ? 0 : value.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			PropertyValue other = (PropertyValue) obj;
			//if (!getOuterType().equals(other.getOuterType()))
			//	return false;
			if (value == null) {
				if (other.value != null)
					return false;
			} else if (!value.equals(other.value))
				return false;
			return true;
		}
	}
	
	/**
	 *  class for storage of dynamically assigned property values including depth param
	 */
	public class DynamicPropertyValue extends PropertyValue {
		int depth = 0;
		
		public DynamicPropertyValue(int depth) {
			super();
			this.depth = depth;
		}
		
		public DynamicPropertyValue(String value, int depth) {
			super(value);
			this.depth = depth;
		}
		
		public int getDepth() {
			return depth;
		}
		
		public String toString() {
			return getValue() + "(" + getDepth() + ")";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			//result = prime * result + getOuterType().hashCode();
			result = prime * result + depth;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!super.equals(obj))
				return false;
			if (getClass() != obj.getClass())
				return false;
			DynamicPropertyValue other = (DynamicPropertyValue) obj;
			//if (!getOuterType().equals(other.getOuterType()))
			//	return false;
			if (depth != other.depth)
				return false;
			return true;
		}

		@SuppressWarnings("unused")
		private PropertyList getOuterType() {
			return PropertyList.this;
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((values == null) ? 0 : values.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PropertyList other = (PropertyList) obj;
		if (values == null) {
			if (other.values != null)
				return false;
		} else if (!values.equals(other.values))
			return false;
		return true;
	}

}

