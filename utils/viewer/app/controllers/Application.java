package controllers;

import play.mvc.*;
import play.db.jpa.*;
import play.routing.JavaScriptReverseRouter;

import views.html.*;
import models.DataLoader;
import models.Field;
import models.Load;
import models.Register;
import models.RegisterLite;
import models.SQLQuery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//import org.hibernate.annotations.OrderBy;

//import com.fasterxml.jackson.databind.JsonNode;

import static play.libs.Json.*;

public class Application extends Controller {


    // ------------------ page render methods 
	
    public Result regsetPage(Integer id, Integer cdepth, Integer mdepth) {
        return ok(regset.render(id, cdepth, mdepth));
    }

    public Result loadPage(Boolean errMsg, String responseMsg, String fileOwner) {
        return ok(load.render(errMsg, responseMsg, fileOwner));
    }

    public Result registerPage(Integer id) {
        return ok(register.render(id));
    }
    
    public Result filterPage(Integer cid, String scope, String dids, String rstype, String rntype, String namemode, String rname, Integer rwidth, String raddr, Boolean unaddr, String rcat, Boolean dtest, String fntype, String fnamemode, String fname, String faccess, String fscat, Boolean fnoinit, Boolean fvol, Boolean fcount, Boolean fintr, Boolean fdcomp) {
        return ok(filter.render(cid, scope, dids, rstype, rntype, namemode, rname, rwidth, raddr, unaddr, rcat, dtest, fntype, fnamemode, fname, faccess, fscat, fnoinit, fvol, fcount, fintr, fdcomp));
    }

    // ----------------- db transaction methods
    
    /** get decendant registers for reg of specified id 
     * 
     * @param id
     * @param cdepth - depth of current register
     * @param mdepth - max relative depth to be retrieved
     * @return json Result
     */
    @Transactional(readOnly = true)
    public Result getDescendants(Integer id, Integer cdepth, Integer mdepth) { 
    	boolean rootId = (id == null) || (id <= 0);
        // generate query
        SQLQuery query = new SQLQuery("select r from Register r");
        query.addOrderBy("r.id");
        // if non-0 id then get selected descendants
        if (!rootId) {
        	query.addDescendantSubQuery(id);
        }
        // if non-0 relative depth, then get regs below abs depth 
        //System.out.println("Application: cdepth=" + cdepth + ", mdepth=" + mdepth);
    	if (rootId) {
    		mdepth = 1;
    		query.addAndWhere("r.depth = 0");  // TODO allow override if filers applied
    	}
    	else if ((cdepth != null ) && (mdepth != null ) & (mdepth > 0)) 
        	query.addAndWhere("r.depth <= " + (cdepth + mdepth));
        //System.out.println(query);
        //else if ((maxDepth != null ) && (maxDepth > 0))
        // 	query += " and r.depth <= " + maxDepth;
        int limit = 2000;
    	@SuppressWarnings("unchecked")
		List<Register> descendants = (List<Register>) JPA.em().createQuery(query.toString()).setMaxResults(limit).getResultList();
        //System.out.println("---- 1 --->" + toJson(descendants).toString().length());  
        List<RegisterLite> descendantsLite = RegisterLite.getList(descendants, false);  // do not include parentPath in result
        //System.out.println("---- 2 --->" + toJson(descendantsLite).toString().length());  
        // if rootId, merge in the data tag also (override parentPath)
        if (rootId && !descendantsLite.isEmpty()) {
        	// load table is small so just get the entire table and convert to a map
        	HashMap<Short, String> tags = new HashMap<Short, String>();
            SQLQuery lquery = new SQLQuery("select l from Load l");
            @SuppressWarnings("unchecked")
    		List<Load> loads = (List<Load>) JPA.em().createQuery(lquery.toString()).getResultList();
            for (Load ld: loads) {
            	tags.put(ld.getId(), ld.getLabel());
                //System.out.println("Application getDescendants: " + ld.getId() + ": " + ld.getLabel()+ ": " + tags.get(ld.getId()));
            }
            // now override path in descendantlist
            for(RegisterLite descendant: descendantsLite) {
            	descendant.parentPath = tags.get(descendant.loadId);
                //System.out.println("Application getDescendants: " + descendant.loadId + ": " + tags.get(descendant.loadId));
            }
        }
        //System.out.println(toJson(descendantsLite));  
        return ok(toJson(descendantsLite));
    }

    /** get registers matching specified filter 
     * 
     * @param rname
     * @return json Result
     */
    @Transactional(readOnly = true)
    public Result filterRegs(Integer cid, String scope, String dids, String rstype, String rntype, String namemode, String rname, Integer rwidth, String raddr, Boolean unaddr, String rcat, Boolean dtest, String fntype, String fnamemode, String fname, String faccess, String fscat, Boolean fnoinit, Boolean fvol, Boolean fcount, Boolean fintr, Boolean fdcomp) {
    	List<Register> empty = new ArrayList<Register>();
		//System.out.println("filterRegs: raddr=" + raddr + ", unaddr=" + unaddr +  ", rcat=" + rcat + ", faccess=" + faccess);
	
        // exit if invalid query info
    	boolean useId = false;
    	if (scope == null) return ok(toJson(empty));
    	if (scope.equals("cur")) {
    		//System.out.println("filterRegs: cur scope detected");
    		if ((cid == null) || (cid == 0)) return ok(toJson(empty));
    		useId = true;
    	}
    	else if (scope.equals("ids")) {
    		//System.out.println("filterRegs: did scope detected");
    		if ((dids == null) || (dids.isEmpty())) return ok(toJson(empty));
    		if (!dids.matches("\\d+( *, *\\d+)* *")) return ok(toJson(empty));  // insure valid id string format
    	}
    	else return ok(toJson(empty));
		//System.out.println("filterRegs: cid/dids OK");

    	// check for valid name
    	boolean rnameValid = true;
    	if ((rname == null) || rname.isEmpty()) rnameValid = false;
    	else if (rname.length()<2) rnameValid = false;
    	else if (rntype.equals("name") && !rname.matches("[a-zA-Z][a-zA-Z0-9_]+")) rnameValid = false;
    	else if (!rntype.equals("name") && !rname.matches("[a-zA-Z0-9\\._][a-zA-Z0-9\\._ ]*[a-zA-Z0-9\\._]")) rnameValid = false;
		//System.out.println("filterRegs: rname OK=" + rnameValid);
    	
    	// check for valid width
    	boolean rwidthValid = true;
    	if ((rwidth == null) || (rwidth == 0)) rwidthValid = false;
    	else if (rwidth > 16) rwidthValid = false;
		//System.out.println("filterRegs: rwidth=" + rwidth + ", OK=" + rwidthValid);
    	
    	// check for valid address
    	boolean raddrValid = true;
    	if ((raddr == null) || raddr.isEmpty()) raddrValid = false;
    	else if (!raddr.matches("[a-fA-F0-9]{1,10}")) raddrValid = false;
    	Long address = raddrValid ? Long.valueOf(raddr, 16) : null;
		//System.out.println("filterRegs: raddr=" + address + ", OK=" + raddrValid);
    	
    	// check for valid reg category
    	boolean rcatValid = true;
    	if ((rcat == null) || rcat.isEmpty()) rcatValid = false;
		//System.out.println("filterRegs: rcat OK=" + rcatValid);
    	
    	// check for valid field name
    	boolean fnameValid = true;
    	if ((fname == null) || fname.isEmpty()) fnameValid = false;
    	else if (fname.length()<2) fnameValid = false;
    	else if (fntype.equals("name") && !fname.matches("[a-zA-Z][a-zA-Z0-9_]+")) fnameValid = false;
    	else if (!fntype.equals("name") && !fname.matches("[a-zA-Z0-9\\._][a-zA-Z0-9\\._ ]*[a-zA-Z0-9\\._]")) fnameValid = false;
		//System.out.println("filterRegs: fname OK=" + fnameValid);
    	
    	// check for valid field access
    	boolean faccessValid = true;
    	if ((faccess == null) || faccess.isEmpty()) faccessValid = false;
		//System.out.println("filterRegs: faccess OK=" + faccessValid);
    	
    	// check for valid field subcategory
    	boolean fSubCatValid = true;
    	if ((fscat == null) || fscat.isEmpty()) fSubCatValid = false;
		//System.out.println("filterRegs: fscat OK=" + fSubCatValid);
     	
    	// set valid field query
    	boolean fieldQueryValid = fnameValid || faccessValid || fSubCatValid || fnoinit || fvol || fcount || fintr || fdcomp;
		//System.out.println("filterRegs: field query=" + fieldQueryValid);
    	
    	// exit if no sub-queries specifed
    	boolean validQuery = rnameValid || rwidthValid || raddrValid || unaddr || dtest || rcatValid || fieldQueryValid;
    	if (!validQuery) return ok(toJson(new ArrayList<Register>()));
    	
        // generate base query using either descendant or data id scope
    	String fieldJoin = fieldQueryValid? ", Field f" : "";
        SQLQuery query = new SQLQuery("select distinct r from Register r" + fieldJoin);
        query.addOrderBy("r.id");
    	// if unique addresses are specified, modify query 
        SQLQuery unSubQuery = new SQLQuery("select max(ur.id) from Register ur");
        unSubQuery.addHaving("count(ur.baseAddr) = 1");
        unSubQuery.addGroupBy("ur.baseAddr, ur.isRegset, ur.highAddr, ur.reps, ur.stride");  //, ur.highAddr, ur.reps, ur.stride
        boolean useUniqueAddrSubQuery = unaddr && !useId && !raddrValid;        
        
        // if processing descendants 
        if (useId) {
        	query.addDescendantSubQuery(cid);
        }
        // else if processing data ids
        else {
        	// multiple data ids specified
        	if (dids.contains(","))
        		if (useUniqueAddrSubQuery) unSubQuery.addWhere("loadId in (" + dids + ")");
        		else query.addWhere("r.loadId in (" + dids + ")");
        	// else only 1 data id
        	else
        		if (useUniqueAddrSubQuery) unSubQuery.addWhere("loadId = " + dids);
        		else query.addWhere("r.loadId = " + dids);
        }
        
        // if an address search, ignore other filter options and use recursive search  
        if (raddrValid) {
            return addressFilter(address, query);
        }
        
        // add reg name select to query
        if (rnameValid) {
        	// select target db field
        	String dbTarget = "r.name";  // default to name
        	if (rntype.equals("label")) dbTarget = "r.shortText";
        	else if (rntype.equals("description")) dbTarget = "r.longText";
        	else if (rntype.equals("path")) dbTarget = "r.parentPath";
        	// create query string
        	if (namemode.equals("is"))
        		query.addAndWhere(dbTarget + " = '" + rname + "'");
        	else if (namemode.startsWith("start"))
        		query.addAndWhere(dbTarget + " like '" + rname + "%'");
        	else if (namemode.startsWith("end"))
        		query.addAndWhere(dbTarget + " like '%" + rname + "'");        	
        	else if (namemode.startsWith("contain"))
        		query.addAndWhere(dbTarget + " like '%" + rname + "%'");
        	// add register/regset select
        	//System.out.println("rstype = " + rstype);
        	if (rstype.equals("Reg Set")) 
        		query.addAndWhere("r.isRegset is TRUE");
        	else
        		query.addAndWhere("r.isRegset is FALSE");
        }
        
        // add reg width select to query
        if (rwidthValid) {
             query.addAndWhere("r.width = (" + rwidth + " * 32)");
        }
        
        // add reg category select to query
        if (rcatValid) {
        	 if (rcat.equals("none")) {
                 query.addAndWhere("r.isRegset is FALSE");
                 query.addAndWhere("r.catCode = 0");       		 
        	 }
        	 else
                 query.addAndWhere("r.catCode = " + getCatCode(rcat));       		         		 
        }
       
        // if only unique addresses add the subquery clause
        if (useUniqueAddrSubQuery) {
            query.addAndWhere("r.id in (" + unSubQuery + ")");
        }
        
        // add reg dont test to query
        if (dtest) {
             query.addAndWhere("r.donttest is TRUE");
        }
        
        // add field join criteria  
        if (fieldQueryValid) {
            query.addAndWhere("f.regId = r.id");
            // add field txt query if specified
            if (fnameValid) {
            	// select target db field
            	String dbTarget = "f.name";  // default to name
            	if (fntype.equals("label")) dbTarget = "f.shortText";
            	else if (fntype.equals("description")) dbTarget = "f.longText";
            	// create query string
            	if (fnamemode.equals("is"))
            		query.addAndWhere(dbTarget + " = '" + fname + "'");
            	else if (fnamemode.startsWith("start"))
            		query.addAndWhere(dbTarget + " like '" + fname + "%'");
            	else if (fnamemode.startsWith("end"))
            		query.addAndWhere(dbTarget + " like '%" + fname + "'");        	
            	else if (fnamemode.startsWith("contain"))
            		query.addAndWhere(dbTarget + " like '%" + fname + "%'");
            }
            // add other queries
            if (faccessValid) query.addAndWhere("f.access = '" + faccess + "'");
            if (fnoinit) query.addAndWhere("f.rst is null");
            if (fvol) query.addAndWhere("f.hwMod is TRUE");
            if (fcount) query.addAndWhere("f.counter is TRUE");
            if (fintr) query.addAndWhere("f.intr is TRUE");
            if (fdcomp) query.addAndWhere("f.dontcompare is TRUE");
            // add subcategory select to query
            if (fSubCatValid) {
            	 if (fscat.equals("none")) query.addAndWhere("f.subCatCode = 0");       		 
            	 else query.addAndWhere("f.subCatCode = " + getSubCatCode(fscat));       		         		 
            }
        }
        
        //System.out.println(query);
        int limit = 500;
        @SuppressWarnings("unchecked")
		List<Register> results = (List<Register>) JPA.em().createQuery(query.toString()).setMaxResults(limit).getResultList();
        List<RegisterLite> resultsLite = RegisterLite.getList(results, true);  // return parentPath in results
        return ok(toJson(resultsLite));
    }

	/** return a the subcategory code given a subcategory string */
    private String getSubCatCode(String fscat) {
		if ("INFO".equals(fscat)) return "1"; 
		if ("MAJOR".equals(fscat)) return "2"; 
		if ("FATAL".equals(fscat)) return "4"; 
		if ("MINOR_RECOVERABLE".equals(fscat)) return "8"; 
		if ("MINOR_TRANSIENT".equals(fscat)) return "16"; 
		return "8";
	}

	/** return a the category code given a category string */
    private String getCatCode(String rcat) {
		if ("STATIC_CONFIG".equals(rcat)) return "1"; 
		if ("DYNAMIC_CONFIG".equals(rcat)) return "2"; 
		if ("CONSTRAINED_CONFIG".equals(rcat)) return "4"; 
		if ("STAT_COUNTER".equals(rcat)) return "8"; 
		if ("ERROR_COUNTER".equals(rcat)) return "16"; 
		if ("STATE".equals(rcat)) return "32"; 
		if ("INTERRUPT".equals(rcat)) return "64"; 
		if ("DIAGNOSTIC".equals(rcat)) return "128"; 
		if ("DEBUG".equals(rcat)) return "256"; 
		if ("CGATE_UNSAFE".equals(rcat)) return "512"; 
		return "1024";
	}

	/** walk thru register hierarchy to find a specified set of addresses 
     * @param query */
    @Transactional(readOnly = true)
    private Result addressFilter(Long address, SQLQuery query) {
		// get the list of matching addresses
    	query.addAddressClause(address, (long) 0);  // no offset for initial query
        //System.out.println(query);
        @SuppressWarnings("unchecked")
		List<Register> results = (List<Register>) JPA.em().createQuery(query.toString()).getResultList();
        // if no results then exit
        if (results.isEmpty()) return ok(toJson(results));
        // if already found a matching reg then exit
        boolean foundRegMatch = false;
        Register firstRegSet = null;
        for (Register reg : results) {
        	if (!reg.isRegset) foundRegMatch = true;
        	else if (firstRegSet == null) firstRegSet = reg;  // save first matching regset	// FIXME - will not work for multi-data id case
        }
        if (foundRegMatch) return ok(toJson(results));
        
        // otherwise look for a match in the regset
        List<Register> newregs = addressCheck(address, (long) 0, firstRegSet);
        List<RegisterLite> newregsLite = RegisterLite.getList(newregs, true);   // return parentPath in results      
        return ok(toJson(newregsLite));
 	}

    /** given a regset containing a target address, recursively search for the target reg */
    @Transactional(readOnly = true)
	private List<Register> addressCheck(Long targetAddress, Long baseOffset, Register reg) {
    	List<Register> regList = new ArrayList<Register>();
    	// compute rep number in this regset and new offset
    	Long rep = (reg.reps == 1)? 0 : (targetAddress - (reg.baseAddr + baseOffset)) / reg.stride;
    	Long newOffset = (reg.reps == 1)? baseOffset : baseOffset + (rep) * reg.stride;
    	//System.out.println("reg=" + reg.name + ", id=" + reg.id + ", regset=" + reg.isRegset + ", baseOffset=" + baseOffset + ", newOffset=" + newOffset + ", maxreps=" + reg.reps + ", rep=" + rep);
		// verify that rep is in range for this register
    	if (rep >= reg.reps) return(regList);
		// verify that target address is in actual range for this register set
    	if (targetAddress > (newOffset + reg.highAddr)) return(regList);
		// otherwise add to list
    	regList.add(reg);
    	//System.out.println("keeping reg=" + reg.name + ", id=" + reg.id + ", rep=" + rep);
		
		// search for any direct descendants that match address using offset
        SQLQuery query = new SQLQuery("select r from Register r");
        query.addDescendantSubQuery(reg.id);
        query.addAndWhere("r.depth = " + (reg.depth + 1));  // get children
    	query.addAddressClause(targetAddress, newOffset);  // use new address offset
        //System.out.println(query);
        @SuppressWarnings("unchecked")
		List<Register> results = (List<Register>) JPA.em().createQuery(query.toString()).getResultList();
		
		// if a reg match then add to list and return
        if (results.isEmpty()) return regList;
        Register firstRegSet = null;
        for (Register rg : results) {
        	if (!rg.isRegset) {  // if register found, we're done
        		regList.add(rg);
        		return regList;
        	}
        	else if (firstRegSet == null) firstRegSet = rg;  // save first matching regset	
        }
		// if a regset match and then call recursively with offset
        regList.addAll(addressCheck(targetAddress, newOffset, firstRegSet));
		
		return regList;
	}

	/** get fields for reg of specified id 
     * 
     * @param regId
     * @return json Result
     */
    @Transactional(readOnly = true)
    public Result getFields(Integer regId) { 
    	if ((regId == null) || (regId < 1)) return ok(toJson(new ArrayList<Field>()));
        // generate query
        SQLQuery query = new SQLQuery("select f from Field f");
        query.addWhere("f.regId = " + regId);
        //query.addOrderBy("f.lowIdx desc");  // FIXME - this doesn't work w/ hibernate?
        //System.out.println(query);
        @SuppressWarnings("unchecked")
		List<Field> fields = (List<Field>) JPA.em().createQuery(query.toString()).getResultList();
        // order by didn't work so sort 
		Collections.sort(fields, new Comparator<Field>() {
			@Override
			public int compare(Field f1, Field f2) {
				return (f2.lowIdx > f1.lowIdx) ? 1 : -1;
			}		
		});
        //System.out.println(toJson(fields));  
        return ok(toJson(fields));
    }

    @Transactional(readOnly = true)
    public Result getAncestors(Integer id) {
    	boolean rootId = (id == null) || (id <= 0);
    	if (rootId) {
    		return ok(toJson(new ArrayList<Register>()));  // return empty list if root
    	}
        // generate query
        SQLQuery query = new SQLQuery("select r from Register r");
        // if non-0 id then get selected descendants
        SQLQuery subQuery = new SQLQuery("select d.parent from Descendant d");
        subQuery.addWhere("d.descendant = " + id);
        query.addWhere("r.id in (" + subQuery + ")");
        @SuppressWarnings("unchecked")
		List<Register> ancestors = (List<Register>) JPA.em().createQuery(query.toString()).getResultList();
        //System.out.println(toJson(registers));  //TODO
        return ok(toJson(ancestors));
    }

    @Transactional(readOnly = true)
    public Result getLoads() {
        // generate query
        SQLQuery query = new SQLQuery("select l from Load l");
        @SuppressWarnings("unchecked")
		List<Load> loads = (List<Load>) JPA.em().createQuery(query.toString()).getResultList();
        return ok(toJson(loads));
    }

    /** load db from specified xml file */
    @Transactional
    public Result loadFile() {
        // set default parm values
        String fname = null;
        String flabel = null; 
        Boolean freplace = false;
        Integer replaceid = 0;
        String fowner = "any";
        // extract post parms
        final Map<String, String[]> values = request().body().asFormUrlEncoded();
        if (values != null) {
            try {
                fname = values.get("fname")[0];
                flabel = values.get("flabel")[0]; 
                freplace = "1".equals(values.get("freplace")[0]);
                String replaceidStr = values.get("replaceid")[0];
                if (replaceidStr != null) replaceid =  Integer.valueOf(replaceidStr);
                String fownerStr = values.get("fowner")[0];
                if ((fownerStr != null) && !fownerStr.isEmpty()) fowner = fownerStr;
            } catch (Exception e) {}
        	//System.out.println("--- Application loadFile: fowner=" + fowner + ", fname=" + fname + ", flabel=" + flabel + ", freplace=" + freplace + ", replaceid=" + replaceid);
            //for (String key: values.keySet()) 
            //	System.out.println("  --- key: " + key + ", val[0]=" + values.get(key)[0]);
        }
    	// get ids of any old data using this filename 
    	List<Short> deleteIds = new ArrayList<Short>();
    	if (freplace) {
    		if (replaceid>0) deleteIds.add(replaceid.shortValue());  // replace the specified ID only
    		else deleteIds.addAll(Load.getIdsForFile(fname));  // replace all matching name
    	}
    	Short newId = DataLoader.load(fname, flabel);
    	boolean success = (newId != null);
    	//System.out.println("--- Application loadFile: f=" + fname + ", newId=" + newId + ", sucess=" + success);
    	int delCount = 0;
    	if (freplace && !deleteIds.isEmpty() && success) delCount = Load.deleteIds(deleteIds);
    	if (!success) return redirect(routes.Application.loadPage(true, "XML load failed", fowner));
    	String delStr = (delCount > 0)? " - " + delCount + " elements deleted" : "";
        return redirect(routes.Application.loadPage(false, "Sucessfully loaded XML" + delStr, fowner));  
    }

    /** delete db entries for specified load id */
    @Transactional
    public Result delete(/*Integer id*/) { 
        // set default parm values
        Integer id = null;
        String fowner = "any";
        // extract post parms
        final Map<String, String[]> values = request().body().asFormUrlEncoded();
        if (values != null) {
            try {
                id = Integer.valueOf(values.get("id")[0]);
                String fownerStr = values.get("fowner")[0];
                if ((fownerStr != null) && !fownerStr.isEmpty()) fowner = fownerStr;
            } catch (Exception e) {}
        	//System.out.println("--- Application delete: fowner=" + fowner + ", id=" + id);
        }
    	// exit if id is invalid
    	if ((id == null) || (id < 1)) return redirect(routes.Application.loadPage(true, "No elements deleted", fowner)); 
    	//System.out.println("--- db delete selected --");       	
        int delCount = Load.deleteId(id);
        return redirect(routes.Application.loadPage(false, "Sucessfully deleted " + delCount + " element(s)", fowner));  
    }

    // ------------------ javascript route generator

    public Result jsRoutes() {
        return ok(
            JavaScriptReverseRouter.create("jsRoutes",
              routes.javascript.Application.regsetPage(),
              routes.javascript.Application.loadPage(),
              routes.javascript.Application.registerPage(),
              routes.javascript.Application.filterPage(),
              routes.javascript.Application.getDescendants(),
              routes.javascript.Application.getAncestors(),
              routes.javascript.Application.getFields(),
              routes.javascript.Application.getLoads(),
              routes.javascript.Application.loadFile(),
              routes.javascript.Application.filterRegs(),
              routes.javascript.Application.delete()
            )
        ).as("text/javascript");
    }

}
