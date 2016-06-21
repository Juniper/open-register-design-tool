/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved.
 */
package ordt.output.cppmod;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ordt.extract.Ordt;
import ordt.extract.RegNumber;
import ordt.extract.RegNumber.NumBase;
import ordt.extract.RegNumber.NumFormat;
import ordt.output.OutputLine;

public class CppModClass {
   String name;
   private List<String> parents = new ArrayList<String>();
   private List<String> classDependencies = new ArrayList<String>();
   private List<String> includes = new ArrayList<String>();
   private List<String> children = new ArrayList<String>();  // list of child names
   
   private List<String> privateDefines = new ArrayList<String>();
   private List<CppMethod> privateMethods = new ArrayList<CppMethod>();
   private List<String> protectedDefines = new ArrayList<String>();
   private List<CppMethod> protectedMethods = new ArrayList<CppMethod>();
   private List<String> publicDefines = new ArrayList<String>();
   private List<CppMethod> publicMethods = new ArrayList<CppMethod>();
   
   private CppMethod activeConstructor = null;   
   private HashMap<String, CppMethod> taggedMethods = new HashMap<String, CppMethod>();
   
   public enum Vis {PUBLIC, PROTECTED, PRIVATE}
   
   public CppModClass(String name) {
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
   
   // --------------------------- rdl structure methods -----------------------------
   
   /** create a design-specific ordt_regset class */
   private static CppModClass createRegset(String className, boolean isRoot) {
	   CppModClass newClass = new CppModClass(className);
	   newClass.addParent("ordt_regset");
	   CppMethod nMethod;
	   // constructors
	   if (isRoot) {
		   nMethod = newClass.addConstructor(Vis.PUBLIC, className + "()");  // dont pass start/end into root constructor
		   newClass.tagMethod("root constructor", nMethod);  // tag this method so we can update		   
	   }
	   nMethod = newClass.addConstructor(Vis.PUBLIC, className + "(uint64_t _m_startaddress, uint64_t _m_endaddress)");
	   nMethod.addInitCall("ordt_regset(_m_startaddress, _m_endaddress)");
	   // override the child ptr update function
	   nMethod = newClass.addMethod(Vis.PUBLIC, "virtual void update_child_ptrs()");   
	   nMethod.addStatement("m_children.clear();");
	   newClass.tagMethod("update_child_ptrs", nMethod);
	   return newClass;
   }

   /** create a design-specific ordt_regset child class */
   public static CppModClass createRegset(String className) {
	   return createRegset(className, false);
   }
   
   /** create a design-specific root level ordt_regset class */
   public static CppModClass createRootRegset(String className) {
	   return createRegset(className, true);
   }

   /** return true if this model element has children */
   public boolean hasChildren() {
	   return !children.isEmpty();
   }

   /** update root level class info 
 * @param className */
   public void addRootInitCall(String className, RegNumber startAddress, RegNumber endAddress) {
	   CppMethod rootConstructor = this.getTaggedMethod("root constructor"); // first method of root is constructor w/ no args
	   rootConstructor.addInitCall(className + "(" + startAddress.toFormat(NumBase.Hex, NumFormat.Address) + ", " + endAddress.toFormat(NumBase.Hex, NumFormat.Address) + ")"); // delegated constructor
   }

   /** add info to reference a child regset in this c++ class 
    * @param className - name of child class
    * @param instName - name of child instance
    * @param startOffset - offset of child regset start address
    * @param byteSize - size in bytes of child regset (each rep)
    * @param reps - number of time child is replicated
    * @param stride - address stride of child replications
    */
   public void addChildRegsetInfo(String className, String instName, RegNumber startOffset, RegNumber byteSize, int reps, RegNumber stride) {
	   children.add(instName);
	   // compute high address
	   RegNumber endOffset = new RegNumber(startOffset);
	   endOffset.add(byteSize);
	   endOffset.subtract(1);
	   CppMethod ptrUpdateMethod = this.getTaggedMethod("update_child_ptrs"); 
	   // if a replicated regset use ordt_regset_array class
	   if (reps>1) {
		   // create child define 
		   this.addDefine(Vis.PUBLIC, "ordt_addr_elem_array<" + className + "> " + instName);  
		   // create child init call
		   String strideStr = ((stride == null) || (!stride.isDefined()))? byteSize.toFormat(NumBase.Hex, NumFormat.Address) : stride.toFormat(NumBase.Hex, NumFormat.Address);
		   this.addInitCall(instName + "(_m_startaddress + " + startOffset.toFormat(NumBase.Hex, NumFormat.Address) + 
				   ", _m_startaddress + " + endOffset.toFormat(NumBase.Hex, NumFormat.Address) + ", " + reps + ", " + strideStr + ")");
	   }
	   // otherwise if a single regset
	   else {
		   // create child define 
		   this.addDefine(Vis.PUBLIC, className + " " + instName);  
		   // create init
		   this.addInitCall(instName + "(_m_startaddress + " + startOffset.toFormat(NumBase.Hex, NumFormat.Address) + 
				   ", _m_startaddress + " + endOffset.toFormat(NumBase.Hex, NumFormat.Address) + ")");
	   }
	   // add to the ordered child list in both constructor and update method 
	   this.addConstructorStatement("m_children.push_back(&" + instName + ");");  // push ptr of child onto vector 
	   ptrUpdateMethod.addStatement("m_children.push_back(&" + instName + ");");  // push ptr of child onto vector 
   }

   /** create a design-specific ordt_reg child class */
   public static CppModClass createReg(String className) {
	   CppModClass newClass = new CppModClass(className);
	   newClass.addParent("ordt_reg");
	   // constructor
	   CppMethod nMethod = newClass.addConstructor(Vis.PUBLIC, className + "(uint64_t _m_startaddress, uint64_t _m_endaddress)");
	   nMethod.addInitCall("ordt_reg(_m_startaddress, _m_endaddress)");
	   // overload write methods
	   nMethod = newClass.addMethod(Vis.PUBLIC, "virtual void write(const uint64_t &addr, const ordt_data &wdata)");
	   nMethod.addStatement("   std::cout << \"reg " + className + " write: ---- addr=\"<< addr << \", data=\" << wdata.to_string() << \"\\n\";");
	   nMethod.addStatement("   if (this->hasStartAddress(addr))");
	   nMethod.addStatement("      this->write(wdata);");
	   nMethod = newClass.addMethod(Vis.PUBLIC, "virtual void write(const ordt_data &wdata)");  
	   newClass.tagMethod("write", nMethod);  
	   nMethod.addStatement("std::lock_guard<std::mutex> m_guard(m_mutex);");  // grab reg lock
	   // overload read methods
	   nMethod = newClass.addMethod(Vis.PUBLIC, "virtual void read(const uint64_t &addr, ordt_data &rdata)");  
	   nMethod.addStatement("   std::cout << \"reg " + className + " read: ---- addr=\"<< addr << \"\\n\";");
	   nMethod.addStatement("   if (this->hasStartAddress(addr))");
	   nMethod.addStatement("      this->read(rdata);");
	   nMethod.addStatement("   else rdata.clear();");
	   nMethod = newClass.addMethod(Vis.PUBLIC, "virtual void read(ordt_data &rdata)");  
	   newClass.tagMethod("read", nMethod);  
	   nMethod.addStatement("rdata.clear();");
	   nMethod.addStatement("for (int widx=0; widx<((m_endaddress - m_startaddress + 1)/4); widx++) rdata.push_back(0);");
	   return newClass;
   }

   /** add info to reference a child reg in this c++ class  
    * @param className - name of child class
    * @param instName - name of child instance
    * @param startOffset - offset of child reg start address
    * @param byteSize - size in bytes of child reg (each rep)
    * @param reps - number of time child is replicated
    * @param stride - address stride of child replications
    */
   public void addChildRegInfo(String className, String instName, RegNumber startOffset, Integer byteSize,
		   int reps, RegNumber stride) {
	   // handle child reg same a regset
	   addChildRegsetInfo(className, instName, startOffset, new RegNumber(byteSize), reps, stride);
   }


   /** add info to reference a child field in this c++ class  
    * @param instName - name of child instance
    * @param lowIndex - low index of this field within reg
    * @param width - width of field in bits
    */
   public void addChildFieldInfo(String instName, Integer lowIndex, Integer width, RegNumber resetVal, String rMode, String wMode) {
	   children.add(instName);
	   // set field data type based on width
	   String fieldType;  
	   //fieldType = "uint64_t";
	   //
	   if (width <= 8) fieldType = "uint_fast8_t";
	   else if (width <= 32) fieldType = "uint32_t";
	   //else fieldType = "uint64_t";
	   //
	   else if (width <= 64) fieldType = "uint64_t";
	   else  fieldType = "ordt_data";
	   // create child define 
	   this.addDefine(Vis.PUBLIC, "ordt_field<" + fieldType + "> " + instName); 
	   // create field init call
	   String initStr = "";
	   if ("ordt_data".equals(fieldType)) { // special call if a wide field
		   if (((resetVal == null) || !resetVal.isDefined())) initStr = "0"; // default to reset value of 0
		   else if (resetVal.isNonZero()) { 
			   Ordt.warnMessage("C++ model does not support non-zero init values for wide fields, field=" + instName);
		       initStr="0";
		   }
		   else initStr = resetVal.toFormat(NumBase.Hex, NumFormat.Address);
		   int words = width/32;
		   this.addInitCall(instName + "(" + lowIndex + ", " + width + "," + words + ", " + initStr + ", " + rMode + ", " + wMode + ")");
	   }
	   // else if a base type init
	   else {
		   if (((resetVal == null) || !resetVal.isDefined())) initStr = "0"; // default to reset value of 0
		   else initStr = resetVal.toFormat(NumBase.Hex, NumFormat.Address);
		   this.addInitCall(instName + "(" + lowIndex + ", " + width + ", " + initStr + ", " + rMode + ", " + wMode + ")");		   
	   }
	   // add field read/write calls in reg read method
	   CppMethod read = this.getTaggedMethod("read");
	   read.addStatement(instName + ".read(rdata);");
	   CppMethod write = this.getTaggedMethod("write");
	   write.addStatement(instName + ".write(wdata);");
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
