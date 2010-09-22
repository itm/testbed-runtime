package de.itm.uniluebeck.tr.wiseml.merger.internals.parse.elements;

import de.itm.uniluebeck.tr.wiseml.merger.internals.parse.WiseMLElementParser;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReader;

public class DescriptionParser extends WiseMLElementParser<String> {

	public DescriptionParser(WiseMLTreeReader reader) {
		super(reader);
	}

	@Override
	protected void parseStructure() {
		this.structure = reader.getText();
	}

}
