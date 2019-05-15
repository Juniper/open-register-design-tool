package models;

import javax.persistence.*;

@Entity
public class Field {

	@Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
	public int id;

    public String name;
    public Short lowIdx;
    public Short width;
    public String access;
    public String rst;
    public Boolean hwMod;
    public Boolean dontcompare;
	public Integer subCatCode = 0;
    public Boolean counter;
    public Boolean intr;
    public String shortText;
    public String longText = "";  // init since we'll append
	public int regId;
}
