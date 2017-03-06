/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved.
 */
package ordt.output;


import ordt.extract.DefinedProperties;
import ordt.extract.ModComponent;
import ordt.extract.ModIndexedInstance;
import ordt.extract.ModInstance;
import ordt.extract.PropertyList;

/** class of properties needed for display of active signal instance */
public class FieldSetProperties extends InstanceProperties {
	private Integer fieldSetWidth = 0;   // default to empty fieldset        
	private Integer lowIndex = null;   // no specific index set

	/** init properties using defaults -> component -> instance values */
	public FieldSetProperties(ModInstance fieldSetInst) {	
		super(fieldSetInst);  // init instance, id
	}
	
	public FieldSetProperties(FieldSetProperties oldInstance) {
		super(oldInstance);
	}
	
	/** display info  */
    @Override
	public void display() {
		super.display();
		System.out.println("  FieldSetProperty info:" );  
		System.out.println("   fieldset width=" + this.getFieldSetWidth());  		
		System.out.println("   fieldset lowIndex=" + this.getLowIndex());  		
	}
	
	/** extract properties from the calling instance */
    @Override
	public void extractProperties(PropertyList pList) {
		super.extractProperties(pList);  // extract common parameters
		//Jrdl.infoMessage("FieldSetProperties: id=" + getId() + ", pList=" + pList);
		// go directly to extractInstance to get fieldset width/indices
		setFieldSetWidth(getExtractInstance());  // set width of fieldset
    }	
    
	/** extract a PropertyList of user defined parameters for this instance */
    @Override
	protected void extractUserDefinedProperties(PropertyList pList) {
		setUserDefinedProperties(pList, DefinedProperties.userFieldSetPropertySet);
	}

	/** get extractInstance
	 *  @return the extractInstance
	 */
	@Override
	public ModIndexedInstance getExtractInstance() {
		return (ModIndexedInstance) extractInstance;
	}

	// -------------------------- fieldset width/index methods ------------------------------

	/** get fieldSetWidth
	 *  @return the fieldSetWidth
	 */
	public Integer getFieldSetWidth() {
		return fieldSetWidth;
	}

	/** set fieldSetWidth from inst/comp properties
	 *  @param instance
	 */
	public void setFieldSetWidth(ModIndexedInstance fsetInst) {
		ModComponent regComp = fsetInst.getRegComp();  // get the component of this instance
		// otherwise look for width set in instance 
		if (fsetInst.getWidth() != null)   
			this.fieldSetWidth = fsetInst.getWidth(); 
		// if instance prop is set then use it
		else if (fsetInst.hasProperty("fieldstructwidth")) 
			this.fieldSetWidth = fsetInst.getIntegerProperty("fieldstructwidth"); 
		// otherwise look for a fieldstructwidth set in component
		else if ((regComp != null) && (regComp.hasProperty("fieldstructwidth"))) {
			this.fieldSetWidth = regComp.getIntegerProperty("fieldstructwidth");
		}
		else this.fieldSetWidth = 1;
	}

	/** get lowIndex
	 *  @return the lowIndex
	 */
	public Integer getLowIndex() {
		return lowIndex;
	}
	
	/** set lowIndex 
	 *  @param lowIndex the lowIndex to set
	 */
	public void setLowIndex(Integer lowIdx) {
		this.lowIndex = lowIdx;
	}


}
