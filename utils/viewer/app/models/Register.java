package models;

import javax.persistence.*;

@Entity
public class Register {

	@Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
	public int id;

    public String name;
    public Long baseAddr;
    public Long highAddr;
    public Short width;
    public Boolean extRoot = false;
    public boolean isRegset = false;
    public Boolean isMap = false;
    public int reps = 1;
    public int stride = 4;
    public String shortText;
    public String longText = ""; // init since we'll append
	public byte depth;
	public short loadId;
    public String access;
    public Boolean alias = false;
	public Integer catCode = 0;
    public Boolean donttest = false;
    public String parentPath;
}
