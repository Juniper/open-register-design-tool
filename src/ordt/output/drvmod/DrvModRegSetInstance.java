package ordt.output.drvmod;

import java.util.ArrayList;
import java.util.List;

public class DrvModRegSetInstance extends DrvModBaseInstance {
	
	List<DrvModBaseInstance> children = new ArrayList<DrvModBaseInstance>();
	
	DrvModRegSetInstance(String name, int mapId, long addressOffset, int reps, long addressStride) {
		super(name, mapId, addressOffset, reps, addressStride);
	}

	public List<DrvModBaseInstance> getChildren() {
		return children;
	}

	public void addChild(DrvModBaseInstance child) {
		this.children.add(child);
	}

}
