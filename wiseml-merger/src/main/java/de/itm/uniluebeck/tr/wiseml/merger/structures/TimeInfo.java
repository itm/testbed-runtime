package de.itm.uniluebeck.tr.wiseml.merger.structures;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;

import de.itm.uniluebeck.tr.wiseml.merger.enums.Unit;

public class TimeInfo {
	
	private DateTime start;
	private DateTime end;
	private long duration;
	private Unit unit;
	private boolean endDefined;
	
	public TimeInfo(String start, String end, Unit unit) {
		this(new DateTime(start), new DateTime(end), unit);
	}
	
	public TimeInfo(String start, long duration, Unit unit) {
		this(new DateTime(start), duration, unit);
	}
	
	public TimeInfo(DateTime start, DateTime end, Unit unit) {
		this.start = start;
		this.end = end;
		this.unit = unit;
		
		this.duration = convertMillisToDuration(new Interval(start, end).toDurationMillis(), unit);
		this.endDefined = true;
	}
	
	public TimeInfo(DateTime start, long duration, Unit unit) {
		this.start = start;
		this.duration = duration;
		this.unit = unit;
		
		this.end = new Interval(start, new Duration(convertDurationToMillis(duration, unit))).getEnd();
		this.endDefined = false;
	}
	
	public DateTime getStart() {
		return start;
	}

	public DateTime getEnd() {
		return end;
	}

	public long getDuration() {
		return duration;
	}

	public Unit getUnit() {
		return unit;
	}

	public boolean isEndDefined() {
		return endDefined;
	}

	private static long convertMillisToDuration(long millis, Unit unit) {
		switch (unit) {
		case milliseconds: return millis;
		case seconds: return millis / 1000;
		default: throw new IllegalArgumentException("'"+unit+"' is not a time unit");
		}
	}
	
	private static long convertDurationToMillis(long duration, Unit unit) {
		switch (unit) {
		case milliseconds: return duration;
		case seconds: return duration * 1000;
		default: throw new IllegalArgumentException("'"+unit+"' is not a time unit");
		}
	}

}
