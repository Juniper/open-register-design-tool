/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved.
 */
package ordt.annotate;

import ordt.extract.model.ModComponent.CompType;

public class AnnotateNullCommand extends AnnotateCommand {

	public AnnotateNullCommand(CompType commandTarget, boolean pathUsesComponents,	String pathStr) {
		super(commandTarget, pathUsesComponents, pathStr);
	}

	/** return string description of this cmd */
	@Override
	public String getSignature() {
		return "null annotation: " + super.getSignature();
	}

}
