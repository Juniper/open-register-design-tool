/*
 * Copyright (c) 2017 Juniper Networks, Inc. All rights reserved.
 */
package ordt.output.drvmod.py;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import ordt.output.common.MsgUtils;
import ordt.output.common.OutputLine;

public class PyBaseModClass {
   protected String name;
   protected List<String> parents = new ArrayList<String>();
   protected List<String> children = new ArrayList<String>();  // list of child names
   
   protected List<String> defines = new ArrayList<String>();
   protected List<PyMethod> methods = new ArrayList<PyMethod>();
   
   protected PyMethod activeConstructor = null;   
   protected HashMap<String, PyMethod> taggedMethods = new HashMap<String, PyMethod>();
      
   public PyBaseModClass(String name) {
       this.name = name;	
   }
   
   /** add a parent */
   public void addParent(String parent) {
	   parents.add(parent);
   }
      
   /** add a define */
   public void addDefine(String def) {
       defines.add(def);
   }
   
   /** add a method and return return created PyMethod
    * @param sig - method signature
    */
   public PyMethod addMethod(String sig) {
	   PyMethod newMethod = new PyMethod(sig);
	   methods.add(newMethod);
	   return newMethod;
   }
      
   /** add a constructor method, update activeConstructor, and return created PyMethod 
    * @param sig - method signature
    */
   public PyMethod addConstructor(String sig) {
	   PyMethod newConst = addMethod(sig);
	   activeConstructor = newConst;
	   return newConst;
   }
   
   /** add an init call to the active constructor */
   public void addInitCall(String iCall) {
	   if (activeConstructor == null) {
		   MsgUtils.errorExit("active python constructor is null in addInitCall");
	   }
	   activeConstructor.addInitCall(iCall);
   }

   /** add a statement to the active constructor */
   public void addConstructorStatement(String stmt) {
	   if (activeConstructor == null) {
		   MsgUtils.errorExit("active python constructor is null in addConstructorStatement");
	   }
	   activeConstructor.addStatement(stmt);
   }

   /** add a method to hashmap by tag string so it can be referenced later */
   public void tagMethod(String tag, PyMethod nMethod) {
	   taggedMethods.put(tag, nMethod);
   }

   /** get a method referenced by tag string */
   public PyMethod getTaggedMethod(String tag) {
	   return taggedMethods.get(tag);
   }

   // --------------------------- nested classes -----------------------------
   
   public class PyMethod {
	   private String signature;
	   private List<String> initCalls = new ArrayList<String>();
	   private List<String> statements = new ArrayList<String>();
	   
	   PyMethod(String signature) {
		   this.signature = signature;
	   }

	   /** add an initialization string */
	   public void addInitCall(String iCall) {
		   initCalls.add(iCall);
	   }
	   
	   /** add a method statement */
	   public void addStatement(String stmt) {
		   statements.add(stmt);
	   }

	   /** returns code for this method */
	   public Collection<? extends OutputLine> getPy(Integer indentLvl) {
		   List<OutputLine> outList = new ArrayList<OutputLine>();
		   outList.add(new OutputLine(indentLvl , "def " + signature.trim() + ":"));
		   indentLvl += 2;
		   // add constructor init calls if needed
		   if (!initCalls.isEmpty()) {
			  Iterator<String> it = initCalls.iterator();
			  while (it.hasNext()) {
				  outList.add(new OutputLine(indentLvl , it.next())); 
			  }
		   }
		   for (String stmt: statements)
			   outList.add(new OutputLine(indentLvl , stmt));
		   indentLvl -= 2;
		   outList.add(new OutputLine(indentLvl , ""));
		   return outList;
	   }
   }
   

   /** return true if this model element has children */
   public boolean hasChildren() {
	   return !children.isEmpty();
   }

   // --------------------------- output generation methods -----------------------------

   /** generate py output lines for this class
    * @return list of OutputLine
    */
   public List<OutputLine> genPy() {
	   List<OutputLine> outList = new ArrayList<OutputLine>();
	   int indentLvl = 0;
	   
	   // class define statement 
	   String parentStr = "";
	   for (String par : parents) {
		   String prefix = parentStr.isEmpty()? "(" : ", ";
		   parentStr += prefix + par;
	   }
	   if (!parentStr.isEmpty()) parentStr += ")";
	   
	   outList.add(new OutputLine(indentLvl , "class " + name + parentStr + ":"));
	   indentLvl += 2;

	   // add defs
	   for (String stmt: defines) {
		   outList.add(new OutputLine(indentLvl , stmt)); 
	   }
	   
	   //add methods
	   outList.add(new OutputLine(indentLvl , ""));
	   //System.out.println("PyBaseModClass genPy: methods=" + methods.size());
	   for (PyMethod method: methods) {
			outList.addAll(method.getPy(indentLvl));
	   }
	   
	   // close class
	   indentLvl -= 2;
	   outList.add(new OutputLine(indentLvl , ""));
	   	   
	   return outList;
   }
   
}
