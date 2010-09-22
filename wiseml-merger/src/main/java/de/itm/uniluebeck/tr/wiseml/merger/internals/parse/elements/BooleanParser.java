package de.itm.uniluebeck.tr.wiseml.merger.internals.parse.elements;

import de.itm.uniluebeck.tr.wiseml.merger.internals.parse.WiseMLElementParser;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReader;

public class BooleanParser extends WiseMLElementParser<Boolean> {

	public BooleanParser(WiseMLTreeReader reader) {
		super(reader);
	}

	@Override
	protected void parseStructure() {
		String value = reader.getText().toLowerCase();
		if (value.equals("true") || value.equals("false")) {
			structure = Boolean.valueOf(value);
		}
		try {
			Integer intValue = Integer.parseInt(value);
			structure = Boolean.valueOf(intValue != 0);
		} catch (NumberFormatException e) {
			// not an integer
		}
		if (value.equals("yes")) {
			structure = Boolean.TRUE;
		}
		if (value.equals("no")) {
			structure = Boolean.FALSE;
		}
		reader.exception("could not parse boolean value", null);
	}

}
