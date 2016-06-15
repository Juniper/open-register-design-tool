/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved.
 */
package ordt.extract;

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
}
