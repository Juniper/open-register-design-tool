/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved.
 */
package ordt.annotate;

import java.util.ArrayList;
import java.util.List;

import ordt.extract.model.ModComponent;
import ordt.extract.model.ModInstance;
import ordt.extract.model.ModComponent.CompType;

public class AnnotateCommand {

   private CompType commandTarget;
   private boolean pathUsesComponents = false;
   private List<String> path = new ArrayList<String> ();
   protected int changeCount = 0;  // maintain count of annotated model elements
   
   public AnnotateCommand(CompType commandTarget, boolean pathUsesComponents, String pathStr) {
		this.commandTarget = commandTarget;
		this.pathUsesComponents = pathUsesComponents;
		String pathArray [] = pathStr.split("\\.");
		for (int idx = 0; idx < pathArray.length; idx++) path.add(pathArray[idx]);
        //System.out.println("AnnotateCommand: targ=" + commandTarget + ", comps=" + pathUsesComponents +
        //		", path=" + pathStr + ", path.size()=" + path.size() );
	}

   public CompType getCommandTarget() {
	   return commandTarget;
   }

   public boolean pathUsesComponents() {
	   return pathUsesComponents;
   }
   
   /** return count of model elements processed by this command */
   public int getChangeCount() {
	   return changeCount;
   }

   /** return string description of this cmd */
   public String getSignature() {
	   String comps = pathUsesComponents? "components" : "instances";
	   return "target=" + commandTarget + ", " + comps +
       		" " + getPathStr();
   }

   /** check for name/target match and process component - sets current pathLevel and match state for this cmd 
    * - returns true is no child elements need to be processed */
   public boolean processComponent(ModComponent modComponent, int pathLevel) {
	   boolean pathElementMatched = false;  // detect matching path element name
	   // only process if name matches
	   String currentElem = getCurrentPathElement(pathLevel);
	   //System.out.println("AnnotateCommand processComponent: " + modComponent.getFullId() + ", pathlevel=" + pathLevel + ", currentElem=" + currentElem);
	   if ("*".equals(currentElem) || modComponent.getId().equals(currentElem)) {
		   pathElementMatched = true;
		   // only process if target matches and last path or singleton
		   boolean targetMatch = (commandTarget == null) || (commandTarget == modComponent.getCompType()) ||
				   ((commandTarget == ModComponent.CompType.REGSET) && (ModComponent.CompType.ADDRMAP == modComponent.getCompType()));  // also check ADDRMAPs if looking for REGSET
		   if (targetMatch && (isPathEnd(pathLevel) || isSingletonPath() || isDoubleStarLeaf(pathLevel))) {
			   processComponent(modComponent);
		   }   
	   }
	   return isDone(pathLevel, pathElementMatched);
   }
   
   /** process component - overridden by child command classes */
   public void processComponent(ModComponent modComponent) {
		changeCount++; // bump the change count
   }

   /** check for name/target match and process instance - sets current pathLevel and match state for this cmd 
    *  - returns true is no child elements need to be processed */
   public boolean processInstance(ModInstance modInstance, int pathLevel) {
	   boolean pathElementMatched = false;  // detect matching path element name
	   // only process if name matches
	   String currentElem = getCurrentPathElement(pathLevel);
	   if ("*".equals(currentElem) || modInstance.getId().equals(currentElem)) {
		   pathElementMatched = true;
		   // only process if target matches and last path or singleton
		   ModComponent comp = modInstance.getRegComp();
		   boolean targetMatch = (commandTarget == null) || (commandTarget == comp.getCompType()) ||
				   ((commandTarget == ModComponent.CompType.REGSET) && (ModComponent.CompType.ADDRMAP == comp.getCompType()));  // also check ADDRMAPs if looking for REGSET
		   if (targetMatch && (isPathEnd(pathLevel) || isSingletonPath() || isDoubleStarLeaf(pathLevel))) {
			   //System.out.println("AnnotateCommand processInstance: target=" + commandTarget + ", comp type=" + comp.getCompType());
			   //System.out.println("AnnotateCommand processInstance: currentElem=" + currentElem + ", mod id=" + modInstance.getId());
			   processInstance(modInstance);
		   }   
	   }
	   return isDone(pathLevel, pathElementMatched);
   }
   
   /** process instance - overridden by child command classes */
   public void processInstance(ModInstance modInstance) {
	   changeCount++; // bump the change count
   }

   // ------- private methods
   
   /** return the element in path referenced by pathLevel or null if an invalid pathLevel */
   private String getCurrentPathElement(int pathLevel) {
	   if (isDoubleStarLeaf(pathLevel)) return "*";  // if current pathLevel is at/beyond path end and last elem is double star, treat all sublevels as wildcard
	   if (isSingletonPath()) return path.get(0);  // return first element if a singleton  
	   if (pathLevelInvalid(pathLevel)) return null;  // return null if invalid index
	   return path.get(pathLevel);
   }

   /** return catenated instance string from path array */
   private String getPathStr() {
	   String retStr = "";
	   for (String str : path) {
		   String prefix = retStr.isEmpty()? "" : ".";
		   retStr += prefix + str;
	   }
	   return retStr;
   }

   /** return true if command has no child levels to be processed (after current level) */
   private boolean isDone(int pathLevel, boolean pathElementMatched) {
	   if (isDoubleStarLeaf(pathLevel)) return false;  // process all children if path ends with **
	   if (isSingletonPath()) return false;  // process all children if a singleton
	   if (!pathElementMatched) return true;  // if name mismatch we're done
	   if (isPathEnd(pathLevel) || pathLevelInvalid(pathLevel)) return true;  // we're done if current is path end or invalid index
	   return false;
   }
   
   /** retun true if processing last element in path */
   private boolean isPathEnd(int pathLevel) {
	   return pathLevel == path.size() - 1;
   }
   
   /** retun true if only a single element in path */
   private boolean isSingletonPath() {
	   return path.size() == 1;
   }
   
   /** return true if pathLevel is not in valid range */
   private boolean pathLevelInvalid(int pathLevel) {
	   return (pathLevel<0) || (pathLevel>path.size()-1);
   }
   
   /** return true if end path element is doublestar and pathLevel is at/after end */
   private boolean isDoubleStarLeaf(int pathLevel) {
	   int lastIdx = path.size()-1;
	   if (lastIdx < 0) return false;
	   String lastElem = path.get(lastIdx);
	   if ((pathLevel >= lastIdx) && "**".equals(lastElem)) return true;
	   return false;
   }

}
