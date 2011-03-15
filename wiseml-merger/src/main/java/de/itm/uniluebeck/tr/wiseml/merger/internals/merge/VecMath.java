package de.itm.uniluebeck.tr.wiseml.merger.internals.merge;

import de.itm.uniluebeck.tr.wiseml.merger.structures.Coordinate;

public class VecMath {
	
	public static Coordinate computeCenter(Coordinate... coordinates) {
		if (coordinates.length == 0) {
			return null;
		}
		double x = 0;
		double y = 0;
		double z = 0;
		int hits = 0;
		boolean hasZ = false;
		
		for (int i = 0; i < coordinates.length; i++) {
			if (coordinates[i] != null) {
				x += coordinates[i].getX().doubleValue();
				y += coordinates[i].getY().doubleValue();
				
				if (coordinates[i].getZ() != null) {
					z += coordinates[i].getZ().doubleValue();
					hasZ = true;
				}
				
//				if (coordinates[i].getPhi() != null
//					|| coordinates[i].getTheta() != null) {
//					throw new IllegalArgumentException(
//							"coordinates with angles not supported");
//				}
				
				hits++;
			}
		}

		Coordinate result = new Coordinate();
		
		result.setX(Double.valueOf(x / hits));
		result.setY(Double.valueOf(y / hits));
		
		if (hasZ) {
			result.setZ(Double.valueOf(z / hits));
		}
		
		result.setPhi(0.0);
		result.setTheta(0.0);
		
		return result;
	}

}
