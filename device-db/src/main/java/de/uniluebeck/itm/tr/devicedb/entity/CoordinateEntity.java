package de.uniluebeck.itm.tr.devicedb.entity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import eu.wisebed.wiseml.Coordinate;

@Entity
public class CoordinateEntity {
	
	@Id
    @GeneratedValue
    protected long id;

	protected double x;

	protected double y;

	protected double z;

	protected double phi;

	protected double theta;
	
	public static CoordinateEntity fromCoordinate(final Coordinate position) {
		if ( position == null) return null;
		
		final CoordinateEntity entity = new CoordinateEntity();
		entity.x = position.getX();
		entity.y = position.getY();
		entity.z = position.getZ();
		entity.phi = position.getPhi();
		entity.theta = position.getTheta();
		return entity;
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
