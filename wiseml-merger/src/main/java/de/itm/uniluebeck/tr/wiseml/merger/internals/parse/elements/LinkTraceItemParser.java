package de.itm.uniluebeck.tr.wiseml.merger.internals.parse.elements;

import de.itm.uniluebeck.tr.wiseml.merger.internals.parse.WiseMLElementParser;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReader;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReaderHelper;
import de.itm.uniluebeck.tr.wiseml.merger.structures.LinkTraceItem;

public class LinkTraceItemParser extends WiseMLElementParser<LinkTraceItem> {

	public LinkTraceItemParser(WiseMLTreeReader reader) {
		super(reader);
	}

	@Override
	protected void parseStructure() {
		structure = new LinkTraceItem(
				WiseMLTreeReaderHelper.getAttributeValue(
						reader.getAttributeList(), "source"),
				WiseMLTreeReaderHelper.getAttributeValue(
						reader.getAttributeList(), "target"));
		while (reader.nextSubElementReader()) {
			switch (reader.getSubElementReader().getTag()) {
			case rssi:
				structure.setRssi(reader.getSubElementReader().getText());
				break;
			case data:
				structure.addDataItem(
						WiseMLTreeReaderHelper.getAttributeValue(
								reader.getSubElementReader().getAttributeList(), 
								"key"),
						reader.getSubElementReader().getText());
				break;
			}
		}
	}

}
