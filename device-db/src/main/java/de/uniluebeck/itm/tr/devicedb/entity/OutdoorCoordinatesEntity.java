package de.uniluebeck.itm.tr.devicedb.entity;

import javax.annotation.Nullable;
import javax.persistence.*;

@Cacheable
@Entity(name = "OutdoorCoordinates")
public class OutdoorCoordinatesEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@Nullable
	private Double longitude;

	@Nullable
	private Double latitude;

	@Nullable
	private Double x;

	@Nullable
	private Double y;

	@Nullable
	private Double z;

	@Nullable
	private Double rho;

	@Nullable
	private Double phi;

	@Nullable
	private Double theta;

	public Long getId() {
		return id;
	}

	public void setId(final Long id) {
		this.id = id;
	}

	@Nullable
	public Double getLatitude() {
		return latitude;
	}

	public void setLatitude(@Nullable final Double latitude) {
		this.latitude = latitude;
	}

	@Nullable
	public Double getLongitude() {
		return longitude;
	}

	public void setLongitude(@Nullable final Double longitude) {
		this.longitude = longitude;
	}

	@Nullable
	public Double getPhi() {
		return phi;
	}

	public void setPhi(@Nullable final Double phi) {
		this.phi = phi;
	}

	@Nullable
	public Double getRho() {
		return rho;
	}

	public void setRho(@Nullable final Double rho) {
		this.rho = rho;
	}

	@Nullable
	public Double getTheta() {
		return theta;
	}

	public void setTheta(@Nullable final Double theta) {
		this.theta = theta;
	}

	@Nullable
	public Double getX() {
		return x;
	}

	public void setX(@Nullable final Double x) {
		this.x = x;
	}

	@Nullable
	public Double getY() {
		return y;
	}

	public void setY(@Nullable final Double y) {
		this.y = y;
	}

	@Nullable
	public Double getZ() {
		return z;
	}

	public void setZ(@Nullable final Double z) {
		this.z = z;
	}
}
