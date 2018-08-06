/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved. 
 */
package ordt.extract.model;

import java.util.ArrayList;
import java.util.List;

import ordt.output.common.MsgUtils;

/** enum class type
 *  @author snellenbach      
 *  Nov 12, 2013
 *
 */
public class ModEnum extends ModComponent {
	private List<ModEnumElement> enumElements = new ArrayList<ModEnumElement>();;   // enum elements contained in this enum
	private Integer width;  // width of this enum encoding
	
	/** write info to stdout  */
	@Override
    public void display() {
			String parID = "";
			if (parent != null) parID = parent.getId();
			System.out.println("\n---------" + this.getClass() + "   id=" + getId()+ "   parent=" + parID); 
			System.out.println("         full id=" + getFullId()); 
			// display enum elements
			System.out.println("    elements:");
			for (ModEnumElement enumElement: enumElements) {
				System.out.println("        " + enumElement.getId() + "=" + enumElement.getValue());
				System.out.println("             name=" + enumElement.getName());
				System.out.println("             description=" + enumElement.getDesc());
			}	
			// display instances of
			System.out.println("    instances of:");
			for (ModInstance inst: instancesOf) {
				System.out.println("        id=" + inst.getId());
			}	
			// display properties
			System.out.println("    properties:");
			System.out.println("        " + properties);
	}
    

	/** add a child enum element
	 *  @param regenumelement to add as child
	 */
	public void addEnumElement(ModEnumElement regEnumElem) {
		// check width
		if ((regEnumElem != null) && (regEnumElem.getValue() != null)) {
			Integer elemWidth = regEnumElem.getValue().getVectorLen();
			//System.out.println("--- adding enum=" + regEnumElem.getValue() + " with width=" + elemWidth);
			if ((width == null) && (elemWidth != null)) {
				width = elemWidth;
			}
			else {
				if ((elemWidth != null) && (elemWidth != width)) 
					MsgUtils.errorMessage("non-matching enum value widths in " + this.getFullId());
			}
			enumElements.add(regEnumElem);		
		}
	}


	/** return a child enum element with specified id or null if not found
	 *  @param elemId - id to be found
	 */
	public ModEnumElement findEnumElement(String elemId) {
		for (ModEnumElement enumElement: enumElements) {
			if (enumElement.getId().equals(elemId)) return enumElement;
		}			
		return null;
	}

	/** get enumElements
	 *  @return the enumElements
	 */
	public List<ModEnumElement> getEnumElements() {
		return enumElements;
	}


	/** return width of this enum
	 *  @return the width
	 */
	public Integer getWidth() {
		return width;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((enumElements == null) ? 0 : enumElements.hashCode());
		result = prime * result + ((width == null) ? 0 : width.hashCode());
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
		ModEnum other = (ModEnum) obj;
		if (enumElements == null) {
			if (other.enumElements != null)
				return false;
		} else if (!enumElements.equals(other.enumElements))
			return false;
		if (width == null) {
			if (other.width != null)
				return false;
		} else if (!width.equals(other.width))
			return false;
		return true;
	}

}
