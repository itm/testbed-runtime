package de.itm.uniluebeck.tr.wiseml.merger.internals.parse;

import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLTag;

public interface ParserCallback {
	void nextStructure(WiseMLTag tag, Object obj);
}
