/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved.
 */
package ordt.parameters;

public class ExtStringParameter extends ExtParameter<String> {

	ExtStringParameter(String name, String value) {
		super(name, value);
	}

	@Override
	public void set(String valStr) {
		value = valStr.replace("\"", "");  // remove dquotes
	}

	@Override
	public String toString() {
		return value;
	}
}
