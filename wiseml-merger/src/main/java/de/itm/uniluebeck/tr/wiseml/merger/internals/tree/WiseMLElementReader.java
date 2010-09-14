package de.itm.uniluebeck.tr.wiseml.merger.internals.tree;

import java.util.List;

import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLAttribute;
import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLTag;

public interface WiseMLElementReader extends WiseMLTreeReader {
	
	public List<WiseMLAttribute> getAttributeList();
	
	public WiseMLTag getTag();
	public String getText();

}
