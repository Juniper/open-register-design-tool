/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved.
 */
package ordt.output;

import ordt.extract.RegNumber;
import ordt.extract.RegNumber.NumBase;
import ordt.extract.RegNumber.NumFormat;
import ordt.extract.model.ModAddressableInstance;
import ordt.extract.model.ModInstance;
import ordt.parameters.ExtParameters;
import ordt.parameters.Utils;

/** extracted properties of an addressable instance (reg/regset properties) created during model walk */
public abstract class AddressableInstanceProperties extends InstanceProperties {

	protected RegNumber baseAddress;
	protected RegNumber relativeBaseAddress;   // base address of reg relative to parent

	// external register group parameters
	protected int extAddressWidth = 0;   // width of word address range for this group
	protected int extLowBit = 0;  // low bit in external address range
	
	private boolean externalDecode = false;   // inst declared as external decode

	public AddressableInstanceProperties(ModInstance extractInstance) {
		super(extractInstance);
	}

	public AddressableInstanceProperties(AddressableInstanceProperties oldInstance) {
		super(oldInstance);
		// set AddressableInstanceProperty info
		setRelativeBaseAddress(oldInstance.getRelativeBaseAddress());  
		setBaseAddress(oldInstance.getBaseAddress());  
		setExtAddressWidth(oldInstance.getExtAddressWidth());  
		setExtLowBit(oldInstance.getExtLowBit());  
		setExternalDecode(oldInstance.isExternalDecode());  
	}
	
	/** display info AddressableInstanceProperties info */
	public void display() {
		super.display();
		System.out.println("  AddressableInstanceProperty info:" );  
		System.out.println("   base address=" + this.getBaseAddress());  		
		System.out.println("   relative base address=" + this.getRelativeBaseAddress());  		
		System.out.println("   ext addr width=" + this.getExtAddressWidth());  		
		System.out.println("   ext addr low bit=" + this.getExtLowBit());  		
		System.out.println("   external decode=" + this.isExternalDecode());  		
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
	/** get extAddressWidth
	 *  @return the extAddressWidth
	 */
	public int getExtAddressWidth() {
		return extAddressWidth;
	}

	/** set extRegWidth
	 *  @param extRegWidth the extRegWidth to set
	 */
	public void setExtAddressWidth(int extAddressWidth) {
		this.extAddressWidth = extAddressWidth;
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
	
	/** get extractInstance
	 *  @return the extractInstance
	 */
	@Override
	public ModAddressableInstance getExtractInstance() {
		return (ModAddressableInstance) extractInstance;
	}

	/** get externalDecode
	 *  @return the externalDecode
	 */
	public boolean isExternalDecode() {
		return externalDecode;
	}

	/** set externalDecode
	 *  @param externalDecode the externalDecode to set
	 */
	public void setExternalDecode(boolean externalDecode) {
		//System.out.println("AddressableInstanceProperties setExternalDecode: " + externalDecode);
		this.externalDecode = externalDecode;
	}

	/** return true if this addressable instance is a register */
	public abstract boolean isRegister();
	
    /** return the max register width within this addressable instance */
	public abstract int getMaxRegWidth();
	
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

}
