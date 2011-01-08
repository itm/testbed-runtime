package de.itm.uniluebeck.tr.wiseml.merger.internals.tree.elements;

import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLAttribute;
import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLTag;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLStructureReader;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReader;

public class CustomReader extends WiseMLStructureReader {

	public CustomReader(
			WiseMLTreeReader parent, 
			WiseMLTag tag, 
			WiseMLAttribute[] attributes, 
			String text) {
		super(new Element(parent, tag, attributes, null, text));
	}
	
	

}
