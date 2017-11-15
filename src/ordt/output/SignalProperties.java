/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved. 
 */
package ordt.output;

import ordt.extract.PropertyList;
import ordt.extract.model.ModComponent;
import ordt.extract.model.ModIndexedInstance;
import ordt.extract.model.ModInstance;

/** class of properties needed for display of active signal instance */
public class SignalProperties extends InstanceProperties {
	// TODO sync, async 

	private Integer signalWidth = 1;   // default to scalar        
	private Integer lowIndex = 0;   // default is 0 low index
	private boolean isRhsReference = false;  // true if this signal is referenced in rhs of an assign
	private boolean isDefaultReset = false;
	private boolean isLogicReset = false;
	private boolean isActiveLow = false;

	private RhsExpression assignExpr;        // assignment reference for this signal

	/** init properties using defaults -> component -> instance values */
	public SignalProperties(ModInstance regInst) {	
		super(regInst);  // init instance, id
	}
	
	/** extract properties from the calling instance */
    @Override
	public void extractProperties(PropertyList pList) {
		super.extractProperties(pList);  // extract common parameters
		//Jrdl.infoMessage("SignalProperties: id=" + getId() + ", pList=" + pList);
		
		// go directly to extractInstance to get width/index  
		setSignalWidth(getExtractInstance());
		setLowIndex(getExtractInstance());
		
		// set signal assignment ref 
		if (pList.hasProperty("signalAssign")) {
			setAssignExpr(pList.getProperty("signalAssign"), pList.getDepth("signalAssign"));
			//System.out.println("SignalProperties: id=" + getId() + ", found signal assign");
		}

		// cpuif_reset, hwif_reset
		if (pList.hasTrueProperty("cpuif_reset")) {
			this.setDefaultReset(true);
		}
		if (pList.hasTrueProperty("field_reset")) {
			this.setLogicReset(true);
		}
		
		// activehigh, activelow
		if (pList.hasTrueProperty("activelow") || pList.hasFalseProperty("activehigh")) {
			this.setActiveLow(true);
		}
    }	
    
    /** get signalWidth
	 *  @return the signalWidth
	 */
	public Integer getSignalWidth() {
		return signalWidth;
	}

	/** set signalWidth from inst/comp properties
	 *  @param instance
	 */
	public void setSignalWidth(ModIndexedInstance regInst) {		
		ModComponent regComp = regInst.getRegComp();  // get the component of this instance
		// if instance prop is set then use it
		if (regInst.hasProperty("signalwidth")) 
			this.signalWidth = regInst.getIntegerProperty("signalwidth"); 
		// otherwise look for a signalwidth set in component
		else if ((regComp != null) && (regComp.hasProperty("signalwidth")))
			this.signalWidth = regComp.getIntegerProperty("signalwidth");
		// otherwise look for width set in instance 
		else if (regInst.getWidth() != null)   
			this.signalWidth = regInst.getWidth(); 
		else this.signalWidth = 1;
		//System.out.println("SignalProperties setSignalWidth: inst=" + this.getInstancePath() + ", w=" + signalWidth);
	}

	/** get signal array string with explicit range indices
	 *  @param instance
	 */
	public String getSignalArrayString() {
		String retStr = "";
		// if a scalar then return empty string
		int width = getSignalWidth();
		if (width < 2) retStr = "";
		// if no explicit bit locations
		else if (getLowIndex() == null) retStr = " [" + (getSignalWidth() - 1) + ":0] ";
		else retStr = " [" + (getLowIndex() + width - 1) + ":" + getLowIndex() + "] ";
		return retStr;
	}

	/** get signal array string with low range index starting at 0
	 *  @param instance
	 */
	public String getBaseSignalArrayString() {
		String retStr = "";
		// if a scalar then return empty string
		int width = getSignalWidth();
		if (width < 2) retStr = "";
		// if no explicit bit locations
		else retStr = " [" + (getSignalWidth() - 1) + ":0] ";
		return retStr;
	}

	/** get lowIndex
	 *  @return the lowIndex
	 */
	public Integer getLowIndex() {
		return lowIndex;
	}

	/** set lowIndex  from inst/comp properties
	 *  @param reginstance that lowindex will be extracted from
	 */
	public void setLowIndex(ModIndexedInstance regInst) {
		// if instance prop is set then use it
		if (regInst.getOffset() != null) 
			this.lowIndex = regInst.getOffset(); 
		else
			this.lowIndex = 0;
	}

	public boolean hasAssignExpr() {
		return (assignExpr != null);
	}

	/** set the assign expression for this signal */
	public void setAssignExpr(String rawExpression, int depth) {
		this.assignExpr = new RhsExpression(rawExpression, depth);
	}

	/** return the assign expression for this signal */
	public RhsExpression getAssignExpr() {
		return assignExpr;
	}

	/** returns true if this signal is referenced on rhs of an assign */
	public boolean isRhsReference() {
		return isRhsReference;
	}

	/** set isRhsReference - true if this signal is referenced on rhs of an assign */
	public void setRhsReference(boolean isRhsReference) {
		this.isRhsReference = isRhsReference;
	}

	/** get isDefaultReset
	 *  @return the isDefaultReset
	 */
	public boolean isDefaultReset() {
		return isDefaultReset;
	}

	/** set isDefaultReset
	 *  @param isDefaultReset the isDefaultReset to set
	 */
	public void setDefaultReset(boolean isDefaultReset) {
		this.isDefaultReset = isDefaultReset;
	}

	/** get isLogicReset
	 *  @return the isLogicReset
	 */
	public boolean isLogicReset() {
		return isLogicReset;
	}

	/** set isLogicReset
	 *  @param isLogicReset the isLogicReset to set
	 */
	public void setLogicReset(boolean isLogicReset) {
		this.isLogicReset = isLogicReset;
	}

	/** get isActiveLow
	 *  @return the isActiveLow
	 */
	public boolean isActiveLow() {
		return isActiveLow;
	}

	/** set isActiveLow
	 *  @param isActiveLow the isActiveLow to set
	 */
	public void setActiveLow(boolean isActiveLow) {
		this.isActiveLow = isActiveLow;
	}

	/** get extractInstance
	 *  @return the extractInstance
	 */
	@Override
	public ModIndexedInstance getExtractInstance() {
		return (ModIndexedInstance) extractInstance;
	}


}
