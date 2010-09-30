package de.itm.uniluebeck.tr.wiseml.merger.internals.tree.elements;

import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLTag;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLStructureReader;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReader;

public class DescriptionReader extends WiseMLStructureReader {

	public DescriptionReader(WiseMLTreeReader parent, String description) {
		super(createPureTextElement(parent, WiseMLTag.description, description));
	}

}
