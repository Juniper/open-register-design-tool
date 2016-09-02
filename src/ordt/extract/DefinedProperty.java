package ordt.extract;

import ordt.extract.DefinedProperties.DefinedPropertyType;

public class DefinedProperty {
	private String name;
	private DefinedPropertyType type;
	private String defaultValue;
	private int usage;   // components allowed to use this property
	private boolean hidden = false;
	
	DefinedProperty(String name, DefinedPropertyType type, String defaultValue, int usage, boolean hidden) {
		super();
		this.name = name;
		this.type = type;
		this.defaultValue = defaultValue;
		this.usage = usage;
		this.hidden = hidden;
	}
}
