package de.uniluebeck.itm.tr.devicedb.dto;

import eu.wisebed.wiseml.Coordinate;

public class CoordinateDto {

	protected double x;

	protected double y;

	protected Double z;

	protected Double phi;

	protected Double theta;

	public static CoordinateDto fromCoordinate(final Coordinate position) {
		final CoordinateDto dto = new CoordinateDto();
		dto.x = position.getX();
		dto.y = position.getY();
		dto.z = position.getZ();
		dto.phi = position.getPhi();
		dto.theta = position.getTheta();
		return dto;
	}

	public Coordinate toCoordinate() {
		final Coordinate coordinate = new Coordinate();
		coordinate.setX(x);
		coordinate.setY(y);
		coordinate.setZ(z);
		coordinate.setPhi(phi);
		coordinate.setTheta(theta);
		return coordinate;
	}
}
