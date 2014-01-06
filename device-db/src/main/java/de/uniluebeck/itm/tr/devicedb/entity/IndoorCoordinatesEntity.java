package de.uniluebeck.itm.tr.devicedb.entity;

import javax.annotation.Nullable;
import javax.persistence.*;

@Cacheable
@Entity(name = "IndoorCoordinates")
public class IndoorCoordinatesEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@Nullable
	private String building;

	@Nullable
	private String floor;

	@Nullable
	private String room;

	private double x;

	private double y;

	@Nullable
	private Double z;

	@Nullable
	private String backgroundImage;

	@Nullable
	public String getBackgroundImage() {
		return backgroundImage;
	}

	public void setBackgroundImage(@Nullable final String backgroundImage) {
		this.backgroundImage = backgroundImage;
	}

	@Nullable
	public String getBuilding() {
		return building;
	}

	public void setBuilding(@Nullable final String building) {
		this.building = building;
	}

	@Nullable
	public String getFloor() {
		return floor;
	}

	public void setFloor(@Nullable final String floor) {
		this.floor = floor;
	}

	public Long getId() {
		return id;
	}

	public void setId(final Long id) {
		this.id = id;
	}

	@Nullable
	public String getRoom() {
		return room;
	}

	public void setRoom(@Nullable final String room) {
		this.room = room;
	}

	public Double getX() {
		return x;
	}

	public void setX(final double x) {
		this.x = x;
	}

	public Double getY() {
		return y;
	}

	public void setY(final double y) {
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
