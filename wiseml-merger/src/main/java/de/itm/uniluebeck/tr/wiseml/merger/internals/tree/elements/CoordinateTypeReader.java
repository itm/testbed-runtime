package de.itm.uniluebeck.tr.wiseml.merger.internals.tree.elements;

import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLTag;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLStructureReader;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReader;


public class CoordinateTypeReader extends WiseMLStructureReader {

	public CoordinateTypeReader(
			final WiseMLTreeReader parent, 
			final String coordinateType) {
		super(createPureTextElement(parent, WiseMLTag.coordinateType, coordinateType));
	}

}
