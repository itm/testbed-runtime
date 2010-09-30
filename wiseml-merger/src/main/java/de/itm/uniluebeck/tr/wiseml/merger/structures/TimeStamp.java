package de.itm.uniluebeck.tr.wiseml.merger.structures;

import org.joda.time.DateTime;
import org.joda.time.Interval;

import de.itm.uniluebeck.tr.wiseml.merger.enums.Unit;

public class TimeStamp {
	
	private int offset;
	private Unit unit;
	private DateTime start;
	private DateTime instant;
	
	private boolean offsetDefined;
	
	public TimeStamp(int offset, Unit unit, DateTime start) {
		this.offset = offset;
		this.unit = unit;
		this.start = start;
		this.offsetDefined = true;
		computeInstant();
	}
	
	public TimeStamp(DateTime instant, Unit unit, DateTime start) {
		this.instant = instant;
		this.unit = unit;
		this.start = start;
		this.offsetDefined = false;
		computeOffset();
	}
	
	public boolean isOffsetDefined() {
		return offsetDefined;
	}

	public int getOffset() {
		return offset;
	}
	
	public Unit getUnit() {
		return unit;
	}
	
	public DateTime getStart() {
		return start;
	}
		
	public DateTime getInstant() {
		return instant;
	}
	
	private void computeInstant() {
		switch (unit) {
		case seconds: 
			instant = start.plusSeconds(offset);
			break;
		case milliseconds: 
			instant = start.plusMillis(offset);
			break;
		}
	}
	
	private void computeOffset() {
		Interval interval = new Interval(start, instant);
		switch (unit) {
		case seconds: 
			offset = (int)(interval.toDurationMillis() / 1000);
			break;
		case milliseconds: 
			offset = (int)(interval.toDurationMillis());
			break;
		}
	}

}
