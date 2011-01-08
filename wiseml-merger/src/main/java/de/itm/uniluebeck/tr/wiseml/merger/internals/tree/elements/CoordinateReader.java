package de.itm.uniluebeck.tr.wiseml.merger.internals.tree.elements;

import de.itm.uniluebeck.tr.wiseml.merger.internals.WiseMLTag;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLStructureReader;
import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReader;
import de.itm.uniluebeck.tr.wiseml.merger.structures.Coordinate;

public class CoordinateReader extends WiseMLStructureReader {
	
	public CoordinateReader(
			final WiseMLTreeReader parent, 
			final WiseMLTag enclosingTag,
			final Coordinate coordinate) {
		super(new Element( // <?>
				parent, 
				enclosingTag,
				null,
				getDoubleElementArray(coordinate),
				null));
	}
	
	private static Element[] getDoubleElementArray(Coordinate coordinate) {
		// count elements
		int count = 2;
		if (coordinate.getZ() != null) count++;
		if (coordinate.getPhi() != null) count++;
		if (coordinate.getTheta() != null) count++;
		
		// create elements
		int index = 0;
		Element[] result = new Element[count];
		result[index++] = createDoubleElement(coordinate.getX(), WiseMLTag.x);
		result[index++] = createDoubleElement(coordinate.getY(), WiseMLTag.y);
		if (coordinate.getZ() != null) result[index++] = createDoubleElement(coordinate.getZ(), WiseMLTag.z);
		if (coordinate.getPhi() != null) result[index++] = createDoubleElement(coordinate.getPhi(), WiseMLTag.phi);
		if (coordinate.getTheta() != null) result[index++] = createDoubleElement(coordinate.getTheta(), WiseMLTag.theta);
		
		return result;
		
	}
	
	private static Element createDoubleElement(final Double d, final WiseMLTag tag) {
		return new Element(
				null,
				tag,
				null,
				null,
				d.toString());
	}

}
