/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved. 
 */
package ordt.extract.model;

import ordt.extract.RegNumber;

/** class of addressable model component instance (addrmap, regset, reg) */
public class ModAddressableInstance extends ModInstance {
	// address elements
    private RegNumber address;
    private RegNumber addressIncrement;
    private RegNumber addressModulus;  
    private RegNumber addressShift;  
    
	public ModAddressableInstance() {
		isAddressable = true;
	}
	/** write info to stdout */
	@Override
	public void display (boolean showInstanceComponent) {
			String compID = "";
			if (regComp != null) {
				compID = regComp.getId();
				if ((compID == null) || compID.isEmpty()) compID = "<anonymous>";
			}
			System.out.println("\n--------- Addressable Instance of " + compID + "   id=" + getFullId() ); 
			//System.out.println("         full id=" + getFullId()); 
			System.out.println("    replication count=" + getRepCount());   
			System.out.println("    root instance=" + isRootInstance());   
			System.out.println("    address=" + getAddress() + ",  addressInc=" + getAddressIncrement() + ",  addressMod=" + getAddressModulus());
			// display parms
			System.out.println("    properties:");
			System.out.println("        " + properties);

	        // optionally display component
			if ((regComp != null) && showInstanceComponent) regComp.display();
	}

	// -------------- address methods
	
	/** get Address
	 *  @return the Address
	 */
	public RegNumber getAddress() {
		return address;
	}

	/** set Address
	 *  @param  the address to set
	 */
	public void setAddress(RegNumber address) {
		this.address = address;
	}

	public void setAddress(String numStr) {
		if (numStr != null) setAddress(new RegNumber(numStr));			
	}

	/** get addressIncrement
	 *  @return the addressIncrement
	 */
	public RegNumber getAddressIncrement() {
		return addressIncrement;
	}

	/** set addressIncrement
	 *  @param addressIncrement the addressIncrement to set
	 */
	public void setAddressIncrement(RegNumber addressIncrement) {
		this.addressIncrement = addressIncrement;
	}

	public void setAddressIncrement(String numStr) {
		if (numStr != null) setAddressIncrement(new RegNumber(numStr));					
	}

	/** get addressModulus
	 *  @return the addressModulus
	 */
	public RegNumber getAddressModulus() {
		return addressModulus;
	}

	/** set addressModulus
	 *  @param addressModulus the addressModulus to set
	 */
	public void setAddressModulus(RegNumber addressModulus) {
		this.addressModulus = addressModulus;
	}

	public void setAddressModulus(String numStr) {
		if (numStr != null) setAddressModulus(new RegNumber(numStr));					
	}

	/** get addressShift
	 *  @return the addressShift
	 */
	public RegNumber getAddressShift() {
		return addressShift;
	}

	/** set addressShift
	 *  @param addressShift the addressShift to set
	 */
	public void setAddressShift(RegNumber addressShift) {
		this.addressShift = addressShift;
	}

	public void setAddressShift(String numStr) {
		if (numStr != null) setAddressShift(new RegNumber(numStr));					
	}
	
	/** return the aligned size of this instance by multiplying component aligned size or increment by reps */
	public RegNumber getAlignedSize() {
		if (this.getRepCount()<2) {
			//if (regComp.getId().equals("yt_fabio_switch_sopcasc1")) System.out.println("ModAddressableInstance setAlignedSize: repCount=" + this.getRepCount() + ", return=" + this.regComp.getAlignedSize());
			return new RegNumber(this.regComp.getAlignedSize()); // if no reps, use comp size
		}
		RegNumber size = (this.getAddressIncrement() != null) ? new RegNumber(this.getAddressIncrement()) : 
            new RegNumber(this.regComp.getAlignedSize());  // compute size of this instance or use increment if specified
        size.multiply(this.getRepCount()); 
		//if (regComp.getId().equals("yt_fabio_switch_sopcasc1")) System.out.println("ModAddressableInstance setAlignedSize: repCount[N]=" + this.getRepCount() + ", return=" + size);
        return size;
	}
	
	/** set a numeric instance variable - overridden in ModInstance child classes
	 * 
	 * @param key - if this key is found, instance value will be updated to val
	 * @param val - value to be used
	 */
	@Override
	protected void updateInstanceVar(String key, String val) {
		if ("address".equals(key)) {  // can be passed from comp in jspec
			setAddress(val);
			//System.err.println("ModInstance " + this.getFullId() + ": updating key=" + key + " with value=" + val);
		}
		else if ("addrinc".equals(key)) setAddressIncrement(val);   // can be passed from comp in jspec
		else if ("addrmod".equals(key)) setAddressModulus(val); // TODO - ever happen??
		else if ("addrshift".equals(key)) setAddressShift(val);
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((address == null) ? 0 : address.hashCode());
		result = prime * result + ((addressIncrement == null) ? 0 : addressIncrement.hashCode());
		result = prime * result + ((addressModulus == null) ? 0 : addressModulus.hashCode());
		result = prime * result + ((addressShift == null) ? 0 : addressShift.hashCode());
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
		ModAddressableInstance other = (ModAddressableInstance) obj;
		if (address == null) {
			if (other.address != null)
				return false;
		} else if (!address.equals(other.address))
			return false;
		if (addressIncrement == null) {
			if (other.addressIncrement != null)
				return false;
		} else if (!addressIncrement.equals(other.addressIncrement))
			return false;
		if (addressModulus == null) {
			if (other.addressModulus != null)
				return false;
		} else if (!addressModulus.equals(other.addressModulus))
			return false;
		if (addressShift == null) {
			if (other.addressShift != null)
				return false;
		} else if (!addressShift.equals(other.addressShift))
			return false;
		return true;
	}

}
