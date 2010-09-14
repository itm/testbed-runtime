package de.itm.uniluebeck.tr.wiseml.merger.internals.parse.elements;

import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLTag;
import de.itm.uniluebeck.tr.wiseml.merger.internals.parse.WiseMLElementParser;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLElementReader;
import de.itm.uniluebeck.tr.wiseml.merger.structures.Coordinate;

public class CoordinateParser extends WiseMLElementParser<Coordinate> {
	
	private WiseMLTag enclosingTag;

	public CoordinateParser(final WiseMLElementReader reader, final WiseMLTag enclosingTag) {
		super(reader);
		this.enclosingTag = enclosingTag;
	}

	@Override
	protected void parseStructure() {
		WiseMLElementReader reader = null;
		if (enclosingTag == null) {
			reader = this.reader;
		} else {
			assertTag(this.reader, enclosingTag);
			this.reader.nextSubElementReader();
			reader = (WiseMLElementReader)this.reader.getSubElementReader();
		}
		assertTag(reader, WiseMLTag.coordinate);
		
		structure = new Coordinate();
		
		reader.nextSubElementReader();
		structure.setX(parseDoubleElement((WiseMLElementReader)reader.getSubElementReader()));
		
		reader.nextSubElementReader();
		structure.setY(parseDoubleElement((WiseMLElementReader)reader.getSubElementReader()));
		
		while (!reader.isFinished()) {
			reader.nextSubElementReader();
			WiseMLElementReader nextReader = (WiseMLElementReader)reader.getSubElementReader();
			
			switch (nextReader.getTag()) {
			case z: structure.setZ(parseDoubleElement(nextReader)); break;
			case phi: structure.setPhi(parseDoubleElement(nextReader)); break;
			case theta: structure.setTheta(parseDoubleElement(nextReader)); break;
			}
		}
	}
	
	private Double parseDoubleElement(final WiseMLElementReader reader) {
		String text = ((WiseMLElementReader)reader).getText();
		try {
			return Double.parseDouble(text);
		} catch (NumberFormatException e) {
			reader.exception("could not parse coordinate", e);
		}
		return null;
	}

}
