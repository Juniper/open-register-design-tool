/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved.
 */
package ordt.parameters;

import java.util.List;

public class ExtStringListParameter extends ExtParameter<List<String>> {

	ExtStringListParameter(String name, List<String> value) {
		super(name, value);
	}

	@Override
	public void set(String valStr) {
		value.add(valStr.replace("\"", ""));  // remove dquotes and add		
	}

	@Override
	public String toString() {
		return value.toString();
	}

}
