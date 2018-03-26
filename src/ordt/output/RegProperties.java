/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved.
 */
package ordt.output;

import ordt.extract.Ordt;

import ordt.extract.DefinedProperties;
import ordt.extract.PropertyList;
import ordt.extract.RegNumber;
import ordt.extract.model.ModComponent;
import ordt.extract.model.ModInstance;
import ordt.extract.model.ModRegister;
import ordt.parameters.ExtParameters;
import ordt.parameters.Utils;

/** extracted properties of a register instance created during model walk */
public class RegProperties extends AddressableInstanceProperties {
			
	private Integer regWidth = ModRegister.defaultWidth;   // default to 32b register
	private JspecCategory category = new JspecCategory("");  // category property
	private Integer filledBits = 0;  // initially no fields instanced
	private Integer fieldCount = 0;  // initially no fields instanced
	
	private int [] availableBits;  // track available bit locations in reg as fields added
	private int highAvailableIdx = 0;  // high available index with an assigned location blow
	private Integer fieldSetOffset = 0;  // current accumulated field offset used to compute new index
	private Integer minValidOffset = 0;  // min valid offset for field packing
	private boolean fieldOffsetsFromZero;  // field offset direction for this reg
	private int fieldHash = 0; // hash of this regs fields
	
	private String aliasedId = "";   // if this reg instance is an alias, store base instance
	
	private boolean hasInterruptFields = false;   // reg contains interrupt fields
	
	// output-specific status info
	private boolean hasInterruptOutputDefined = false;   // reg has interrupt output
	private boolean hasHaltOutputDefined = false;   // reg has halt output
	
	// uvmregs test info
	private Boolean isUvmMem = null;  // store explicitly set uvm_mem state
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
		//Ordt.infoMessage("RegProperties: id=" + getId() + ", pList=" + pList);

	    // go directly to extractInstance to get reg width, external, and alias parms
		
		// set reg width and initialize fields for field addition
		setRegWidth();      // set the width of this reg based on properties
		// initially all bits available for field assignment
		//System.out.println("RegProperties extractProperties: regwidth=" + getRegWidth());
		availableBits = new int [getRegWidth() + 1];   // initializes all to 0
		availableBits[0] = getRegWidth();
		highAvailableIdx = 0;  // init bit 0 as high available index
		
		/*if (getInstancePath().contains("stats.spin")) { 
		    //display();
	        System.out.println("RegProperties extractInstance: inst=" + getInstancePath() + ", parmlist=" + pList);
   	    }*/
									
		if (extractInstance.hasProperty("aliasedId")) {
			//System.out.println("RegProperties: creating properties for aliased register, id=" + regInst.getId() + ", aliasedId=" + regInst.getProperty("aliasedId") );
			setAliasedId(extractInstance.getProperty("aliasedId"));
		}
		// check register width
		if (!isExternalDecode()) checkRegWidth();  // ext decode is set in super
		
		// now extract from the combined instance properties
		// extract boolean test properties
		if (pList.hasTrueProperty("donttest")) setDontTest(true);
		else if (pList.hasFalseProperty("donttest")) setDontTest(false);
		
		if (pList.hasTrueProperty("dontcompare")) setDontCompare(true);
		else if (pList.hasFalseProperty("dontcompare")) setDontCompare(false);
		
		// extract jspec attributes to be passed thru
		if (pList.hasProperty("js_attributes")) setJspecAttributes(pList.getProperty("js_attributes"));
	
		// category property
		if (pList.hasProperty("category")) {
			String catStr = pList.getProperty("category");
			setCategory(catStr); 
		}
		
		// extract uvmregs test info
		if (pList.hasTrueProperty("uvmreg_is_mem")) setUvmMem(true);
		else if (pList.hasFalseProperty("uvmreg_is_mem")) setUvmMem(false);
		
		if (pList.hasTrueProperty("uvmreg_prune")) setUvmRegPrune(true);
		else if (pList.hasFalseProperty("uvmreg_prune")) setUvmRegPrune(false);
		
		// extract c++ model info
		if (pList.hasTrueProperty("cppmod_prune")) setCppModPrune(true);
		else if (pList.hasFalseProperty("cppmod_prune")) setCppModPrune(false);
	}
    
	/** extract a PropertyList of user defined parameters for this instance */
    @Override
	protected void extractSpecialPropertyLists(PropertyList pList) {
		setUserDefinedProperties(pList, DefinedProperties.userDefRegPropertyNames);
		setJsPassthruProperties(pList, DefinedProperties.jsPassthruRegPropertyNames);
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
		// else use default reg size
		if (regWidth==null) regWidth = ModRegister.defaultWidth; 
		//System.out.println("setting width to " + regWidth);
	}
	
	private void checkRegWidth() {
		// check for valid regwidth
		//if (!Utils.isPowerOf2(regWidth))   
		//	Jrdl.warnMessage("Non power of 2 register width (" + regWidth + ") specified in " + extractInstance.getFullId() + ".  Address alignment must be on next highest power of 2.");
        int minRegWidth = ExtParameters.getMinDataSize();
        int maxRegWidth = minRegWidth * 16;
		if (!Utils.isInRange(regWidth, minRegWidth, maxRegWidth))   
			Ordt.errorMessage("Invalid register width (" + regWidth + ") specified in " + extractInstance.getFullId() + ".  Size should be between " + minRegWidth + " and " + maxRegWidth + ".");	
	}
	
	/** set the width of register directly
	 */
	public void setRegWidth(Integer width) {
		regWidth = width;
		// check for valid regwidth
        int minRegWidth = ExtParameters.getMinDataSize();
		if ((regWidth % minRegWidth) != 0) {
			Ordt.errorMessage("Invalid register width (" + regWidth + ") specified in " + extractInstance.getFullId() + ".  Size must be a multiple of min_data_size (" + minRegWidth + ").");
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

	/** return true is this register array should be modeled as uvm_mem */
	public boolean isUvmMem() {
		if (!isReplicated()) return false;  // only replicated regs can be modeled as uvm_mem
		if (isUvmMem != null) return isUvmMem;  // use explicit setting
		return (getRepCount() >= ExtParameters.uvmregsIsMemThreshold());  // otherwise model as a mem if over replication threshold
	}
	
	/** identify this reg as a uvm memory */
	public void setUvmMem(boolean isUvmMem) {
		this.isUvmMem = isUvmMem;
	}
	
	/** return true if this register is to be pruned from uvmreg model output */
	public boolean uvmRegPrune() {
		return uvmRegPrune;
	}
	
	public void setUvmRegPrune(boolean uvmRegPrune) {
		this.uvmRegPrune = uvmRegPrune;
        //System.out.println("RegProperties setUvmRegPrune=" + uvmRegPrune);
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

	/** set current offset info to be used for field packing within a fieldset hierarchy
	 * 
	 * @param fieldSetProperties - new fieldset to be added
	 * @param parentFsOffset - offset of this fieldsets parent in the register 
	 * @return relative offset of this fieldset in its parent
	 */
	public int addFieldSet(FieldSetProperties fieldSetProperties, int parentFsOffset) {
		Integer fsOffset = fieldSetProperties.getExtractInstance().getOffset();  // get specified relative offset, could be fixed or floating
		//System.out.println("RegProperties addFieldSet: incoming state (highAvailableIdx=" + highAvailableIdx + ",fieldSetOffset=" + fieldSetOffset + ", minValidOffset=" + minValidOffset + ")");
		// adjust current fieldset offset
		fieldSetOffset = (fsOffset==null)? highAvailableIdx : parentFsOffset + fsOffset;  // TODO null offset is correct only if packing from low to high since highAvailableIdx is absolute
		if (fieldSetOffset<parentFsOffset) Ordt.errorExit("Unable to fit fieldset " + fieldSetProperties.getId() + " in register " + getInstancePath());
        // set the min allowed register offset including any padding
		Integer bitPaddingOffset = ((ModRegister) getExtractInstance().getRegComp()).getPadBits();
		int newMinValidOffset = bitPaddingOffset + fieldSetOffset;
		if (newMinValidOffset>minValidOffset) minValidOffset = newMinValidOffset; // only update if offset increases
		//System.out.println("RegProperties addFieldSet: outgoing state (highAvailableIdx=" + highAvailableIdx + ",fieldSetOffset=" + fieldSetOffset + ", minValidOffset=" + minValidOffset + ")");
        return fieldSetOffset - parentFsOffset;
	}

	/** restore fieldset offset info after exiting a fieldset in the hierarchy 
	 * @param newOffset - total fieldset offset for all fsets in current hierarchy (or null if next available bit position should be used)
	 * @param minOffset - min valid fieldset offset for field placement (total of fieldset offsets + width w/o padding)
	 * */
	public void restoreFieldSetOffsets(Integer newOffset, Integer minOffset) {
		//System.out.println("RegProperties restoreFieldSetOffsets: inputs (newOffset=" + newOffset + ", minOffset=" + minOffset + ")");
		//System.out.println("RegProperties restoreFieldSetOffsets: incoming state (highAvailableIdx=" + highAvailableIdx + ",fieldSetOffset=" + fieldSetOffset + ", minValidOffset=" + minValidOffset + ")");
		// adjust current offset
		fieldSetOffset = (newOffset==null)? highAvailableIdx : newOffset;  // TODO null offset is correct only if packing from low to high since highAvailableIdx is absolute
        // set the min allowed register offset including any padding
		Integer bitPaddingOffset = ((ModRegister) getExtractInstance().getRegComp()).getPadBits();
		if (minOffset!=null) minValidOffset = bitPaddingOffset + minOffset; 
		// adjust highAvailableIdx if below minValidOffset
		if (highAvailableIdx < minValidOffset) highAvailableIdx = minValidOffset;
		//if (getId().equals("blabla")) 
		//System.out.println("RegProperties restoreFieldSetOffsets: outgoing state (highAvailableIdx=" + highAvailableIdx + ",fieldSetOffset=" + fieldSetOffset + ", minValidOffset=" + minValidOffset + ")");
		return;
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
		//if (getId().equals("scfg_data")) System.out.println("RegProperties addField: id=" + fieldProperties.getInstancePath() + ", offset=" + offset + ", width=" + width + ", fsetOffset=" + fieldSetOffset + ", bitPaddingOffset=" + bitPaddingOffset + ", regwidth=" + getRegWidth() + ", highAvailableIdx=" + highAvailableIdx);
		
		// compute the actual field index  
		Integer lowFieldIndex = 0;
		if (offset != null) {
			offset += fieldSetOffset;  // explicit offset is supplied so add any fieldset offset
			offset += bitPaddingOffset;  // also add an offset if bit padding is needed  TODO - this assumes padding is same dir as offset (pad from msb if pack from msb)
			lowFieldIndex = addFixedField(offset, width);  // explicit location used
		}
		else lowFieldIndex = addFloatingField(width);  // FIXME - fieldsetoffset and padding should constrain valid ranges (min valid has these)
		// now create the output array index string
		if (lowFieldIndex == null) {
			Ordt.errorExit("Unable to fit all fields in " + getRegWidth() + "b register instance " + getInstancePath());
		}
		//if (getId().equals("scfg_data")) System.out.println("RegProperties addField: id=" + fieldProperties.getInstancePath() + ", adding at reg lowFieldIndex=" + lowFieldIndex);
		fieldCount++;  // bump the field count
		return lowFieldIndex;   
	}

	/** verify that field at specified location can fit in register
	 * @param offset - full offset sum of this field including padding, fieldset offset, and relative field offset
	 * @param width - field width in bits
	 * @return specified index of field or null if no suitable index found */
	private Integer addFixedField(Integer offset, Integer width) {
		// convert specified offset to an index value depending on direction
		Integer lowIndex = fieldOffsetsFromZero ? offset : getRegWidth() - offset - width; 
		if (lowIndex+width > highAvailableIdx) highAvailableIdx=lowIndex+width;  // save new high bit
		//if (getId().equals("scfg_data")) System.out.println("RegProperties addFixedField: id=" + getId() + ", rwidth=" + getRegWidth() + ", offset=" + offset + ", width=" + width + ", computed lowIndex=" + lowIndex);
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
		//if (getId().equals("scfg_data")) System.out.println("RegProperties addFixedField: id=" + getId() + " returns null");
		return null;
	}

	/** find first available register index that can fit this width  // TODO - floating field only works w/ pack from 0
	 * @return index of field or null if no suitable index found */
	private Integer addFloatingField(Integer width) {
		//if (getId().equals("scfg_data")) System.out.println("RegProperties addFloatingField: id=" + getId() + ", rwidth=" + getRegWidth() + ", fwidth=" + width);
		for (int idx=0; idx<getRegWidth(); idx++) {
			// if space found above minIdx then update the avail info and return the low index
			if ((idx>=minValidOffset) && (availableBits[idx]>=width)) {  
				filledBits += width;  // bump the filled bits count
				availableBits[idx+width] = availableBits[idx] - width;  // change available space after new field
				availableBits[idx] = 0;  // no longer space available here
				if (idx+width > highAvailableIdx) highAvailableIdx=idx+width;  // save new high bit
				return idx;
			}
			// otherwise if space found at minIdx then update the avail info and return the low index
			else if ((idx<minValidOffset) && (availableBits[idx]>=(minValidOffset-idx)+width)) {  
				filledBits += width;  // bump the filled bits count
				availableBits[minValidOffset+width] = availableBits[idx] - ((minValidOffset-idx)+width);  // change available space after new field
				availableBits[minValidOffset] = 0;  // no longer space available here
				availableBits[idx] = minValidOffset-idx;  // change available space before new field
				if (minValidOffset+width > highAvailableIdx) highAvailableIdx=minValidOffset+width;  // save new high bit
				return minValidOffset;
			}
		}
		//if (getId().equals("scfg_data")) System.out.println("RegProperties addFloatingField: id=" + getId() + " returns null");
		return null;  // search failed so return null
	}

	/** true if register has unused bits
	 */
	public boolean hasNopBits() {
		return getRegWidth() != filledBits;
	}

    /** get the field hash value for this reg */
	public int getFieldHash() {
		return this.fieldHash;
	}

    /** set the field hash value for this reg */
	public void setFieldHash(int fieldHash) {
		this.fieldHash = fieldHash;
	}

	/** hashcode/equals overrides 
	 * - ignores cppModPrune, uvmRegPrune, filledBits, availableBits, fieldSetOffset in compare
	 * - adds extractInstance.getAddressIncrement(), fieldHash to compare
	 * - field hash is updated in outputbuilder
	 * - optionally includes id in super 
	 */
	@Override
	public int hashCode(boolean includeId) {
		final int prime = 31;
		int result = super.hashCode(includeId);
		result = prime * result + ((aliasedId == null) ? 0 : aliasedId.hashCode());
		result = prime * result + ((category == null) ? 0 : category.hashCode());
		result = prime * result + ((fieldCount == null) ? 0 : fieldCount.hashCode());
		result = prime * result + (fieldOffsetsFromZero ? 1231 : 1237);
		result = prime * result + (hasHaltOutputDefined ? 1231 : 1237);
		result = prime * result + (hasInterruptFields ? 1231 : 1237);
		result = prime * result + (hasInterruptOutputDefined ? 1231 : 1237);
		result = prime * result + (isUvmMem() ? 1231 : 1237);  // FIXME - add equals/hash modes
		result = prime * result + ((jspecAttributes == null) ? 0 : jspecAttributes.hashCode());
		result = prime * result + ((regWidth == null) ? 0 : regWidth.hashCode());
		result = prime * result + fieldHash;
		result = prime * result + ((getExtractInstance().getAddressIncrement() == null) ? 0 : getExtractInstance().getAddressIncrement().hashCode());
		//System.out.println("reg hashCode for id=" + getId() + " is " + result);
		return result;
	}
	
	/** hashcode/equals overrides 
	 * - field hash is updated in outputbuilder
	 * - by default, id is not included in parent reg hash (used for uvmregs output class reuse) 
	 */
	@Override
	public int hashCode() {
		//System.out.println("reg hashCode for id=" + getId() + " is " + hashCode(false));
		return hashCode(false);
	}

	@Override
	public boolean equals(Object obj, boolean includeId) {
		if (this == obj)
			return true;
		if (!super.equals(obj, includeId)) {
			//System.out.println("RegProperties equals: equals fail in super for id=" + getId() + " vs " + ((RegProperties) obj).getId());
			return false;
		}
		if (getClass() != obj.getClass())
			return false;
		RegProperties other = (RegProperties) obj;
		if (aliasedId == null) {
			if (other.aliasedId != null)
				return false;
		} else if (!aliasedId.equals(other.aliasedId))
			return false;
		if (category == null) {
			if (other.category != null)
				return false;
		} else if (!category.equals(other.category))
			return false;
		if (fieldCount == null) {
			if (other.fieldCount != null)
				return false;
		} else if (!fieldCount.equals(other.fieldCount))
			return false;
		if (fieldOffsetsFromZero != other.fieldOffsetsFromZero)
			return false;
		if (hasHaltOutputDefined != other.hasHaltOutputDefined)
			return false;
		if (hasInterruptFields != other.hasInterruptFields)
			return false;
		if (hasInterruptOutputDefined != other.hasInterruptOutputDefined)
			return false;
		if (isUvmMem() != other.isUvmMem())
			return false;
		if (jspecAttributes == null) {
			if (other.jspecAttributes != null)
				return false;
		} else if (!jspecAttributes.equals(other.jspecAttributes))
			return false;
		if (regWidth == null) {
			if (other.regWidth != null)
				return false;
		} else if (!regWidth.equals(other.regWidth))
			return false;
	    if (fieldHash != other.fieldHash){
			//System.out.println("RegProperties equals: equals fail in field hash for id=" + getId() + " vs " + ((RegProperties) obj).getId());
			return false;
		}
		if (getExtractInstance().getRegComp() == null) {  // use model component compare if fieldhash is equal
			if (other.getExtractInstance().getRegComp() != null)
				return false;
		} else if (!getExtractInstance().getRegComp().equals(other.getExtractInstance().getRegComp())) {
			System.out.println("RegProperties equals fail in extractInstance.getRegComp() for this=" + getInstancePath() + ", other=" + other.getInstancePath());
			return false;
		}
		if (getExtractInstance().getAddressIncrement() == null) {
			if (other.getExtractInstance().getAddressIncrement() != null)
				return false;
		} else if (!getExtractInstance().getAddressIncrement().equals(other.getExtractInstance().getAddressIncrement()))
			return false;
		return true;
	}

	@Override
	public boolean equals(Object obj) {
		return equals(obj, false);  // id is not included by default
	}

}
