package de.uniluebeck.itm.tr.common.dto;

import javax.annotation.Nullable;

public class CoordinateDto {

	@Nullable
	private IndoorCoordinatesDto indoorCoordinates;

	@Nullable
	private OutdoorCoordinatesDto outdoorCoordinates;

	@Nullable
	public IndoorCoordinatesDto getIndoorCoordinates() {
		return indoorCoordinates;
	}

	public void setIndoorCoordinates(@Nullable final IndoorCoordinatesDto indoorCoordinates) {
		this.indoorCoordinates = indoorCoordinates;
	}

	@Nullable
	public OutdoorCoordinatesDto getOutdoorCoordinates() {
		return outdoorCoordinates;
	}

	public void setOutdoorCoordinates(@Nullable final OutdoorCoordinatesDto outdoorCoordinates) {
		this.outdoorCoordinates = outdoorCoordinates;
	}
}
