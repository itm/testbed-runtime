package de.uniluebeck.itm.tr.devicedb.entity;

import javax.annotation.Nullable;
import javax.persistence.*;

@Cacheable
@Entity(name = "Coordinate")
public class CoordinateEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@Nullable
	@OneToOne(cascade = CascadeType.ALL)
	private OutdoorCoordinatesEntity outdoorCoordinates;

	@Nullable
	@OneToOne(cascade = CascadeType.ALL)
	private IndoorCoordinatesEntity indoorCoordinates;

	public long getId() {
		return id;
	}

	public void setId(final Long id) {
		this.id = id;
	}

	@Nullable
	public IndoorCoordinatesEntity getIndoorCoordinates() {
		return indoorCoordinates;
	}

	public void setIndoorCoordinates(@Nullable final IndoorCoordinatesEntity indoorCoordinates) {
		this.indoorCoordinates = indoorCoordinates;
	}

	@Nullable
	public OutdoorCoordinatesEntity getOutdoorCoordinates() {
		return outdoorCoordinates;
	}

	public void setOutdoorCoordinates(@Nullable final OutdoorCoordinatesEntity outdoorCoordinates) {
		this.outdoorCoordinates = outdoorCoordinates;
	}
}
