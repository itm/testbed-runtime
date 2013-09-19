package de.uniluebeck.itm.tr.common.dto;

import javax.annotation.Nullable;

public class CoordinateDto {

	@Nullable
	private IndoorCoordinatesDto indoorCoordinate;

	@Nullable
	private OutdoorCoordinatesDto outdoorCoordinate;

	@Nullable
	public IndoorCoordinatesDto getIndoorCoordinate() {
		return indoorCoordinate;
	}

	public void setIndoorCoordinate(@Nullable final IndoorCoordinatesDto indoorCoordinate) {
		this.indoorCoordinate = indoorCoordinate;
	}

	@Nullable
	public OutdoorCoordinatesDto getOutdoorCoordinate() {
		return outdoorCoordinate;
	}

	public void setOutdoorCoordinate(@Nullable final OutdoorCoordinatesDto outdoorCoordinate) {
		this.outdoorCoordinate = outdoorCoordinate;
	}
}
