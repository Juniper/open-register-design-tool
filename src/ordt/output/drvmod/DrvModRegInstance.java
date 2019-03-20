package ordt.output.drvmod;

import java.util.ArrayList;
import java.util.List;

import ordt.extract.RegNumber;
import ordt.extract.model.ModRegister;

public class DrvModRegInstance extends DrvModBaseInstance {
	
	private int width = ModRegister.defaultWidth;
	private List<DrvModField> fields = new ArrayList<DrvModField>();
	
	DrvModRegInstance(String name, int mapId, int width) {
		super(name, mapId);
		this.width=width;
	}

	public List<DrvModField> getFields() {
		return fields;
	}

	public void addField(String name, int lowIndex, int width, boolean readable, boolean writeable, RegNumber reset) {
		this.fields.add(new DrvModField(name, lowIndex, width, readable, writeable, reset));
	}
	
	public int getWidth() {
		return width;
	}

	@Override
	/** walk tree and process instances matching map/reg criteria */
	public void process(Integer mapId, boolean regsOnly, boolean processOnce) {
		if (!processOnce || !hasBeenProcessed) {
			builder.processRegInstance(this);
			hasBeenProcessed = true;
		}
	}

	public class DrvModField {
		public String name;
		public int lowIndex;
		public int width;
		public boolean readable;
		public boolean writeable;
		public RegNumber reset;
		
		private DrvModField(String name, int lowIndex, int width, boolean readable, boolean writeable, RegNumber reset) {
			super();
			this.name = name;
			this.lowIndex = lowIndex;
			this.width = width;
			this.readable = readable;
			this.writeable = writeable;
			this.reset = reset;
		}

		@Override
		public int hashCode() {  // removed outertype from hash calculation
			final int prime = 31;
			int result = 1;
			result = prime * result + lowIndex;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result + (readable ? 1231 : 1237);
			result = prime * result + width;
			result = prime * result + (writeable ? 1231 : 1237);
			result = prime * result + (((reset == null) || !reset.isDefined()) ? 0 : reset.getValue().hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {  // removed outertype from equals calculation
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			DrvModField other = (DrvModField) obj;
			if (lowIndex != other.lowIndex)
				return false;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			if (readable != other.readable)
				return false;
			if (width != other.width)
				return false;
			if (writeable != other.writeable)
				return false;
			if ((reset == null) || !reset.isDefined()) {
				if ((other.reset != null) && other.reset.isDefined())
					return false;
			} else if (!reset.getValue().equals(other.reset.getValue()))
				return false;
			return true;
		}

		public String isReadable() {
			return readable? "true" : "false";
		}

		public String isWriteable() {
			return writeable? "true" : "false";
		}
		
	}
	
	@Override
	public int hashCode() {  
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((fields == null) ? 0 : fields.hashCode());
		result = prime * result + width;
		//System.out.println("DrvModRegInstance hashCode:  return=" + result + ", name=" + getName() );
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj)) 
			return false;
		if (getClass() != obj.getClass())
			return false;
		DrvModRegInstance other = (DrvModRegInstance) obj;
		if (fields == null) {
			if (other.fields != null)
				return false;
		} else if (!fields.equals(other.fields))
			return false;
		if (width != other.width)
			return false;
		return true;
	}

}
