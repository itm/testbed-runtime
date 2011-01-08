package de.itm.uniluebeck.tr.wiseml.merger.internals.tree.elements;

import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLTag;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLStructureReader;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReader;
import de.itm.uniluebeck.tr.wiseml.merger.structures.TimeStamp;

public class TimeStampReader extends WiseMLStructureReader {

	public TimeStampReader(WiseMLTreeReader parent, TimeStamp timestamp) {
		super(new Element(
				parent, 
				WiseMLTag.timestamp, 
				null, 
				null,
				timestamp.toString()));
	}

}
