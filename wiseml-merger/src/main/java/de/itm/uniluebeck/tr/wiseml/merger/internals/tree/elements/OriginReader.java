package de.itm.uniluebeck.tr.wiseml.merger.internals.tree.elements;

import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLTag;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReader;
import de.itm.uniluebeck.tr.wiseml.merger.structures.Coordinate;

public class OriginReader extends CoordinateReader {

	public OriginReader(WiseMLTreeReader parent, Coordinate coordinate) {
		super(parent, WiseMLTag.origin, coordinate);
	}

}
