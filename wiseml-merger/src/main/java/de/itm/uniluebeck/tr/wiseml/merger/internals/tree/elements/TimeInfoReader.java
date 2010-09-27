package de.itm.uniluebeck.tr.wiseml.merger.internals.tree.elements;

import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLTag;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLStructureReader;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReader;
import de.itm.uniluebeck.tr.wiseml.merger.structures.TimeInfo;

public class TimeInfoReader extends WiseMLStructureReader {

	public TimeInfoReader(
			final WiseMLTreeReader parent, 
			final TimeInfo timeInfo) {
		super(new Element(parent, WiseMLTag.timeinfo, null, new Element[]{
				createPureTextElement(
						null, 
						WiseMLTag.start, 
						timeInfo.getStart().toString()),
				((timeInfo.isEndDefined())?
						createPureTextElement(
								null,
								WiseMLTag.end,
								timeInfo.getEnd().toString())
						:
						createPureTextElement(
								null,
								WiseMLTag.duration,
								Long.toString(timeInfo.getDuration()))),
				createPureTextElement(
						null,
						WiseMLTag.unit,
						timeInfo.getUnit().name())
		}, null));
	}

}
