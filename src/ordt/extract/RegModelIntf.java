/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved. 
 */
package ordt.extract;

import ordt.extract.model.ModComponent;
import ordt.extract.model.ModInstance;

public interface RegModelIntf {
    /** get root component
	 *  @return the root
	 */
	public ModComponent getRoot();
    
	/** get root instance
	 *  @return the root
	 */
	public ModInstance getRootInstance();    
	
	/** get root instanced component (usually base addrmap)
	 *  @return the root
	 */
	public ModComponent getRootInstancedComponent();
	
	/** get jrdlInputFile name
	 *  @return the jrdlInputFile
	 */
	public String getOrdtInputFile();
	
	/** return true if field offsets are relative to zero or max reg/fieldset width (rdl=true, jspac=false) **/
	public boolean fieldOffsetsFromZero();		
	
}
