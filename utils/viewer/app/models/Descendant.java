
package models;

import javax.persistence.*;
//import javax.validation.Constraint;

/** class containing parent-descendent pairs (including self) */
@Entity
public class Descendant {
	@Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
	public long id;

    public int parent;
    public int descendant;

}
