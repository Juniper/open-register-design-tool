package ordt.output.drvmod;

import java.util.ArrayList;
import java.util.List;

public class DrvModRegInstance extends DrvModBaseInstance {
	
	private List<DrvModField> fields = new ArrayList<DrvModField>();
	
	DrvModRegInstance(String name, int mapId, long addressOffset, int reps, long addressStride) {
		super(name, mapId, addressOffset, reps, addressStride);
	}

	public List<DrvModField> getFields() {
		return fields;
	}

	public void addField(String name, int lowIndex, int width, boolean readable, boolean writeable) {
		this.fields.add(new DrvModField(name, lowIndex, width, readable, writeable));
	}
	
	public class DrvModField {
		public String name;
		public int lowIndex;
		public int width;
		public boolean readable;
		public boolean writeable;
		
		private DrvModField(String name, int lowIndex, int width, boolean readable, boolean writeable) {
			super();
			this.name = name;
			this.lowIndex = lowIndex;
			this.width = width;
			this.readable = readable;
			this.writeable = writeable;
		}
		
	}

}
