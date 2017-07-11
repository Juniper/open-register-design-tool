/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved.
 */
package ordt.annotate;

import ordt.extract.model.ModComponent;
import ordt.extract.model.ModInstance;
import ordt.extract.model.ModComponent.CompType;

public class AnnotateShowCommand extends AnnotateCommand {
	private boolean showInstanceComponent = false;
	
	/** create a set_property command 
	 * 
	 * @param commandTarget - component type to match (REG, FIELD, etc) or null for all
	 * @param pathUsesComponents - true if path is component names, false if path is inst names
	 * @param pathStr - path string / if only one element, all levels will be searched for specified name
	 * @param showInstanceComponent - if true, instance of each matching instance will also be displayed
	 */
	public AnnotateShowCommand(CompType commandTarget, boolean pathUsesComponents, String pathStr, boolean showInstanceComponent) {
		super(commandTarget, pathUsesComponents, pathStr);
		this.showInstanceComponent = showInstanceComponent;
	}

	/** return string description of this cmd */
	@Override
	public String getSignature() {
		return "display: " + super.getSignature() + ", show component info with instances=" + showInstanceComponent;
	}

	/** process a show command on component */
	@Override
	public void processComponent(ModComponent modComponent) {
		changeCount++; // bump the change count
		modComponent.display();  // show the specified component info
	}
	   
	/** process a show command on instance */
	@Override
	public void processInstance(ModInstance modInstance) {
		//System.out.println("AnnotateSetCommand processInstance: setting p=" + property + ", v=" + value + " in inst=" + modInstance.getId());
		changeCount++; // bump the change count
		modInstance.display(showInstanceComponent);  // show the specified instance info
	}

}
