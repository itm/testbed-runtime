package de.itm.uniluebeck.tr.wiseml.merger.internals.parse.elements;

import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLTag;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReader;


public class OriginParser extends CoordinateParser {

	public OriginParser(WiseMLTreeReader reader) {
		super(reader, WiseMLTag.origin);
	}

}
