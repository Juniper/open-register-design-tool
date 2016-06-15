/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved.
 */
package ordt.output;


import ordt.extract.ModIndexedInstance;
import ordt.extract.ModInstance;

/** class of properties needed for display of active signal instance */
public class FieldSetProperties extends InstanceProperties {

	/** init properties using defaults -> component -> instance values */
	public FieldSetProperties(ModInstance fieldSetInst) {	
		super(fieldSetInst);  // init instance, id
	}
	
	public FieldSetProperties(FieldSetProperties oldInstance) {
		super(oldInstance);
	}
	
	/** display info  *
    @Override
	public void display() {
		super.display();
		//System.out.println("  FieldSetProperty info:" );  
		//System.out.println("   max reg width=" + this.getMaxRegWidth());  		
	}*/
	
	/** extract properties from the calling instance *
    @Override
	public void extractProperties(PropertyList pList) {
		super.extractProperties(pList);  // extract common parameters
		//Jrdl.infoMessage("SignalProperties: id=" + getId() + ", pList=" + pList);
    }*/	

	/** get extractInstance
	 *  @return the extractInstance
	 */
	@Override
	public ModIndexedInstance getExtractInstance() {
		return (ModIndexedInstance) extractInstance;
	}


}
