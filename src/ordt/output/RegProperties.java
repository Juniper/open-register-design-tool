/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved.
 */
package ordt.output;

import ordt.extract.Ordt;
import ordt.extract.DefinedProperties;
import ordt.extract.ModComponent;
import ordt.extract.ModInstance;
import ordt.extract.ModRegister;
import ordt.extract.PropertyList;
import ordt.extract.RegNumber;
import ordt.parameters.ExtParameters;
import ordt.parameters.Utils;

/** extracted properties of a register instance created during model walk */
public class RegProperties extends AddressableInstanceProperties {
			
	private Integer regWidth = ExtParameters.getMinDataSize();   // default to 32b register
	private JspecCategory category = new JspecCategory("");  // category property
	private Integer filledBits = 0;  // initially no fields instanced
	private Integer fieldCount = 0;  // initially no fields instanced
	private int [] availableBits;  // track available bit locations in reg as fields added
	private int fieldSetOffset = 0;  // current accumulated field offset used to compute new index
	private boolean fieldOffsetsFromZero;  // field offset direction for this reg
	
	private String aliasedId = "";   // if this reg instance is an alias, store base instance
	
	private boolean hasInterruptFields = false;   // reg contains interrupt fields
	
	// output-specific status info
	private boolean hasInterruptOutputDefined = false;   // reg has interrupt output
	private boolean hasHaltOutputDefined = false;   // reg has halt output
	
	// register properties - will be true if any field has corresponding access
	private boolean isSwReadable = false; 
	private boolean isSwWriteable = false;
	
	// uvmregs test info
	private boolean isMem = false;
	private boolean uvmRegPrune = false;
	
	// cppmod info
	private boolean cppModPrune = false;
	
	// jspec attributes
	private String jspecAttributes;
		
	/** init properties using defaults -> component -> instance values */
	public RegProperties(ModInstance regInst, boolean fieldOffsetsFromZero) {
		super(regInst);  // init instance, id, external
		this.fieldOffsetsFromZero = fieldOffsetsFromZero;
	}

	@Override
	public boolean isRegister() {
		return true;
	}
	
	// TODO - add clone constructor and display for regprops
	/** extract properties from the calling instance */
    @Override
	public void extractProperties(PropertyList pList) {
		super.extractProperties(pList);  // extract common parameters
		
	    // go directly to extractInstance to get reg width, external, and alias parms
		
		// set reg width and initialize fields for field addition
		setRegWidth();      // set the width of this reg based on properties
		// initially all bits available for field assignment
		//System.out.println("RegProperties extractProperties: regwidth=" + getRegWidth());
		availableBits = new int [getRegWidth() + 1];   // initializes all to 0
		availableBits[0] = getRegWidth();

		/*if (getInstancePath().contains("stats.spin")) { 
		    //display();
	        System.out.println("RegProperties extractInstance: inst=" + getInstancePath() + ", parmlist=" + pList);
   	    }*/
									
		if (extractInstance.hasProperty("aliasedId")) {
			//System.out.println("RegProperties: creating properties for aliased register, id=" + regInst.getId() + ", aliasedId=" + regInst.getProperty("aliasedId") );
			setAliasedId(extractInstance.getProperty("aliasedId"));
		}
		// set external decode 
		if (pList.hasTrueProperty("external_decode")) setExternalDecode(true);
		// otherwise check register width
		else checkRegWidth();
		
		// now extract from the combined instance properties
		// extract boolean test properties
		if (pList.hasTrueProperty("donttest")) setDontTest(true);
		if (pList.hasTrueProperty("dontcompare")) setDontCompare(true);
		
		// extract jspec attributes to be passed thru
		if (pList.hasProperty("js_attributes")) setJspecAttributes(pList.getProperty("js_attributes"));
	
		// category property
		if (pList.hasProperty("category")) {
			String catStr = pList.getProperty("category");
			setCategory(catStr); 
		}
		
		// extract uvmregs test info
		if (pList.hasTrueProperty("uvmreg_is_mem")) setMem(true);
		if (pList.hasTrueProperty("uvmreg_prune")) setUvmRegPrune(true);
		
		// extract c++ model info
		if (pList.hasTrueProperty("cppmod_prune")) setCppModPrune(true);
	}
    
	/** extract a PropertyList of user defined parameters for this instance */
    @Override
	protected void extractUserDefinedProperties(PropertyList pList) {
		setUserDefinedProperties(pList, DefinedProperties.userRegPropertySet);
	}

	/** set the width of register based on instance/component properties
	 */
	private void setRegWidth() {
		ModComponent regComp = extractInstance.getRegComp();  // get the component of this instance
		// if instance prop is set then use it
		if (extractInstance.hasProperty("regwidth")) 
			regWidth = extractInstance.getIntegerProperty("regwidth"); 
		// otherwise look for a regwidth set in component  // TODO - is this needed or will always be picked up in instance props?
		else if ((regComp != null) && (regComp.hasProperty("regwidth"))) {
			regWidth = regComp.getIntegerProperty("regwidth");
		}
		// else use default min reg size
		if (regWidth==null) regWidth = ExtParameters.getMinDataSize(); 

		//System.out.println("setting width to " + regWidth);
	}
	
	private void checkRegWidth() {
		// check for valid regwidth
		//if (!Utils.isPowerOf2(regWidth))   
		//	Jrdl.warnMessage("Non power of 2 register width (" + regWidth + ") specified in " + extractInstance.getFullId() + ".  Address alignment must be on next highest power of 2.");
		if (!Utils.isInRange(regWidth, 32, 1024))   
			Ordt.errorMessage("Invalid register width (" + regWidth + ") specified in " + extractInstance.getFullId() + ".  Size should be between 32 and 1024.");
				
	}
	
	/** set the width of register directly
	 */
	public void setRegWidth(Integer width) {
		regWidth = width;
		// check for valid regwidth
		if ((regWidth % ExtParameters.getMinDataSize()) != 0) {
			Ordt.errorMessage("invalid register width (" + regWidth + ") specified");
		}
	}

	@Override
	public int getMaxRegWidth() {
		return getRegWidth();
	}
	
	/** get regWidth
	 */
	public Integer getRegWidth() {   // TODO - all the following could change to getMaxRegWidth
		return regWidth;
	}
	
	/** get regWidth in bytes
	 */
	public Integer getRegByteWidth() {
		return getRegWidth()/8;
	}
	
	/** get regWidth in words
	 */
	public Integer getRegWordWidth() {
		return getRegWidth()/ExtParameters.getMinDataSize();
	}
	
	/** get bits needed to address register words  // TODO - dup with getRegWordBits?
	 */
	public Integer getRegWordHighBit() {
		return (new RegNumber(getRegWordWidth())).getMinusOneHighestBit();
	}
	
	/** get the address stride for this reg (specified increment or width) */
	public RegNumber getAddrStride() {
		RegNumber incr = this.getExtractInstance().getAddressIncrement();
		if (incr == null) {
			// bump repeat stride to next power of 2   
			int incBytes = !Utils.isPowerOf2(this.getRegWidth()) ? (Utils.getNextHighestPowerOf2(this.getRegWidth())/8) : this.getRegByteWidth(); 
			incr = new RegNumber(incBytes);   
		}
		return incr;
	}

	/** returns true if at least one category is set
	 * (should only be called in finishReg since field info will affect)
	 */
	public boolean hasCategory() {
		return category.hasValue();
	}

	/** returns true if specified category string is set 
	 * (should only be called in finishReg since field info will affect) */
	public boolean hasCategory(String string) {
		return category.hasValue(string);
	}

	/** get category (should only be called in finishReg since field info will affect)
	 *  @return the category
	 */
	public JspecCategory getCategory() {
		return category;
	}

	/** set category
	 *  @param category the category to set
	 */
	public void setCategory(String category) {
		this.category = new JspecCategory(category);
	}
	
	/** isAlias
	 *  @return true if this reg instance is an alias
	 */
	public boolean isAlias() {
		return ((getAliasedId() != null) && (getAliasedId().length() > 0)) ;
	}

	/** get aliasedId
	 *  @return the aliasedId
	 */
	public String getAliasedId() {
		return aliasedId;
	}

	/** set aliasedId
	 *  @param aliasedId the aliasedId to set
	 */
	public void setAliasedId(String aliasedId) {
		this.aliasedId = aliasedId;
	}

	/** get alais baseName
	 *  @return alais the baseName
	 */
	public String getAliasBaseName() {
		return getAliasInstancePath().replace('.', '_');
	}

	/** get instancePath of the Aliased register
	 *  @return the aliasInstancePath
	 */
	public String getAliasInstancePath() {
		// if alias string is absolute use it 
		if (getAliasedId().contains(".")) return getAliasedId();
		// else a relative path so swap id
		String retStr = getInstancePath();
		retStr = retStr.replaceFirst(getId() + "$", getAliasedId());
		return retStr;
	}

	/** get hasInterruptFields - return true if an intr field is contained in this reg
	 */
	public boolean hasInterruptFields() {
		return hasInterruptFields;
	}

	/** set hasInterruptFields 
	 */
	public void setHasInterruptFields(boolean hasInterruptFields) {
		this.hasInterruptFields = hasInterruptFields;
	}

	/** get hasInterruptOutputDefined - output-specific status info
	 */
	public boolean hasInterruptOutputDefined() {
		return hasInterruptOutputDefined;
	}

	/** set hasInterruptOutputDefined - output-specific status info
	 */
	public void setHasInterruptOutputDefined(boolean hasInterruptOutputDefined) {
		this.hasInterruptOutputDefined = hasInterruptOutputDefined;
	}

	/** get hasHaltOutputDefined - output-specific status info
	 *  @return the hasHaltOutputDefined
	 */
	public boolean hasHaltOutputDefined() {
		return hasHaltOutputDefined;
	}

	/** set hasHaltOutputDefined - output-specific status info
	 *  @param hasHalt the hasHalt to set
	 */
	public void setHasHaltOutputDefined(boolean hasHaltOutputDefined) {
		this.hasHaltOutputDefined = hasHaltOutputDefined;
	}

	/** get isSwReadable (valid after fields processed)
	 *  @return the isSwReadable
	 */
	public boolean isSwReadable() {
		return isSwReadable;
	}

	/** set isSwReadable 
	 *  @param isSwReadable the isSwReadable to set
	 */
	public void setSwReadable(boolean isSwReadable) {
		this.isSwReadable = isSwReadable;
	}

	/** get isSwWriteable (valid after fields processed)
	 *  @return the isSwWriteable
	 */
	public boolean isSwWriteable() {
		return isSwWriteable;
	}

	/** set isSwWriteable
	 *  @param isSwWriteable the isSwWriteable to set
	 */
	public void setSwWriteable(boolean isSwWriteable) {
		this.isSwWriteable = isSwWriteable;
	}

	/** get uvmreg isMem */
	public boolean isMem() {
		return isMem;
	}
	
	/** set uvmreg isMem */
	public void setMem(boolean isMem) {
		this.isMem = isMem;
	}
	
	/** return true if this register is to be pruned from uvmreg model output */
	public boolean uvmRegPrune() {
		return uvmRegPrune;
	}
	
	public void setUvmRegPrune(boolean uvmRegPrune) {
		this.uvmRegPrune = uvmRegPrune;
	}

	/** return true if this register is to be pruned from c++ model output */
	public boolean cppModPrune() {
		return cppModPrune;
	}

	public void setCppModPrune(boolean cppModPrune) {
		this.cppModPrune = cppModPrune;
	}


	/** return true if register contains fields with dont_test set
	 *  @return true if has dont_test fields
	 *
	public boolean hasDontTestFields() {
		RegNumber dontTestMask = getDontTestMask();
		return (dontTestMask.isNonZero());
	}*/

	/** get jspecAttributes
	 *  @return the jspecAttributes
	 */
	public String getJspecAttributes() {
		return jspecAttributes;
	}

	/** set jspecAttributes
	 *  @param jspecAttributes the jspecAttributes to set
	 */
	public void setJspecAttributes(String jspecAttributes) {
		this.jspecAttributes = jspecAttributes;
	}

	/** get filledBits
	 *  @return the filledBits
	 */
	public Integer getFilledBits() {
		return filledBits;
	}

	/** get fieldCount
	 *  @return the fieldCount
	 */
	public Integer getFieldCount() {
		return fieldCount;
	}

	/** adjust offset for a new fieldset hierarchy 
	 * @param newOffset - total fieldset offset from all fsets in hierarchy  
	 * */
	public void setFieldSetOffset(Integer newOffset) {
		// adjust current offset
		fieldSetOffset = newOffset;
		//System.out.println("RegProperties setFieldSetOffset: offset=" + newOffset);
	}

	/** add a new field to this register based on next available bits
	 *  @return the index to be used for field or null if no valid index */
	public Integer addField(FieldProperties fieldProperties) {
		// if field is an interrupt then mark this register
		if (fieldProperties.isInterrupt()) setHasInterruptFields(true);
		
		// compute field indices within register
		Integer width = fieldProperties.getFieldWidth(); 
		Integer offset = fieldProperties.getExtractInstance().getOffset();  // get the relative offset of this field
		Integer bitPaddingOffset = ((ModRegister) getExtractInstance().getRegComp()).getPadBits();
		//System.out.println("RegProperties addField: id=" + fieldProperties.getInstancePath() + ", offset=" + offset + ", width=" + width + ", fsetOffset=" + fieldSetOffset + ", bitPaddingOffset=" + bitPaddingOffset + ", regwidth=" + getRegWidth());
		
		// compute the actual field index  
		Integer lowFieldIndex = 0;
		if (offset != null) {
			offset += fieldSetOffset;  // explicit offset is supplied so add any fieldset offset
			offset += bitPaddingOffset;  // also add an offset if bit padding is needed  TODO - this assumes padding is same dir as offset (pad from msb if pack from msb)
			lowFieldIndex = addFixedField(offset, width);  // explicit location used
		}
		else lowFieldIndex = addFloatingField(width);
		// now create the output array index string
		if (lowFieldIndex == null) {
			Ordt.errorExit("Unable to fit all fields in register instance " + getId());
		}
		fieldCount++;  // bump the field count
		return lowFieldIndex;   
	}

	/** verify that field at specified location can fit in register
	 * @return specified index of field or null if no suitable index found */
	private Integer addFixedField(Integer offset, Integer width) {
		// convert specified offset to an index value depending on direction
		Integer lowIndex = fieldOffsetsFromZero ? offset : getRegWidth() - offset - width; 
		//System.out.println("RegProperties addFixedField id=" + getId() + ", rwidth=" + getRegWidth() + ", foffset=" + offset + ", fwidth=" + width + ", computed lowIndex=" + lowIndex);
		// find next index that could fit this field
		for (int idx=lowIndex; idx>=0; idx--) {
			// if field fits update available and return lowIndex
			if (availableBits[idx] >= width + (lowIndex - idx)) {
				filledBits += width;  // bump the filled bits count
				// adjust available space above field
				if (availableBits[idx] >= width + (lowIndex - idx))
				   availableBits[lowIndex+width] = availableBits[idx] - width - (lowIndex - idx);
				availableBits[idx] = lowIndex - idx;  // adjust avail space below field
				return lowIndex;
			}
		}
		return null;
	}

	/** find first available register index that can fit this width
	 * @return index of field or null if no suitable index found */
	private Integer addFloatingField(Integer width) {
		for (int idx=0; idx<getRegWidth(); idx++) {
			// if space found then update the avail info and return the low index
			if (availableBits[idx]>=width) {  
				filledBits += width;  // bump the filled bits count
				availableBits[idx+width] = availableBits[idx] - width;  // change available space
				availableBits[idx] = 0;  // no longer space avialable here
				return idx;
			}
		}
		return null;  // search failed so return null
	}

	/** true if register has unused bits
	 */
	public boolean hasNopBits() {
		return getRegWidth() != filledBits;
	}

}
