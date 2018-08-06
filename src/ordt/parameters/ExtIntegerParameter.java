/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved.
 */
package ordt.parameters;

import ordt.output.common.MsgUtils;

public class ExtIntegerParameter extends ExtParameter<Integer> {

	ExtIntegerParameter(String name, Integer value) {
		super(name, value);
	}

	@Override
	public void set(String valStr) {
		Integer intval = Utils.numStrToInteger(valStr);
		if (intval != null) {
			this.value = intval;
			//System.out.println("ExtParameters.ExtIntegerParameter : " + name + " = " + valStr + ", resolved to " + intval);
		} 
		else MsgUtils.errorMessage("invalid " + name + " parameter value specified (" + value + ").");
	}

	@Override
	public String toString() {
		return value.toString();
	}

}
