/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved. 
 */
package ordt.output;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ordt.extract.Ordt;
import ordt.output.systemverilog.SystemVerilogDefinedSignals;

/** class describing a single reference in a rhs assignment **/
public class RhsReference {
   private String deRef;
   private String instancePath;
   private String[] instancePathElems = new String[0];  
   private int depth;   // ancestor depth from instancePath leaf where assignment occurred
   private boolean wellFormedSignalName = true;  // false if returned signal name is a complex expression 

   public RhsReference(String rawReference, int depth) {
	   this.depth = depth;
	   parseRawReference(rawReference);  // extract instance and deref  
   }
   
   /** return assign depth (depth from instancePath leaf in assignment stmt determined by lhs hierarchy)
    */
   public int getDepth() {
	   return depth;
   }
   
   /** get full raw reference string (instance plus any deref)  
    *  @return the rawReference
    */
   public String getRawReference() {
	   return hasDeRef() ? instancePath + "->" + deRef : instancePath;
   }
   
   /** returns true if this rhsref has a deref
    *  @return the depth
    */
   public boolean hasDeRef() {
	   return (deRef != null);
   }
   
   /** returns true if this rhsref has deref matching specified string
    *  @return the depth
    */
   public boolean hasDeRef(String ref) {
	   return hasDeRef() && deRef.equals(ref);
   }
   
   /** returns deref */
   public String getDeRef() {
	   return deRef;
   }

   /** build a reference name based on current inst hierarchy and input string */  
   public  String getReferenceName(InstanceProperties instProperties, boolean getRelativePath) {
	   if (getRelativePath) return getRelativeReferenceName(instProperties);
	   return getFullReferenceName(instProperties);
   }
   
   /** returns number of elements in reference path */
   public int getPathCount() {
	   if (instancePath == null) return 0;
	   return instancePathElems.length;
   }
   
   /** returns element  elements in reference path */
   public String getPathElement(int index) {
	   if (instancePath == null) return null;
	   if ((index < 0) || (index > instancePathElems.length - 1)) return null;
	   return instancePathElems[index];
   }

   /** return the name of the field in this reference or null if none */
   public String getFieldName() {
	   // return null if this isn't a field reference  
	   if (isRegRef()) return null;
	   return getPathElement(instancePathElems.length - 1); // return last path elem
   }

   /** return the name of the register in this reference or null if none */
   public String getRegName() {
	   // return null if this isn't a field reference  
	   if (isRegRef()) return getPathElement(instancePathElems.length - 1); // return last path elem
	   return getPathElement(instancePathElems.length - 2); // return next to last last path elem
   }

   /** return true if this is a register reference path (no field) */
   private boolean isRegRef() {
	   return hasDeRef() && (deRef.equals("intr") || deRef.equals("halt"));
   }

   /** return number of reg set elements in path */
   public int getRegSetPathLength() {
	   int rsLen = 0; 
	   if (isRegRef()) rsLen = getPathCount() - 1; // remove reg
	   else rsLen = getPathCount() - 2;  // remove reg and field
	   if (rsLen < 0) rsLen = 0;
	   return rsLen;
   }

   /** return the reg set portion of the instance path */
   public String getRegSetPath() {
	   int rsLen = getRegSetPathLength();
	   if (rsLen < 1) return ""; // no reg sets in path
	    StringBuffer sb = new StringBuffer();
	    for (int idx=0; idx < rsLen; idx++) {  
	        if (idx != 0) sb.append('.');
	        sb.append(instancePathElems[idx]);
	    }
	    return sb.toString();
   }	
   
   /** get raw instancePath of reference (can contain field wildcards)
    *  @return the instancePath
    */
   public String getInstancePath() {
	   return instancePath;
   }
   
   /** get raw instancePath of reference with field wildcards resolved to id of specified field
    *  @param field - field instance whose id will replace wildcard
    *  @return the instancePath
    */
   public String getResolvedFieldWildcardPath(InstanceProperties field) {
	   String retStr = instancePath;
	   String fieldId = field.getId();
	   if (retStr.contains("*")) retStr = retStr.replace("*", fieldId);
	   return retStr;
   }

   /** returns false if returned signal name is a complex expression */ 
   public boolean isWellFormedSignalName() {
	   return wellFormedSignalName;
   }
   
   // ----------------- private methods -----------------

   
   /** extract instance and deref from rawReference */
   private void parseRawReference(String rawReference) {  
	   // extract the deref if there is one
	   Pattern p = Pattern.compile("^\\s*(\\S+)\\s*->\\s*(\\S+)\\s*$");
	   Matcher m = p.matcher(rawReference);
	   if (m.matches()) {
			this.instancePath = m.group(1); 
			this.deRef = m.group(2);
			if (this.deRef.equals("nextposedge") || this.deRef.equals("nextnegedge")) wellFormedSignalName = false;
	   }
	   else {
			this.instancePath = rawReference; 
	   }
	   if (instancePath != null) this.instancePathElems = instancePath.split("\\.");
   }

   /** return relative reference if reg string is of register.field format. else return null  
    *    this is used by uvmregs builder to generate reg+field references that will use common paths of lhs instance */
   private String getRelativeReferenceName(InstanceProperties instProperties) {
	   
	   // resolve field wildcards and dots to create ref name
	   String instPath = getResolvedFieldWildcardPath(instProperties).replace('.','_');  // clean up instance path
	   
	   // get signal name based on deRef with no prefix appended
	   String newName = getResolvedSignalName(instPath, false);
	   
	   //System.out.println("RhsReference: getRelativeReferenceName, raw=" + rawReference + ", name=" + newName);
	   return newName;
   }

   /** build a reference name based on current inst hierarchy and input string - only valid if ref is wellformed */  
   private  String getFullReferenceName(InstanceProperties instProperties) {
	   //System.out.println("------RhsReference: getFullReferenceName, raw=" + rawReference + ", d=" + depth + ", instancepath=" + instancePath);
	   //System.out.println("      RhsReference: getFullReferenceName, fieldInstPath=" + instProperties.getInstancePath());
	   
	   // get basepath from current instance path using depth
	   String basePath = getBasePath(instProperties.getInstancePath(), depth);
	   String newPath = basePath + getResolvedFieldWildcardPath(instProperties);
	   newPath = newPath.replace('.','_');  // clean up instance path
	   /*if (!newPath.equals(instPath)) {
		   System.out.println("      RhsReference: getFullReferenceName, old path=" + instPath);
		   System.out.println("      RhsReference: getFullReferenceName, new path=" + newPath);
	   }*/
	   
	   // get the full signal name based on deRef
	   String newName = getResolvedSignalName(newPath, true);
	   
	   //System.out.println("RhsReference: new name=" + newName);
	   return newName;
   }

   /** extract path prefix by removing number of levels specified by depth */
   private String getBasePath(String instancePath, int depth) {
	   // if depth is 0, just return the inst path
	   if (depth < 1) return instancePath + ".";
	   // get the number of levels in the instance path
	   int instLevels = instancePath.length() - instancePath.replace(".", "").length() + 1;
	   // if depth is greater than or equal to levels, return null string
	   if (depth >= instLevels) return "";
	   // otherwise remove leaf levels
	   Pattern p = Pattern.compile("^(\\S+)(\\.[^\\.]+){" + depth + "}$");
	   Matcher m = p.matcher(instancePath);
	   if (m.matches()) {
			return m.group(1) + "."; 
	   }
	   return instancePath;
   }

   /** return the signal name according to deRef type of this RhsReference */
   private String getResolvedSignalName(String instancePath, boolean addPrefix) {
	   // no deref so signal name is field itself (or signal)
	   String resName = SystemVerilogDefinedSignals.getResolvedRhsSignalExpression(deRef, instancePath, addPrefix);
	   if (resName != null) return resName;
	   // if not supported flag an error
	   Ordt.errorExit("unsupported right hand assignment reference: " + getRawReference());
	   return null;
   }

   public static void main(String[] args) {
	   RhsReference ref = new RhsReference("rhs3.rhs2.rhs1",3);
	   // test base path extraction 
	   /*
	   String inst = "";
	   for (int d=0; d<4; d++)  System.out.println("inst path=" + inst + ", d=" + d + ", base=" + ref.getBasePath(inst,d));
	    inst = "level1";
	   for (int d=0; d<4; d++)  System.out.println("inst path=" + inst + ", d=" + d + ", base=" + ref.getBasePath(inst,d));
	    inst = "level2.level1";
	   for (int d=0; d<4; d++)  System.out.println("inst path=" + inst + ", d=" + d + ", base=" + ref.getBasePath(inst,d));
	    inst = "level3.level2.level1";
	   for (int d=0; d<4; d++)  System.out.println("inst path=" + inst + ", d=" + d + ", base=" + ref.getBasePath(inst,d));
	    inst = "level4.level3.level2.level1";
	   for (int d=0; d<4; d++)  System.out.println("inst path=" + inst + ", d=" + d + ", base=" + ref.getBasePath(inst,d));
	   */
	   //ref = new RhsReference("",2);
	   //ref = new RhsReference("rhs1->intr",2);
	   ref = new RhsReference("rhs3.rhs2.rhs1->intr",2);
	   //for (int d=0; d<4; d++) 
	   //   System.out.println("path=" + ref.getInstancePath() + ", idx=" + d + ", path n=" + ref.getPathCount() + ", elem=" + ref.getPathElement(d));
	   System.out.println("path=" + ref.getInstancePath() + ", path len=" + ref.getPathCount() + ", reg=" + ref.getRegName() + ", field=" + ref.getFieldName() + ", rs len=" + ref.getRegSetPathLength() + ", rs=" + ref.getRegSetPath());
   }


}
