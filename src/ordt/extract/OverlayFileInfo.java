package ordt.extract;

/** class containing overlay input file info */
public class OverlayFileInfo {
	String name;
	String tag;
	String parentTag;
	public OverlayFileInfo(String name, String tag, String parentTag) {
		super();
		this.name = name;
		this.tag = tag;
		this.parentTag = parentTag;
	}
	public String getName() {
		return name;
	}
	public String getTag() {
		return tag;
	}
	public String getParentTag() {
		return parentTag;
	}
	
}
