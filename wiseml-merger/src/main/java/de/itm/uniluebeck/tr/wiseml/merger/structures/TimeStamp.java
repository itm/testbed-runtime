package de.itm.uniluebeck.tr.wiseml.merger.structures;

import org.joda.time.DateTime;

import de.itm.uniluebeck.tr.wiseml.merger.enums.Unit;

public class TimeStamp {
	
	private long offset;
	private Unit unit;
	private DateTime start;
	
	public long getOffset() {
		return offset;
	}
	
	public Unit getUnit() {
		return unit;
	}
	
	public DateTime getStart() {
		return start;
	}
	
	public void setOffset(long offset) {
		this.offset = offset;
	}
	
	public void setUnit(Unit unit) {
		this.unit = unit;
	}
	
	public void setStart(DateTime start) {
		this.start = start;
	}

}
