package de.itm.uniluebeck.tr.wiseml.merger.internals.merge.elements;

import de.itm.uniluebeck.tr.wiseml.merger.structures.TimeStamp;

public class TimeStampedItemDefinition 
implements Comparable<TimeStampedItemDefinition>{
	
	private TimeStamp timeStamp;
	//private WiseMLTreeReader parentReader;
	private int inputIndex;

	public TimeStampedItemDefinition(TimeStamp timeStamp,
			//WiseMLTreeReader parentReader,
			int inputIndex) {
		this.timeStamp = timeStamp;
		//this.parentReader = parentReader;
		this.inputIndex = inputIndex;
	}

	@Override
	public int compareTo(TimeStampedItemDefinition o) {
		return timeStamp.getInstant().compareTo(o.timeStamp.getInstant());
	}

	public TimeStamp getTimeStamp() {
		return timeStamp;
	}
/*
	public WiseMLTreeReader getParentReader() {
		return parentReader;
	}
*/
	public int getInputIndex() {
		return inputIndex;
	}

}
