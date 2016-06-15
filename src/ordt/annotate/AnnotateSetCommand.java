/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved.
 */
package ordt.annotate;

import ordt.extract.ModComponent;
import ordt.extract.ModInstance;
import ordt.extract.ModComponent.CompType;

public class AnnotateSetCommand extends AnnotateCommand {
	   private String property;
	   private String value;

	/** create a set_property command 
	 * 
	 * @param commandTarget - component type to match (REG, FIELD, etc) or null for all
	 * @param pathUsesComponents - true if path is component names, false if path is inst names
	 * @param pathStr - path string / if only one element, all levels will be searched for specified name
	 * @param property - property name to be set
	 * @param value - property value to be set
	 */
	public AnnotateSetCommand(CompType commandTarget, boolean pathUsesComponents, String pathStr, String property, String value) {
		super(commandTarget, pathUsesComponents, pathStr);
        this.property = property;
        this.value = value;
        //System.out.println("AnnotateSetCommand: prop=" + property + ", value=" + value);
	}

	public String getProperty() {
		return property;
	}

	public String getValue() {
		return value;
	}

	/** return string description of this cmd */
	@Override
	public String getSignature() {
		return "set property: " + super.getSignature() + ", property=" + property + ", value=" + value;
	}

	/** process a set_property command on component */
	@Override
	public void processComponent(ModComponent modComponent) {
		changeCount++; // bump the change count
		modComponent.setProperty(property, value, 0);  // set the specified property
	}
	   
	/** process a set_property command on instance */
	@Override
	public void processInstance(ModInstance modInstance) {
		//System.out.println("AnnotateSetCommand processInstance: setting p=" + property + ", v=" + value + " in inst=" + modInstance.getId());
		changeCount++; // bump the change count
		modInstance.setProperty(property, value, 0);  // set the specified property
	}

}
