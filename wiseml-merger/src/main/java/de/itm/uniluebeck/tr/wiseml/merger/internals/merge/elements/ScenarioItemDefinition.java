package de.itm.uniluebeck.tr.wiseml.merger.internals.merge.elements;

import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReader;
import de.itm.uniluebeck.tr.wiseml.merger.structures.TimeStamp;

public class ScenarioItemDefinition 
implements Comparable<ScenarioItemDefinition>{
	
	private TimeStamp timeStamp;
	private WiseMLTreeReader parentReader;

	public ScenarioItemDefinition(TimeStamp timeStamp,
			WiseMLTreeReader parentReader) {
		this.timeStamp = timeStamp;
		this.parentReader = parentReader;
	}

	@Override
	public int compareTo(ScenarioItemDefinition o) {
		return timeStamp.getInstant().compareTo(o.timeStamp.getInstant());
	}

	public TimeStamp getTimeStamp() {
		return timeStamp;
	}

	public WiseMLTreeReader getParentReader() {
		return parentReader;
	}

}
