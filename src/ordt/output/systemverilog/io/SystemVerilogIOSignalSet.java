package ordt.output.systemverilog.io;

import java.util.ArrayList;
import java.util.List;

public class SystemVerilogIOSignalSet extends SystemVerilogIOElement {
	protected List<SystemVerilogIOElement> childList = new ArrayList<SystemVerilogIOElement>(); // signals/signalsets in this signalset

	@Override
	public String getDefName(String prefix) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<SystemVerilogIOSignal> getIOSignals(Integer fromLoc, Integer toLoc) {
		// TODO Auto-generated method stub
		return null;
	}

}
