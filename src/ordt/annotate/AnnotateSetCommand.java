/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved.
 */
package ordt.annotate;

import ordt.extract.model.ModComponent;
import ordt.extract.model.ModInstance;
import ordt.extract.model.ModComponent.CompType;

public class AnnotateSetCommand extends AnnotateCommand {
	   private String property;
	   private String value;
	   private boolean isDefault;

	/** create a set_property command 
	 * 
	 * @param commandTarget - component type to match (REG, FIELD, etc) or null for all
	 * @param pathUsesComponents - true if path is component names, false if path is inst names
	 * @param pathStr - path string / if only one element, all levels will be searched for specified name
	 * @param isDefault - true if this is a default property
	 * @param property - property name to be set
	 * @param value - property value to be set
	 */
	public AnnotateSetCommand(CompType commandTarget, boolean pathUsesComponents, String pathStr, boolean isDefault, String property, String value) {
		super(commandTarget, pathUsesComponents, pathStr);
        this.property = property;
        this.value = value;
        this.isDefault = isDefault;
        //System.out.println("AnnotateSetCommand: prop=" + property + ", value=" + value + ", isDefault=" + isDefault);
	}

	public String getProperty() {
		return property;
	}

	public String getValue() {
		return value;
	}

	public boolean isDefault() {
		return isDefault;
	}

	/** return string description of this cmd */
	@Override
	public String getSignature() {
		return "set property: " + super.getSignature() + ", property=" + property + ", value=" + value + ", isDefault=" + isDefault;
	}

	/** process a set_property command on component */
	@Override
	public void processComponent(ModComponent modComponent) {
		changeCount++; // bump the change count
		// since annotate occurs after comp info is cascaded, set property on child instances
		if (isDefault()) {
			for (ModInstance inst: modComponent.getInstancesOf())
				inst.setDefaultProperty(property, value);  // set the specified property
		}
		else {
			for (ModInstance inst: modComponent.getInstancesOf())
				inst.setProperty(property, value, 0);  // set the specified property
		}
		//System.out.println("AnnotateSetCommand processComponent: count=" + changeCount + ", " + getSignature());
	}
	   
	/** process a set_property command on instance */
	@Override
	public void processInstance(ModInstance modInstance) {
		//System.out.println("AnnotateSetCommand processInstance: setting p=" + property + ", v=" + value + " in inst=" + modInstance.getId());
		changeCount++; // bump the change count
		if (isDefault()) modInstance.setDefaultProperty(property, value);  // set the specified property
		else modInstance.setProperty(property, value, 0);  // set the specified property
	}

}
