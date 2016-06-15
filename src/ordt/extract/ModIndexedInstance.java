/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved.
 */
package ordt.extract;

import ordt.parameters.Utils;

/** class of indexed model component instances (fieldset, field, signal) */
public class ModIndexedInstance extends ModInstance {
	// store indeces by width and offset (from high or low boolean set in model extractor)
	protected Integer width = 1;   // bit width of this instance 
	protected Integer offset;   // offset of this instance relative to parent

    // numeric params needed for math ops
	protected Integer lowIndex;   // low index of the array  // FIXME - remove, this assumes absolute field indexing, need relative for jspec: indexIsRelative, msbPacking
	protected Integer highIndex;   // high index of the array (this is repcount if no range specified)
	
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
	
	// ----------------------- index methods

	/** set repCount and lowIndex based on lt array index change  // TODO little endian only
	 *  @param  the arrayIdx1 value
	 *
	public void updateLeftArrayIndex(String numStr) {
		Integer idx1;
		//Integer idx2 = getLowIndex();
		try {
		  if (numStr != null) {
			  idx1 = Integer.valueOf(numStr);
			  setHighIndex(idx1);		  }
		} catch (NumberFormatException e) {
		   System.err.println("error parsing integer array value of <" + numStr + ">");  //FIXME
		}
	}*/
	
	/** set repCount and lowIndex based on rt array index change
	 *  @param the arrayIdx2 value
	 *
	public void updateRightArrayIndex(String numStr) {  // TODO little endian only
		//Integer idx1 = getHighIndex();
		//System.out.println("--- idx1=" + idx1 + " idx2str=" + numStr);
		Integer idx2;
		try {
		  if (numStr != null) {
			  idx2 = Integer.valueOf(numStr);
			  setLowIndex(idx2);
		  }
		} catch (NumberFormatException e) {
		   System.err.println("error parsing integer array value of <" + numStr + ">"); //FIXME
		}
	}*/

	/** get lowIndex (called in setLowIndex of field/signalPropertie)
	 *  @return the lowIndex
	 *
	public Integer getLowIndex() {
		return lowIndex;
	}*/

	/** set lowIndex
	 *  @param lowIndex the lowIndex to set
	 *
	public void setLowIndex(Integer lowIndex) {
		this.lowIndex = lowIndex;
	}*/

	/** get highIndex
	 *  @return the highIndex
	 *
	public Integer getHighIndex() {
		return highIndex;
	}*/

	/** set highIndex
	 *  @param highIndex the highIndex to set
	 *
	public void setHighIndex(Integer highIndex) {
		this.highIndex = highIndex;
	}*/

	/** get width   
	 * @param if true, return the default value rather than a null
	 *  @return the repCount
	 *
	public Integer getWidth(boolean useDefault) {
		Integer reps = null;   
		if (useDefault) reps = 1;   // default to one replication
		if (getHighIndex() != null) {
			if (getLowIndex() != null) reps = Math.abs(getHighIndex()-getLowIndex()) + 1;   // calculate from explicit range
			else reps = getHighIndex();  // specified replication count
		}
		return reps;
	}*/

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
}
