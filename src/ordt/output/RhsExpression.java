/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved.
 */
package ordt.output;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ordt.extract.Ordt;
import ordt.extract.RegNumber;

/** class describing a rhs expression which includes operators and rhs references */
public class RhsExpression {
	
	private String baseExpression; 
	private List<RhsReference> refs = new ArrayList<RhsReference>();

   public RhsExpression(String rawExpression, int depth) {
	   parseRawExpression(rawExpression, depth);  // extract instance and deref  
   }
   
   /** extract baseExpression and list of references from rawExpression */
   private void parseRawExpression(String rawExpression, int depth) { 
	   //System.out.println("RhsExpression parseRawExpression: --- rawExpression =" + rawExpression);
	   baseExpression = "";
	   boolean matchFail= false;
	   int refId = 0;
	   // search for 3 part pattern of form: allowed lead characters + reference + allowed trailing characters
	   // brackets are assumed to be replicated component indices unless trailing a reference, where they are interpreted as a verilog signal slice
	   Pattern refPattern = Pattern.compile("^([\\s\\&\\|\\^\\~\\<\\>\\(\\)\\{\\}\\,\\[\\]]*)([\\w\\.\\'\\[\\]]+\\w(\\s*->\\s*\\w+)?)([\\s\\&\\|\\^\\~\\<\\>\\(\\)\\{\\}\\,\\[\\]].*)?$");
	   Matcher m;
	   // start with the full expression and iteratively extract references to be resolved	   
	   String expression = rawExpression;
	   while (!matchFail) {
		   m = refPattern.matcher(expression);
		   // if another ref found, save it and tag the base string
		   if (m.matches()) {
			   String leadString = m.group(1);
			   String refString = m.group(2);
			   expression = m.group(4);
			   //for (int idx=1; idx<=m.groupCount(); idx++)
			   RegNumber elemNum = new RegNumber(refString);
			   // if a number is detected then keep it as-is
			   if (elemNum.isDefined()) {
				   baseExpression += leadString + refString;
			   }
			   //otherwise save the ref and add a tag in the overall expression
			   else {
				   //System.out.println("RhsExpression parseRawExpression: setting exp#" + refId + ": " + refString);
				   baseExpression += leadString + "$" + refId++ + " ";
				   refs.add(new RhsReference(refString, depth));
			   }
			   if ((expression == null) || expression.isEmpty()) matchFail = true;  // done if nothing left to parse
		   }
		   else {
			   //System.out.println("RhsExpression parseRawExpression: matcher failed w/ refId =" + refId + ", expression=" + expression);
			   matchFail = true;
			   if ((refId>0) && expression.matches("^[\\s\\}\\)\\[\\]\\d]+$")) {  // save any trailing rt parens/brackets/arrray indicators
				   baseExpression += expression;
				   baseExpression = baseExpression.trim();
			   }
			   else Ordt.errorMessage("parse of rhs expression (" + rawExpression + ") failed.");
		   }   
	   }  // while
	   /*System.out.println("rawExpression=" + rawExpression);
	   System.out.println("baseExpression=" + baseExpression);
	   for (RhsReference ref: refs) {
		   System.out.println("ref=" + ref.getRawReference());
	   }*/
   }

   /** return the raw expression */
   public String getRawExpression() {
	   String retExpression = baseExpression;
	   for (int idx=0; idx<refs.size(); idx++) retExpression = retExpression.replaceFirst("\\$" + idx, refs.get(idx).getRawReference());
	   return retExpression;   
   }
	
   /** return a list of references in this expression */
   public List<RhsReference> getRefList() {
	   return refs;
   }
	
   /** return list of raw references in this expression */
   public List<String> getRawRefNameList() {
	   List<String> retList = new ArrayList<String>();
	   for (RhsReference ref: refs) retList.add(ref.getRawReference());
	   return retList;
   }
	
   /** return list of resolved references in this expression *
   public List<String> getResolvedRefNameList(InstanceProperties instProperties, HashMap<String, SignalProperties> userDefinedSignals) {
	   List<String> retList = new ArrayList<String>();
	   for (RhsReference ref: refs) {
		   String fullRef = ref.getReferenceName(instProperties, false);
		   if ((fullRef != null) && fullRef.startsWith("rg_")) {  // if this is a field/signal reference, resolve it
			   String sigRef = fullRef.replaceFirst("rg_", "sig_");
			   if  (userDefinedSignals.containsKey(sigRef)) fullRef=sigRef;
		   }
		   retList.add(fullRef);
	   }
	   return retList;
   }*/

   /** return the resolved rtl expression. 
    *     note - since method uses userDefinedSignals it is only usable after generation phase */
   public String getResolvedExpression(InstanceProperties instProperties, HashMap<String, SignalProperties> userDefinedSignals) {
	   String retExpression = baseExpression;
	   //System.out.println("RhsExpression getResolvedExpression: baseExpression=" + baseExpression);
	   for (int idx=0; idx<refs.size(); idx++) {
		   String fullRef = refs.get(idx).getReferenceName(instProperties, false);
		   if ((fullRef != null) && fullRef.startsWith("rg_")) {  // if this is a field/signal reference, resolve it
			   String sigRef = fullRef.replaceFirst("rg_", "sig_");
			   if  (userDefinedSignals.containsKey(sigRef)) fullRef=sigRef;
		   }
		   retExpression = retExpression.replaceFirst("\\$" + idx, fullRef);
	   }
	   return retExpression;   
   }
   
   public static void main(String[] args) {
	   RhsExpression expr = new RhsExpression("bla.bla->we & rhs3.rhs2.rhs1->next",3);
	   System.out.println("raw expr=" + expr.getRawExpression());
   }


}
