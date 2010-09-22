package de.itm.uniluebeck.tr.wiseml.merger.internals.parse.elements;

import de.itm.uniluebeck.tr.wiseml.merger.enums.Unit;
import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLTag;
import de.itm.uniluebeck.tr.wiseml.merger.internals.parse.WiseMLElementParser;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReader;
import de.itm.uniluebeck.tr.wiseml.merger.structures.TimeInfo;

public class TimeInfoParser extends WiseMLElementParser<TimeInfo> {

	public TimeInfoParser(WiseMLTreeReader reader) {
		super(reader);
	}

	@Override
	protected void parseStructure() {
		reader.nextSubElementReader(); // <start>
		String startText = reader.getSubElementReader().getText();
		
		reader.nextSubElementReader(); // <end> or <duration>
		boolean endDefined = (reader.getSubElementReader().getTag().equals(WiseMLTag.end));
		String end = null;
		String duration = null;
		if (endDefined) {
			end = reader.getSubElementReader().getText();
		} else {
			duration = reader.getSubElementReader().getText();
		}
		
		reader.nextSubElementReader(); // <unit>
		Unit unit = Unit.valueOf(reader.getSubElementReader().getText());
		
		// TODO
		this.structure = new TimeInfo(); // TODO
	}

}
