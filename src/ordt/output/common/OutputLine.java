/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved.
 */
package ordt.output.common;

import java.util.HashMap;

/** class defining a line of output
 */
public class OutputLine {
	private int indent;
	private String line;
	private Integer specialType = null;  // use to indicate overidden OutputLine class for special processsing (null = no special type defined)
	private static final int DEFAULT_SPECIAL_TYPE = 0;
	private boolean hasTextReplacements = false;  // indication that test replacements should be processed in this line
	//private static List<ReplacePair> replacements = new ArrayList<ReplacePair>();
	private static HashMap<String, String> replacements = new HashMap<String, String>();
	
	/**
	 * @param indent
	 * @param line
	 */
	public OutputLine(int indent, String line) {
		this.indent = indent;
		this.line = line;
	}

	/** set line value */
	public void setLine(String line) {
		this.line = line;			
	}

	/** add a suffix string to a line */
	public void addLineSuffix(String suffix) {
		setLine(getLine() + suffix);
	}

	/** get indent
	 *  @return the indent
	 */
	public int getIndent() {
		return indent;
	}

	/** set indent
	 *  @param indent the indent to set
	 */
	public void setIndent(int indent) {
		this.indent = indent;
	}

	/** return line string with text values replaced if specified
	 *  @return the line
	 */
	public String getLine() {
		if (hasTextReplacements) return replaceText();
		return line;
	}

	/** return line string with text values in replacements list replaced */
	private String replaceText() {
		String retStr = line;
		for (String key : replacements.keySet()) {
			retStr = retStr.replaceAll(key, replacements.get(key));
		}
		return retStr;
	}
	
	/** set text replace pairs */
	public static void setReplacements(HashMap<String, String> newReplacements) {
		if (newReplacements != null) replacements = newReplacements;
	}
	
	/** get special
	 *  @return the special
	 */
	public boolean isSpecial() {
		return (specialType != null);
	}

	public Integer getSpecialType() {
		return specialType;
	}

	public void setSpecialType(Integer specialType) {
		this.specialType = specialType;
	}

	/** set specialType to default special value
	 */
	public void setSpecialType() {
		setSpecialType(DEFAULT_SPECIAL_TYPE);
	}

	public boolean hasTextReplacements() {
		return hasTextReplacements;
	}

	public void setHasTextReplacements(boolean hasTextReplacements) {
		this.hasTextReplacements = hasTextReplacements;
	}
	
    public static void main(String[] args) throws Exception {
    	HashMap<String, String> repStrings = new HashMap<String, String>();
    	repStrings.put("line", "booger");
    	repStrings.put("is a", "isn't a");
    	
    	OutputLine line = new OutputLine(0, "this is a line;");
    	line.setHasTextReplacements(true);
    	OutputLine.setReplacements(repStrings);
    	System.out.println(line.getLine());
    }

}
