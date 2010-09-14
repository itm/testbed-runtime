package de.itm.uniluebeck.tr.wiseml.merger.internals.parse.elements;

import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLTag;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLElementReader;


public class OriginParser extends CoordinateParser {

	public OriginParser(WiseMLElementReader reader) {
		super(reader, WiseMLTag.origin);
	}

}
