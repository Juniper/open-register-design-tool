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
	private String jspecTypedefName;
	private String jspecInstanceName;
	private Integer jspecInstanceRepeat;

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
		System.out.println("   js_typedef_name=" + this.getJspecTypedefName());  		
		System.out.println("   js_instance_name=" + this.getJspecInstanceName());  		
		System.out.println("   js_instance_repeat=" + this.getJspecInstanceRepeat());  		
	}

	/** extract properties from the calling instance */
    @Override
	public void extractProperties(PropertyList pList) {
		super.extractProperties(pList);  // extract common parameters
		//if (getId().equals("tx")) System.out.println("RegSetProperties extractProperties: " + getId() + "\n" + pList);
		// set jspec root instance/typedef control parameters
		if (pList.hasProperty("js_typedef_name")) setJspecTypedefName(pList.getProperty("js_typedef_name"));
		if (pList.hasProperty("js_instance_name")) setJspecInstanceName(pList.getProperty("js_instance_name"));
		if (pList.hasProperty("js_instance_repeat")) setJspecInstanceRepeat(pList.getIntegerProperty("js_instance_repeat"));
	} 

	/** update basic info for the root regmap.  no post-prop assigns, path info	 */
	public void updateRootInstanceInfo() {
		// before creating instance list update default instance properties 
		updateDefaultProperties(extractInstance.getDefaultProperties());
		// create a property list for holding combined info for this instance
		PropertyList mergedList = new PropertyList();
		// now add defined default instance properties
		mergedList.updateProperties(instDefaultProperties);   // start with instance defaults 

		// now add the base property info
		mergedList.updateProperties(extractInstance.getProperties());
		extractProperties(mergedList);   // now that we have combined parameter list, extract instance info
	}

	/** extract a PropertyList of user defined parameters for this instance */
    @Override
	protected void extractSpecialPropertyLists(PropertyList pList) {
		setUserDefinedProperties(pList, DefinedProperties.userDefRegSetPropertyNames);
		setJsPassthruProperties(pList, DefinedProperties.jsPassthruRegSetPropertyNames);
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

	public String getJspecTypedefName() {
		return jspecTypedefName;
	}

	public void setJspecTypedefName(String jspecTypedefName) {
		this.jspecTypedefName = jspecTypedefName;
	}

	public String getJspecInstanceName() {
		return jspecInstanceName;
	}
	
	public void setJspecInstanceName(String jspecInstanceName) {
		this.jspecInstanceName = jspecInstanceName;
	}

	public Integer getJspecInstanceRepeat() {
		return jspecInstanceRepeat;
	}
   
	public void setJspecInstanceRepeat(Integer jspecInstanceRepeat) {
		this.jspecInstanceRepeat = jspecInstanceRepeat;
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
	
	/** hashcode/equals overrides 
	 * - child hash is updated in outputbuilder
	 * - optionally includes id in super 
	 */
	@Override
	public int hashCode(boolean includeId) {
		final int prime = 31;
		int result = super.hashCode(includeId);
		result = prime * result + childHash;
		result = prime * result + maxRegWidth;
		return result;
	}
	
	/** hashcode/equals overrides 
	 * - child hash is updated in outputbuilder
	 * - by default, id is not included in parent regset hash (used for uvmregs output class reuse) 
	 */
	@Override
	public int hashCode() {
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

	@Override
	public boolean equals(Object obj) {
		return equals(obj, false);  // id is not included by default
	}

}
