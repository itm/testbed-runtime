package de.uniluebeck.itm.tr.devicedb.dto;

import eu.wisebed.wiseml.Coordinate;

public class CoordinateDto {

	private double x;

	private double y;

	private Double z;

	private Double phi;

	private Double theta;

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

	public double getX() {
		return x;
	}

	public void setX(double x) {
		this.x = x;
	}

	public double getY() {
		return y;
	}

	public void setY(double y) {
		this.y = y;
	}

	public Double getZ() {
		return z;
	}

	public void setZ(Double z) {
		this.z = z;
	}

	public Double getPhi() {
		return phi;
	}

	public void setPhi(Double phi) {
		this.phi = phi;
	}

	public Double getTheta() {
		return theta;
	}

	public void setTheta(Double theta) {
		this.theta = theta;
	}
	
}	
