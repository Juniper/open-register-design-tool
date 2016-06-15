/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved.
 */
package ordt.parameters;

public class ExtBooleanParameter extends ExtParameter<Boolean> {

	ExtBooleanParameter(String name, Boolean value) {
		super(name, value);
	}

	@Override
	public void set(String valStr) {
		this.value = valStr.equals("true");
	}

	@Override
	public String toString() {
		return value.toString();
	}

}
