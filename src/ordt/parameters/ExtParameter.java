/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved.
 */
package ordt.parameters;

public abstract class ExtParameter<T> {
	String name;
	T value;
	ExtParameter(String name, T value) {
		this.name = name;
		this.value = value;
	}
	
	abstract public void set(String valStr); 
	
	public void set(T value) {
		this.value = value;
	}
	
	public T get() {
		return this.value;
	}
	
	abstract public String toString();

}
