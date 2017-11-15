/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved. 
 */
package ordt.extract.model;

import ordt.extract.Ordt;
import ordt.parameters.Utils;

/** class of indexed model component instances (fieldset, field, signal) */
public class ModIndexedInstance extends ModInstance {
	// store indeces by width and offset (from high or low boolean set in model extractor)
	protected Integer width = 1;   // bit width of this instance 
	protected Integer offset;   // offset of this instance relative to parent
	
	public ModIndexedInstance() {
		isIndexed = true;
	}

	/** write info to stdout */
	@Override
	public void display (boolean showInstanceComponent) {
			String compID = "";
			if (regComp != null) {
				compID = regComp.getId();
				if ((compID == null) || compID.isEmpty()) compID = "<anonymous>";
			}
			System.out.println("\n--------- Indexed Instance of " + compID + "   id=" + getFullId() ); 
			//System.out.println("         full id=" + getFullId()); 
			System.out.println("    replication count=" + getRepCount() + ",   offset=" + getOffset() + ",   width=" + getWidth());   
			System.out.println("    root instance=" + isRootInstance());   
			// display parms
			System.out.println("    properties:");
			System.out.println("        " + properties);

	        // optionally display component
			if ((regComp != null) && showInstanceComponent) regComp.display();
	}

	// ----------------------- index methods
	
	/** get width
	 *  @return the width
	 */
	public Integer getWidth() {
		return width;
	}

	/** set width
	 *  @param width the width to set
	 */
	public void setWidth(Integer width) {
		if (width<=0) Ordt.errorExit("Invalid width/bit range specified in instance " + getId());
		this.width = width;
	}

	/** get offset
	 *  @return the offset
	 */
	public Integer getOffset() {
		return offset;
	}

	/** set offset
	 *  @param offset the offset to set
	 */
	public void setOffset(Integer offset) {
		this.offset = offset;
	}
	
	/** set a numeric instance variable - overridden in ModInstance child classes
	 * 
	 * @param key - if this key is found, instance value will be updated to val
	 * @param val - value to be used
	 */
	@Override
	protected void updateInstanceVar(String key, String val) {
		if ("fieldwidth".equals(key)) {  // can be passed from comp in jspec
			setWidth(Utils.strToInteger(val, " in instance " + getId()));
			//System.err.println("ModInstance " + this.getFullId() + ": updating key=" + key + " with value=" + val);
		}
	}

	@Override
	// NOTE: currently used for uvm class reuse
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((offset == null) ? 0 : offset.hashCode());
		result = prime * result + ((width == null) ? 0 : width.hashCode());
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
		ModIndexedInstance other = (ModIndexedInstance) obj;
		if (offset == null) {
			if (other.offset != null)
				return false;
		} else if (!offset.equals(other.offset))
			return false;
		if (width == null) {
			if (other.width != null)
				return false;
		} else if (!width.equals(other.width))
			return false;
		return true;
	}
}
