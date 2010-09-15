package de.itm.uniluebeck.tr.wiseml.merger.internals.parse.elements;

import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLTag;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReader;

public class PositionParser extends CoordinateParser {

	public PositionParser(WiseMLTreeReader reader) {
		super(reader, WiseMLTag.position);
	}

}
