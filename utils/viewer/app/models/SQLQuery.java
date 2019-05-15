package models;

import java.util.ArrayList;
import java.util.List;

public class SQLQuery {
   private String select;
   private List<String> whereClauses = new ArrayList<String>();
   private String havingClause;
   private String orderBy;
   private String groupBy;
   
   public SQLQuery(String select) {
	  this.select = select;   
   }
   
   public void addWhere(String clause) {
	  whereClauses.add(" " + clause);
   }

   public void addAndWhere(String clause) {
	   if (whereClauses.isEmpty()) addWhere(clause);
	   else addWhere("and " + clause);   
   }
	
	public void addOrWhere(String clause) {
	   if (whereClauses.isEmpty()) addWhere(clause);
	   else addWhere("or " + clause);   
	}
	
   /* add order by clause */
   public void addOrderBy(String orderBy) {
		this.orderBy = orderBy;
   }
   
   /* add group by clause */
   public void addGroupBy(String groupBy) {
	   this.groupBy = groupBy;   	
   }
	
   /* add having clause */
   public void addHaving(String having) {
		this.havingClause = having;
   }

   /** add a subquery matching all descendant ids of specified reg id */ 
   public void addDescendantSubQuery(Integer cid) {
       SQLQuery decSubQuery = new SQLQuery("select d.descendant from Descendant d");
       decSubQuery.addWhere("d.parent = " + cid);
       decSubQuery.addAndWhere("d.parent <> d.descendant");
       this.addAndWhere("r.id in (" + decSubQuery + ")");  
   }

   /** add an address match where clause */
   public void addAddressClause(Long targetAddress, long offset) {
	   this.addAndWhere("(r.baseAddr = " + (targetAddress - offset));  // non replicated reg match
	   this.addOrWhere( "(r.baseAddr <= " + (targetAddress - offset) + " and r.isRegset is TRUE and r.highAddr >= " + (targetAddress - offset) + ")");  // non replicated regset match
	   this.addOrWhere( "(r.baseAddr <= " + (targetAddress - offset) + " and r.reps > 1 and (r.baseAddr + r.reps*r.stride) > " + (targetAddress - offset) + ") )");  // replicated match
   }
   
   //public static 
   /** generate the query string */
   public String toString() {
	   String query = select;
	   boolean firstWhere = true;
	   for (String clause : whereClauses) {
		   if (firstWhere) query += " where";
		   query += clause;
		   firstWhere = false;
	   }
	   if (groupBy != null) query += " group by " + groupBy;
	   if (havingClause != null) query += " having " + havingClause;
	   if (orderBy != null) query += " order by " + orderBy;
	   return query;
   }
	
   /** generate comma separated string */
   public static String toShortString(List<Short> ids) {
	   String idStr = "";
	   Boolean firstId = true;
	   for (Short id: ids) {
		   String prefix = firstId? "" : ",";
		   idStr = idStr + prefix + id;
		   firstId = false;
	   }
	   return idStr;
   }
   
   /** generate comma separated string */
   public static String toIntString(List<Integer> ids) {
	   String idStr = "";
	   Boolean firstId = true;
	   for (Integer id: ids) {
		   String prefix = firstId? "" : ",";
		   idStr = idStr + prefix + id;
		   firstId = false;
	   }
	   return idStr;
   }

}
