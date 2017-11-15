/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved. 
 */
package ordt.output.verilog;

import ordt.extract.RegModelIntf;
import ordt.output.systemverilog.SystemVerilogTestBuilder;

public class VerilogTestBuilder extends SystemVerilogTestBuilder {

	public VerilogTestBuilder(RegModelIntf model) {
		super(model);
		setLegacyVerilog(true);  // rtl does not use systemverilog constructs
	}

	/**
	 * @param parentBuilder
	 */
	public VerilogTestBuilder(SystemVerilogTestBuilder parentBuilder, boolean isTestModule) {
		super(parentBuilder, isTestModule);
		setLegacyVerilog(true);  // rtl does not use systemverilog constructs
	}
	
}
