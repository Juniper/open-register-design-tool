/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved. 
 */
package ordt.extract.model;

import ordt.extract.Ordt;
import ordt.extract.RegNumber;

/** class containing info for an enum value within a RegENum */
public class ModEnumElement {
	String id;    // id of this enumElement
	RegNumber value = null;    // value of this enumElement
	String name;     
	String desc;

	/** get id
	 *  @return the id
	 */
	public String getId() {
		return id;
	}
	/** set id
	 *  @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}
	/** get value
	 *  @return the value
	 */
	public RegNumber getValue() {
		return value;
	}
	/** set value
	 *  @param value the value to set
	 */
	public void setValue(String valueString) {
		this.value = new RegNumber(valueString);
		//System.out.println("enum value specified : " + valueString + ", converted to " + this.value);
		if (this.value == null) Ordt.errorMessage("invalid enum value specified : " + valueString);
	}
	
	/** get name
	 *  @return the name
	 */
	public String getName() {
		return name;
	}
	/** set name
	 *  @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	/** get desc
	 *  @return the desc
	 */
	public String getDesc() {
		return desc;
	}
	/** set desc
	 *  @param desc the desc to set
	 */
	public void setDesc(String desc) {
		this.desc = desc;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((desc == null) ? 0 : desc.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		ModEnumElement other = (ModEnumElement) obj;
		if (desc == null) {
			if (other.desc != null)
				return false;
		} else if (!desc.equals(other.desc))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}
}
