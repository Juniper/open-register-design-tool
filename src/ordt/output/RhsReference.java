/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved. 
 */
package ordt.output;

import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ordt.output.common.MsgUtils;
import ordt.extract.model.ModComponent;
import ordt.extract.model.ModInstance;
import ordt.output.systemverilog.SystemVerilogDefinedOrdtSignals;
import ordt.parameters.Utils;

/** class describing a single reference in a rhs assignment **/
public class RhsReference {
   private String deRef;
   private String instancePath;
   private String[] instancePathElems = new String[0];  
   private int depth;   // ancestor depth from instancePath leaf where assignment occurred
   private boolean wellFormedSignalName = true;  // false if returned signal name is a complex expression 
   private boolean sameAddrmap = true;  // true if this rhs reference is in same addrmap as lhs 
   private boolean userSignal = false;  // true if this rhs reference is a user-defined signal
   
   private static Stack<InstanceProperties> instancePropertyStack;  // pointer to builder active instance path

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
   public boolean isRegRef() {
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

   /** returns true if this rhs reference is in same addrmap as lhs of the assign */
   public boolean isSameAddrmap() {
	   return sameAddrmap;
   }

   /** returns true if this rhs reference is a user-defined signal */
   public boolean isUserSignal() {
	   return userSignal;
   }

   /** set ptr to builder instancePropertyStack */
   public static void setInstancePropertyStack(Stack<InstanceProperties> instancePropertyStack) {
	   RhsReference.instancePropertyStack = instancePropertyStack;
   }
   
   // ----------------- private methods -----------------

   /** extract instance and deref from rawReference */
   private void parseRawReference(String rawReference) {  
	   // extract the instancePath and deref if there is one
	   String rawInstancePath = null;
	   Pattern p = Pattern.compile("^\\s*(\\S+)\\s*->\\s*(\\S+)\\s*$");
	   Matcher m = p.matcher(rawReference);
	   if (m.matches()) {
		   rawInstancePath = m.group(1); 
		   this.deRef = m.group(2);
		   if (this.deRef.equals("nextposedge") || this.deRef.equals("nextnegedge")) wellFormedSignalName = false;
	   }
	   else {
		   rawInstancePath = rawReference; 
	   }
	   this.instancePath = (rawInstancePath != null)? rawInstancePath.replace("[", "_").replace("]", "") : null;  // replace array indices with index suffixes
	   if (instancePath != null) this.instancePathElems = instancePath.split("\\.");
		   
	   //System.out.println("RhsReference parseRawReference: raw=" + rawReference + ", deRef=" + deRef + ", depth=" + depth + ", # instancePathElems=" + instancePathElems.length);
	   //for (int idx=0; idx<instancePathElems.length; idx++)
		   //System.out.println("RhsReference parseRawReference:      instancePathElem=" + instancePathElems[idx]);
	   
	   // now use instance stack to check for a valid reference
	   ModComponent ancModComp = getAssignAncestorComp();   // find assignment ancestor component in model
	   // search for rhs instance in model
	   ModInstance rhsInstance = null;
	   if (ancModComp != null) {
		   List<String> pathList = Utils.pathToList(rawInstancePath, false, false);
		   // first see if ref is for a user defined signal
		   /*if (pathList.size() == 1) {  // TODO enable for ancestor signal accessibility
			   rhsInstance = findSignalInstance(pathList.get(0));
			   if (rhsInstance != null) {
				   userSignal = true;
				   if (hasDeRef()) 
					   MsgUtils.errorMessage("Invalid rhs signal reference in assignment (" + rawReference + ")");  // user signal reference with a dref is not allowed
				   return;
			   }
		   }
		   // if not a signal */
		   
		   // find model instance in scope of the component where assign was made
		   rhsInstance = ancModComp.findInstance(pathList);
		   //System.out.println("RhsReference parseRawReference: rawInstancePath=" + rawInstancePath + ", pathList.size()=" + pathList.size() + ", ancModComp.getId()=" + ancModComp.getId());
		   // check for an addrmap mismatch between lhs and rhs of assign
		   this.sameAddrmap = detectAddrmapMismatch(rawInstancePath, ancModComp);
		   // detect addrmap mismatch between lhs and rhs
		   if (!sameAddrmap) {
			   MsgUtils.warnMessage("Rhs assignment reference " + rawReference + " is in a different addrmap than lhs reference.");
			   //if (this.isRegRef())	MsgUtils.warnMessage("Rhs assignment reference " + rawReference + " is in a different addrmap than lhs reference.");
			   //else	MsgUtils.errorMessage("Rhs assignment reference " + rawReference + " is in a different addrmap than lhs reference.");
		   }
	   }
	   // if no inst found then error
	   if (rhsInstance == null) 
		   MsgUtils.errorExit("Unable to resolve rhs assignment reference " + rawReference);
	   // set signal
	   if (rhsInstance.getRegComp().isSignal()) {
		   //System.out.println("RhsReference parseRawReference: found a signal reference, id=" + rhsInstance.getFullId());
		   userSignal = true;
		   if (hasDeRef()) 
			   MsgUtils.errorMessage("Invalid rhs signal reference in assignment (" + rawReference + ")");  // user signal reference with a dref is not allowed
	   }
   }

// ---- methods referencing active instance stack

   /*private ModInstance findSignalInstance(String string) {
	   // TODO - will allow signals defined in ancestors to be accessible
	   return null;
    }*/

/** return the index in the instance stack where this assignment occurred */
   private int getAssignStackIndex() {
	   return instancePropertyStack.size() - (depth + 1);
   }
   
   /** return the component of a member of the instance stack given a stack index */
   private ModComponent getAssignComponent(int stackIndex) {
	   // if one deeper than stack return the root instanced component
	   ModComponent ancModComp = null;
	   if (stackIndex == -1) {
		   if (!instancePropertyStack.isEmpty()) {
			   InstanceProperties ancestor = instancePropertyStack.get(0);  // get stack root
			   ModInstance ancModInst = ancestor.getExtractInstance();
			   ancModComp = ancModInst.getParent();
		   }
	   }
	   // otherwise return the model instance at assignment depth
	   else if (stackIndex > -1) {
		   InstanceProperties ancestor = instancePropertyStack.get(stackIndex); 
		   ModInstance ancModInst = ancestor.getExtractInstance();
		   ancModComp = ancModInst.getRegComp();
	   }
	   return ancModComp;
   }
 
   /** check for an addrmap mismatch between lhs and rhs of assign - return false on mismatch 
    * @param rawInstancePath - rhs reference path
    * @param ancModComp - component at depth where assign was made
    */
   private boolean detectAddrmapMismatch(String rawInstancePath, ModComponent ancModComp) {
	   // find rhs reference addrmap instance
	   List<String> pathList = Utils.pathToList(rawInstancePath, false, false);
	   ModInstance rhsAddrMapInst = ancModComp.findLastAddrmap(pathList);  
	   // find rhs reference addrmap instance		   
	   ModInstance lhsAddrMapInst = null;  
	   int ancIndex = getAssignStackIndex();  
	   if (ancIndex < 0) ancIndex = 0;
	   for (int idx=ancIndex; idx<instancePropertyStack.size(); idx++) {
		   ModInstance inst = instancePropertyStack.get(idx).getExtractInstance();
		   if (inst.getRegComp().isAddressMap()) lhsAddrMapInst = inst;
	   }
	   // return false if lhs and rhs addrmap instances differ
	   return ( ((rhsAddrMapInst == null) && (lhsAddrMapInst == null)) || ((rhsAddrMapInst != null) && rhsAddrMapInst.equals(lhsAddrMapInst)) );
   }

   /** return the model ancestor where assignment was made using the active instance stack and depth. return null if depth is invalid relative to stack size. */
   private ModComponent getAssignAncestorComp() {
	   int ancIndex = getAssignStackIndex();
	   return getAssignComponent(ancIndex);
   }

   // ----

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
	   String resName = SystemVerilogDefinedOrdtSignals.getResolvedRhsSignalExpression(deRef, instancePath, addPrefix);
	   if (resName != null) return resName;
	   // if not supported flag an error
	   MsgUtils.errorExit("unsupported right hand assignment reference: " + getRawReference());
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
