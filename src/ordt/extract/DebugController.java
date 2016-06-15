/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved.
 */
package ordt.extract;

public abstract class DebugController {

    /** return true if debug settings will be used */
	abstract public boolean isActive();

	/** add annotations to the design model before output generation, eg
	 *   AnnotateShowCommand cmd = new AnnotateShowCommand(CompType.REG, false, "sram", true);  // comp path=false, show instance comp=true
     *   AnnotateSetCommand cmd = new AnnotateSetCommand(CompType.REG, false, "sram", "external", "DEFAULT");  // comp path=false
     *   ExtParameters.getAnnotations().add(cmd);
	 */
	abstract public void addAnnotations();

	/** override jrdl input/output info */
	abstract public void setRunInfo();

}
