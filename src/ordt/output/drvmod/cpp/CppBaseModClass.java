/*
 * Copyright (c) 2017 Juniper Networks, Inc. All rights reserved.
 */
package ordt.output.drvmod.cpp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ordt.extract.Ordt;
import ordt.output.OutputLine;

public class CppBaseModClass {
   protected String name;
   protected List<String> parents = new ArrayList<String>();
   protected List<String> classDependencies = new ArrayList<String>();
   protected List<String> includes = new ArrayList<String>();
   protected List<String> children = new ArrayList<String>();  // list of child names
   
   protected List<String> privateDefines = new ArrayList<String>();
   protected List<CppMethod> privateMethods = new ArrayList<CppMethod>();
   protected List<String> protectedDefines = new ArrayList<String>();
   protected List<CppMethod> protectedMethods = new ArrayList<CppMethod>();
   protected List<String> publicDefines = new ArrayList<String>();
   protected List<CppMethod> publicMethods = new ArrayList<CppMethod>();
   
   protected CppMethod activeConstructor = null;   
   protected HashMap<String, CppMethod> taggedMethods = new HashMap<String, CppMethod>();
   
   public enum Vis {PUBLIC, PROTECTED, PRIVATE}
   
   public CppBaseModClass(String name) {
       this.name = name;	
   }

   // -----------------------------
   
   /** add a parent */
   public void addParent(String parent) {
	   parents.add(parent);
   }
   
   /** add a forward dependency */
   public void addClassDependency(String dep) {
	   classDependencies.add(dep);
   }
   
   /** add an include */
   public void addInclude(String inc) {
	   includes.add(inc);
   }
   
   /** add a define */
   public void addDefine(Vis vis, String def) {
	   switch (vis) {
	   case PRIVATE:
		   privateDefines.add(def); break;
	   case PROTECTED:
		   protectedDefines.add(def); break;
	   case PUBLIC:
		   publicDefines.add(def); break;
	   default:
		   break;
	   }   
   }
   
   /** add a method and return return created CppMethod
    * @param vis - method visibility
    * @param isTemplate - true is method is a template
    * @param sig - method signature
    */
   public CppMethod addMethod(Vis vis, boolean isTemplate, String sig) {
	   CppMethod newMethod = new CppMethod(isTemplate, sig);
	   switch (vis) {
	   case PRIVATE:
		   privateMethods.add(newMethod); break;
	   case PROTECTED:
		   protectedMethods.add(newMethod); break;
	   case PUBLIC:
		   publicMethods.add(newMethod); break;
	   default:
		   break;
	   }   
	   return newMethod;
   }
   
   /** add a non-template method and return return created CppMethod
    * @param vis - method visibility
    * @param sig - method signature
    */
   public CppMethod addMethod(Vis vis, String sig) {
	   return addMethod(vis, false, sig);
   }


   /** add a template method and return return created CppMethod
    * @param vis - method visibility
    * @param sig - method signature
    */
   public CppMethod addTemplateMethod(Vis vis, String sig) {
	   return addMethod(vis, true, sig);
   }
   
   /** add a constructor method, update activeConstructor, and return created CppMethod 
    * @param vis - method visibility
    * @param sig - method signature
    */
   public CppMethod addConstructor(Vis vis, String sig) {
	   CppMethod newConst = addMethod(vis, false, sig);
	   activeConstructor = newConst;
	   return newConst;
   }
   
   /** add an init call to the active constructor */
   public void addInitCall(String iCall) {
	   if (activeConstructor == null) {
		   Ordt.errorExit("active c++ constructor is null in addInitCall");
	   }
	   activeConstructor.addInitCall(iCall);
   }

   /** add a statement to the active constructor */
   public void addConstructorStatement(String stmt) {
	   if (activeConstructor == null) {
		   Ordt.errorExit("active c++ constructor is null in addConstructorStatement");
	   }
	   activeConstructor.addStatement(stmt);
   }

   /** add a method to hashmap by tag string so it can be referenced later */
   public void tagMethod(String tag, CppMethod nMethod) {
	   taggedMethods.put(tag, nMethod);
   }

   /** get a method referenced by tag string */
   public CppMethod getTaggedMethod(String tag) {
	   return taggedMethods.get(tag);
   }

   // --------------------------- nested classes -----------------------------
   
   public class CppMethod {
	   private String signature;
	   private boolean isTemplate = false;
	   private List<String> initCalls = new ArrayList<String>();
	   private List<String> statements = new ArrayList<String>();
	   
	   CppMethod(boolean isTemplate, String signature) {
		   this.isTemplate = isTemplate;
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

	   /** return true is method is a template */
	   public boolean isTemplate() {
		   return isTemplate;
	   }

	   /** returns the signature of this method */
	   public String getSignature() {
		   return signature;
	   }

	   public Collection<? extends OutputLine> getMethod(Integer indentLvl, boolean addNamespace) {
		   List<OutputLine> outList = new ArrayList<OutputLine>();
		   // indicate template 
		   if (isTemplate) 
			   outList.add(new OutputLine(indentLvl , "template<typename T>"));
		   // add class namespace to method
		   String newSignature = this.getSignature();
		   if (newSignature.startsWith("pure virtual")) return outList;  // no body if pure virtual method
		   if (newSignature.startsWith("virtual ")) {
			   newSignature = newSignature.replaceFirst("virtual\\s*", "");
		   }
		   if (addNamespace) {
			   String patternString = "(^\\s*|.*\\s&?)(\\w+|operator[\\=\\&\\|\\~])\\s*(\\(.*)";
			   Pattern pattern = Pattern.compile(patternString);
			   Matcher matcher = pattern.matcher(newSignature);
			   if (matcher.find()) {
				   // build new signature string
				   newSignature = matcher.group(1) + " " + name + "::" + matcher.group(2) + matcher.group(3);
				   //System.out.println("CppModClass getMethod: old=" + this.signature);
				   //System.out.println("CppModClass getMethod: new=" + newSignature);
			   }
			   else Ordt.errorMessage("parse of C++ method signature='" + this.getSignature() + "' failed");	        	
		   }	  
		   // add constructor init calls if needed
		   if (initCalls.isEmpty()) {
			  outList.add(new OutputLine(indentLvl++ , newSignature.trim() + " {"));	   
		   }
		   else {
			  outList.add(new OutputLine(indentLvl++ , newSignature.trim()));
			  Iterator<String> it = initCalls.iterator();
			  String prefix = ": ";
			  while (it.hasNext()) {
				  String iCall = it.next();
				  String suffix = it.hasNext()? "," : " {";
				  outList.add(new OutputLine(indentLvl , prefix + iCall + suffix)); 
				  prefix = "  ";
			  }
		   }
		   for (String stmt: statements)
			   outList.add(new OutputLine(indentLvl , stmt));
		   outList.add(new OutputLine(--indentLvl , "}"));
		   outList.add(new OutputLine(indentLvl , ""));
		   return outList;
	   }
   }
   

   /** return true if this model element has children */
   public boolean hasChildren() {
	   return !children.isEmpty();
   }


   // --------------------------- output generation methods -----------------------------

   /** generate header output lines for this class
    * @return list of OutputLine
    */
   public List<OutputLine> genHeader(boolean includeGuard) {
	   List<OutputLine> outList = new ArrayList<OutputLine>();
	   int indentLvl = 0;

	   // generate include guard
	   if (includeGuard) {
		   outList.add(new OutputLine(indentLvl , "#ifndef __" + name.toUpperCase() + "_H_INCLUDED__"));	
		   outList.add(new OutputLine(indentLvl , "#define __" + name.toUpperCase() + "_H_INCLUDED__"));
		   outList.add(new OutputLine(indentLvl , ""));		   
	   }

	   // forward declared classes TODO
	   
	   // includes
	   if (!includes.isEmpty()) {
		   for (String inc: includes) {
			   String incStr = "#include " + (inc.startsWith("<")? inc : "\"" + inc + "\"");
			   outList.add(new OutputLine(indentLvl , incStr)); 
		   }
		   outList.add(new OutputLine(indentLvl , ""));		   
	   }
	   
	   // class define statement
	   String parentStr = "";
	   for (String par : parents) {
		   String prefix = parentStr.isEmpty()? " :" : ",";
		   parentStr += prefix + " public " + par;
	   }
	   outList.add(new OutputLine(indentLvl++ , "class " + name + parentStr + " {"));

	   // private data/methods
	   outList.addAll(getClassDefStmts(indentLvl, "private:", privateDefines, privateMethods));
	   
	   // protected data/methods
	   outList.addAll(getClassDefStmts(indentLvl, "protected:", protectedDefines, protectedMethods));
	   
	   // public data/methods
	   outList.addAll(getClassDefStmts(indentLvl, "public:", publicDefines, publicMethods));
	   
	   // close class
	   outList.add(new OutputLine(--indentLvl , "};"));
	   outList.add(new OutputLine(indentLvl , ""));
	   
	   // close include guard
	   if (includeGuard) 
	      outList.add(new OutputLine(indentLvl , "#endif // __" + name.toUpperCase() + "_H_INCLUDED__"));
	   
	   return outList;
   }
   
   /** generate class var/method statements with specified permission */
   private Collection<? extends OutputLine> getClassDefStmts(Integer indentLvl, String label, List<String> defines, List<CppMethod> methods) {
	   List<OutputLine> outList = new ArrayList<OutputLine>();
	   if (!(defines.isEmpty() && methods.isEmpty())) {
		   outList.add(new OutputLine(indentLvl++ , label));
		   for (String stmt: defines) {
			   outList.add(new OutputLine(indentLvl , stmt + ";")); 
		   }
		   for (CppMethod method: methods) {
			   if (method.isTemplate())
				   outList.add(new OutputLine(indentLvl , "template<typename T>"));
			   if (method.getSignature().startsWith("pure virtual ")) {  //
				   String newSignature = method.getSignature().replaceFirst("pure\\s*", "") + " = 0;";
				   outList.add(new OutputLine(indentLvl , newSignature));
			   }
			   else outList.add(new OutputLine(indentLvl , method.getSignature() + ";"));
		   }
		   indentLvl--;
	   }
	return outList;
}

   /** generate method output lines  */
   public List<OutputLine> genMethods(boolean addNamespace, boolean genTemplates) {   
	   List<OutputLine> outList = new ArrayList<OutputLine>();
	   int indentLvl = 0;
	   outList.add(new OutputLine(indentLvl , "// ------------------ " + name + " methods ------------------"));	
	   outList.add(new OutputLine(indentLvl , ""));
	   for (CppMethod method: privateMethods) {
		   if (method.isTemplate() == genTemplates)
		     outList.addAll(method.getMethod(indentLvl, addNamespace));
	   }
	   for (CppMethod method: protectedMethods) {
		   if (method.isTemplate() == genTemplates)
			 outList.addAll(method.getMethod(indentLvl, addNamespace));
	   }
	   for (CppMethod method: publicMethods) {
		   if (method.isTemplate() == genTemplates)
			 outList.addAll(method.getMethod(indentLvl, addNamespace));
	   }
	   return outList;
   }

   /** generate non-template method output lines  */
   public List<OutputLine> genMethods(boolean addNamespace) {   
	   return genMethods(addNamespace, false);
   }
}
