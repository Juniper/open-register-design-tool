/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved. 
 */
package ordt.output.verilog;

import ordt.extract.RegModelIntf;
import ordt.output.systemverilog.SystemVerilogBuilder;

public class VerilogBuilder extends SystemVerilogBuilder {

	public VerilogBuilder(RegModelIntf model) {
		super(model);
		setLegacyVerilog(true);  // rtl does not use systemverilog constructs
	}

	/**
	 * @param parentBuilder
	 */
	public VerilogBuilder(SystemVerilogBuilder parentBuilder, boolean isTestModule) {
		super(parentBuilder, isTestModule);
		setLegacyVerilog(true);  // rtl does not use systemverilog constructs
	}
	
}
