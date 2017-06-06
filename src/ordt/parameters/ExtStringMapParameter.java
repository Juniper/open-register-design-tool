/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved.
 */
package ordt.parameters;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExtStringMapParameter extends ExtParameter<HashMap<String,String>> {

	ExtStringMapParameter(String name, HashMap<String,String> value) {
		super(name, value);
	}

	@Override
	public void set(String valStr) {
		String valArray [] = valStr.replace("\"", "").split("\\s*,\\s*");
		for(int idx=0; idx<valArray.length; idx++) {
			//System.out.println("val[" + idx + "]=" + valArray[idx]);
			Pattern p = Pattern.compile("^\\s*(\\S+)\\s*=\\s*(\\S+)\\s*$");
			Matcher m = p.matcher(valArray[idx]);
			if (m.matches()) {
				String parm = m.group(1);
				String val = m.group(2);
				//System.out.println(parm + ":" + val);
				value.put(parm, val);		
			}
		}
	}

	@Override
	public String toString() {
		return value.toString();
	}

}
