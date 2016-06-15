/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved.
 */
package ordt.output;

import ordt.extract.ModInstance;
import ordt.extract.PropertyList;
import ordt.extract.RegNumber;
import ordt.extract.RegNumber.NumBase;
import ordt.extract.RegNumber.NumFormat;
import ordt.parameters.ExtParameters;

/** extracted properties of a regset instance created during model walk */
public class RegSetProperties extends AddressableInstanceProperties {
	
	private int maxRegWidth = ExtParameters.getMinDataSize();  // maximum sized register found in this regset - default to min pio data width  
	private RegNumber highAddress;  // highest valid address

	public RegSetProperties(ModInstance regSetInst) {
		super(regSetInst);  // init instance, id, name, decription text
	}
	
	public RegSetProperties(RegSetProperties oldInstance) {
		super(oldInstance);
		// set RegSetProperty info
		setMaxRegWidth(oldInstance.getMaxRegWidth());  
	}
	
	/** display info AddressableInstanceProperties info */
    @Override
	public void display() {
		super.display();
		System.out.println("  RegSetProperty info:" );  
		System.out.println("   max reg width=" + this.getMaxRegWidth());  		
	}

	/** extract properties from the calling instance */
    @Override
	public void extractProperties(PropertyList pList) {
		super.extractProperties(pList);  // extract common parameters
		//if (getId().equals("tx")) System.out.println("RegSetProperties extractProperties: " + getId() + "\n" + pList);
		// set external decode only if a regset
		if (pList.hasTrueProperty("external_decode")) setExternalDecode(true);
	} 
	
	/** get maxRegWidth
	 *  @return the maxRegWidth
	 */
	public int getMaxRegWidth() {
		return maxRegWidth;
	}
	
	/** get maxRegWidth in bytes
	 */
	public int getMaxRegByteWidth() {
		return getMaxRegWidth() / 8;
	}
	
	/** get maxRegWidth in words
	 */
	public int getMaxRegWordWidth() {
		return getMaxRegWidth() / ExtParameters.getMinDataSize();
	}

	/** set maxRegWidth
	 *  @param maxRegWidth the maxRegWidth to set
	 */
	public void setMaxRegWidth(int maxRegWidth) {
		this.maxRegWidth = maxRegWidth;
	}
	
	/** update maxRegWidth if new value is higher
	 *  @param maxRegWidth the maxRegWidth to set
	 *  @return true if value was updated
	 */
	public boolean updateMaxRegWidth(int maxRegWidth) {
		if (maxRegWidth > getMaxRegWidth()) {
			setMaxRegWidth(maxRegWidth);
			return true;
		}
		return false;
	}

	/** set the highest valid address for this regset */
	public void setHighAddress(RegNumber highAddress) {
		this.highAddress = highAddress;
		
	}

	/** return highest valid address in this regset (only valid in finishRegSet call) */
	public RegNumber getHighAddress() {
		return highAddress;
	}
	
	/** get full high address including base offset (only valid in finishRegSet call)
	 */
	public RegNumber getFullHighAddress() {
		RegNumber fullHigh = new RegNumber(ExtParameters.getLeafBaseAddress());  
		fullHigh.setVectorLen(ExtParameters.getLeafAddressSize());
		fullHigh.setNumBase(NumBase.Hex);
		fullHigh.setNumFormat(NumFormat.Address);
		fullHigh.add(getHighAddress());
		return fullHigh;
	}

}
