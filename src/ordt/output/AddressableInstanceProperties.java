/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved. 
 */
package ordt.output;

import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ordt.output.common.MsgUtils;
import ordt.extract.RegNumber;
import ordt.extract.RegNumber.NumBase;
import ordt.extract.RegNumber.NumFormat;
import ordt.extract.model.ModAddressableInstance;
import ordt.extract.model.ModInstance;
import ordt.output.systemverilog.SystemVerilogDefinedOrdtSignals;
import ordt.output.systemverilog.SystemVerilogDefinedOrdtSignals.DefSignalType;
import ordt.parameters.ExtParameters;
import ordt.parameters.Utils;

/** extracted properties of an addressable instance (reg/regset properties) created during model walk */
public abstract class AddressableInstanceProperties extends InstanceProperties {

	protected RegNumber baseAddress;
	protected RegNumber relativeBaseAddress;   // base address of reg relative to parent

	// external register group parameters
	private int extInstAddressWidth = 0;   // width of word address range for this reg/group (only includes instance size, not ancestor bits used in rep_level option)
	protected int extLowBit = 0;  // low bit in external address range
	
    // external/map type
	public enum ExtType { INTERNAL, PARALLEL, EXTERNAL_DECODE, BBV5, SRAM, SERIAL8, RING }
	private ExternalType externalType = new ExternalType(ExtType.INTERNAL);   // external interface type (init to internal)
	private boolean rootExternal = false;   // is instance root instance of an external reg set (in root builder map)
	private boolean addressMap = false;   // is an external address map
	private boolean localExternal = false;   // is instance external in its local address map 
	private boolean localRootExternal = false;   // is instance root external in its local address map 
	
	// register properties - will be true if any field has corresponding access
	private boolean isSwReadable = false; 
	private boolean isSwWriteable = false;
	
	public AddressableInstanceProperties(ModInstance extractInstance) {
		super(extractInstance);
		// set external status at object create (special case since it is used in model generateOutput and builder pushInstance)
		if (extractInstance.hasProperty("external"))  setExternalTypeFromString(extractInstance.getProperty("external"));
	}

	public AddressableInstanceProperties(AddressableInstanceProperties oldInstance) {
		super(oldInstance);
		// set AddressableInstanceProperty info
		setExternalType(oldInstance.getExternalType());  
		setRootExternal(oldInstance.isRootExternal());  
		setAddressMap(oldInstance.isAddressMap());  
		setRelativeBaseAddress(oldInstance.getRelativeBaseAddress());  
		setBaseAddress(oldInstance.getBaseAddress());  
		setExtInstAddressWidth(oldInstance.getExtAddressWidth());  
		setExtLowBit(oldInstance.getExtLowBit());  
	}
	
	/** display info AddressableInstanceProperties info */
	public void display() {
		super.display();
		System.out.println("  AddressableInstanceProperty info:" );  
		System.out.println("   base address=" + this.getBaseAddress());  		
		System.out.println("   relative base address=" + this.getRelativeBaseAddress());  		
		System.out.println("   ext addr width=" + this.getExtAddressWidth());  		
		System.out.println("   ext addr low bit=" + this.getExtLowBit());  		
		System.out.println("   external=" + this.externalType);  
		System.out.println("   root external=" + this.isRootExternal());  
		System.out.println("   is address map=" + this.isAddressMap());  		
	}
	
	/** return the name for specified defined signal type using the current instance path (or alternate if rep_level is specified)
	 *  This is a catenation of prefix, input pathStr, and suffix */
	@Override
	public String getFullSignalName(DefSignalType sigType) {
		return SystemVerilogDefinedOrdtSignals.getFullName(sigType, getExtBaseName(), true);
	}

	/** return the instance path derived name including alternate ancestor names if instance has rep_level is specified */
	public String getExtBaseName() {
		return hasExternalRepLevel()? this.getExternalType().getRepLevelBasename() : getInstancePath().replace('.', '_');
	}

	/** get baseAddress
	 *  @return the baseAddress
	 */
	public RegNumber getBaseAddress() {
		return baseAddress;
	}

	/** set baseAddress
	 *  @param baseAddress the baseAddress to set
	 */
	public void setBaseAddress(RegNumber baseAddress) {
		this.baseAddress = new RegNumber(baseAddress);  // use a copy, not reference
	}

	/** get full base address including base offset
	 */
	public RegNumber getFullBaseAddress() {
		RegNumber fullBase = new RegNumber(ExtParameters.getPrimaryBaseAddress());  
		fullBase.setVectorLen(ExtParameters.getLeafAddressSize());
		fullBase.setNumBase(NumBase.Hex);
		fullBase.setNumFormat(NumFormat.Address);
		fullBase.add(getBaseAddress());
		return fullBase;
	}

	/** get relativeBaseAddress
	 *  @return the relativeBaseAddress
	 */
	public RegNumber getRelativeBaseAddress() {
		return relativeBaseAddress;
	}

	/** get pre-computed aligned size of this instance (single rep - calls component.getAlignedSize) 
	 */
	public RegNumber getAlignedSize() {
		return getExtractInstance().getRegComp().getAlignedSize();
	}

	/** set relativeBaseAddress
	 *  @param relativeBaseAddress the relativeBaseAddress to set
	 */
	public void setRelativeBaseAddress(RegNumber relativeBaseAddress) {
		this.relativeBaseAddress = new RegNumber(relativeBaseAddress);  // use a copy, not reference;
	}
	
	// -------------
	
	/** get extractInstance
	 *  @return the extractInstance
	 */
	@Override
	public ModAddressableInstance getExtractInstance() {
		return (ModAddressableInstance) extractInstance;
	}

	/** return true if this instance is an addrmap
	 */
	public boolean isAddressMap() {
		return addressMap;
	}

	/** mark this instance as an addrmap
	 */
	public void setAddressMap(boolean addressMap) {
		this.addressMap = addressMap;
	}
	
	// ------- external region methods
	
	/** returns address width of word address range for this reg/group (only includes instance size, not ancestor bits used in rep_level option) */
	public int getExtInstAddressWidth() {
		return extInstAddressWidth;
	}
	
	/** return full address width of external region for this instance */
	public int getExtAddressWidth() {
		return getExtInstAddressWidth() + getExternalType().getRepLevelAddrWidth();
	}

	/** sets the address width of word address range for this reg/group (only includes instance size, not ancestor bits used in rep_level option) */
	public void setExtInstAddressWidth(int extInstAddressWidth) {
		this.extInstAddressWidth = extInstAddressWidth;
	}

	/** get the low bit index of external address range
	 */
	public int getExtLowBit() {
		return extLowBit;
	}

	/** set extLowBit
	 *  @param extLowBit the extLowBit to set
	 */
	public void setExtLowBit(int extLowBit) {
		this.extLowBit = extLowBit;
	}

	/** return index range for a group of replicated external registers */
	public String getExtAddressArrayString() {
		if (getExtAddressWidth() > 1) return " [" + (getExtLowBit() + getExtAddressWidth() - 1) + ":" + getExtLowBit() + "] ";
		else return "";
	}

	/** true if address size is same as reg size */
	public boolean isSingleExtReg() {
		return (isRegister() && isExternal() && (Utils.getBits(getMaxRegWordWidth()) == getExtAddressWidth()));
	}

	/** true if external region has an address */
	public boolean hasExtAddress() {
		return (getExtAddressWidth() > 0)  && !isSingleExtReg();
	}

	/** true if external region instance (ignoring ancestor rep_level bits) has an address */
	public boolean hasExtInstAddress() {
		return (getExtInstAddressWidth() > 0)  && !isSingleExtReg();
	}

	/** true if external region has mult-word width */
	public boolean hasExtSize() {
		return (getMaxRegWordWidth() > 1) && !isSingleExtReg();
	}

	/** true if this external instance will define rtl structures (first ancestor iteration of a rep_external region or not rep_external)  */
	public boolean definesExtControls() {
		return !hasExternalRepLevel() || getExternalType().isFirstRepLevelIteration();
	}
	
	/** get externalType
	 *  @return the externalType
	 */
	public ExternalType getExternalType() {
		return externalType;
	}

	/** set externalType
	 *  @param externalType the externalType to set
	 */
	public void setExternalType(ExternalType externalType) {
		this.externalType = externalType;
	}
	
	/* return true is instance externalType matches specified value */
	public boolean hasExternalType(ExtType eType) {
		return (externalType.getType() == eType);
	}

	/** get external
	 *  @return the external
	 */
	public boolean isExternal() {
		return ((externalType != null) && externalType.isExternal());
	}

	/** set this instance as internal  */
	public void setInternal() {
		this.externalType = new ExternalType(ExtType.INTERNAL);  // internal
	}

	/** set external type for this instance from a string (null indicates internal) */
	public void setExternalTypeFromString(String externalStr) {
		String scrubbedStr = externalStr;
		// return as internal if null string
		if (externalStr == null) {
			this.externalType = new ExternalType(ExtType.INTERNAL); 
			return;
		}
        // otherwise clean up string since all whitespace has been removed
		else {
			scrubbedStr = scrubbedStr.replace("dly=", " dly=");
			scrubbedStr = scrubbedStr.replace("opt=", " opt=");
			scrubbedStr = scrubbedStr.replace("rep_level=", " rep_level=");
			scrubbedStr = scrubbedStr.replace("field_data=", " field_data=");
			scrubbedStr = scrubbedStr.replace("_D", " dly="); // replace old parameter form
			scrubbedStr += " ";
		}
		
		// extract valid external type info
		if ("DEFAULT".equals(externalStr)) this.externalType = new ExternalType(ExtType.PARALLEL);
		else if (externalStr.startsWith("PARALLEL") | externalStr.startsWith("SRAM")) {
			this.externalType = externalStr.startsWith("PARALLEL")? new ExternalType(ExtType.PARALLEL) : new ExternalType(ExtType.SRAM);
			// extract opt
			String optMode = extractParamFromString(scrubbedStr, "opt", false);
			if (optMode != null) {
				switch (optMode) {
				case "YES" : this.externalType.addParm("optimize", 1);  break;
				case "NO" :  this.externalType.addParm("no_optimize", 1);  break;
				case "KEEP_NACK" : this.externalType.addParm("keep_nack", 1);  break;
				}
			}
			// extract field_data
			String fData = extractParamFromString(scrubbedStr, "field_data", false);
			if ((fData != null) && fData.equals("YES")) this.externalType.addParm("field_data", 1);
			// extract rep_level
			String rLvl = extractParamFromString(scrubbedStr, "rep_level", true);
			if (rLvl != null) {
				Integer repLevel = Integer.valueOf(rLvl);
				if (repLevel > 0) 
				   this.externalType.addParm("rep_level", repLevel);  // if this parm is added, rep_level info will be collected in builder
			}
		}
		else if ("EXTERNAL_DECODE".equals(externalStr)) this.externalType = new ExternalType(ExtType.EXTERNAL_DECODE);
		else if ("BBV5_8".equals(externalStr)) {
			this.externalType = new ExternalType(ExtType.BBV5);
			this.externalType.addParm("width", 8);
		}
		else if ("BBV5_16".equals(externalStr)) {
			this.externalType = new ExternalType(ExtType.BBV5);
			this.externalType.addParm("width", 16);
		}
		else if (externalStr.startsWith("SERIAL8")) {
			int delay=0;  // default delay
			String dlyStr = extractParamFromString(scrubbedStr, "dly", true);
			if (dlyStr != null) delay = Integer.valueOf(dlyStr);
			this.externalType = new ExternalType(ExtType.SERIAL8);
			this.externalType.addParm("delay", delay);
		}
		else if (externalStr.startsWith("RING")) {
			int delay=0;  // default delay
			int width=16;  // default width
			String dlyStr = extractParamFromString(scrubbedStr, "dly", true);
			if (dlyStr != null) delay = Integer.valueOf(dlyStr);
			if (externalStr.contains("RING8")) width = 8;
			else if (externalStr.contains("RING32")) width = 32;
			this.externalType = new ExternalType(ExtType.RING);
			this.externalType.addParm("width", width);
			this.externalType.addParm("delay", delay);
		}
		else MsgUtils.errorExit("Invalid external interface type (" + externalStr + ") detected in instance " + getId());
		//System.out.println("InstanceProperties setExternal: input=" + externalStr + ", new val=" + this.externalType + ", inst=" + getId());
	}

	/** find assign of form param = value in baseStr and return value or null if search fails
	 * 
	 * @param baseStr - input string that will be searched for assignment
	 * @param param - param name on lhs of assign to be found
	 * @param intValue - if true, value must be an integer
	 * @return
	 */
	private String extractParamFromString(String baseStr, String param, boolean intValue) {
		Pattern p = intValue? Pattern.compile(".* " + param + "=(\\d+) .*") : Pattern.compile(".* " + param + "=(\\w+) .*");
		Matcher m = p.matcher(baseStr);
		if (m.matches()) {
			String value = m.group(1);
			//System.out.println("AddressableInstanceProperties extractParamFromString: " + param + ": " + value);
			return value;
		}
		//System.out.println("AddressableInstanceProperties extractParamFromString: " + param + " not found in str=" + baseStr);
		return null;
	}
	
	/** return true if this instance is external and has a rep_level option */
	public boolean hasExternalRepLevel () {
		return isExternal() && externalType.hasParm("rep_level");
	}
	
	/** return true if this instance is external and has a field_data option */
	public boolean useExtFieldData () {
		return isExternal() && externalType.hasParm("field_data");
	}
	
	// ------
	
	/** get rootExternal (set by stack push into outputBuilder)
	 *  @return the rootExternal
	 */
	public boolean isRootExternal() {
		return rootExternal;
	}

	/** set rootExternal
	 *  @param rootExternal the rootExternal to set
	 */
	public void setRootExternal(boolean rootExternal) {
		this.rootExternal = rootExternal;
	}

	/** get externalDecode
	 *  @return the externalDecode
	 */
	public boolean isExternalDecode() {
		return externalType.isExternalDecode();
	}
	
	public boolean isLocalExternal() {
		return localExternal;
	}

	public void setLocalExternal(boolean localExternal) {
		this.localExternal = localExternal;
	}

	public boolean isLocalRootExternal() {
		return localRootExternal;
	}

	public void setLocalRootExternal(boolean localRootExternal) {
		this.localRootExternal = localRootExternal;
	}

	/** ExternalType class carrying parameters */
	public class ExternalType {
		private ExtType type = ExtType.INTERNAL;
		private HashMap<String, Integer> parms = new HashMap<String, Integer>();
		private String repLevelBasename;  // alternate pathname to be used if rep_level option is specified
		private List<Integer> repLevelAddrBits;  // number of address bits in each ancestor if rep_level option is specified
		private List<Integer> repLevelIndices;  // current instance index of each ancestor if rep_level option is specified
		// constructors
		public ExternalType(ExtType type) {
			this.type = type;
		}
		// type getters setters
		public ExtType getType() {
			return type;
		}
		public boolean isExternal() {
			return (type != ExtType.INTERNAL);
		}
		public boolean isExternalDecode() {
			return (type == ExtType.EXTERNAL_DECODE);
		}
		public void setType(ExtType type) {
			this.type = type;
		}
		// parm getters/setters
		public Integer getParm(String parm) {
			return parms.get(parm);
		}
		public boolean hasParm(String parm) {
			return parms.containsKey(parm);
		}
		public void removeParm(String parm) {
			if (hasParm(parm)) parms.remove(parm);
		}
		public void addParm(String parm, Integer value) {
			parms.put(parm,  value);
		}
		@Override
		public String toString() {
			return type.toString() + parms;
		}
		// rep_level option methods  TODO
		public String getRepLevelBasename() {
			return repLevelBasename;
		}
		public void setRepLevelBasename(String repLevelBasename) {
			this.repLevelBasename = repLevelBasename;
		}
		public List<Integer> getRepLevelAddrBits() {
			return repLevelAddrBits;
		}
		public void setRepLevelAddrBits(List<Integer> repLevelAddrBits) {
			this.repLevelAddrBits = repLevelAddrBits;
		}
		public List<Integer> getRepLevelIndices() {
			return repLevelIndices;
		}
		public void setRepLevelIndices(List<Integer> repLevelIndices) {
			this.repLevelIndices = repLevelIndices;
		}
		/** return true if this is first iteration of a rep_level group that will be collapsed into one external interface */
		public boolean isFirstRepLevelIteration() {
			for (Integer idx : repLevelIndices) if (idx != 0) return false;
			return true;
		}
		/** returns address width of ancestor bits used in rep_level option */
		public int getRepLevelAddrWidth() {
			if (repLevelAddrBits == null) return 0;
			int width = 0;
			for (Integer bits : repLevelAddrBits) width += bits;
			return width;
		}
		/** return the repLevel */
		public int getRepLevel() {
			Integer rLev = getParm("rep_level");
			return (rLev==null)? 0 : rLev;
		}
		/** return verilog value of rep_level ancestor bits */
		public String getRepLevelValueString() {
			String outStr = "";
			for (int idx=0; idx<getRepLevel(); idx++) {
				String prefix = ((idx + 1) == getRepLevel())? "" : ",";
				outStr = prefix + repLevelAddrBits.get(idx) + "'d" + repLevelIndices.get(idx) + outStr;
			}
			if (getRepLevel() > 1) outStr = "{" + outStr + "}";
			return outStr;
		}
	}

	// ------------

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

	/** return true if this addressable instance is a register */
	public abstract boolean isRegister();
	
    /** return the max register width within this addressable instance */
	public int getMaxRegWidth() {
		return this.getExtractInstance().getRegComp().getMaxRegWidth();   // return precomputed maxregwidth from model
	}

    /** return the write enable width of this addressable instance */
	public int getWriteEnableWidth() {
		return getMaxRegWidth()/ExtParameters.sysVerWriteEnableSize();
	}

	/** get max register width within this addressable instance in bytes */
	public int getMaxRegByteWidth() {
		return getMaxRegWidth()/8;
	}
	
	/** get max register width within this addressable instance in words */
	public int getMaxRegWordWidth() {
		return getMaxRegWidth()/ExtParameters.getMinDataSize();
	}
	
	/** get bits needed to address register words within this addressable instance */
	public Integer getMaxRegWordHighBit() {
		return (new RegNumber(getMaxRegWordWidth())).getMinusOneHighestBit();
	}

	/** return the array string for this max width register in this AddressableInstance */
	public String getMaxRegArrayString() {
		return  " [" + (getMaxRegWidth() - 1) + ":0] ";
	}

	/** hashcode/equals overrides 
	 * - ignores externalType in compare
	 * - optionally includes id in super so reg/regset childs can support both
	 */
	@Override
	public int hashCode(boolean includeId) {
		final int prime = 31;
		int result = super.hashCode(includeId);
		result = prime * result + (addressMap ? 1231 : 1237);
		result = prime * result + (rootExternal ? 1231 : 1237);
		result = prime * result + (isSwReadable ? 1231 : 1237);
		result = prime * result + (isSwWriteable ? 1231 : 1237);
		return result;
	}
	
	/** hashcode/equals overrides 
	 * - regs/regsets should not be calling default
	 */
	@Override
	public int hashCode() {
		MsgUtils.errorExit("AddressableInstanceProperty hash is being called directly for id=" + getId());
		return hashCode(false);
	}

	@Override
	public boolean equals(Object obj, boolean includeId) {
		if (this == obj)
			return true;
		if (!super.equals(obj, includeId))
			return false;
		if (getClass() != obj.getClass())
			return false;
		AddressableInstanceProperties other = (AddressableInstanceProperties) obj;
		if (addressMap != other.addressMap)
			return false;
		if (rootExternal != other.rootExternal)
			return false;
		if (isSwReadable != other.isSwReadable)
			return false;
		if (isSwWriteable != other.isSwWriteable)
			return false;
		return true;
	}

}
