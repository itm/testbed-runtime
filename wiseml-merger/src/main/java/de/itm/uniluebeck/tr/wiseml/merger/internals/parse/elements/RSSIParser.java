package de.itm.uniluebeck.tr.wiseml.merger.internals.parse.elements;

import de.itm.uniluebeck.tr.wiseml.merger.enums.DataType;
import de.itm.uniluebeck.tr.wiseml.merger.enums.Unit;
import de.itm.uniluebeck.tr.wiseml.merger.internals.parse.WiseMLElementParser;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReader;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReaderHelper;
import de.itm.uniluebeck.tr.wiseml.merger.structures.RSSI;

public class RSSIParser extends WiseMLElementParser<RSSI> {

	public RSSIParser(WiseMLTreeReader reader) {
		super(reader);
	}

	@Override
	protected void parseStructure() {
		structure = new RSSI();
		structure.setDataType(DataType.valueOf(
				WiseMLTreeReaderHelper.getAttributeValue(
						reader.getAttributeList(), "datatype")));
		structure.setUnit(Unit.valueOf(
				WiseMLTreeReaderHelper.getAttributeValue(
						reader.getAttributeList(), "unit")));
		structure.setDefaultValue(
				WiseMLTreeReaderHelper.getAttributeValue(
						reader.getAttributeList(), "default"));
	}

}
