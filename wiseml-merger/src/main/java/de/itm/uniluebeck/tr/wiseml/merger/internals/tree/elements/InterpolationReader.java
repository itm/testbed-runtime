package de.itm.uniluebeck.tr.wiseml.merger.internals.tree.elements;

import de.itm.uniluebeck.tr.wiseml.merger.enums.Interpolation;
import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLTag;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLStructureReader;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReader;

public class InterpolationReader extends WiseMLStructureReader {

	public InterpolationReader(
			final WiseMLTreeReader parent, 
			final Interpolation interpolation) {
		super(createPureTextElement(parent, WiseMLTag.interpolation, interpolation.name()));
	}

}
