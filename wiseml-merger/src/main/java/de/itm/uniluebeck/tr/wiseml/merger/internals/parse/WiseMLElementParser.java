package de.itm.uniluebeck.tr.wiseml.merger.internals.parse;

import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLTag;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLElementReader;

public abstract class WiseMLElementParser<T> {
	
	protected WiseMLElementReader reader;
	protected T structure;
	
	public WiseMLElementParser(final WiseMLElementReader reader) {
		this.reader = reader;
		this.structure = null;
 	}
	
	public T getParsedStructure() {
		if (structure == null) {
			parseStructure();
		}
		return structure;
	}
	
	protected abstract void parseStructure();

	protected static void assertTag(WiseMLElementReader reader, WiseMLTag tag) {
		if (!tag.equals(reader.getTag())) {
			throw new RuntimeException("expected <"+tag.getLocalName()+">, got <"+reader.getTag()+">");
		}
	}

}
