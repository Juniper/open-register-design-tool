package models;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.*;
//import javax.validation.Constraint;

import play.db.jpa.JPA;
import play.db.jpa.Transactional;

/** class containing loaded xml file info (including self) */
@Entity
public class Load {

	@Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
	protected short id;

	protected Timestamp timestamp;
	protected String file;
	protected String label;
	protected String user;
	protected Integer rootRegId;

	public short getId() {
		return id;
	}

	public void setId(short id) {
		this.id = id;
	}

	public Timestamp getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Timestamp timestamp) {
		this.timestamp = timestamp;
	}

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public Integer getRootRegId() {
		return rootRegId;
	}

	public void setRootRegId(Integer rootRegId) {
		this.rootRegId = rootRegId;
	}

    /** return data Ids matching the specified file name */
    @Transactional(readOnly = true)
	public static List<Short> getIdsForFile(String fname) {
    	List<Short> ids = new ArrayList<Short>();
        // generate query
        SQLQuery query = new SQLQuery("select l from Load l");
        query.addWhere("file = '" + fname + "'");
        @SuppressWarnings("unchecked")
		List<Load> loads = (List<Load>) JPA.em().createQuery(query.toString()).getResultList();
        for (Load ld:loads) ids.add(ld.id);
		return ids;
	}

    /** delete all loads with given ids */
    @Transactional
	public static int deleteIds(List<Short> ids) {
    	int delCount = 0;
    	if ((ids == null) || ids.isEmpty()) return delCount;
      	SQLQuery query = new SQLQuery("delete from Load");  
        if (ids.size() == 1) query.addWhere("id = " + ids.get(0));
        else {
        	String idStr = SQLQuery.toShortString(ids);
        	query.addWhere("id in (" + idStr + ")");
        }
        delCount = JPA.em().createQuery(query.toString()).executeUpdate();
    	//System.out.println("--- Load deleteIds: removed " + delCount + " load rows");
        return delCount;
  	}

    /** delete all loads with given integer id */
	public static int deleteId(Integer id) {
		if (id == null) return 0;
    	List<Short> ids = new ArrayList<Short>();
    	ids.add(id.shortValue());
		return deleteIds(ids);		
	}

}
