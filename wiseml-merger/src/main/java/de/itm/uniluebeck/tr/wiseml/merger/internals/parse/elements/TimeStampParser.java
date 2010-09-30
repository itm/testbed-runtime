package de.itm.uniluebeck.tr.wiseml.merger.internals.parse.elements;

import org.joda.time.DateTime;

import de.itm.uniluebeck.tr.wiseml.merger.internals.parse.WiseMLElementParser;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReader;
import de.itm.uniluebeck.tr.wiseml.merger.structures.TimeInfo;
import de.itm.uniluebeck.tr.wiseml.merger.structures.TimeStamp;

public class TimeStampParser extends WiseMLElementParser<TimeStamp> {

	private TimeInfo timeInfo;
	
	public TimeStampParser(WiseMLTreeReader reader, TimeInfo timeInfo) {
		super(reader);
		this.timeInfo = timeInfo;
	}

	@Override
	protected void parseStructure() {
		try {
			structure = new TimeStamp(
					Integer.parseInt(reader.getText()), 
					timeInfo.getUnit(), 
					timeInfo.getStart());
		} catch (NumberFormatException e) {
			structure = new TimeStamp(
					new DateTime(reader.getText()),
					timeInfo.getUnit(),
					timeInfo.getStart());
		}
	}

}
