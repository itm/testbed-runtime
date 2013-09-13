package de.uniluebeck.itm.tr.devicedb.entity;

import eu.wisebed.wiseml.Coordinate;

import javax.persistence.*;

@Entity(name="Coordinate")
@Cacheable
public class CoordinateEntity {
	
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;

	private double x;

	private double y;

	private Double z;

	private Double phi;

	private Double theta;
	
	public CoordinateEntity() {
		
	}
	
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
	
	public long getId() {
		return id;
	}
}
