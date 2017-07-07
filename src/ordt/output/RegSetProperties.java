/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved.
 */
package ordt.output;

import ordt.extract.DefinedProperties;
import ordt.extract.PropertyList;
import ordt.extract.RegNumber;
import ordt.extract.RegNumber.NumBase;
import ordt.extract.RegNumber.NumFormat;
import ordt.extract.model.ModInstance;
import ordt.parameters.ExtParameters;

/** extracted properties of a regset instance created during model walk */
public class RegSetProperties extends AddressableInstanceProperties {
	
	private int maxRegWidth = ExtParameters.getMinDataSize();  // maximum sized register found in this regset - default to min pio data width  
	private RegNumber highAddress;  // highest valid address
	private int childHash = 0; // hash of this regset's children
	// jspec compatibility parameters
	private String jspecMacroName;
	private String jspecMacroMode;
	private String jspecNamespace;

	public RegSetProperties(ModInstance regSetInst) {
		super(regSetInst);  // init instance, id, name, description text
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
		// set jspec pass-thru parameters
		if (pList.hasProperty("js_macro_name")) setJspecMacroName(pList.getProperty("js_macro_name"));
		if (pList.hasProperty("js_macro_mode")) setJspecMacroMode(pList.getProperty("js_macro_mode"));
		if (pList.hasProperty("js_namespace")) setJspecNamespace(pList.getProperty("js_namespace"));
	} 
    
	/** extract a PropertyList of user defined parameters for this instance */
    @Override
	protected void extractUserDefinedProperties(PropertyList pList) {
		setUserDefinedProperties(pList, DefinedProperties.userRegSetPropertySet);
	}
	
	/** return the max reg width in this regset in bits
	 */
	public int getMaxRegWidth() {
		return maxRegWidth;
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
		RegNumber fullHigh = new RegNumber(ExtParameters.getPrimaryBaseAddress());  
		fullHigh.setVectorLen(ExtParameters.getLeafAddressSize());
		fullHigh.setNumBase(NumBase.Hex);
		fullHigh.setNumFormat(NumFormat.Address);
		fullHigh.add(getHighAddress());
		return fullHigh;
	}

	public String getJspecMacroName() {
		return jspecMacroName;
	}

	public void setJspecMacroName(String jspecMacroName) {
		this.jspecMacroName = jspecMacroName;
	}

	public String getJspecMacroMode() {
		return jspecMacroMode;
	}

	public void setJspecMacroMode(String jspecMacroMode) {
		this.jspecMacroMode = jspecMacroMode;
	}

	public String getJspecNamespace() {
		return jspecNamespace;
	}

	public void setJspecNamespace(String jspecNamespace) {
		this.jspecNamespace = jspecNamespace;
	}

	@Override
	public boolean isRegister() {
		return false;
	}

	/** set the hash for this regsets children */
	public void updateChildHash(int newChildHash) {
		final int prime = 31;
		this.childHash = prime * this.childHash + newChildHash;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + childHash;
		result = prime * result + maxRegWidth;
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
		RegSetProperties other = (RegSetProperties) obj;
		if (childHash != other.childHash)
			return false;
		if (getExtractInstance().getRegComp() == null) {  // use model component compare if childHash is equal
			if (other.getExtractInstance().getRegComp() != null)
				return false;
		} else if (!getExtractInstance().getRegComp().equals(other.getExtractInstance().getRegComp())) {
			//System.out.println("RegSetProperties equals() fail for this=" + getInstancePath() + ", other=" + other.getInstancePath());
			return false;
		}
		if (maxRegWidth != other.maxRegWidth)
			return false;
		return true;
	}

}
