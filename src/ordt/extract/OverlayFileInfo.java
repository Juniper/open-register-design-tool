package ordt.extract;

/** class containing overlay input file info */
public class OverlayFileInfo {
	String name;
	String tag;
	public OverlayFileInfo(String name, String tag) {
		super();
		this.name = name;
		this.tag = tag;
	}
	public String getName() {
		return name;
	}
	public String getTag() {
		return tag;
	}
}
