/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved.
 */
package ordt.output.systemverilog;

/** class to hold basic info (name, idx, size) for verilog signals */
public class SystemVerilogSignal {
	protected String name;
	protected int lowIndex;
	protected int size;
	/**
	 * @param name
	 * @param array
	 */
	public SystemVerilogSignal(String name, int lowIndex, int size) {
		this.name = name;
		this.lowIndex = lowIndex;
		this.size = size;
	}
	
	/** return the name used for definitions (includes) prefixed array string */
	public String getDefName() {
		return SystemVerilogBuilder.genDefArrayString(lowIndex, size) + name;
	}
	
	/** return the array string used for definitions (includes) prefixed array string */
	public String getDefArray() {
		return SystemVerilogBuilder.genDefArrayString(lowIndex, size);
	}
	
	/** return the name of the signal */
	public String getName () {
		return name;
	}
	/** get lowIndex
	 *  @return the lowIndex
	 */
	public int getLowIndex() {
		return lowIndex;
	}

	/** get size
	 *  @return the size
	 */
	public int getSize() {
		return size;
	}

	/** by default return definition format */
	public String toString () {
		return SystemVerilogBuilder.genDefArrayString(lowIndex, size) + name;
	}
}
