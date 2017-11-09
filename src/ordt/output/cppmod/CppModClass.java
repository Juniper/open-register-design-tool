/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved.
 */
package ordt.output.cppmod;

import ordt.extract.Ordt;
import ordt.extract.RegNumber;
import ordt.extract.RegNumber.NumBase;
import ordt.extract.RegNumber.NumFormat;
import ordt.output.drvmod.cpp.CppBaseModClass;

/** class for describing c++ classes used in CppMod */
public class CppModClass extends CppBaseModClass {
   
   public CppModClass(String name) {
		super(name);
	}

   // --------------------------- cpp model structure methods -----------------------------
   
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
	   nMethod = newClass.addMethod(Vis.PUBLIC, "virtual int write(const uint64_t &addr, const ordt_data &wdata)");
	   nMethod.addStatement("#ifdef ORDT_PIO_VERBOSE");
	   nMethod.addStatement("   std::cout << \"--> write of reg " + className + " at addr=\"<< addr << \", data=\" << wdata.to_string() << \"\\n\";");
	   nMethod.addStatement("#endif");
	   nMethod.addStatement("   if (this->hasStartAddress(addr)) {");
	   nMethod.addStatement("      this->write(wdata);");
	   nMethod.addStatement("      return 0;");
	   nMethod.addStatement("   }");
	   nMethod.addStatement("#ifdef ORDT_PIO_VERBOSE");
	   nMethod.addStatement("   std::cout << \"--> write to invalid address \" << addr << \" in reg " + className + "\\n\";" );
	   nMethod.addStatement("#endif");
	   nMethod.addStatement("   return 8;" );
	   
	   nMethod = newClass.addMethod(Vis.PUBLIC, "virtual void write(const ordt_data &wdata)");  
	   newClass.tagMethod("write", nMethod);  // tag this method so field info can be appended
	   nMethod.addStatement("std::lock_guard<std::mutex> m_guard(m_mutex);");  // grab reg lock
	   
	   // overload read methods
	   nMethod = newClass.addMethod(Vis.PUBLIC, "virtual int read(const uint64_t &addr, ordt_data &rdata)");  
	   nMethod.addStatement("#ifdef ORDT_PIO_VERBOSE");
	   nMethod.addStatement("   std::cout << \"--> read of reg " + className + " at addr=\"<< addr << \"\\n\";");
	   nMethod.addStatement("#endif");
	   nMethod.addStatement("   if (this->hasStartAddress(addr)) {");
	   nMethod.addStatement("      this->read(rdata);");
	   nMethod.addStatement("      return 0;");
	   nMethod.addStatement("   }");
	   nMethod.addStatement("#ifdef ORDT_PIO_VERBOSE");
	   nMethod.addStatement("   std::cout << \"--> read to invalid address \" << addr << \" in reg " + className + "\\n\";" );
	   nMethod.addStatement("#endif");
	   nMethod.addStatement("   rdata.clear();");
	   nMethod.addStatement("   return 8;" );
	   
	   nMethod = newClass.addMethod(Vis.PUBLIC, "virtual void read(ordt_data &rdata)");  
	   newClass.tagMethod("read", nMethod);  // tag this method so field info can be appended
	   nMethod.addStatement("rdata.clear();");
	   nMethod.addStatement("for (uint64_t widx=0; widx<((m_endaddress - m_startaddress + 1)/4); widx++) rdata.push_back(0);");
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

}
